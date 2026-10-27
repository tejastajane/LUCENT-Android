package com.tejas.liquidplayer.player

import android.content.Context
import android.net.Uri
import com.tejas.liquidplayer.data.TrackOption
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer
import org.videolan.libvlc.util.VLCVideoLayout

/**
 * Owns the libVLC native engine and exposes a small UI-focused playback API.
 * All media URIs are content URIs or SAF URIs; no unsafe filesystem path conversion is used.
 */
class LibVlcController(context: Context, hardwareAcceleration: Boolean = true) {
    private val libVlc = LibVLC(
        context.applicationContext,
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

    fun attach(layout: VLCVideoLayout) {
        if (!viewsAttached && !released) {
            player.attachViews(layout, null, true, true)
            viewsAttached = true
        }
    }

    fun play(uri: Uri) {
        if (released) return
        val media = Media(libVlc, uri)
        try {
            player.media = media
            player.play()
        } finally {
            media.release()
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
        runCatching {
            player.addSlave(0, uri, true)
        }
    }

    fun close() {
        if (released) return
        released = true
        runCatching { player.stop() }
        if (viewsAttached) runCatching { player.detachViews() }
        viewsAttached = false
        runCatching { player.release() }
        runCatching { libVlc.release() }
    }
}
