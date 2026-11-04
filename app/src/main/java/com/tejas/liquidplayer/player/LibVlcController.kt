package com.tejas.liquidplayer.player

import android.content.Context
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import com.tejas.liquidplayer.data.TrackOption
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer
import org.videolan.libvlc.util.VLCVideoLayout

/**
 * Owns the libVLC native engine. Provider URIs are opened by Android and passed as
 * file descriptors, rather than asking VLC's location parser to read content:// itself.
 */
class LibVlcController(context: Context, hardwareAcceleration: Boolean = true) {
    private val appContext = context.applicationContext
    private val libVlc = LibVLC(
        appContext,
        arrayListOf(
            "--audio-time-stretch",
            "--no-drop-late-frames",
            "--no-skip-frames",
            if (hardwareAcceleration) "--avcodec-hw=any" else "--avcodec-hw=none"
        )
    )
    val player = MediaPlayer(libVlc)
    private var viewsAttached = false
    private var released = false
    private var contentDescriptor: ParcelFileDescriptor? = null
    private var cachedPlaybackFile: File? = null

    private companion object {
        const val MAX_PROVIDER_CACHE_BYTES = 4L * 1024L * 1024L * 1024L
        const val MIN_FREE_SPACE_RESERVE_BYTES = 256L * 1024L * 1024L
        const val PROGRESS_UPDATE_INTERVAL_BYTES = 1024L * 1024L
    }

    fun attach(layout: VLCVideoLayout) {
        if (!viewsAttached && !released) {
            player.attachViews(layout, null, true, true)
            viewsAttached = true
        }
    }

    /**
     * Resolve and attach a media source without starting playback. This lets the UI prepare
     * the video even when another app temporarily owns audio focus.
     */
    fun prepare(uri: Uri): String? {
        if (released) return "The player has already been closed."
        return try {
            runCatching { player.stop() }
            // Keep libVLC at unity gain; the Android media stream owns user volume.
            runCatching { player.volume = 100 }
            contentDescriptor?.close()
            contentDescriptor = null
            cachedPlaybackFile?.delete()
            cachedPlaybackFile = null
            val media = when (uri.scheme?.lowercase()) {
                "content" -> {
                    val descriptor = appContext.contentResolver.openFileDescriptor(uri, "r")
                        ?: error("Android returned no file descriptor for this video.")
                    contentDescriptor = descriptor
                    Media(libVlc, descriptor.fileDescriptor)
                }
                "file" -> Media(libVlc, uri.path ?: error("The video file path is empty."))
                else -> Media(libVlc, uri)
            }
            try {
                player.setMedia(media)
            } finally {
                media.release()
            }
            null
        } catch (error: Exception) {
            runCatching { contentDescriptor?.close() }
            contentDescriptor = null
            Log.e("LiquidPlayer/VLC", "Unable to prepare media: $uri", error)
            "Couldn't open this video: ${error.message ?: "Android could not provide readable media."}"
        }
    }

    /** Start the media source previously attached by [prepare]. */
    fun start(): String? {
        if (released) return "The player has already been closed."
        return try {
            player.play()
            null
        } catch (error: Exception) {
            Log.e("LiquidPlayer/VLC", "Unable to start prepared media", error)
            "Couldn't start this video: ${error.message ?: "libVLC could not start playback."}"
        }
    }

    /** Convenience path for callers that want to prepare and immediately start. */
    fun play(uri: Uri): String? = prepare(uri) ?: start()

    /**
     * Retry a content-provider video from a seekable local cache file. Some Android document
     * providers expose a pipe or descriptor that libVLC cannot seek correctly; the copy is
     * created only after the direct-descriptor path fails, and is deleted when playback ends.
     */
    suspend fun retryUsingCachedCopy(
        uri: Uri,
        onProgress: (copiedBytes: Long, expectedBytes: Long?) -> Unit = { _, _ -> }
    ): String? {
        if (released) return "The player has already been closed."
        if (uri.scheme != "content") return "This video provider does not support a local-cache retry."

        val cached = try {
            withContext(Dispatchers.IO) {
                val metadata = runCatching {
                    appContext.contentResolver.query(
                        uri,
                        arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE),
                        null,
                        null,
                        null
                    )?.use { cursor ->
                        if (!cursor.moveToFirst()) null else {
                            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                            val name = if (nameIndex >= 0) cursor.getString(nameIndex).orEmpty() else ""
                            val size = if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) {
                                cursor.getLong(sizeIndex).takeIf { it > 0L }
                            } else null
                            name to size
                        }
                    }
                }.getOrNull()
                val displayName = metadata?.first.orEmpty()
                val expectedBytes = metadata?.second

                // Leave 256 MiB for the OS and app state, and never let a provider copy
                // consume an unbounded amount of cache when size metadata is missing.
                val freeSpace = appContext.cacheDir.usableSpace
                val safeCapacity = minOf(
                    MAX_PROVIDER_CACHE_BYTES,
                    (freeSpace - MIN_FREE_SPACE_RESERVE_BYTES).coerceAtLeast(0L)
                )
                if (safeCapacity <= 0L) {
                    error("There isn't enough free storage to safely retry this provider video.")
                }
                if (expectedBytes != null && expectedBytes > safeCapacity) {
                    error("This provider video is too large to cache safely with the available free storage.")
                }
                val copyLimit = expectedBytes ?: safeCapacity
                val extension = displayName.substringAfterLast('.', "")
                    .takeIf { it.length in 2..5 && it.all(Char::isLetterOrDigit) }
                val file = File.createTempFile(
                    "lucent-vlc-retry-",
                    extension?.let { ".$it" } ?: ".media",
                    appContext.cacheDir
                )
                var completed = false
                try {
                    val input = appContext.contentResolver.openInputStream(uri)
                        ?: error("Android could not open this video as a readable stream.")
                    var copiedBytes = 0L
                    var nextProgressBytes = 0L
                    input.use { stream ->
                        file.outputStream().buffered().use { output ->
                            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                            while (true) {
                                currentCoroutineContext().ensureActive()
                                val read = stream.read(buffer)
                                if (read < 0) break
                                if (copiedBytes + read > copyLimit) {
                                    error("The provider stream exceeded its reported size or safe cache limit.")
                                }
                                output.write(buffer, 0, read)
                                copiedBytes += read
                                if (copiedBytes >= nextProgressBytes) {
                                    withContext(Dispatchers.Main.immediate) {
                                        onProgress(copiedBytes, expectedBytes)
                                    }
                                    nextProgressBytes = copiedBytes + PROGRESS_UPDATE_INTERVAL_BYTES
                                }
                            }
                        }
                    }
                    if (copiedBytes == 0L) error("The selected video is empty.")
                    if (expectedBytes != null && copiedBytes != expectedBytes) {
                        error("The provider returned an incomplete video stream.")
                    }
                    withContext(Dispatchers.Main.immediate) {
                        onProgress(copiedBytes, expectedBytes)
                    }
                    completed = true
                    file
                } finally {
                    if (!completed) file.delete()
                }
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            Log.e("LiquidPlayer/VLC", "Unable to cache provider video: $uri", error)
            return "Couldn't read this video from Android storage: ${error.message ?: "unknown provider error"}"
        }

        return withContext(Dispatchers.Main.immediate) {
            if (released) {
                cached.delete()
                return@withContext "The player has already been closed."
            }
            try {
                runCatching { player.stop() }
                contentDescriptor?.close()
                contentDescriptor = null
                cachedPlaybackFile?.delete()
                cachedPlaybackFile = cached
                Log.i("LiquidPlayer/VLC", "Retrying playback from a bounded local cache copy")
                val retryMedia = Media(libVlc, cached.absolutePath)
                try {
                    player.play(retryMedia)
                } finally {
                    retryMedia.release()
                }
                null
            } catch (error: Exception) {
                cached.delete()
                cachedPlaybackFile = null
                Log.e("LiquidPlayer/VLC", "Local cache playback retry failed", error)
                "Local playback retry failed: ${error.message ?: "libVLC could not open the cached file."}"
            }
        }
    }

    fun togglePlayPause() {
        if (released) return
        if (player.isPlaying) player.pause() else player.play()
    }

    fun seekBy(deltaMs: Long) {
        if (released) return
        val length = player.length
        val target = (player.time + deltaMs).coerceAtLeast(0L)
        player.time = if (length > 0L) target.coerceAtMost(length) else target
    }

    fun seekTo(positionMs: Long) {
        if (released) return
        player.time = positionMs.coerceAtLeast(0L)
    }

    fun setAspectRatio(ratio: String?) {
        if (!released) player.setAspectRatio(ratio)
    }

    fun audioTracks(): List<TrackOption> = runCatching {
        player.audioTracks?.map { TrackOption(it.id, it.name ?: "Audio track") }.orEmpty()
    }.getOrDefault(emptyList())

    fun subtitleTracks(): List<TrackOption> = runCatching {
        player.spuTracks?.map { TrackOption(it.id, it.name ?: "Subtitle track") }.orEmpty()
    }.getOrDefault(emptyList())

    fun currentAudioTrackId(): Int = runCatching { player.audioTrack }.getOrDefault(-1)

    fun currentSubtitleTrackId(): Int = runCatching { player.spuTrack }.getOrDefault(-1)
    fun currentSubtitleDelayUs(): Long = runCatching { player.spuDelay }.getOrDefault(0L)
    fun setSubtitleDelayUs(delayUs: Long): Boolean {
        if (released) return false
        return runCatching { player.setSpuDelay(delayUs); true }.getOrDefault(false)
    }

    fun selectAudioTrack(id: Int): Boolean =
        !released && runCatching { player.setAudioTrack(id) }.getOrDefault(false)

    fun selectSubtitleTrack(id: Int): Boolean =
        !released && runCatching { player.setSpuTrack(id) }.getOrDefault(false)

    fun addExternalSubtitle(uri: Uri): Boolean =
        !released && runCatching { player.addSlave(0, uri, true) }.getOrDefault(false)

    fun close() {
        if (released) return
        released = true
        runCatching { player.stop() }
        if (viewsAttached) runCatching { player.detachViews() }
        viewsAttached = false
        runCatching { player.release() }
        runCatching { contentDescriptor?.close() }
        contentDescriptor = null
        runCatching { cachedPlaybackFile?.delete() }
        cachedPlaybackFile = null
        runCatching { libVlc.release() }
    }
}
