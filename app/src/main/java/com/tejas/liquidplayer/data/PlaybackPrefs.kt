package com.tejas.liquidplayer.data

import android.content.SharedPreferences

/**
 * Central key and migration policy for per-media playback preferences.
 * New entries use SHA-256 URI identifiers. Legacy hashCode keys remain readable;
 * migration copies values without deleting legacy entries.
 */
internal object PlaybackPrefs {
    private val mediaPreferencePrefixes = listOf(
        "playback_position_", "favourite_", "audio_track_id_",
        "audio_track_label_", "subtitle_track_id_", "subtitle_track_label_",
        "external_subtitle_uri_", "subtitle_delay_us_", "bookmarks_"
    )

    fun key(prefix: String, uriString: String): String = prefix + mediaPreferenceId(uriString)
    fun legacyKey(prefix: String, uriString: String): String = prefix + uriString.hashCode()

    fun playbackPositionKey(uriString: String): String = key("playback_position_", uriString)
    fun legacyPlaybackPositionKey(uriString: String): String = legacyKey("playback_position_", uriString)

    fun readPlaybackPosition(preferences: SharedPreferences, uriString: String, defaultValue: Long = 0L): Long {
        val currentKey = playbackPositionKey(uriString)
        if (preferences.contains(currentKey)) return preferences.getLong(currentKey, defaultValue)
        return preferences.getLong(legacyPlaybackPositionKey(uriString), defaultValue)
    }

    /** Pure migration planning kept separate from Android storage for unit testing. */
    internal fun migrationEntries(existing: Map<String, *>, uriStrings: Iterable<String>): Map<String, Any> {
        val migrated = linkedMapOf<String, Any>()
        uriStrings.forEach { uriString ->
            mediaPreferencePrefixes.forEach prefixLoop@ { prefix ->
                val currentKey = key(prefix, uriString)
                if (existing.containsKey(currentKey)) return@prefixLoop
                val oldValue = existing[legacyKey(prefix, uriString)] ?: return@prefixLoop
                migrated[currentKey] = oldValue
            }
        }
        return migrated
    }

    fun migrateLegacyPreferences(preferences: SharedPreferences, uriStrings: Iterable<String>) {
        val entries = migrationEntries(preferences.all, uriStrings)
        if (entries.isEmpty()) return
        val editor = preferences.edit()
        entries.forEach { (key, value) ->
            when (value) {
                is Long -> editor.putLong(key, value)
                is Int -> editor.putInt(key, value)
                is Boolean -> editor.putBoolean(key, value)
                is String -> editor.putString(key, value)
                is Set<*> -> editor.putStringSet(key, value.filterIsInstance<String>().toSet())
                else -> Unit
            }
        }
        editor.apply()
    }
}
