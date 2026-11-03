package com.tejas.liquidplayer.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoItemTest {
    @Test
    fun normalizeRelativePathRemovesWhitespaceAndBoundarySlashes() {
        assertEquals("Movies/Anime/Season 1", normalizeRelativePath(" /Movies/Anime/Season 1/ "))
    }

    @Test
    fun normalizeRelativePathPreservesDistinctParentsWithSameLeafName() {
        val moviesPath = normalizeRelativePath("Movies/Anime/Season 1/")
        val downloadsPath = normalizeRelativePath("Downloads/Anime/Season 1/")

        assertNotEquals(moviesPath, downloadsPath)
        assertEquals("Season 1", moviesPath.substringAfterLast('/'))
        assertEquals("Season 1", downloadsPath.substringAfterLast('/'))
    }

    @Test
    fun naturalComparisonOrdersEpisodeNumbersNumerically() {
        assertTrue(compareNaturalNames("Episode 2.mkv", "Episode 10.mkv") < 0)
        assertTrue(compareNaturalNames("Episode 10.mkv", "Episode 2.mkv") > 0)
    }

    @Test
    fun naturalComparisonIsCaseInsensitiveButDeterministic() {
        assertTrue(compareNaturalNames("episode 2.mkv", "Episode 10.mkv") < 0)
        assertEquals(0, compareNaturalNames("Episode 2.mkv", "Episode 2.mkv"))
    }

    @Test
    fun legacyRelativePathUsesParentDirectoryAndRemovesPrimaryStoragePrefix() {
        assertEquals(
            "Movies/Anime/Season 1/",
            legacyRelativePath(
                "/storage/emulated/0/Movies/Anime/Season 1/video.mkv",
                "/storage/emulated/0"
            )
        )
    }

    @Test
    fun legacyRelativePathKeepsSecondaryVolumeDistinct() {
        assertEquals(
            "storage/1234-5678/DCIM/Camera/",
            legacyRelativePath(
                "/storage/1234-5678/DCIM/Camera/video.mp4",
                "/storage/emulated/0"
            )
        )
    }

    @Test
    fun mediaPreferenceIdAvoidsKnownStringHashCollisions() {
        val firstUri = "content://media.example/Aa"
        val secondUri = "content://media.example/BB"

        assertEquals(firstUri.hashCode(), secondUri.hashCode())
        assertEquals(64, mediaPreferenceId(firstUri).length)
        assertNotEquals(mediaPreferenceId(firstUri), mediaPreferenceId(secondUri))
        assertEquals(mediaPreferenceId(firstUri), mediaPreferenceId(firstUri))
    }
}
