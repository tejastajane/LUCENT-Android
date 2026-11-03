package com.tejas.liquidplayer.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerGestureMathTest {
    @Test
    fun swipeRightMovesForwardAndSwipeLeftMovesBackward() {
        val forward = horizontalSeekTargetMs(60_000L, 600_000L, 80f, 400f)
        val backward = horizontalSeekTargetMs(60_000L, 600_000L, -80f, 400f)

        assertTrue(forward > 60_000L)
        assertTrue(backward < 60_000L)
        assertEquals(120_000L, forward + backward)
    }

    @Test
    fun seekClampsAtStartAndEnd() {
        assertEquals(0L, horizontalSeekTargetMs(2_000L, 600_000L, -4_000f, 400f))
        assertEquals(600_000L, horizontalSeekTargetMs(598_000L, 600_000L, 4_000f, 400f))
    }

    @Test
    fun shortPrecisionSwipesHaveLowerGainThanLongDrags() {
        val shortTarget = horizontalSeekTargetMs(100_000L, 7_200_000L, 40f, 400f)
        val longTarget = horizontalSeekTargetMs(100_000L, 7_200_000L, 400f, 400f)

        assertTrue(shortTarget - 100_000L < longTarget - 100_000L)
        assertTrue(shortTarget - 100_000L < 120_000L)
    }

    @Test
    fun invalidWidthAndUnknownDurationDoNotCreateASeek() {
        assertEquals(90_000L, horizontalSeekTargetMs(90_000L, 0L, 200f, 400f))
        assertEquals(90_000L, horizontalSeekTargetMs(90_000L, 500_000L, 200f, 0f))
    }
}

class PlayerGestureAxisTest {
    @Test
    fun horizontalDominantMovementSelectsSeek() {
        assertEquals(
            PlayerDragAxis.Horizontal,
            resolvePlayerDragAxis(80f, 20f)
        )
    }

    @Test
    fun verticalDominantMovementSelectsBrightnessOrVolume() {
        assertEquals(
            PlayerDragAxis.Vertical,
            resolvePlayerDragAxis(20f, 80f)
        )
    }

    @Test
    fun ambiguousDiagonalMovementStaysUndecided() {
        assertEquals(null, resolvePlayerDragAxis(40f, 40f))
        assertEquals(null, resolvePlayerDragAxis(46f, 40f))
        assertEquals(null, resolvePlayerDragAxis(40f, 46f))
    }

    @Test
    fun clearDominanceResolvesAfterAnAmbiguousStart() {
        assertEquals(PlayerDragAxis.Horizontal, resolvePlayerDragAxis(70f, 40f))
        assertEquals(PlayerDragAxis.Vertical, resolvePlayerDragAxis(40f, 70f))
    }
}
