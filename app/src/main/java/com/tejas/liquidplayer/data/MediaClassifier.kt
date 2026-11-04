package com.tejas.liquidplayer.data

import java.util.Locale

enum class MediaCategory(val storageValue: String) {
    Movie("movie"),
    TvShow("tv_show"),
    Anime("anime"),
    Other("other");

    companion object {
        fun fromStorageValue(value: String?): MediaCategory? =
            entries.firstOrNull { it.storageValue == value }
    }
}

internal data class MediaClassifierInput(
    val uri: String,
    val name: String,
    val folderIdentity: String,
    val durationMs: Long
)

/**
 * Local, title-independent classification. Runtime is a hard lower bound for Anime and TV Shows.
 * Episode numbering and sibling sequences only disambiguate long-form content (90 minutes or
 * longer) from movies. No show names or category words are searched in file paths.
 */
internal object MediaClassifier {
    const val MIN_ANIME_DURATION_MS = 21L * 60L * 1_000L
    const val MIN_TV_SHOW_DURATION_MS = 29L * 60L * 1_000L
    const val MIN_MOVIE_DURATION_MS = 90L * 60L * 1_000L

    private data class NumberedSibling(
        val folderIdentity: String,
        val normalizedStem: String,
        val episodeNumber: Int
    )

    private val seasonEpisodePattern = Regex(
        """(?i)\bS(\d{1,2})[ ._-]*E(\d{1,3})\b"""
    )
    private val seasonXEpisodePattern = Regex(
        """(?i)(?<!\d)(\d{1,2})\s*[xX]\s*(\d{1,3})(?!\d)"""
    )
    private val seasonWordEpisodePattern = Regex(
        """(?i)\bseason\s*(\d{1,2})\s*episode\s*(\d{1,3})\b"""
    )
    private val episodeWordPattern = Regex(
        """(?i)\b(?:episode|ep)\s*[._ #:-]*(\d{1,3})\b"""
    )
    private val compactEpisodePattern = Regex(
        """(?i)\b(\d{1,2})e(\d{1,3})\b"""
    )
    private val annotationsPattern = Regex(
        """\[[^\]]*\]|\([^)]*\)|\{[^}]*\}"""
    )
    private val resolutionPattern = Regex(
        """(?i)\b\d{3,4}p\b"""
    )
    private val numericSuffixPattern = Regex(
        """(?:^|[ ._-])(\d{1,4})(?:v\d+)?$"""
    )
    private val separatorsPattern = Regex("[._-]+")
    private val whitespacePattern = Regex("\\s+")

    fun classify(items: List<MediaClassifierInput>): Map<String, MediaCategory> {
        val numberedSiblings = items.mapNotNull(::numberedSibling)
        val seriesGroups = numberedSiblings
            .groupBy { it.folderIdentity to it.normalizedStem }
            .filterValues { siblings -> siblings.map(NumberedSibling::episodeNumber).distinct().size >= 3 }
            .keys

        return items.associate { item ->
            val numbered = numberedSibling(item)
            val belongsToSeries = numbered != null &&
                (numbered.folderIdentity to numbered.normalizedStem) in seriesGroups
            val isAnimeRuntime = item.durationMs >= MIN_ANIME_DURATION_MS &&
                item.durationMs < MIN_TV_SHOW_DURATION_MS
            val hasEpisodeStructure = hasExplicitEpisodeNumber(item.name) || belongsToSeries
            val category = when {
                // Runtime boundaries are strict: no clip shorter than 29 minutes is a TV Show.
                isAnimeRuntime -> MediaCategory.Anime
                item.durationMs >= MIN_MOVIE_DURATION_MS && !hasEpisodeStructure -> MediaCategory.Movie
                item.durationMs >= MIN_TV_SHOW_DURATION_MS -> MediaCategory.TvShow
                else -> MediaCategory.Other
            }
            item.uri to category
        }
    }

    private fun hasExplicitEpisodeNumber(name: String): Boolean {
        val stem = name.substringBeforeLast('.', name)
        return hasValidSeasonEpisode(seasonEpisodePattern, stem) ||
            hasValidSeasonEpisode(seasonXEpisodePattern, stem) ||
            hasValidSeasonEpisode(seasonWordEpisodePattern, stem) ||
            episodeWordPattern.containsMatchIn(stem) ||
            hasValidSeasonEpisode(compactEpisodePattern, stem)
    }

    private fun hasValidSeasonEpisode(pattern: Regex, name: String): Boolean =
        pattern.findAll(name).any { match ->
            val season = match.groupValues[1].toIntOrNull()
            val episode = match.groupValues[2].toIntOrNull()
            season != null && season in 1..40 && episode != null && episode in 1..999
        }

    private fun numberedSibling(item: MediaClassifierInput): NumberedSibling? {
        val withoutExtension = item.name.substringBeforeLast('.', item.name)
        val withoutAnnotations = resolutionPattern.replace(annotationsPattern.replace(withoutExtension, " "), " ")
        val suffix = numericSuffixPattern.find(withoutAnnotations.trim()) ?: return null
        val episodeNumber = suffix.groupValues[1].toIntOrNull() ?: return null
        // Four-digit release years are not treated as episode sequence numbers.
        if (episodeNumber in 1900..2099) return null
        val stem = withoutAnnotations.substring(0, suffix.range.first)
            .let { separatorsPattern.replace(it, " ") }
            .let { whitespacePattern.replace(it, " ") }
            .trim()
            .lowercase(Locale.ROOT)
        if (stem.length < 2) return null
        return NumberedSibling(item.folderIdentity, stem, episodeNumber)
    }
}
