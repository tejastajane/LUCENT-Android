package com.tejas.liquidplayer.ui

import android.Manifest
import android.app.Activity
import android.app.RecoverableSecurityException
import android.content.ContentValues
import android.widget.Toast
import android.content.Intent
import android.provider.MediaStore
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.tejas.liquidplayer.data.VideoItem
import com.tejas.liquidplayer.data.VideoRepository
import com.tejas.liquidplayer.data.sortVideosAscending
import com.tejas.liquidplayer.player.VideoPlayerScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun LiquidPlayerApp(
    isDarkTheme: Boolean,
    onThemeChanged: (Boolean) -> Unit,
    isInPictureInPictureMode: Boolean = false
) {
    val context = LocalContext.current
    val repository = remember(context) { VideoRepository(context.contentResolver) }
    val preferences = remember(context) { context.getSharedPreferences("liquid_player", 0) }

    var videos by remember { mutableStateOf<List<VideoItem>>(emptyList()) }
    var mediaRefreshToken by remember { mutableIntStateOf(0) }
    var hasVideoPermission by remember { mutableStateOf(hasMediaPermission(context)) }
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var selectedVideo by remember { mutableStateOf<VideoItem?>(null) }
    var lastPlayedVideo by remember(preferences) {
        mutableStateOf(preferences.getString("last_played_uri", null)?.let { savedUri ->
            VideoItem(
                id = savedUri.hashCode().toLong(),
                uri = Uri.parse(savedUri),
                name = preferences.getString("last_played_name", "Last played video") ?: "Last played video",
                relativePath = preferences.getString("last_played_path", "Recent/") ?: "Recent/",
                durationMs = preferences.getLong("last_played_duration", 0L),
                sizeBytes = preferences.getLong("last_played_size", 0L),
                dateAddedSeconds = preferences.getLong("last_played_date", 0L),
                mimeType = preferences.getString("last_played_mime", null)
            )
        })
    }
    var hardwareAcceleration by remember {
        mutableStateOf(preferences.getBoolean("hardware_acceleration", true))
    }
    var gesturesEnabled by remember {
        mutableStateOf(preferences.getBoolean("gestures_enabled", true))
    }
    var pendingMediaMutation by remember { mutableStateOf<PendingMediaMutation?>(null) }

    val mediaMutationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        val mutation = pendingMediaMutation
        pendingMediaMutation = null
        if (result.resultCode == Activity.RESULT_OK && mutation != null) {
            when (val outcome = executeMediaMutation(context, mutation)) {
                is MediaMutationOutcome.Success -> {
                    Toast.makeText(context, outcome.message, Toast.LENGTH_SHORT).show()
                    mediaRefreshToken++
                }
                is MediaMutationOutcome.NeedsApproval -> {
                    Toast.makeText(context, "Android did not approve the media change.", Toast.LENGTH_SHORT).show()
                }
                is MediaMutationOutcome.Failure -> {
                    Toast.makeText(context, outcome.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    fun startMediaMutation(mutation: PendingMediaMutation) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val request = when (mutation) {
                is PendingMediaMutation.Rename -> MediaStore.createWriteRequest(
                    context.contentResolver,
                    listOf(mutation.video.uri)
                )
                is PendingMediaMutation.Delete -> MediaStore.createDeleteRequest(
                    context.contentResolver,
                    listOf(mutation.video.uri)
                )
            }
            pendingMediaMutation = mutation
            mediaMutationLauncher.launch(
                IntentSenderRequest.Builder(request.intentSender).build()
            )
        } else {
            when (val outcome = executeMediaMutation(context, mutation)) {
                is MediaMutationOutcome.Success -> {
                    Toast.makeText(context, outcome.message, Toast.LENGTH_SHORT).show()
                    mediaRefreshToken++
                }
                is MediaMutationOutcome.NeedsApproval -> {
                    pendingMediaMutation = mutation
                    mediaMutationLauncher.launch(
                        IntentSenderRequest.Builder(outcome.intentSender).build()
                    )
                }
                is MediaMutationOutcome.Failure -> {
                    Toast.makeText(context, outcome.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    fun openVideo(item: VideoItem) {
        lastPlayedVideo = item
        selectedVideo = item
        preferences.edit()
            .putString("last_played_uri", item.uri.toString())
            .putString("last_played_name", item.name)
            .putString("last_played_path", item.relativePath)
            .putLong("last_played_duration", item.durationMs)
            .putLong("last_played_size", item.sizeBytes)
            .putLong("last_played_date", item.dateAddedSeconds)
            .putString("last_played_mime", item.mimeType)
            .apply()
    }

    val mediaPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        hasVideoPermission = hasMediaPermission(context)
    }

    val documentPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            val name = queryDisplayName(context, uri) ?: "Selected video"
            openVideo(
                VideoItem(
                    id = uri.toString().hashCode().toLong(),
                    uri = uri,
                    name = name,
                    relativePath = "Selected files/",
                    durationMs = 0L,
                    sizeBytes = 0L,
                    dateAddedSeconds = System.currentTimeMillis() / 1000L,
                    mimeType = context.contentResolver.getType(uri)
                )
            )
        }
    }

    LaunchedEffect(hasVideoPermission, mediaRefreshToken) {
        videos = if (hasVideoPermission) {
            withContext(Dispatchers.IO) {
                runCatching { repository.loadVideos() }.getOrDefault(emptyList())
            }
        } else {
            emptyList()
        }
    }

    // Keep libVLC's native TextureView outside AnimatedContent's graphics-layer
    // transition. Preserve native surface ownership while the player is shown.
    val activeVideo = selectedVideo
    if (activeVideo != null) {
        val playbackQueue = remember(videos, activeVideo) {
            sortVideosAscending(
                if (videos.any { it.uri == activeVideo.uri }) videos
                else listOf(activeVideo) + videos.filterNot { it.uri == activeVideo.uri }
            )
        }
        LiquidPlayerTheme(darkTheme = true) {
            VideoPlayerScreen(
                video = activeVideo,
                playlist = playbackQueue,
                onSelectVideo = ::openVideo,
                onClose = { selectedVideo = null },
                hardwareAcceleration = hardwareAcceleration,
                gesturesEnabled = gesturesEnabled,
                isInPictureInPictureMode = isInPictureInPictureMode
            )
        }
    } else {
        // Resume only when the previous item has a meaningful, unfinished position.
        // Otherwise the pill must play the newest available library video.
        val resumableVideo = lastPlayedVideo?.takeIf { item ->
            val savedPosition = preferences.getLong(
                "playback_position_${item.uri.toString().hashCode()}",
                0L
            )
            savedPosition > 1_500L &&
                (item.durationMs <= 0L || savedPosition < item.durationMs - 1_500L)
        }
        val playTarget = resumableVideo ?: videos.maxByOrNull { it.dateAddedSeconds }
        val hasResumeHistory = resumableVideo != null && playTarget?.uri == resumableVideo.uri
        MainLibraryUi(
            selectedTab = selectedTab,
            onTabSelected = { selectedTab = it },
            videos = videos,
            hasVideoPermission = hasVideoPermission,
            onRequestMediaPermission = {
                val permissions = when {
                    Build.VERSION.SDK_INT >= 34 -> arrayOf(
                        Manifest.permission.READ_MEDIA_VIDEO,
                        Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
                    )
                    Build.VERSION.SDK_INT >= 33 -> arrayOf(Manifest.permission.READ_MEDIA_VIDEO)
                    else -> arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
                }
                mediaPermissionLauncher.launch(permissions)
            },
            onPlay = ::openVideo,
            onRenameVideo = { item, newName ->
                startMediaMutation(PendingMediaMutation.Rename(item, newName))
            },
            onDeleteVideo = { item ->
                startMediaMutation(PendingMediaMutation.Delete(item))
            },
            recentVideo = playTarget,
            hasResumeHistory = hasResumeHistory,
            onChooseVideoFile = { documentPicker.launch(arrayOf("video/*")) },
            hardwareAcceleration = hardwareAcceleration,
            onHardwareAccelerationChanged = {
                hardwareAcceleration = it
                preferences.edit().putBoolean("hardware_acceleration", it).apply()
            },
            gesturesEnabled = gesturesEnabled,
            onGesturesEnabledChanged = {
                gesturesEnabled = it
                preferences.edit().putBoolean("gestures_enabled", it).apply()
            },
            isDarkTheme = isDarkTheme,
            onThemeChanged = onThemeChanged,
            onRescan = {
                hasVideoPermission = hasMediaPermission(context)
                mediaRefreshToken++
            }
        )
    }
}

private fun hasMediaPermission(context: android.content.Context): Boolean {
    val permissions = when {
        Build.VERSION.SDK_INT >= 34 -> listOf(
            Manifest.permission.READ_MEDIA_VIDEO,
            Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
        )
        Build.VERSION.SDK_INT >= 33 -> listOf(Manifest.permission.READ_MEDIA_VIDEO)
        else -> listOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }
    return permissions.any {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }
}

private fun queryDisplayName(context: android.content.Context, uri: Uri): String? {
    return runCatching {
        context.contentResolver.query(
            uri,
            arrayOf(android.provider.OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null
        )?.use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }
    }.getOrNull()
}


private sealed interface PendingMediaMutation {
    val video: VideoItem

    data class Rename(override val video: VideoItem, val newName: String) : PendingMediaMutation
    data class Delete(override val video: VideoItem) : PendingMediaMutation
}

private sealed interface MediaMutationOutcome {
    data class Success(val message: String) : MediaMutationOutcome
    data class NeedsApproval(val intentSender: android.content.IntentSender) : MediaMutationOutcome
    data class Failure(val message: String) : MediaMutationOutcome
}

private fun executeMediaMutation(
    context: android.content.Context,
    mutation: PendingMediaMutation
): MediaMutationOutcome {
    return try {
        val changedRows = when (mutation) {
            is PendingMediaMutation.Rename -> {
                val values = ContentValues().apply {
                    put(MediaStore.Video.Media.DISPLAY_NAME, mutation.newName)
                }
                context.contentResolver.update(mutation.video.uri, values, null, null)
            }
            is PendingMediaMutation.Delete -> {
                context.contentResolver.delete(mutation.video.uri, null, null)
            }
        }

        if (changedRows > 0) {
            when (mutation) {
                is PendingMediaMutation.Rename -> MediaMutationOutcome.Success("Video renamed.")
                is PendingMediaMutation.Delete -> MediaMutationOutcome.Success("Video deleted.")
            }
        } else {
            MediaMutationOutcome.Failure("The video could not be changed. Refresh the library and try again.")
        }
    } catch (error: SecurityException) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && error is RecoverableSecurityException) {
            MediaMutationOutcome.NeedsApproval(error.userAction.actionIntent.intentSender)
        } else {
            MediaMutationOutcome.Failure(error.message ?: "Permission denied while changing the video.")
        }
    } catch (error: Exception) {
        MediaMutationOutcome.Failure(error.message ?: "The video could not be changed.")
    }
}
