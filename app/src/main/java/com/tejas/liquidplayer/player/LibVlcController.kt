package com.tejas.liquidplayer.player

import android.content.Context
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import android.util.Log
import kotlinx.coroutines.Dispatchers
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

    fun attach(layout: VLCVideoLayout) {
        if (!viewsAttached && !released) {
            player.attachViews(layout, null, true, true)
            viewsAttached = true
        }
    }

    /** Returns null if playback was queued, or an error message for synchronous access failures. */
    fun play(uri: Uri): String? {
        if (released) return "The player has already been closed."
        return try {
            runCatching { player.stop() }
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
            player.play(media)
            null
        } catch (error: Exception) {
            runCatching { contentDescriptor?.close() }
            contentDescriptor = null
            Log.e("LiquidPlayer/VLC", "Unable to start media: $uri", error)
            "Couldn't open this video: ${error.message ?: "Android could not provide readable media."}"
        }
    }

    /**
     * Retry a content-provider video from a seekable local cache file. Some Android document
     * providers expose a pipe or descriptor that libVLC cannot seek correctly; the copy is
     * created only after the direct-descriptor path fails, and is deleted when playback ends.
     */
    suspend fun retryUsingCachedCopy(uri: Uri): String? {
        if (released) return "The player has already been closed."
        if (uri.scheme != "content") return "This video provider does not support a local-cache retry."

        val cached = try {
            withContext(Dispatchers.IO) {
                val displayName = runCatching {
                    appContext.contentResolver.query(
                        uri,
                        arrayOf(OpenableColumns.DISPLAY_NAME),
                        null,
                        null,
                        null
                    )?.use { cursor ->
                        if (cursor.moveToFirst()) cursor.getString(0) else null
                    }
                }.getOrNull().orEmpty()
                val extension = displayName.substringAfterLast('.', "")
                    .takeIf { it.length in 2..5 && it.all(Char::isLetterOrDigit) }
                val file = File.createTempFile(
                    "liquid-vlc-retry-",
                    extension?.let { ".$it" } ?: ".media",
                    appContext.cacheDir
                )
                try {
                    val input = appContext.contentResolver.openInputStream(uri)
                        ?: error("Android could not open this video as a readable stream.")
                    input.use { stream ->
                        file.outputStream().buffered().use { output -> stream.copyTo(output) }
                    }
                    if (file.length() == 0L) error("The selected video is empty.")
                    file
                } catch (error: Exception) {
                    file.delete()
                    throw error
                }
            }
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
                Log.i("LiquidPlayer/VLC", "Retrying playback from a local cache copy")
                player.play(Media(libVlc, cached.absolutePath))
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
