package com.tejas.liquidplayer.data

import java.security.MessageDigest

import android.net.Uri
import java.util.Locale

data class VideoItem(
    val id: Long,
    val uri: Uri,
    val name: String,
    val relativePath: String,
    val durationMs: Long,
    val sizeBytes: Long,
    val dateAddedSeconds: Long,
    val mimeType: String? = null
) {
    /** Normalized MediaStore path used as folder identity, not merely its display name. */
    val folderPath: String
        get() = normalizeRelativePath(relativePath)

    /** Friendly leaf label; use [folderPath] for identity and filtering. */
    val folderName: String
        get() = folderPath.substringAfterLast('/').ifBlank { "Internal storage" }
}

data class TrackOption(val id: Int, val label: String)


private val naturalSortTokens = Regex("""\d+|\D+""")

/**
 * Sorts media names in human/numeric order (Episode 2 before Episode 10).
 */
fun sortVideosAscending(items: List<VideoItem>): List<VideoItem> =
    items.sortedWith { first, second ->
        // Group files by their show/season folder first, then order episodes by filename.
        val folderOrder = compareNaturalNames(first.relativePath, second.relativePath)
        if (folderOrder != 0) folderOrder
        else compareNaturalNames(first.name, second.name)
    }

internal fun normalizeRelativePath(path: String): String = path.trim().trim('/')

private const val RESUME_END_THRESHOLD_MS = 1_500L

internal fun isResumablePlaybackPosition(savedPositionMs: Long, durationMs: Long): Boolean =
    savedPositionMs > RESUME_END_THRESHOLD_MS &&
        (durationMs <= 0L || savedPositionMs < durationMs - RESUME_END_THRESHOLD_MS)

/**
 * Stable, collision-resistant identifier for media-specific preference keys.
 * MediaStore IDs are volume-relative and URI.hashCode() can collide; hash the full URI.
 */
internal fun mediaPreferenceId(uriString: String): String {
    val digest = MessageDigest.getInstance("SHA-256")
        .digest(uriString.toByteArray(Charsets.UTF_8))
    return digest.joinToString("") { byte ->
        (byte.toInt() and 0xff).toString(16).padStart(2, '0')
    }
}

internal fun compareNaturalNames(first: String, second: String): Int {
    val firstTokens = naturalSortTokens.findAll(first.lowercase(Locale.ROOT)).map { it.value }.toList()
    val secondTokens = naturalSortTokens.findAll(second.lowercase(Locale.ROOT)).map { it.value }.toList()
    val sharedTokenCount = minOf(firstTokens.size, secondTokens.size)

    for (index in 0 until sharedTokenCount) {
        val left = firstTokens[index]
        val right = secondTokens[index]
        val leftIsNumber = left.all(Char::isDigit)
        val rightIsNumber = right.all(Char::isDigit)

        val comparison = if (leftIsNumber && rightIsNumber) {
            val normalizedLeft = left.trimStart('0').ifEmpty { "0" }
            val normalizedRight = right.trimStart('0').ifEmpty { "0" }
            normalizedLeft.length.compareTo(normalizedRight.length).takeIf { it != 0 }
                ?: normalizedLeft.compareTo(normalizedRight)
        } else {
            left.compareTo(right, ignoreCase = true)
        }

        if (comparison != 0) return comparison
    }

    val tokenCountOrder = firstTokens.size.compareTo(secondTokens.size)
    return if (tokenCountOrder != 0) tokenCountOrder
    else first.compareTo(second, ignoreCase = true)
}
