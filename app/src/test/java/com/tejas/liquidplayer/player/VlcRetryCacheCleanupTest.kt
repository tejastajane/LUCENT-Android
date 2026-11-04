package com.tejas.liquidplayer.player

import java.io.File
import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VlcRetryCacheCleanupTest {
    @Test fun removesOnlyOrphanRetryFilesAndLeavesOtherCacheData() {
        val cache = Files.createTempDirectory("lucent-cache-test").toFile()
        try {
            val retry = File(cache, "lucent-vlc-retry-abcd1234.mkv").apply { writeText("stale") }
            val unrelated = File(cache, "thumbnail.jpg").apply { writeText("keep") }
            val removed = cleanupOrphanedVlcRetryCache(cache)
            assertEquals(1, removed)
            assertFalse(retry.exists())
            assertTrue(unrelated.exists())
        } finally {
            cache.deleteRecursively()
        }
    }

    @Test fun missingCacheDirectoryIsHarmless() {
        val missing = File(Files.createTempDirectory("lucent-parent").toFile(), "missing")
        assertEquals(0, cleanupOrphanedVlcRetryCache(missing))
        missing.parentFile?.deleteRecursively()
    }
}
