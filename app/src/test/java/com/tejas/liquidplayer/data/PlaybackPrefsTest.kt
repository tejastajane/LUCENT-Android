package com.tejas.liquidplayer.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackPrefsTest {
    @Test fun playbackPositionUsesStableUriIdentifier() {
        val uri = "content://media/external/video/media/42"
        assertEquals("playback_position_" + mediaPreferenceId(uri), PlaybackPrefs.playbackPositionKey(uri))
        assertEquals("playback_position_" + uri.hashCode(), PlaybackPrefs.legacyPlaybackPositionKey(uri))
    }

    @Test fun migrationCopiesLegacyValuesAcrossSupportedPreferenceTypes() {
        val uri = "content://media/external/video/media/42"
        val suffix = uri.hashCode().toString()
        val existing = mapOf(
            "playback_position_" + suffix to 120_000L,
            "favourite_" + suffix to true,
            "audio_track_id_" + suffix to 7,
            "audio_track_label_" + suffix to "English",
            "bookmarks_" + suffix to setOf("30000", "90000")
        )
        val migrated = PlaybackPrefs.migrationEntries(existing, listOf(uri))
        val newSuffix = mediaPreferenceId(uri)
        assertEquals(120_000L, migrated["playback_position_" + newSuffix])
        assertEquals(true, migrated["favourite_" + newSuffix])
        assertEquals(7, migrated["audio_track_id_" + newSuffix])
        assertEquals("English", migrated["audio_track_label_" + newSuffix])
        assertEquals(setOf("30000", "90000"), migrated["bookmarks_" + newSuffix])
    }

    @Test fun existingShaValuesAreNeverOverwrittenAndMigrationIsIdempotent() {
        val uri = "content://media/external/video/media/99"
        val current = PlaybackPrefs.playbackPositionKey(uri)
        val legacy = PlaybackPrefs.legacyPlaybackPositionKey(uri)
        val existing = mapOf(current to 500L, legacy to 100L)
        val first = PlaybackPrefs.migrationEntries(existing, listOf(uri))
        val second = PlaybackPrefs.migrationEntries(existing + first, listOf(uri))
        assertFalse(first.containsKey(current))
        assertTrue(second.isEmpty())
    }

    @Test fun migrationDoesNotInventValuesForUnseenUris() {
        val uri = "content://media/external/video/media/100"
        assertTrue(PlaybackPrefs.migrationEntries(emptyMap<String, Any>(), listOf(uri)).isEmpty())
    }
}
