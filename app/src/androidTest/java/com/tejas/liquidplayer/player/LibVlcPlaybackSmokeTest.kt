package com.tejas.liquidplayer.player

import android.net.Uri
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.tejas.liquidplayer.debug.PlaybackHostActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.videolan.libvlc.MediaPlayer
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

@RunWith(AndroidJUnit4::class)
class LibVlcPlaybackSmokeTest {
    @Test
    fun localVideoOpensAndPlaybackTimeAdvances() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val fixture = File(context.cacheDir, "liquidplayer-smoke.mp4")
        assertTrue("Smoke-test MP4 wasn't copied into app cache", fixture.exists() && fixture.length() > 0L)

        val scenario = ActivityScenario.launch(PlaybackHostActivity::class.java)
        var controller: LibVlcController? = null
        val playbackResult = AtomicInteger(0) // 1 = playing, -1 = decode/open error
        val playbackEvent = CountDownLatch(1)

        try {
            scenario.onActivity { activity ->
                val instance = LibVlcController(activity, hardwareAcceleration = false)
                controller = instance
                instance.attach(activity.videoLayout)
                instance.player.setEventListener(MediaPlayer.EventListener { event ->
                    when (event.type) {
                        MediaPlayer.Event.Playing -> {
                            playbackResult.set(1)
                            playbackEvent.countDown()
                        }
                        MediaPlayer.Event.EncounteredError -> {
                            playbackResult.set(-1)
                            playbackEvent.countDown()
                        }
                    }
                })
                instance.play(Uri.fromFile(fixture))
            }

            assertTrue(
                "libVLC did not emit Playing or EncounteredError within 25 seconds",
                playbackEvent.await(25, TimeUnit.SECONDS)
            )
            assertEquals("libVLC emitted a playback error for the generated MP4", 1, playbackResult.get())

            val player = requireNotNull(controller).player
            val firstTime = player.time
            Thread.sleep(900)
            val laterTime = player.time
            assertTrue("Video playback time did not advance ($firstTime -> $laterTime)", laterTime > firstTime)
        } finally {
            scenario.onActivity { controller?.close() }
            scenario.close()
        }
    }
}
