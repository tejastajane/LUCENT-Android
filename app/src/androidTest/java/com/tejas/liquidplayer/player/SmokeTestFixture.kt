package com.tejas.liquidplayer.player

import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File

/**
 * Loads the generated MP4 from the instrumentation APK into the target app's private cache.
 * This keeps fixture setup independent of a pre-installed app or adb run-as shell copy.
 */
internal object SmokeTestFixture {
    private const val FIXTURE_NAME = "liquidplayer-smoke.mp4"

    fun copyIntoAppCache(targetContext: Context): File {
        val destination = File(targetContext.cacheDir, FIXTURE_NAME)
        InstrumentationRegistry.getInstrumentation().context.assets
            .open(FIXTURE_NAME)
            .use { input ->
                destination.outputStream().use { output -> input.copyTo(output) }
            }

        check(destination.exists() && destination.length() > 0L) {
            "Instrumentation APK is missing $FIXTURE_NAME"
        }
        return destination
    }
}
