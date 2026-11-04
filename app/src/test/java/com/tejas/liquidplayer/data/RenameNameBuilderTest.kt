package com.tejas.liquidplayer.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RenameNameBuilderTest {
    @Test fun namesWithDotsKeepOriginalExtension() {
        val result = buildRenameFileName("Episode.mkv", "Mr. Robot S01E01")
        assertEquals("Mr. Robot S01E01.mkv", result.fileName)
        assertNull(result.error)
    }

    @Test fun existingOriginalExtensionIsNotDuplicatedCaseInsensitively() {
        assertEquals("Film.MKV", buildRenameFileName("Film.mkv", "Film.MKV").fileName)
    }

    @Test fun aDifferentExtensionDoesNotReplaceOriginalExtension() {
        assertEquals("Film.mp4.mkv", buildRenameFileName("Film.mkv", "Film.mp4").fileName)
    }

    @Test fun rejectsEmptyNamesAndPathSeparatorsOrNulls() {
        assertEquals(RenameNameError.Empty, buildRenameFileName("Film.mkv", "   ").error)
        assertEquals(RenameNameError.InvalidCharacter, buildRenameFileName("Film.mkv", "../Film").error)
        assertEquals(RenameNameError.InvalidCharacter, buildRenameFileName("Film.mkv", "bad\u0000name").error)
    }

    @Test fun rejectsNamesOver255Utf8BytesAfterExtensionIsApplied() {
        val tooLong = "é".repeat(126)
        assertEquals(RenameNameError.TooLong, buildRenameFileName("Film.mkv", tooLong).error)
    }
}
