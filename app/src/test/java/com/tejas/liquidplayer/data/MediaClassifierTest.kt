package com.tejas.liquidplayer.data

import org.junit.Assert.assertEquals
import org.junit.Test

class MediaClassifierTest {
    private fun item(
        uri: String,
        name: String,
        folder: String = "volume|downloads",
        duration: Long = 0L
    ) = MediaClassifierInput(uri, name, folder, duration)

    @Test fun recognizesGenericEpisodeNumberFormatsForLongEpisodes() {
        val longEpisode = MediaClassifier.MIN_MOVIE_DURATION_MS + 1L
        val result = MediaClassifier.classify(listOf(
            item("1", "Show.S02E05.mkv", duration = longEpisode),
            item("2", "Show 1x03.mkv", duration = longEpisode),
            item("3", "Season 2 Episode 4.mp4", duration = longEpisode),
            item("4", "Episode 12.mkv", duration = longEpisode)
        ))
        assertEquals(MediaCategory.TvShow, result["1"])
        assertEquals(MediaCategory.TvShow, result["2"])
        assertEquals(MediaCategory.TvShow, result["3"])
        assertEquals(MediaCategory.TvShow, result["4"])
    }

    @Test fun genericSequentialSiblingsDisambiguateLongEpisodesFromMovies() {
        val longEpisode = MediaClassifier.MIN_MOVIE_DURATION_MS + 1L
        val result = MediaClassifier.classify(listOf(
            item("1", "[Group] Project North - 001 (1080p).mkv", duration = longEpisode),
            item("2", "[Group] Project North - 002 (1080p).mkv", duration = longEpisode),
            item("3", "[Group] Project North - 003 (1080p).mkv", duration = longEpisode)
        ))
        assertEquals(setOf(MediaCategory.TvShow), result.values.toSet())
    }

    @Test fun shortCameraClipsNeverBecomeTvShowsFromNumberedSiblingNames() {
        val result = MediaClassifier.classify(listOf(
            item("1", "IMG_6297.MOV", folder = "volume|Quick Share", duration = 4_000L),
            item("2", "IMG_6350.MOV", folder = "volume|Quick Share", duration = 4_000L),
            item("3", "IMG_6351.MOV", folder = "volume|Quick Share", duration = 7_000L),
            item("4", "IMG_6456.MOV", folder = "volume|Quick Share", duration = 26_000L),
            item("5", "Show.S01E01.mkv", duration = 15L * 60L * 1_000L)
        ))
        assertEquals(setOf(MediaCategory.Other), result.values.toSet())
    }

    @Test fun oneNumberedFileDoesNotBecomeATvShowByItself() {
        val result = MediaClassifier.classify(listOf(item("1", "Project North - 001.mkv")))
        assertEquals(MediaCategory.Other, result["1"])
    }

    @Test fun resolutionDimensionsDoNotLookLikeEpisodeNumbers() {
        val result = MediaClassifier.classify(listOf(item("1", "Holiday 1920x1080.mp4")))
        assertEquals(MediaCategory.Other, result["1"])
    }

    @Test fun runtimeBandClassifiesAnimeFromTwentyOneMinutesUntilTwentyNineMinutes() {
        val result = MediaClassifier.classify(listOf(
            item("1", "Twenty One Minutes.mkv", duration = MediaClassifier.MIN_ANIME_DURATION_MS),
            item(
                "2",
                "Twenty Eight Fifty Nine.mkv",
                duration = MediaClassifier.MIN_TV_SHOW_DURATION_MS - 1L
            ),
            item("3", "Under Twenty One.mkv", duration = MediaClassifier.MIN_ANIME_DURATION_MS - 1L),
            item("4", "Twenty Nine Minutes.mkv", duration = MediaClassifier.MIN_TV_SHOW_DURATION_MS)
        ))
        assertEquals(MediaCategory.Anime, result["1"])
        assertEquals(MediaCategory.Anime, result["2"])
        assertEquals(MediaCategory.Other, result["3"])
        assertEquals(MediaCategory.TvShow, result["4"])
    }

    @Test fun animeRuntimeTakesPrecedenceOverEpisodeNumbering() {
        val result = MediaClassifier.classify(listOf(
            item("1", "Show.S01E01.mkv", duration = 22L * 60L * 1_000L)
        ))
        assertEquals(MediaCategory.Anime, result["1"])
    }

    @Test fun tvShowRuntimeStartsAtTwentyNineMinutesAndPreservesMovieThreshold() {
        val result = MediaClassifier.classify(listOf(
            item("1", "Twenty Nine Minutes.mkv", duration = MediaClassifier.MIN_TV_SHOW_DURATION_MS),
            item("2", "Eighty Nine Fifty Nine.mkv", duration = MediaClassifier.MIN_MOVIE_DURATION_MS - 1L),
            item("3", "Ninety Minute Feature.mkv", duration = MediaClassifier.MIN_MOVIE_DURATION_MS)
        ))
        assertEquals(MediaCategory.TvShow, result["1"])
        assertEquals(MediaCategory.TvShow, result["2"])
        assertEquals(MediaCategory.Movie, result["3"])
    }

    @Test fun noSpecificTitleOrFolderWordIsUsedAsAnAnimeTag() {
        val result = MediaClassifier.classify(listOf(
            item("1", "Naruto.mkv", folder = "volume|Anime"),
            item("2", "One Piece - special.mkv", folder = "volume|Anime")
        ))
        assertEquals(MediaCategory.Other, result["1"])
        assertEquals(MediaCategory.Other, result["2"])
    }
}
