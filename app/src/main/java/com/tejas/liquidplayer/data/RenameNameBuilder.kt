package com.tejas.liquidplayer.data

internal enum class RenameNameError { Empty, InvalidCharacter, TooLong }

internal data class RenameNameResult(
    val fileName: String? = null,
    val error: RenameNameError? = null
)

/**
 * Builds a safe display name and preserves the original extension even when the
 * requested base name contains other dots.
 */
internal fun buildRenameFileName(originalName: String, requestedName: String): RenameNameResult {
    val requested = requestedName.trim()
    if (requested.isEmpty()) return RenameNameResult(error = RenameNameError.Empty)
    if ('/' in requested || '\u0000' in requested) {
        return RenameNameResult(error = RenameNameError.InvalidCharacter)
    }

    val extension = originalName.substringAfterLast('.', "")
        .takeIf { originalName.lastIndexOf('.') > 0 && it.isNotBlank() }
    val finalName = if (extension != null && !requested.endsWith(".$extension", ignoreCase = true)) {
        "$requested.$extension"
    } else {
        requested
    }
    if (finalName.toByteArray(Charsets.UTF_8).size > 255) {
        return RenameNameResult(error = RenameNameError.TooLong)
    }
    return RenameNameResult(fileName = finalName)
}
