package com.tejas.liquidplayer.data

import android.net.Uri

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
    val folderName: String
        get() = relativePath.trimEnd('/').substringAfterLast('/').ifBlank { "Internal storage" }
}

data class TrackOption(val id: Int, val label: String)
