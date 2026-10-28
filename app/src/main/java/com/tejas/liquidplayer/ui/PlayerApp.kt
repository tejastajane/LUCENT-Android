package com.tejas.liquidplayer.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.text.format.Formatter
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
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
import com.tejas.liquidplayer.data.BrowseEntry
import com.tejas.liquidplayer.player.VideoPlayerScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun LiquidPlayerApp(isDarkTheme: Boolean, onThemeChanged: (Boolean) -> Unit) {
    val context = LocalContext.current
    val repository = remember(context) { VideoRepository(context.contentResolver) }
    val preferences = remember(context) { context.getSharedPreferences("liquid_player", 0) }

    var videos by remember { mutableStateOf<List<VideoItem>>(emptyList()) }
    var mediaRefreshToken by remember { mutableIntStateOf(0) }
    var hasVideoPermission by remember { mutableStateOf(hasMediaPermission(context)) }
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var selectedVideo by remember { mutableStateOf<VideoItem?>(null) }
    var lastPlayedVideo by remember(preferences) {
        mutableStateOf(
            preferences.getString("last_played_uri", null)?.let { savedUri ->
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
            }
        )
    }
    var selectedFolder by rememberSaveable { mutableStateOf<String?>(null) }
    var storageTreeUri by rememberSaveable { mutableStateOf<Uri?>(null) }
    var browsePath by rememberSaveable { mutableStateOf(listOf<String>()) }
    var browseEntries by remember { mutableStateOf<List<BrowseEntry>>(emptyList()) }
    var hardwareAcceleration by remember {
        mutableStateOf(preferences.getBoolean("hardware_acceleration", true))
    }
    var gesturesEnabled by remember {
        mutableStateOf(preferences.getBoolean("gestures_enabled", true))
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

    val treePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            }
            storageTreeUri = uri
            browsePath = emptyList()
        }
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
            withContext(Dispatchers.IO) { runCatching { repository.loadVideos() }.getOrDefault(emptyList()) }
        } else {
            emptyList()
        }
    }

    LaunchedEffect(storageTreeUri, browsePath) {
        browseEntries = withContext(Dispatchers.IO) {
            runCatching { repository.listTree(context, storageTreeUri, browsePath) }
                .getOrDefault(emptyList())
        }
    }

    AnimatedContent(
        targetState = selectedVideo,
        transitionSpec = {
            if (targetState == null) {
                (slideInHorizontally(tween(280)) { -it / 3 } + fadeIn(tween(180))) togetherWith
                    (slideOutHorizontally(tween(240)) { it / 4 } + fadeOut(tween(120)))
            } else {
                (slideInHorizontally(tween(280)) { it } + fadeIn(tween(180))) togetherWith
                    (slideOutHorizontally(tween(240)) { -it / 4 } + fadeOut(tween(120)))
            }
        },
        label = "library-player-transition"
    ) { activeVideo ->
        if (activeVideo != null) {
            val playbackQueue = remember(videos, activeVideo) {
                if (videos.any { it.uri == activeVideo.uri }) videos
                else listOf(activeVideo) + videos.filterNot { it.uri == activeVideo.uri }
            }
            LiquidPlayerTheme(darkTheme = true) {
                VideoPlayerScreen(
                    video = activeVideo,
                    playlist = playbackQueue,
                    onSelectVideo = ::openVideo,
                    onClose = { selectedVideo = null },
                    hardwareAcceleration = hardwareAcceleration,
                    gesturesEnabled = gesturesEnabled
                )
            }
        } else {
            MainLibraryUi(
                selectedTab = selectedTab, onTabSelected = { selectedTab = it }, videos = videos,
                hasVideoPermission = hasVideoPermission,
                onRequestMediaPermission = {
                    val permissions = when {
                        Build.VERSION.SDK_INT >= 34 -> arrayOf(Manifest.permission.READ_MEDIA_VIDEO, Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)
                        Build.VERSION.SDK_INT >= 33 -> arrayOf(Manifest.permission.READ_MEDIA_VIDEO)
                        else -> arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
                    }
                    mediaPermissionLauncher.launch(permissions)
                },
                selectedFolder = selectedFolder,
                onFolderSelected = { folder -> selectedFolder = folder; selectedTab = 0 },
                storageTreeUri = storageTreeUri, browsePath = browsePath, browseEntries = browseEntries,
                onChooseStorageFolder = { treePicker.launch(storageTreeUri) },
                onBrowseBack = { browsePath = browsePath.dropLast(1) },
                onBrowseEntry = { entry ->
                    if (entry.isDirectory) browsePath = browsePath + entry.name
                    else {
                        val isVideo = entry.mimeType?.startsWith("video/") == true ||
                            entry.name.substringAfterLast('.', "").lowercase() in setOf("mkv", "mp4", "mov", "avi", "webm", "m4v", "3gp", "ts", "wmv", "flv", "mpg", "mpeg")
                        if (isVideo) openVideo(
                            VideoItem(
                                id = entry.uri.toString().hashCode().toLong(), uri = entry.uri, name = entry.name,
                                relativePath = "Storage browser/", durationMs = 0L, sizeBytes = entry.sizeBytes,
                                dateAddedSeconds = System.currentTimeMillis() / 1000L, mimeType = entry.mimeType
                            )
                        )
                    }
                },
                onPlay = ::openVideo,
                recentVideo = lastPlayedVideo ?: videos.maxByOrNull { it.dateAddedSeconds },
                hasResumeHistory = lastPlayedVideo?.let {
                    preferences.getLong("playback_position_${it.uri.toString().hashCode()}", 0L) > 1_500L
                } ?: false,
                onChooseVideoFile = { documentPicker.launch(arrayOf("video/*")) },
                hardwareAcceleration = hardwareAcceleration,
                onHardwareAccelerationChanged = { hardwareAcceleration = it; preferences.edit().putBoolean("hardware_acceleration", it).apply() },
                gesturesEnabled = gesturesEnabled,
                onGesturesEnabledChanged = { gesturesEnabled = it; preferences.edit().putBoolean("gestures_enabled", it).apply() },
                isDarkTheme = isDarkTheme, onThemeChanged = onThemeChanged,
                onRescan = { hasVideoPermission = hasMediaPermission(context); mediaRefreshToken++ }
            )
        }
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
        context.contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }
    }.getOrNull()
}
