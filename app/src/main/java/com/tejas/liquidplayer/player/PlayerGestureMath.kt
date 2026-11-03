package com.tejas.liquidplayer.player

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToLong

internal data class HorizontalSeekPreview(
    val startPositionMs: Long,
    val targetPositionMs: Long
) {
    val deltaMs: Long
        get() = targetPositionMs - startPositionMs
}

/**
 * Converts a horizontal drag into a preview target.
 *
 * The first 35% of a screen-width uses a fine curve; movement beyond that uses
 * a coarser curve. This makes short precision swipes useful without making long
 * drags impractically slow. One screen-width maps to roughly 30-126 seconds,
 * depending on the duration of the current video.
 */
internal fun horizontalSeekTargetMs(
    startPositionMs: Long,
    durationMs: Long,
    dragDistancePx: Float,
    surfaceWidthPx: Float
): Long {
    if (durationMs <= 0L) return startPositionMs.coerceAtLeast(0L)
    val start = startPositionMs.coerceIn(0L, durationMs)
    if (surfaceWidthPx <= 0f || !surfaceWidthPx.isFinite() || !dragDistancePx.isFinite()) {
        return start
    }

    val timePerScreenMs = (durationMs / 60L).coerceIn(30_000L, 120_000L)
    val normalizedDistance = abs(dragDistancePx / surfaceWidthPx)
    val fineDistance = min(normalizedDistance, 0.35f)
    val coarseDistance = max(normalizedDistance - 0.35f, 0f)
    val curveDistance = fineDistance * 0.4f + coarseDistance * 1.4f
    val direction = if (dragDistancePx < 0f) -1f else 1f
    val delta = (timePerScreenMs * curveDistance * direction).roundToLong()

    return (start + delta).coerceIn(0L, durationMs)
}
