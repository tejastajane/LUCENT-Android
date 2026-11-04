package com.tejas.liquidplayer.data

import java.util.Locale

internal enum class MediaCategory(val storageValue: String) {
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
 * Local, title-independent classification. It uses generic episode numbering and sibling
 * sequence structure only; it never searches for show names or category words in file paths.
 * Anime is intentionally left to an explicit user override because filenames alone cannot
 * reliably distinguish anime from other episodic video.
 */
internal object MediaClassifier {
    const val MIN_MOVIE_DURATION_MS = 90L * 60L * 1_000L

    private data class NumberedSibling(
        val folderIdentity: String,
        val normalizedStem: String,
        val episodeNumber: Int
    )

    private val seasonEpisodePattern = Regex(
        """(?i)\\bS(\\d{1,2})[ ._-]*E(\\d{1,3})\\b"""
    )
    private val seasonXEpisodePattern = Regex(
        """(?i)(?<!\\d)(\\d{1,2})\\s*[xX]\\s*(\\d{1,3})(?!\\d)"""
    )
    private val seasonWordEpisodePattern = Regex(
        """(?i)\\bseason\\s*(\\d{1,2})\\s*episode\\s*(\\d{1,3})\\b"""
    )
    private val episodeWordPattern = Regex(
        """(?i)\\b(?:episode|ep)\\s*[._ #:-]*(\\d{1,3})\\b"""
    )
    private val compactEpisodePattern = Regex(
        """(?i)\\b(\\d{1,2})e(\\d{1,3})\\b"""
    )
    private val annotationsPattern = Regex(
        """\\[[^\\]]*\\]|\\([^)]*\\)|\\{[^}]*\\}"""
    )
    private val resolutionPattern = Regex(
        """(?i)\\b\\d{3,4}p\\b"""
    )
    private val numericSuffixPattern = Regex(
        """(?:^|[ ._-])(\\d{1,4})(?:v\\d+)?$"""
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
            val category = when {
                hasExplicitEpisodeNumber(item.name) || belongsToSeries -> MediaCategory.TvShow
                item.durationMs >= MIN_MOVIE_DURATION_MS -> MediaCategory.Movie
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
