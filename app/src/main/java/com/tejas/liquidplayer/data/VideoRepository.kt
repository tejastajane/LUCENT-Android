package com.tejas.liquidplayer.data

import android.content.ContentUris
import android.content.ContentResolver
import android.os.Build
import android.provider.MediaStore
import java.util.Locale

class VideoRepository(private val resolver: ContentResolver) {
    fun loadVideos(): List<VideoItem> {
        val columns = buildList {
            add(MediaStore.Video.Media._ID)
            add(MediaStore.Video.Media.DISPLAY_NAME)
            add(MediaStore.Video.Media.DURATION)
            add(MediaStore.Video.Media.SIZE)
            add(MediaStore.Video.Media.DATE_ADDED)
            add(MediaStore.Video.Media.MIME_TYPE)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                add(MediaStore.Video.Media.RELATIVE_PATH)
            }
        }
        val results = mutableListOf<VideoItem>()
        val sort = "${MediaStore.Video.Media.DATE_ADDED} DESC"
        resolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            columns.toTypedArray(),
            null,
            null,
            sort
        )?.use { cursor ->
            val idIndex = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val nameIndex = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
            val durationIndex = cursor.getColumnIndex(MediaStore.Video.Media.DURATION)
            val sizeIndex = cursor.getColumnIndex(MediaStore.Video.Media.SIZE)
            val dateIndex = cursor.getColumnIndex(MediaStore.Video.Media.DATE_ADDED)
            val mimeIndex = cursor.getColumnIndex(MediaStore.Video.Media.MIME_TYPE)
            val pathIndex = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) cursor.getColumnIndex(MediaStore.Video.Media.RELATIVE_PATH) else -1

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idIndex)
                results += VideoItem(
                    id = id,
                    uri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id),
                    name = cursor.getString(nameIndex).orEmpty(),
                    relativePath = if (pathIndex >= 0) cursor.getString(pathIndex).orEmpty() else "Movies/",
                    durationMs = if (durationIndex >= 0) cursor.getLong(durationIndex) else 0L,
                    sizeBytes = if (sizeIndex >= 0) cursor.getLong(sizeIndex) else 0L,
                    dateAddedSeconds = if (dateIndex >= 0) cursor.getLong(dateIndex) else 0L,
                    mimeType = if (mimeIndex >= 0) cursor.getString(mimeIndex) else null
                )
            }
        }
        return results
    }


}

fun formatDuration(durationMs: Long): String {
    if (durationMs <= 0L) return "--:--"
    val totalSeconds = durationMs / 1000L
    val seconds = totalSeconds % 60
    val minutes = (totalSeconds / 60) % 60
    val hours = totalSeconds / 3600
    return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, seconds)
    else "%d:%02d".format(totalSeconds / 60, seconds)
}
