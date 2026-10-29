package com.tejas.liquidplayer.debug

import android.app.Activity
import android.os.Bundle
import android.view.ViewGroup
import org.videolan.libvlc.util.VLCVideoLayout

/**
 * Debug-only Activity used by the emulator smoke test to give libVLC a real window/surface.
 */
class PlaybackHostActivity : Activity() {
    lateinit var videoLayout: VLCVideoLayout
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        videoLayout = VLCVideoLayout(this)
        setContentView(
            videoLayout,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )
    }
}
