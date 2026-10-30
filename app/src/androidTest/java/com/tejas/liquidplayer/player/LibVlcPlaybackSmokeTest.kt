package com.tejas.liquidplayer.player

import android.content.ContentValues
import android.provider.MediaStore
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.tejas.liquidplayer.debug.PlaybackHostActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.videolan.libvlc.MediaPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

@RunWith(AndroidJUnit4::class)
class LibVlcPlaybackSmokeTest {
    @Test
    fun mediaStoreContentUriOpensAndPlaybackTimeAdvances() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val fixture = SmokeTestFixture.copyIntoAppCache(context)

        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, "liquidplayer-smoke.mp4")
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/LiquidPlayerTest")
        }
        val contentUri = requireNotNull(
            context.contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
        ) { "Could not create a MediaStore test video" }

        val scenario = ActivityScenario.launch(PlaybackHostActivity::class.java)
        var controller: LibVlcController? = null
        val playbackResult = AtomicInteger(0)
        val playbackEvent = CountDownLatch(1)
        val retryAttempted = java.util.concurrent.atomic.AtomicBoolean(false)

        try {
            val outputStream = requireNotNull(
                context.contentResolver.openOutputStream(contentUri, "w")
            ) { "Could not write the MediaStore test video" }
            outputStream.use { output ->
                fixture.inputStream().use { input -> input.copyTo(output) }
            }
            scenario.onActivity { activity ->
                val instance = LibVlcController(activity, hardwareAcceleration = false)
                controller = instance
                instance.attach(activity.videoLayout)
                instance.player.setEventListener(MediaPlayer.EventListener { event ->
                    when (event.type) {
                        MediaPlayer.Event.Playing -> { playbackResult.set(1); playbackEvent.countDown() }
                        MediaPlayer.Event.EncounteredError -> {
                            if (retryAttempted.compareAndSet(false, true)) {
                                CoroutineScope(Dispatchers.IO).launch {
                                    val retryError = instance.retryUsingCachedCopy(contentUri)
                                    if (retryError != null) {
                                        playbackResult.set(-1)
                                        playbackEvent.countDown()
                                    }
                                }
                            } else {
                                playbackResult.set(-1)
                                playbackEvent.countDown()
                            }
                        }
                    }
                })
                val immediateError = instance.play(contentUri)
                if (immediateError != null) {
                    playbackResult.set(-1)
                    playbackEvent.countDown()
                    throw AssertionError(immediateError)
                }
            }

            assertTrue("libVLC did not emit Playing or EncounteredError within 25 seconds", playbackEvent.await(25, TimeUnit.SECONDS))
            assertEquals("libVLC emitted a playback error for the MediaStore Uri", 1, playbackResult.get())
            val player = requireNotNull(controller).player
            val firstTime = player.time
            Thread.sleep(900)
            assertTrue("Video playback time did not advance ($firstTime -> ${player.time})", player.time > firstTime)
        } finally {
            scenario.onActivity { controller?.close() }
            scenario.close()
            context.contentResolver.delete(contentUri, null, null)
        }
    }
}
