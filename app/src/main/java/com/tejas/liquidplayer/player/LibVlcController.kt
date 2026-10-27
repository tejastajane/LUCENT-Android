package com.tejas.liquidplayer.player

import android.content.Context
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.Log
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

    fun selectAudioTrack(id: Int) {
        if (!released) player.setAudioTrack(id)
    }

    fun selectSubtitleTrack(id: Int) {
        if (!released) player.setSpuTrack(id)
    }

    fun addExternalSubtitle(uri: Uri) {
        if (released) return
        runCatching { player.addSlave(0, uri, true) }
    }

    fun close() {
        if (released) return
        released = true
        runCatching { player.stop() }
        if (viewsAttached) runCatching { player.detachViews() }
        viewsAttached = false
        runCatching { player.release() }
        runCatching { contentDescriptor?.close() }
        contentDescriptor = null
        runCatching { libVlc.release() }
    }
}
