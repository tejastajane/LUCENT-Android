package com.tejas.liquidplayer.ui

import android.Manifest
import android.app.Activity
import android.app.RecoverableSecurityException
import android.content.ContentValues
import android.widget.Toast
import android.content.Intent
import android.database.ContentObserver
import android.provider.MediaStore
import android.provider.Settings
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
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
import com.tejas.liquidplayer.data.mediaPreferenceId
import com.tejas.liquidplayer.data.PlaybackPrefs
import com.tejas.liquidplayer.data.MediaCategory
import com.tejas.liquidplayer.data.isResumablePlaybackPosition
import com.tejas.liquidplayer.data.sortVideosAscending
import com.tejas.liquidplayer.player.VideoPlayerScreen
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun LiquidPlayerApp(
    isDarkTheme: Boolean,
    onThemeChanged: (Boolean) -> Unit,
    isInPictureInPictureMode: Boolean = false,
    incomingVideoUri: Uri? = null,
    incomingVideoRequestId: Int = 0,
    onIncomingVideoConsumed: () -> Unit = {}
) {
    val context = LocalContext.current
    val repository = remember(context) { VideoRepository(context.contentResolver) }
    val playerPreferences = remember(context) { context.getSharedPreferences("liquid_player", 0) }
    val settingsPreferences = remember(context, playerPreferences) {
        context.getSharedPreferences("lucent_settings", 0).also { settings ->
            listOf("dark_theme", "hardware_acceleration", "gestures_enabled").forEach { key ->
                if (!settings.contains(key) && playerPreferences.contains(key)) {
                    settings.edit()
                        .putBoolean(key, playerPreferences.getBoolean(key, true))
                        .apply()
                }
            }
        }
    }

    var videos by remember { mutableStateOf<List<VideoItem>>(emptyList()) }
    var mediaRefreshToken by remember { mutableIntStateOf(0) }
    var favouriteVersion by remember { mutableIntStateOf(0) }
    var categoryOverrideVersion by remember { mutableIntStateOf(0) }
    var pullRefreshToken by remember { mutableIntStateOf(0) }
    var completedPullRefreshToken by remember { mutableIntStateOf(0) }
    var isRefreshing by remember { mutableStateOf(false) }
    var hasMediaScanError by remember { mutableStateOf(false) }
    var hasVideoPermission by remember { mutableStateOf(hasMediaPermission(context)) }
    var hasMediaManagementAccess by remember { mutableStateOf(canManageMedia(context)) }
    var showMediaManagementOnboarding by remember { mutableStateOf(false) }
    var pendingDeleteAfterMediaAccess by remember {
        mutableStateOf<PendingMediaMutation.Delete?>(null)
    }
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var pendingLegacyMutation by remember { mutableStateOf<PendingMediaMutation?>(null) }
    var selectedVideo by remember { mutableStateOf<VideoItem?>(null) }
    var lastPlayedVideo by remember(playerPreferences) {
        mutableStateOf(playerPreferences.getString("last_played_uri", null)?.let { savedUri ->
            VideoItem(
                id = mediaPreferenceId(savedUri).take(15).toLong(16),
                uri = Uri.parse(savedUri),
                name = playerPreferences.getString("last_played_name", "Last played video") ?: "Last played video",
                relativePath = playerPreferences.getString("last_played_path", "Recent/") ?: "Recent/",
                durationMs = playerPreferences.getLong("last_played_duration", 0L),
                sizeBytes = playerPreferences.getLong("last_played_size", 0L),
                dateAddedSeconds = playerPreferences.getLong("last_played_date", 0L),
                mimeType = playerPreferences.getString("last_played_mime", null),
                volumeName = playerPreferences.getString("last_played_volume", null)
            )
        })
    }
    var hardwareAcceleration by remember {
        mutableStateOf(settingsPreferences.getBoolean("hardware_acceleration", true))
    }
    var gesturesEnabled by remember {
        mutableStateOf(settingsPreferences.getBoolean("gestures_enabled", true))
    }
    var pendingMediaMutation by remember { mutableStateOf<PendingMediaMutation?>(null) }

    val mediaMutationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        val mutation = pendingMediaMutation
        pendingMediaMutation = null
        if (result.resultCode == Activity.RESULT_OK && mutation != null) {
            // MediaStore.createDeleteRequest performs the deletion itself after approval.
            // Do not issue a second ContentResolver.delete for the same item.
            if (mutation is PendingMediaMutation.Delete && Build.VERSION.SDK_INT == Build.VERSION_CODES.R) {
                Toast.makeText(context, "Video deleted.", Toast.LENGTH_SHORT).show()
                mediaRefreshToken++
            } else {
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
    }

    val legacyWritePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        val mutation = pendingLegacyMutation
        pendingLegacyMutation = null
        if (granted && mutation != null) {
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
        } else if (mutation != null) {
            Toast.makeText(
                context,
                "Storage write access is required to rename or delete media on Android 9 and earlier.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    val mediaManagementSettingsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        hasMediaManagementAccess = canManageMedia(context)
        if (!hasMediaManagementAccess && pendingDeleteAfterMediaAccess != null) {
            pendingDeleteAfterMediaAccess = null
            Toast.makeText(
                context,
                "LUCENT needs media management access before it can delete videos.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    LaunchedEffect(hasVideoPermission, hasMediaManagementAccess) {
        if (hasMediaManagementAccess) {
            settingsPreferences.edit()
                .putBoolean("media_management_onboarding_completed", true)
                .apply()
            showMediaManagementOnboarding = false
        } else if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            hasVideoPermission &&
            !settingsPreferences.getBoolean("media_management_onboarding_completed", false)
        ) {
            showMediaManagementOnboarding = true
        }
    }

    fun finishMediaManagementOnboarding() {
        settingsPreferences.edit()
            .putBoolean("media_management_onboarding_completed", true)
            .apply()
        showMediaManagementOnboarding = false
    }

    fun requestMediaManagementAccess() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val intent = Intent(Settings.ACTION_REQUEST_MANAGE_MEDIA).apply {
                data = Uri.parse("package:${context.packageName}")
            }
            runCatching {
                mediaManagementSettingsLauncher.launch(intent)
            }.onFailure {
                pendingDeleteAfterMediaAccess = null
                Toast.makeText(context, "Unable to open media management settings.", Toast.LENGTH_LONG).show()
            }
        }
    }

    fun startMediaMutation(mutation: PendingMediaMutation) {
        if (mutation is PendingMediaMutation.Delete) {
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                    !canManageMedia(context) -> {
                    pendingDeleteAfterMediaAccess = mutation
                    requestMediaManagementAccess()
                    return
                }
                Build.VERSION.SDK_INT == Build.VERSION_CODES.Q ||
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                    // Android 10 supports recoverable per-file approval. Android 12+
                    // reaches this direct path only after MANAGE_MEDIA has been granted.
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
                    return
                }
            }
        }
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            pendingLegacyMutation = mutation
            legacyWritePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            return
        }
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

    LaunchedEffect(hasMediaManagementAccess, pendingDeleteAfterMediaAccess) {
        val pendingDelete = pendingDeleteAfterMediaAccess
        if (hasMediaManagementAccess && pendingDelete != null) {
            pendingDeleteAfterMediaAccess = null
            startMediaMutation(pendingDelete)
        }
    }

    fun openVideo(item: VideoItem) {
        lastPlayedVideo = item
        selectedVideo = item
        playerPreferences.edit()
            .putString("last_played_uri", item.uri.toString())
            .putString("last_played_name", item.name)
            .putString("last_played_path", item.relativePath)
            .putLong("last_played_duration", item.durationMs)
            .putLong("last_played_size", item.sizeBytes)
            .putLong("last_played_date", item.dateAddedSeconds)
            .putString("last_played_mime", item.mimeType)
            .putString("last_played_volume", item.volumeName)
            .apply()
    }

    LaunchedEffect(incomingVideoUri, incomingVideoRequestId) {
        val uri = incomingVideoUri ?: return@LaunchedEffect
        val mimeType = context.contentResolver.getType(uri)
        if (mimeType != null && !mimeType.startsWith("video/")) {
            Toast.makeText(context, "LUCENT can only open video files.", Toast.LENGTH_LONG).show()
            onIncomingVideoConsumed()
            return@LaunchedEffect
        }
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }
        val name = queryDisplayName(context, uri) ?: "Opened video"
        openVideo(
            VideoItem(
                id = mediaPreferenceId(uri.toString()).take(15).toLong(16),
                uri = uri,
                name = name,
                relativePath = "Opened from another app/",
                durationMs = 0L,
                sizeBytes = 0L,
                dateAddedSeconds = System.currentTimeMillis() / 1_000L,
                mimeType = mimeType?.takeIf { it.startsWith("video/") }
            )
        )
        onIncomingVideoConsumed()
    }

    val mediaPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        hasVideoPermission = hasMediaPermission(context)
    }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    androidx.compose.runtime.DisposableEffect(lifecycleOwner, context) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                val currentPermission = hasMediaPermission(context)
                val permissionChanged = currentPermission != hasVideoPermission
                hasVideoPermission = currentPermission
                hasMediaManagementAccess = canManageMedia(context)

                // Android 14 partial-library grants can change while Settings is open even
                // though the permission itself remains granted.
                val partialMediaAccess = Build.VERSION.SDK_INT >= 34 &&
                    ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_VIDEO) != PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED) == PackageManager.PERMISSION_GRANTED
                if (permissionChanged || partialMediaAccess) mediaRefreshToken++
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // MediaStore change notifications replace unconditional full-library scans on every
    // resume. Coalesce bursts (copy, rename, delete) into a single refresh.
    androidx.compose.runtime.DisposableEffect(context) {
        val handler = Handler(Looper.getMainLooper())
        var pendingRefresh: Runnable? = null
        val observer = object : ContentObserver(handler) {
            override fun onChange(selfChange: Boolean, uri: Uri?) {
                pendingRefresh?.let(handler::removeCallbacks)
                val refresh = Runnable { mediaRefreshToken++ }
                pendingRefresh = refresh
                handler.postDelayed(refresh, 400L)
            }
        }
        context.contentResolver.registerContentObserver(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            true,
            observer
        )
        onDispose {
            context.contentResolver.unregisterContentObserver(observer)
            pendingRefresh?.let(handler::removeCallbacks)
        }
    }

    androidx.compose.runtime.DisposableEffect(playerPreferences) {
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            when {
                key?.startsWith("favourite_") == true -> favouriteVersion++
                key?.startsWith("category_override_") == true -> categoryOverrideVersion++
            }
        }
        playerPreferences.registerOnSharedPreferenceChangeListener(listener)
        onDispose { playerPreferences.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    val favouriteUris = remember(videos, favouriteVersion, playerPreferences) {
        videos.asSequence()
            .filter { item -> PlaybackPrefs.readFavourite(playerPreferences, item.uri.toString()) }
            .map { it.uri.toString() }
            .toSet()
    }

    val categoryOverrides = remember(videos, categoryOverrideVersion, playerPreferences) {
        videos.mapNotNull { item ->
            val uriString = item.uri.toString()
            val category = MediaCategory.fromStorageValue(
                playerPreferences.getString(PlaybackPrefs.key("category_override_", uriString), null)
            )
            category?.let { uriString to it }
        }.toMap()
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
                    id = mediaPreferenceId(uri.toString()).take(15).toLong(16),
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

    LaunchedEffect(hasVideoPermission, mediaRefreshToken, pullRefreshToken) {
        val requestToken = pullRefreshToken
        val isPullRefresh = requestToken > completedPullRefreshToken
        if (isPullRefresh) isRefreshing = true
        var refreshCompleted = false
        try {
            if (!hasVideoPermission) {
                videos = emptyList()
                hasMediaScanError = false
            } else {
                try {
                    videos = withContext(Dispatchers.IO) {
                        repository.loadVideos().also { scanned ->
                            PlaybackPrefs.migrateLegacyPreferences(
                                playerPreferences,
                                scanned.map { it.uri.toString() }
                            )
                        }
                    }
                    hasMediaScanError = false
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: Exception) {
                    hasMediaScanError = true
                    Toast.makeText(context, "Couldn't scan the video library. Try scanning again.", Toast.LENGTH_LONG).show()
                }
            }
            refreshCompleted = true
        } finally {
            // A cancelled scan must not clear the indicator while its replacement scan is
            // still servicing the same pull-to-refresh request.
            if (isPullRefresh && refreshCompleted && pullRefreshToken == requestToken) {
                completedPullRefreshToken = requestToken
                isRefreshing = false
            }
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
            val savedPosition = PlaybackPrefs.readPlaybackPosition(
                playerPreferences,
                item.uri.toString()
            )
            isResumablePlaybackPosition(savedPosition, item.durationMs)
        }
        val playTarget = resumableVideo ?: videos.maxByOrNull { it.dateAddedSeconds }
        val hasResumeHistory = resumableVideo != null && playTarget?.uri == resumableVideo.uri
        MainLibraryUi(
            selectedTab = selectedTab,
            onTabSelected = { selectedTab = it },
            videos = videos,
            hasVideoPermission = hasVideoPermission,
            hasMediaManagementAccess = hasMediaManagementAccess,
            showMediaManagementOnboarding = showMediaManagementOnboarding,
            onMediaManagementOnboardingHandled = ::finishMediaManagementOnboarding,
            isRefreshing = isRefreshing,
            hasMediaScanError = hasMediaScanError,
            favouriteUris = favouriteUris,
            categoryOverrides = categoryOverrides,
            onSetCategoryOverride = { item, category ->
                val key = PlaybackPrefs.key("category_override_", item.uri.toString())
                val editor = playerPreferences.edit()
                if (category == null) editor.remove(key) else editor.putString(key, category.storageValue)
                editor.apply()
            },
            onRequestMediaPermission = {
                val permissions = when {
                    Build.VERSION.SDK_INT >= 34 -> arrayOf(
                        Manifest.permission.READ_MEDIA_VIDEO,
                        Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
                    )
                    Build.VERSION.SDK_INT >= 33 -> arrayOf(Manifest.permission.READ_MEDIA_VIDEO)
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ->
                        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
                    else -> arrayOf(
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    )
                }
                mediaPermissionLauncher.launch(permissions)
            },
            onRequestMediaManagementAccess = ::requestMediaManagementAccess,
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
                settingsPreferences.edit().putBoolean("hardware_acceleration", it).apply()
            },
            gesturesEnabled = gesturesEnabled,
            onGesturesEnabledChanged = {
                gesturesEnabled = it
                settingsPreferences.edit().putBoolean("gestures_enabled", it).apply()
            },
            isDarkTheme = isDarkTheme,
            onThemeChanged = onThemeChanged,
            onPullToRefresh = {
                hasVideoPermission = hasMediaPermission(context)
                pullRefreshToken++
                mediaRefreshToken++
            },
            onRescan = {
                hasVideoPermission = hasMediaPermission(context)
                mediaRefreshToken++
            }
        )
    }
}

private fun canManageMedia(context: android.content.Context): Boolean {
    return Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && MediaStore.canManageMedia(context)
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
            android.util.Log.w("LUCENT/MediaMutation", "Media change was denied by Android", error)
            MediaMutationOutcome.Failure("LUCENT does not have permission to change this video. Refresh the library or check media access in Android settings.")
        }
    } catch (error: Exception) {
        android.util.Log.e("LUCENT/MediaMutation", "Unable to change media", error)
        MediaMutationOutcome.Failure("Couldn't change this video. Please refresh the library and try again.")
    }
}
