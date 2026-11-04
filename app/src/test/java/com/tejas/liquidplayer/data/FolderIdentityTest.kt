package com.tejas.liquidplayer.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class FolderIdentityTest {
    @Test
    fun sameRelativePathOnDifferentVolumesHasDifferentIdentity() {
        val internal = buildFolderIdentity("external_primary", "Movies/Anime/Season 1/")
        val removable = buildFolderIdentity("1234-5678", "Movies/Anime/Season 1/")

        assertNotEquals(internal, removable)
    }

    @Test
    fun sameVolumeAndPathHaveStableIdentity() {
        assertEquals(
            buildFolderIdentity("external_primary", "/Movies/Anime/Season 1/"),
            buildFolderIdentity("external_primary", "Movies/Anime/Season 1")
        )
    }

    @Test
    fun oldDevicesUseOneStableLegacyVolumeIdentity() {
        assertEquals(
            buildFolderIdentity(null, "Movies/"),
            buildFolderIdentity(" ", "/Movies/")
        )
    }
}
