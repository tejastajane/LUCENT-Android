package com.tejas.liquidplayer.player

import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit

/**
 * Exercises the real launcher Activity and its Compose navigation on the CI emulator.
 * This complements the lower-level libVLC test: it catches process crashes while
 * composing Settings and while the actual VideoPlayerScreen is being mounted.
 */
@RunWith(AndroidJUnit4::class)
class MainActivityUiSmokeTest {
    private val instrumentation by lazy { InstrumentationRegistry.getInstrumentation() }
    private val context by lazy { instrumentation.targetContext }
    private lateinit var device: UiDevice
    private var testVideoUri: Uri? = null

    @Before
    fun prepareDevice() {
        device = UiDevice.getInstance(instrumentation)
    }

    @After
    fun removeFixture() {
        runCatching { device.pressHome() }
        testVideoUri?.let { uri ->
            runCatching { context.contentResolver.delete(uri, null, null) }
        }
    }

    @Test
    fun settingsScreenComposesWithoutCrashing() {
        launchRealAppWithTestVideo()

        val settingsTab = device.wait(
            Until.findObject(By.desc("Settings")),
            TimeUnit.SECONDS.toMillis(20)
        )
        assertNotNull("Settings tab wasn't visible in the real app", settingsTab)
        settingsTab!!.click()

        val themeSetting = device.wait(
            Until.findObject(By.text("Dark theme")),
            TimeUnit.SECONDS.toMillis(20)
        )
        assertNotNull("Settings screen did not render its Appearance section", themeSetting)

        val accelerationSetting = device.wait(
            Until.findObject(By.text("Hardware acceleration")),
            TimeUnit.SECONDS.toMillis(10)
        )
        assertNotNull("Settings screen did not render Playback settings", accelerationSetting)
        device.waitForIdle(2_000L)
    }

    @Test
    fun selectingVideoOpensRealPlayerScreenWithoutCrashing() {
        launchRealAppWithTestVideo()

        // MainActivity launches with the Videos page selected. Use the visible media
        // row directly instead of depending on the selected tab's accessibility label.
        val videoRow = device.wait(
            Until.findObject(By.text(TEST_VIDEO_NAME)),
            TimeUnit.SECONDS.toMillis(25)
        )
        assertNotNull(
            "Test video wasn't listed by the real MediaStore library; check permission and scanning",
            videoRow
        )
        videoRow!!.click()

        val backButton = device.wait(
            Until.findObject(By.desc("Back")),
            TimeUnit.SECONDS.toMillis(25)
        )
        assertNotNull(
            "Tapping the video did not mount the actual VideoPlayerScreen",
            backButton
        )

        val toolsButton = device.wait(
            Until.findObject(By.desc("Player tools")),
            TimeUnit.SECONDS.toMillis(10)
        )
        assertNotNull("Player controls did not render after opening the video", toolsButton)

        // Leave the player mounted long enough to catch asynchronous init/composition crashes.
        device.waitForIdle(2_000L)
        Thread.sleep(2_000L)
        assertTrue(
            "Player Activity disappeared after opening the video",
            device.hasObject(By.desc("Back"))
        )
    }

    /**
     * Every test launches its own real MainActivity. No test depends on a prior screen,
     * selected tab, or successful accessibility lookup from another test.
     */
    private fun launchRealAppWithTestVideo() {
        // The CI emulator uses API 30; grant the legacy video-read permission before
        // MainActivity reads MediaStore so the fixture appears in the Videos tab.
        device.executeShellCommand(
            "pm grant ${context.packageName} android.permission.READ_EXTERNAL_STORAGE"
        )
        testVideoUri = insertFixtureIntoMediaStore()

        val launchIntent = requireNotNull(
            context.packageManager.getLaunchIntentForPackage(context.packageName)
        ) { "CI app package has no launcher Activity" }.apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        context.startActivity(launchIntent)
        // Let Activity startup settle. The test itself waits for the control it needs.
        Thread.sleep(1_500L)
    }

    private fun insertFixtureIntoMediaStore(): Uri {
        val fixture = SmokeTestFixture.copyIntoAppCache(context)

        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, TEST_VIDEO_NAME)
            put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
            put(MediaStore.Video.Media.DURATION, 8_000L)
            put(MediaStore.MediaColumns.DATE_ADDED, System.currentTimeMillis() / 1_000L)
            put(
                MediaStore.MediaColumns.RELATIVE_PATH,
                "${android.os.Environment.DIRECTORY_MOVIES}/LiquidPlayerUiSmoke"
            )
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val uri = requireNotNull(
            context.contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
        ) { "Could not create the real-app MediaStore UI test video" }

        try {
            val output = requireNotNull(context.contentResolver.openOutputStream(uri, "w"))
            output.use { stream ->
                fixture.inputStream().use { input -> input.copyTo(stream) }
            }
            val publish = ContentValues().apply {
                put(MediaStore.MediaColumns.IS_PENDING, 0)
            }
            context.contentResolver.update(uri, publish, null, null)
        } catch (failure: Throwable) {
            context.contentResolver.delete(uri, null, null)
            throw failure
        }
        return uri
    }

    private companion object {
        const val TEST_VIDEO_NAME = "LiquidPlayer UI crash smoke.mp4"
    }
}
