package com.tejas.liquidplayer.ui

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import android.content.ClipData
import android.content.Intent
import android.widget.Toast
import android.media.MediaMetadataRetriever
import android.util.Size
import android.view.HapticFeedbackConstants
import android.os.Build
import android.text.format.Formatter
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.shapes.Capsule
import com.kyant.shapes.RoundedRectangle
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.tejas.liquidplayer.ui.liquid.LiquidBottomTab
import com.tejas.liquidplayer.ui.liquid.LiquidBottomTabs
import com.tejas.liquidplayer.ui.liquid.LiquidToggle
import com.tejas.liquidplayer.data.VideoItem
import com.tejas.liquidplayer.data.formatDuration
import com.tejas.liquidplayer.data.sortVideosAscending
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale
import java.text.DateFormat
import java.util.Date

private sealed interface LibraryOverlayState {
    data class Message(
        val title: String,
        val message: String,
        val confirmLabel: String = "Okay",
        val cancelLabel: String? = null,
        val onConfirm: () -> Unit,
        val onCancel: () -> Unit
    ) : LibraryOverlayState
    data class RenameVideo(val item: VideoItem) : LibraryOverlayState
    data class VideoActions(val item: VideoItem) : LibraryOverlayState
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainLibraryUi(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    videos: List<VideoItem>,
    hasVideoPermission: Boolean,
    hasMediaScanError: Boolean,
    favouriteUris: Set<String>,
    hasMediaManagementAccess: Boolean,
    showMediaManagementOnboarding: Boolean,
    onMediaManagementOnboardingHandled: () -> Unit,
    isRefreshing: Boolean,
    onRequestMediaPermission: () -> Unit,
    onRequestMediaManagementAccess: () -> Unit,
    onPlay: (VideoItem) -> Unit,
    onRenameVideo: (VideoItem, String) -> Unit,
    onDeleteVideo: (VideoItem) -> Unit,
    recentVideo: VideoItem?,
    hasResumeHistory: Boolean,
    onChooseVideoFile: () -> Unit,
    hardwareAcceleration: Boolean,
    onHardwareAccelerationChanged: (Boolean) -> Unit,
    gesturesEnabled: Boolean,
    onGesturesEnabledChanged: (Boolean) -> Unit,
    isDarkTheme: Boolean,
    onThemeChanged: (Boolean) -> Unit,
    onRescan: () -> Unit
) {
    val backdrop = rememberLayerBackdrop()
    var selectedFolder by rememberSaveable { mutableStateOf<String?>(null) }
    var searchExpanded by rememberSaveable { mutableStateOf(false) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var activeOverlay by remember { mutableStateOf<LibraryOverlayState?>(null) }
    val context = LocalContext.current
    val showMessage: (String, String) -> Unit = { title, message ->
        activeOverlay = LibraryOverlayState.Message(
            title = title, message = message, confirmLabel = "Done",
            onConfirm = { activeOverlay = null }, onCancel = { activeOverlay = null }
        )
    }

    LaunchedEffect(showMediaManagementOnboarding) {
        if (showMediaManagementOnboarding && activeOverlay == null) {
            activeOverlay = LibraryOverlayState.Message(
                title = "Manage your media",
                message = "Allow LUCENT to manage videos on your device. This lets LUCENT delete a video after its own confirmation, without an additional Android confirmation. Android opens the media management settings page once so you can grant access.",
                confirmLabel = "Continue",
                cancelLabel = "Not now",
                onConfirm = {
                    activeOverlay = null
                    onMediaManagementOnboardingHandled()
                    onRequestMediaManagementAccess()
                },
                onCancel = {
                    activeOverlay = null
                    onMediaManagementOnboardingHandled()
                }
            )
        }
    }

    BackHandler(enabled = selectedTab != 0 || searchExpanded || activeOverlay != null) {
        when (val overlay = activeOverlay) {
            is LibraryOverlayState.Message -> overlay.onCancel()
            is LibraryOverlayState.RenameVideo, is LibraryOverlayState.VideoActions -> activeOverlay = null
            null -> when {
                searchExpanded -> { searchExpanded = false; searchQuery = "" }
                selectedTab != 0 -> { selectedFolder = null; onTabSelected(0) }
            }
        }
    }

    val showRecentPlayState = remember { mutableStateOf(true) }
    val scrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (source == NestedScrollSource.UserInput) {
                    if (available.y < -1f) showRecentPlayState.value = false
                    if (available.y > 1f) showRecentPlayState.value = true
                }
                return Offset.Zero
            }
        }
    }
    Box(Modifier.fillMaxSize().nestedScroll(scrollConnection).background(MaterialTheme.colorScheme.background)) {
        Box(Modifier.fillMaxSize().layerBackdrop(backdrop).background(MaterialTheme.colorScheme.background)) {
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = onRescan,
                modifier = Modifier.fillMaxSize().statusBarsPadding()
            ) {
                Column(Modifier.fillMaxSize()) {
                    AnimatedContent(
                    targetState = if (searchQuery.isNotBlank()) 3 else selectedTab.coerceIn(0, 2),
                    // Directional page transitions are preserved; the navbar remains a separate sibling layer.
                    transitionSpec = {
                        if (targetState > initialState) (slideInHorizontally(tween(250)) { it / 3 } + fadeIn(tween(160))) togetherWith
                            (slideOutHorizontally(tween(220)) { -it / 4 } + fadeOut(tween(120)))
                        else (slideInHorizontally(tween(250)) { -it / 3 } + fadeIn(tween(160))) togetherWith
                            (slideOutHorizontally(tween(220)) { it / 4 } + fadeOut(tween(120)))
                    },
                    label = "videos-folders-settings-transition"
                ) { tab ->
                    when (tab) {
                        0 -> VideosScreen(
                            backdrop = backdrop, videos = videos, hasPermission = hasVideoPermission,
                            hasScanError = hasMediaScanError, favouriteUris = favouriteUris,
                            onRequestPermission = onRequestMediaPermission, selectedFolder = selectedFolder,
                            onClearFolder = { selectedFolder = null }, onPlay = onPlay,
                            onChooseVideoFile = onChooseVideoFile,
                            onRequestActions = { activeOverlay = LibraryOverlayState.VideoActions(it) },
                            onRescan = onRescan
                        )
                        1 -> FoldersScreen(
                            backdrop, videos, hasVideoPermission,
                            onFolderSelected = { folder -> selectedFolder = folder; onTabSelected(0) },
                            onRescan = onRescan
                        )
                        3 -> SearchResultsScreen(
                            backdrop = backdrop, videos = videos, query = searchQuery,
                            hasPermission = hasVideoPermission,
                            hasScanError = hasMediaScanError,
                            onRequestPermission = onRequestMediaPermission,
                            onRescan = onRescan,
                            onPlay = onPlay,
                            onRequestActions = { activeOverlay = LibraryOverlayState.VideoActions(it) }
                        )
                        else -> SettingsScreen(
                            hardwareAcceleration = hardwareAcceleration,
                            onHardwareAccelerationChanged = onHardwareAccelerationChanged,
                            gesturesEnabled = gesturesEnabled,
                            onGesturesEnabledChanged = onGesturesEnabledChanged,
                            isDarkTheme = isDarkTheme,
                            onThemeChanged = onThemeChanged,
                            onRescan = onRescan,
                            onChooseVideoFile = onChooseVideoFile,
                            hasMediaManagementAccess = hasMediaManagementAccess,
                            onManageMediaAccess = onRequestMediaManagementAccess,
                            onShowInfoDialog = showMessage
                        )
                    }
                }
                }
            }
        }
        AnimatedVisibility(
            visible = recentVideo != null && showRecentPlayState.value && !searchExpanded,
            modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 108.dp),
            enter = fadeIn(tween(160)) + slideInVertically(tween(220)) { it * 2 },
            exit = fadeOut(tween(130)) + slideOutVertically(tween(180)) { it * 3 }
        ) {
            val item = recentVideo
            if (item != null) ResumePlaybackPill(
                backdrop = backdrop,
                hasResumeHistory = hasResumeHistory,
                onClick = { onPlay(item) }
            )
        }
        BottomNavigation(
            backdrop = backdrop,
            selectedTab = selectedTab.coerceIn(0, 2),
            onTabSelected = onTabSelected,
            searchExpanded = searchExpanded,
            onSearchExpandedChange = { searchExpanded = it },
            searchQuery = searchQuery,
            onSearchQueryChange = { searchQuery = it },
            modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding()
        )
        when (val overlay = activeOverlay) {
            is LibraryOverlayState.Message -> KyantMessageDialog(
                backdrop, overlay.title, overlay.message, overlay.cancelLabel, overlay.confirmLabel,
                overlay.onCancel, overlay.onCancel, overlay.onConfirm
            )
            is LibraryOverlayState.RenameVideo -> KyantRenameDialog(
                backdrop, overlay.item,
                onDismissRequest = { activeOverlay = null },
                onSave = { item, name ->
                    activeOverlay = null
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        activeOverlay = LibraryOverlayState.Message(
                            title = "Approve rename?",
                            message = "Android may ask you to approve this rename in a system dialog. LUCENT keeps that platform approval for file writes and does not request additional media-location access just to suppress it.",
                            confirmLabel = "Continue",
                            cancelLabel = "Cancel",
                            onConfirm = {
                                activeOverlay = null
                                onRenameVideo(item, name)
                            },
                            onCancel = { activeOverlay = null }
                        )
                    } else {
                        onRenameVideo(item, name)
                    }
                }
            )
            is LibraryOverlayState.VideoActions -> KyantVideoActionSheet(
                backdrop = backdrop,
                onDismissRequest = { activeOverlay = null },
                onInformation = {
                    activeOverlay = LibraryOverlayState.Message(
                        "Information", videoInformationText(context, overlay.item), "Done",
                        onConfirm = { activeOverlay = null }, onCancel = { activeOverlay = null }
                    )
                },
                onRename = { activeOverlay = LibraryOverlayState.RenameVideo(overlay.item) },
                onShare = {
                    val item = overlay.item
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = item.mimeType?.takeIf { it.startsWith("video/") } ?: "video/*"
                        putExtra(Intent.EXTRA_STREAM, item.uri)
                        clipData = ClipData.newUri(context.contentResolver, item.name, item.uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    runCatching { context.startActivity(Intent.createChooser(shareIntent, "Share video")) }
                        .onFailure { Toast.makeText(context, "Unable to share this video.", Toast.LENGTH_SHORT).show() }
                    activeOverlay = null
                },
                onDelete = {
                    val item = overlay.item
                    activeOverlay = LibraryOverlayState.Message(
                        title = "Delete video?",
                        message = "\"${item.name}\" will be permanently deleted from this device. This action cannot be undone.",
                        confirmLabel = "Delete",
                        cancelLabel = "Cancel",
                        onConfirm = {
                            activeOverlay = null
                            onDeleteVideo(item)
                        },
                        onCancel = { activeOverlay = null }
                    )
                }
            )
            null -> Unit
        }
    }
}

@Composable
private fun SearchResultsScreen(
    backdrop: LayerBackdrop,
    videos: List<VideoItem>,
    query: String,
    hasPermission: Boolean,
    hasScanError: Boolean,
    onRequestPermission: () -> Unit,
    onRescan: () -> Unit,
    onPlay: (VideoItem) -> Unit,
    onRequestActions: (VideoItem) -> Unit
) {
    val filtered = remember(videos, query) { sortVideosAscending(videos.filter { it.name.contains(query, ignoreCase = true) || it.relativePath.contains(query, ignoreCase = true) }) }
    Column(Modifier.fillMaxSize()) {
        Text("Search results", Modifier.padding(start = 22.dp, end = 18.dp, top = 14.dp, bottom = 8.dp), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
        if (!hasPermission) EmptyState(backdrop, "Video permission needed", "Allow access to search local videos.", "Allow access", onRequestPermission)
        else if (filtered.isEmpty()) EmptyState(
            backdrop,
            if (hasScanError) "Library scan failed" else "No matches",
            if (hasScanError) {
                "LUCENT couldn't refresh the video library. Scan again to retry."
            } else {
                "Try another title or folder name, or clear the search field above."
            },
            action = if (hasScanError) "Scan again" else null,
            onAction = if (hasScanError) onRescan else null
        )
        else LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 176.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            items(filtered, key = { it.id }) { item -> VideoRow(backdrop, item, onClick = { onPlay(item) }, onRequestActions = onRequestActions) }
        }
    }
}

@Composable
private fun VideosScreen(
    backdrop: LayerBackdrop,
    videos: List<VideoItem>,
    hasPermission: Boolean,
    onRequestPermission: () -> Unit,
    selectedFolder: String?,
    onClearFolder: () -> Unit,
    onPlay: (VideoItem) -> Unit,
    onChooseVideoFile: () -> Unit,
    onRequestActions: (VideoItem) -> Unit,
    hasScanError: Boolean,
    favouriteUris: Set<String>,
    onRescan: () -> Unit
) {
    var category by rememberSaveable { mutableStateOf("All") }
    // Recompute directly from the selected chip. TV detection uses both release
    // naming conventions and media-store folder hierarchy, not only one regex.
    val filtered = videos.filter { item ->
        val name = item.name.lowercase(Locale.ROOT)
        val pathSegments = item.relativePath.split('/').map { it.trim().lowercase(Locale.ROOT) }.filter(String::isNotBlank)
        val looksLikeTvEpisode = isLikelyTvEpisode(item.name, pathSegments)
        val isAnime = pathSegments.any { it.contains("anime") } ||
            listOf("anime", "jujutsu", "naruto", "one.piece", "bleach", "attack.on", "demon.slayer", "suzume", "spirited.away", "frieren", "ippo").any(name::contains)
        val isNonMovieFolder = pathSegments.any { nonMovieFolderPattern.matches(it) }
        val matchesCategory = when (category) {
            "Movies" -> !looksLikeTvEpisode && !isAnime && !isNonMovieFolder && item.durationMs >= 90L * 60L * 1_000L
            "TV Shows" -> looksLikeTvEpisode
            "Anime" -> isAnime
            "Favourites" -> item.uri.toString() in favouriteUris
            "Latest" -> true
            else -> true
        }
        matchesCategory && (
            selectedFolder == null ||
                item.folderIdentity == selectedFolder ||
                item.folderPath == selectedFolder // compatibility with a previously restored selection
            )
    }.let { items ->
        if (category == "Latest") items.sortedByDescending(VideoItem::dateAddedSeconds) else sortVideosAscending(items)
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(start = 22.dp, end = 18.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Videos", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        }
        if (hasScanError && videos.isNotEmpty()) {
            Text(
                "Couldn't refresh the library. Showing the last available list.",
                modifier = Modifier.padding(horizontal = 22.dp, vertical = 4.dp),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }
        if (selectedFolder != null) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                val selectedFolderPath = selectedFolder.substringAfter('|', selectedFolder)
                Text(
                    "Folder: ${selectedFolderPath.replace("/", " › ").ifBlank { "Internal storage" }}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = onClearFolder) { Text("Clear", color = MaterialTheme.colorScheme.onBackground) }
            }
        }
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("All", "Latest", "Movies", "TV Shows", "Anime", "Favourites").forEach { label ->
                FilterChip(
                    selected = category == label,
                    onClick = { category = label },
                    label = { Text(label) },
                    colors = androidx.compose.material3.FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.16f),
                        selectedLabelColor = MaterialTheme.colorScheme.onBackground,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        }
        if (!hasPermission) {
            EmptyState(
                backdrop,
                title = "Your videos stay on your device",
                message = "Allow video access to build your local library, or open a file directly.",
                action = "Allow video access",
                onAction = onRequestPermission,
                secondaryAction = "Choose a video file",
                onSecondaryAction = onChooseVideoFile
            )
        } else if (filtered.isEmpty()) {
            EmptyState(
                backdrop,
                title = when {
                    hasScanError -> "Library scan failed"
                    category == "Favourites" -> "No favourites yet"
                    videos.isEmpty() -> "No videos found"
                    else -> "Nothing matches"
                },
                message = when {
                    hasScanError -> "LUCENT couldn't read the video library. Scan again to retry."
                    category == "Favourites" -> "Favourite a video from the player tools and it will appear here."
                    videos.isEmpty() -> "Videos on your device will appear here after a media scan."
                    else -> "Try another filter or search term."
                },
                action = when {
                    category == "Favourites" -> null
                    hasScanError -> "Scan again"
                    else -> "Choose a video file"
                },
                onAction = when {
                    category == "Favourites" -> null
                    hasScanError -> onRescan
                    else -> onChooseVideoFile
                }
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 176.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                items(filtered, key = { it.id }) { item -> VideoRow(backdrop, item, onClick = { onPlay(item) }, onRequestActions = onRequestActions) }
            }
        }
    }
}

@Composable
private fun FoldersScreen(
    backdrop: LayerBackdrop,
    videos: List<VideoItem>,
    hasPermission: Boolean,
    onFolderSelected: (String?) -> Unit,
    onRescan: () -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        Text("Folder", Modifier.padding(start = 22.dp, end = 18.dp, top = 16.dp, bottom = 18.dp), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
        if (!hasPermission) {
            EmptyState(backdrop, title = "Video permission needed", message = "Allow access to group videos by their storage folders.", action = "Go to Videos", onAction = { onFolderSelected(null) })
        } else {
            val folders = videos.groupBy { it.folderIdentity }
                .toList()
                .sortedWith(
                    compareBy(
                        { it.first.substringAfter('|').lowercase(Locale.ROOT) },
                        { it.first.substringBefore('|').lowercase(Locale.ROOT) }
                    )
                )
            if (folders.isEmpty()) {
                EmptyState(
                    backdrop,
                    title = "No folders yet",
                    message = "Folders containing video files will appear here.",
                    action = "Refresh library",
                    onAction = onRescan
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 14.dp, top = 4.dp, end = 14.dp, bottom = 176.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(folders, key = { it.first }) { (identity, folderVideos) ->
                        val path = identity.substringAfter('|')
                        val name = path.substringAfterLast('/').ifBlank { "Internal storage" }
                        val breadcrumb = path.replace("/", " › ").ifBlank { "Internal storage" }
                        Column(
                            Modifier.fillMaxWidth().clickable { onFolderSelected(identity) }
                        ) {
                            Row(
                                Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 13.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Folder, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(54.dp))
                                Spacer(Modifier.width(16.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(name, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        "$breadcrumb · ${folderVideos.size} media files",
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsScreen(
    hardwareAcceleration: Boolean,
    onHardwareAccelerationChanged: (Boolean) -> Unit,
    gesturesEnabled: Boolean,
    onGesturesEnabledChanged: (Boolean) -> Unit,
    isDarkTheme: Boolean,
    onThemeChanged: (Boolean) -> Unit,
    onRescan: () -> Unit,
    onChooseVideoFile: () -> Unit,
    hasMediaManagementAccess: Boolean,
    onManageMediaAccess: () -> Unit,
    onShowInfoDialog: (String, String) -> Unit
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val appVersion = remember(context) {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "Unknown"
        }.getOrDefault("Unknown")
    }
    // A settings-only backdrop source containing the page underlay, not its controls.
    // This provides a safe external glass sample without capturing the toggles themselves.
    val toggleBackdrop = rememberLayerBackdrop()
    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxSize()
                .layerBackdrop(toggleBackdrop)
                .background(MaterialTheme.colorScheme.background)
        )
        Column(Modifier.fillMaxSize()) {
            Text(
                "Settings",
                Modifier.padding(start = 22.dp, end = 18.dp, top = 16.dp, bottom = 14.dp),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold
            )
            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.PlayArrow,
                        null,
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text(
                            "LUCENT",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "A quiet, glassy player for your local media",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 18.dp,
                    bottom = 148.dp
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    SettingsSection(
                        toggleBackdrop,
                        "Appearance",
                        listOf(
                            SettingItem(
                                Icons.Default.Settings,
                                "Dark theme",
                                "Use the black AMOLED appearance",
                                isDarkTheme,
                                onThemeChanged
                            )
                        )
                    )
                }
                item {
                    SettingsSection(
                        toggleBackdrop,
                        "Playback settings",
                        listOf(
                            SettingItem(
                                Icons.Default.Tune,
                                "Hardware acceleration",
                                "Use libVLC's hardware video decoder",
                                hardwareAcceleration,
                                onHardwareAccelerationChanged
                            ),
                            SettingItem(
                                Icons.Default.Settings,
                                "Playback gestures",
                                "Enable double-tap, swipe-to-seek, and brightness/volume gestures",
                                gesturesEnabled,
                                onGesturesEnabledChanged
                            )
                        )
                    )
                }
                item {
                    SettingsActionSection(
                        "Gesture guide",
                        listOf(
                            ActionItem(
                                Icons.Default.Tune,
                                "How playback gestures work",
                                "Double-tap, swipe-to-seek, brightness, volume and the seek bar",
                                {
                                    onShowInfoDialog(
                                        "Playback gesture guide",
                                        "• Double-tap the left or right side to rewind or advance 10 seconds.\n\n" +
                                            "• Double-tap the centre to play or pause.\n\n" +
                                            "• Swipe horizontally across the picture: left rewinds, right advances. A preview shows the target time; the seek is committed when you lift your finger. Cancelling the gesture does not seek.\n\n" +
                                            "• Swipe vertically on the left side to adjust screen brightness.\n\n" +
                                            "• Swipe vertically on the right side to adjust Android media volume. Hardware volume keys and the on-screen indicator use the same media stream.\n\n" +
                                            "• Tap or drag the timeline to seek precisely. It also exposes an adjustable progress control to accessibility services.\n\n" +
                                            "When player controls are locked, gestures are disabled until you unlock them. Turn off Playback gestures in Settings to disable the gesture shortcuts."
                                    )
                                }
                            )
                        )
                    )
                }
                item {
                    SettingsActionSection(
                        "Your library",
                        listOf(
                            ActionItem(
                                Icons.Default.History,
                                "Scan media",
                                "Refresh videos indexed on this device",
                                onRescan
                            ),
                            ActionItem(
                                Icons.Default.FolderOpen,
                                "Open video file",
                                "Pick a file using Android's file browser",
                                onChooseVideoFile
                            )
                        )
                    )
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    item {
                        SettingsActionSection(
                            "Media management",
                            listOf(
                                ActionItem(
                                    Icons.Default.Delete,
                                    "Direct media deletion",
                                    if (hasMediaManagementAccess) {
                                        "Enabled — LUCENT confirmation only"
                                    } else {
                                        "Grant once for LUCENT-only delete confirmation"
                                    },
                                    onManageMediaAccess
                                )
                            )
                        )
                    }
                }
                item {
                    SettingsActionSection(
                        "About & support",
                        listOf(
                            ActionItem(
                                Icons.Default.Info,
                                "About LUCENT",
                                "Version $appVersion · Local video playback",
                                {
                                    onShowInfoDialog(
                                        "About LUCENT",
                                        "LUCENT is an Android video player for media stored on your device. It discovers videos through Android's media library and can also open individual files. Playback and audio/subtitle decoding are handled by libVLC. Playback progress and per-video audio/subtitle preferences are saved locally.\n\nVersion $appVersion"
                                    )
                                }
                            ),
                            ActionItem(
                                Icons.Default.Favorite,
                                "Designed around Liquid Glass",
                                "Kyant0 Backdrop · Blur, refraction and highlights",
                                {
                                    onShowInfoDialog(
                                        "Liquid Glass design",
                                        "The interface uses Kyant0 Backdrop (version 2.0.1) for real backdrop blur, lens refraction, highlights and shadows in Jetpack Compose. The glass surfaces use neutral white, grey and black tones rather than a blue-tinted material."
                                    )
                                }
                            ),
                            ActionItem(
                                Icons.Default.Info,
                                "Third-party notices",
                                "Licences and component source information",
                                {
                                    val notices = runCatching {
                                        context.assets.open("THIRD_PARTY_NOTICES.md")
                                            .bufferedReader()
                                            .use { it.readText() }
                                    }.getOrElse { "Third-party notices are available in the LUCENT source repository." }
                                    onShowInfoDialog("Third-party notices", notices)
                                }
                            ),
                            ActionItem(
                                Icons.Default.Bookmark,
                                "Project source",
                                "Browse the source code on GitHub",
                                {
                                    runCatching {
                                        uriHandler.openUri("https://github.com/tejastajane/LUCENT-Android")
                                    }.onFailure {
                                        onShowInfoDialog(
                                            "Project source",
                                            "The project page could not be opened automatically. Visit: https://github.com/tejastajane/LUCENT-Android"
                                        )
                                    }
                                }
                            )
                        )
                    )
                }
            }
        }
    }
}



private data class SettingItem(
    val icon: ImageVector,
    val title: String,
    val description: String,
    val checked: Boolean,
    val onCheckedChange: (Boolean) -> Unit
)

private data class ActionItem(
    val icon: ImageVector,
    val title: String,
    val description: String,
    val onClick: () -> Unit
)

@Composable
private fun SettingsSection(backdrop: Backdrop, title: String, items: List<SettingItem>) {
    val dividerColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.10f)
    Column {
        Text(title, Modifier.padding(start = 4.dp, bottom = 8.dp), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.titleSmall)
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(MaterialTheme.colorScheme.surface)) {
            Column {
                items.forEachIndexed { index, item ->
                    Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(item.icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(item.title)
                            Text(item.description, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                        }
                        LiquidToggle(
                            checked = item.checked,
                            onCheckedChange = item.onCheckedChange,
                            backdrop = backdrop
                        )
                    }
                    if (index != items.lastIndex) androidx.compose.foundation.Canvas(Modifier.fillMaxWidth().padding(start = 50.dp, end = 14.dp).height(0.5.dp)) {
                        drawLine(dividerColor, androidx.compose.ui.geometry.Offset.Zero, androidx.compose.ui.geometry.Offset(size.width, 0f), strokeWidth = 1f)
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsActionSection(title: String, items: List<ActionItem>) {
    Column {
        Text(title, Modifier.padding(start = 4.dp, bottom = 8.dp), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.titleSmall)
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(MaterialTheme.colorScheme.surface)) {
            Column {
                items.forEach { item ->
                    Row(
                        Modifier.fillMaxWidth().clickable(onClick = item.onClick).padding(horizontal = 14.dp, vertical = 13.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(item.icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(item.title)
                            Text(item.description, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                        }
                        Text("›", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 26.sp)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VideoRow(
    backdrop: LayerBackdrop,
    item: VideoItem,
    onClick: () -> Unit,
    onRequestActions: (VideoItem) -> Unit
) {
    val context = LocalContext.current
    val thumbnail by produceState<Bitmap?>(initialValue = null, item.uri) {
        value = withContext(Dispatchers.IO) {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(context, item.uri)
                val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: item.durationMs
                retriever.getFrameAtTime(duration.coerceAtLeast(0L) * 100L, MediaMetadataRetriever.OPTION_CLOSEST)
                    ?: if (Build.VERSION.SDK_INT >= 29) runCatching { context.contentResolver.loadThumbnail(item.uri, Size(320, 180), null) }.getOrNull() else null
            } catch (_: Exception) {
                if (Build.VERSION.SDK_INT >= 29) runCatching { context.contentResolver.loadThumbnail(item.uri, Size(320, 180), null) }.getOrNull() else null
            } finally { runCatching { retriever.release() } }
        }
    }
    val preferences = remember(context) { context.getSharedPreferences("liquid_player", 0) }
    val progress = remember(item.uri, item.durationMs, preferences) {
        val saved = preferences.getLong("playback_position_" + item.uri.toString().hashCode(), 0L)
        if (item.durationMs > 0L) (saved.toFloat() / item.durationMs.toFloat()).coerceIn(0f, 1f) else 0f
    }

    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).clickable(onClick = onClick).padding(horizontal = 6.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.width(112.dp).height(70.dp).clip(RoundedCornerShape(13.dp)).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
            if (thumbnail != null) Image(thumbnail!!.asImageBitmap(), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            else Icon(Icons.Default.PlayArrow, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(30.dp))
            if (item.durationMs > 0L) Text(formatDuration(item.durationMs), Modifier.align(Alignment.BottomEnd).padding(4.dp).background(Color.Black.copy(alpha = 0.76f), RoundedCornerShape(5.dp)).padding(horizontal = 5.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall, color = Color.White, fontSize = 10.sp)
            Canvas(Modifier.align(Alignment.BottomStart).fillMaxWidth().height(2.dp)) {
                drawRect(Color.White.copy(alpha = 0.32f))
                if (progress > 0f) drawRect(Color.White, size = this.size.copy(width = this.size.width * progress))
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(item.name, maxLines = 2, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyLarge)
            Text(listOf(if (item.sizeBytes > 0L) Formatter.formatFileSize(context, item.sizeBytes) else "Local video", if (item.durationMs > 0L) formatDuration(item.durationMs) else null, item.mimeType?.substringAfter('/')?.uppercase(Locale.ROOT)).filterNotNull().joinToString("  ·  "), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(item.folderName, color = Color(0xFFAAAAAA), style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.width(2.dp))
        IconButton(onClick = { onRequestActions(item) }) {
            Icon(Icons.Default.MoreVert, contentDescription = "Video options", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(22.dp))
        }
    }


}

@Composable
private fun KyantOverlayScrim(onDismissRequest: () -> Unit) {
    val light = MaterialTheme.colorScheme.background.luminance() > 0.5f
    val tint = if (light) Color(0xFF29293A).copy(alpha = 0.23f) else Color(0xFF121212).copy(alpha = 0.56f)
    Box(Modifier.fillMaxSize().background(tint).clickable(onClick = onDismissRequest))
}

@Composable
private fun KyantButton(
    label: String,
    primary: Boolean,
    light: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val foreground = if (light) Color.Black else Color.White
    val accent = if (light) Color(0xFF0088FF) else Color(0xFF0091FF)
    val tint = if (light) Color(0xFFFAFAFA).copy(alpha = 0.6f) else Color(0xFF121212).copy(alpha = 0.4f)
    Row(
        modifier.clip(Capsule()).background(if (primary) accent else tint.copy(alpha = 0.2f))
            .clickable(onClick = onClick).height(48.dp).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Text(label, color = if (primary) Color.White else foreground, fontSize = 16.sp, maxLines = 1)
    }
}

@Composable
private fun KyantDialogCard(
    backdrop: Backdrop,
    title: String,
    onDismissRequest: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
    actions: @Composable RowScope.() -> Unit
) {
    Box(Modifier.fillMaxSize()) {
        KyantOverlayScrim(onDismissRequest)
        val interaction = remember { MutableInteractionSource() }
        KyantGlassSurface(
            backdrop,
            modifier = Modifier.align(Alignment.Center).padding(horizontal = 32.dp)
                .widthIn(max = 520.dp).fillMaxWidth()
                .clickable(interactionSource = interaction, indication = null) {},
            shape = RoundedRectangle(48.dp)
        ) {
            Column(Modifier.fillMaxWidth()) {
                Text(
                    title,
                    Modifier.padding(start = 28.dp, top = 24.dp, end = 28.dp, bottom = 12.dp),
                    color = if (MaterialTheme.colorScheme.background.luminance() > 0.5f) Color.Black else Color.White,
                    style = MaterialTheme.typography.headlineSmall.copy(fontSize = 24.sp, fontWeight = FontWeight.Medium)
                )
                content()
                Row(
                    Modifier.padding(start = 24.dp, top = 12.dp, end = 24.dp, bottom = 24.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    content = actions
                )
            }
        }
    }
}

@Composable
private fun KyantMessageDialog(
    backdrop: Backdrop,
    title: String,
    message: String,
    cancelLabel: String?,
    confirmLabel: String,
    onDismissRequest: () -> Unit,
    onCancel: () -> Unit,
    onConfirm: () -> Unit
) {
    val light = MaterialTheme.colorScheme.background.luminance() > 0.5f
    val foreground = if (light) Color.Black else Color.White
    KyantDialogCard(
        backdrop, title, onDismissRequest,
        content = {
            Text(
                message,
                Modifier.then(if (light) Modifier else Modifier.graphicsLayer(blendMode = BlendMode.Plus))
                    .padding(horizontal = 24.dp, vertical = 12.dp).heightIn(max = 300.dp)
                    .verticalScroll(rememberScrollState()),
                color = foreground.copy(alpha = 0.68f),
                style = MaterialTheme.typography.bodyMedium
            )
        },
        actions = {
            if (cancelLabel != null) KyantButton(cancelLabel, false, light, onCancel, Modifier.weight(1f))
            else Spacer(Modifier.weight(1f))
            KyantButton(confirmLabel, true, light, onConfirm, Modifier.weight(1f))
        }
    )
}

@Composable
private fun KyantRenameDialog(
    backdrop: Backdrop,
    item: VideoItem,
    onDismissRequest: () -> Unit,
    onSave: (VideoItem, String) -> Unit
) {
    val light = MaterialTheme.colorScheme.background.luminance() > 0.5f
    val foreground = if (light) Color.Black else Color.White
    var value by remember(item.uri, item.name) { mutableStateOf(item.name) }
    KyantDialogCard(
        backdrop, "Rename video", onDismissRequest,
        content = {
            Column(Modifier.padding(horizontal = 24.dp, vertical = 12.dp)) {
                Text("File name", color = foreground.copy(alpha = 0.68f), style = MaterialTheme.typography.labelMedium)
                BasicTextField(
                    value = value,
                    onValueChange = { value = it },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = foreground),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                        .background(if (light) Color.Black.copy(alpha = 0.04f) else Color.White.copy(alpha = 0.06f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 14.dp)
                )
            }
        },
        actions = {
            KyantButton("Cancel", false, light, onDismissRequest, Modifier.weight(1f))
            KyantButton(
                "Save", true, light,
                onClick = {
                    val requested = value.trim()
                    if (requested.isNotBlank()) {
                        val ext = item.name.substringAfterLast('.', "")
                        onSave(item, if (ext.isNotBlank() && !requested.contains('.')) "$requested.$ext" else requested)
                    }
                },
                modifier = Modifier.weight(1f)
            )
        }
    )
}

@Composable
private fun KyantVideoActionSheet(
    backdrop: Backdrop,
    onDismissRequest: () -> Unit,
    onInformation: () -> Unit,
    onRename: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit
) {
    Box(Modifier.fillMaxSize()) {
        KyantOverlayScrim(onDismissRequest)
        val interaction = remember { MutableInteractionSource() }
        KyantGlassSurface(
            backdrop,
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                .clickable(interactionSource = interaction, indication = null) {},
            shape = RoundedCornerShape(topStart = 40.dp, topEnd = 40.dp, bottomStart = 0.dp, bottomEnd = 0.dp)
        ) {
            Column(
                Modifier.fillMaxWidth().navigationBarsPadding()
                    .padding(start = 10.dp, end = 10.dp, top = 12.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Box(
                    Modifier.align(Alignment.CenterHorizontally).padding(bottom = 8.dp)
                        .width(36.dp).height(4.dp).clip(Capsule())
                        .background(if (MaterialTheme.colorScheme.background.luminance() > 0.5f) Color.Black.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.22f))
                )
                VideoActionItem(Icons.Default.Info, "Information", onInformation)
                VideoActionItem(Icons.Default.Edit, "Rename", onRename)
                VideoActionItem(Icons.Default.Share, "Share", onShare)
                VideoActionItem(Icons.Default.Delete, "Delete", onDelete)
            }
        }
    }
}

private fun videoInformationText(context: android.content.Context, item: VideoItem): String =
    buildString {
        appendLine("File name: " + item.name)
        appendLine("Folder: " + item.folderName)
        appendLine("Location: " + item.relativePath)
        if (item.sizeBytes > 0L) appendLine("Size: " + Formatter.formatFileSize(context, item.sizeBytes))
        if (item.durationMs > 0L) appendLine("Duration: " + formatDuration(item.durationMs))
        item.mimeType?.let { appendLine("Format: " + it.substringAfter('/').uppercase(Locale.ROOT)) }
        if (item.dateAddedSeconds > 0L) append("Added: " + DateFormat.getDateTimeInstance().format(Date(item.dateAddedSeconds * 1_000L)))
    }

@Composable
private fun VideoActionItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        val tint = if (label == "Delete") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
        Text(label, color = tint, style = MaterialTheme.typography.bodyLarge)
    }
}
@Composable
private fun EmptyState(
    backdrop: LayerBackdrop,
    title: String,
    message: String,
    action: String? = null,
    onAction: (() -> Unit)? = null,
    secondaryAction: String? = null,
    onSecondaryAction: (() -> Unit)? = null
) {
    Box(Modifier.fillMaxSize().padding(22.dp), contentAlignment = Alignment.Center) {
        Column(Modifier.fillMaxWidth()) {
            Column(Modifier.fillMaxWidth().padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Default.Movie, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(42.dp))
                Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                if (action != null && onAction != null) {
                    Button(
                        onClick = onAction,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.onBackground,
                            contentColor = MaterialTheme.colorScheme.background
                        )
                    ) { Text(action) }
                }
                if (secondaryAction != null && onSecondaryAction != null) {
                    OutlinedButton(onClick = onSecondaryAction, colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onBackground)) { Text(secondaryAction) }
                }
            }
        }
    }
}

@Composable
private fun BottomNavigation(
    backdrop: Backdrop,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    searchExpanded: Boolean,
    onSearchExpandedChange: (Boolean) -> Unit,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    val navItems = listOf(
        Icons.Default.Movie to "Videos",
        Icons.Default.Folder to "Folders",
        Icons.Default.Settings to "Settings"
    )
    val active = navItems[selectedTab.coerceIn(0, navItems.lastIndex)]
    val isLightTheme = MaterialTheme.colorScheme.background.luminance() > 0.5f
    val activeIconColor = if (isLightTheme) Color(0xFF0A84FF) else Color.White
    val inactiveIconColor = MaterialTheme.colorScheme.onSurfaceVariant

    BoxWithConstraints(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        // Keep a single navigation surface alive during the morph: its selected
        // glass pill grows into the compact active-tab button instead of being replaced.
        val groupWidth = maxWidth * 0.69f + 84.dp
        val transition = updateTransition(targetState = searchExpanded, label = "navbar-search-morph")
        val tabsWidth by transition.animateDp(
            transitionSpec = { spring(dampingRatio = 0.88f, stiffness = 320f) },
            label = "tabs-width"
        ) { expanded -> if (expanded) 62.dp else maxWidth * 0.69f }
        val tabsHeight by transition.animateDp(
            transitionSpec = { spring(dampingRatio = 0.88f, stiffness = 360f) },
            label = "tabs-height"
        ) { expanded -> if (expanded) 62.dp else 72.dp }
        val collapseProgress by transition.animateFloat(
            transitionSpec = { spring(dampingRatio = 0.88f, stiffness = 360f) },
            label = "navbar-collapse-progress"
        ) { expanded -> if (expanded) 1f else 0f }
        val searchWidth by transition.animateDp(
            transitionSpec = { spring(dampingRatio = 0.84f, stiffness = 440f) },
            label = "search-width"
        ) { expanded -> if (expanded) maxWidth * 0.69f + 10.dp else 72.dp }
        val searchHeight by transition.animateDp(
            transitionSpec = { spring(dampingRatio = 0.88f, stiffness = 360f) },
            label = "search-height"
        ) { expanded -> if (expanded) 62.dp else 72.dp }

        Row(
            Modifier.width(groupWidth),
            horizontalArrangement = Arrangement.spacedBy(0.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LiquidBottomTabs(
                selectedTabIndex = selectedTab.coerceIn(0, navItems.lastIndex),
                onTabSelected = onTabSelected,
                backdrop = backdrop,
                tabsCount = navItems.size,
                modifier = Modifier.width(tabsWidth).height(tabsHeight),
                collapseProgress = collapseProgress,
                activeContent = { index ->
                    val (icon, label) = navItems[index.coerceIn(0, navItems.lastIndex)]
                    Icon(imageVector = icon, contentDescription = label, tint = activeIconColor, modifier = Modifier.size(26.dp))
                },
                activeLabelContent = { index ->
                    Text(
                        text = navItems[index.coerceIn(0, navItems.lastIndex)].second,
                        color = activeIconColor,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        maxLines = 1
                    )
                },
                onCollapsedClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    onSearchQueryChange("")
                    onSearchExpandedChange(false)
                },
                collapsedContent = {
                    Icon(
                        imageVector = active.first,
                        contentDescription = "Return to ${active.second}",
                        tint = activeIconColor,
                        modifier = Modifier.size(26.dp)
                    )
                }
            ) {
                navItems.forEachIndexed { index, (icon, label) ->
                    LiquidBottomTab(onClick = { onTabSelected(index) }) {
                        Icon(
                            imageVector = icon,
                            contentDescription = label,
                            tint = inactiveIconColor,
                            modifier = Modifier.size(26.dp).graphicsLayer {
                                alpha = if (selectedTab == index) 0f else 1f - collapseProgress
                            }
                        )
                        Text(
                            label,
                            color = if (selectedTab == index) activeIconColor else MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            maxLines = 1,
                            modifier = Modifier.graphicsLayer {
                                alpha = if (selectedTab == index) 0f else 1f - collapseProgress
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.width(12.dp))

            val searchModifier = Modifier.width(searchWidth).height(searchHeight).clip(
                if (searchExpanded) RoundedCornerShape(50.dp) else CircleShape
            ).then(
                if (!searchExpanded) Modifier.clickable {
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    onSearchExpandedChange(true)
                } else Modifier
            )
            GlassSurface(
                backdrop = backdrop,
                modifier = searchModifier,
                cornerRadius = 50.dp,
                surfaceAlpha = 0.055f,
                navMaterial = true
            ) {
                AnimatedContent(
                    targetState = searchExpanded,
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                    transitionSpec = {
                        if (targetState) {
                            (fadeIn(tween(150, delayMillis = 70)) + slideInHorizontally(tween(230)) { it / 8 }) togetherWith
                                (fadeOut(tween(90)) + slideOutHorizontally(tween(150)) { -it / 8 })
                        } else {
                            (fadeIn(tween(150, delayMillis = 40)) + slideInHorizontally(tween(230)) { -it / 8 }) togetherWith
                                (fadeOut(tween(90)) + slideOutHorizontally(tween(150)) { it / 8 })
                        }
                    },
                    label = "search-button-to-field"
                ) { expanded ->
                    if (!expanded) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Search, contentDescription = "Search library", tint = MaterialTheme.colorScheme.onBackground, modifier = Modifier.size(26.dp))
                        }
                    } else {
                        Row(
                            Modifier.fillMaxSize().padding(horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = {
                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                onSearchQueryChange("")
                                onSearchExpandedChange(false)
                            }) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Collapse search", tint = MaterialTheme.colorScheme.onBackground)
                            }
                            BasicTextField(
                                value = searchQuery,
                                onValueChange = onSearchQueryChange,
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onBackground),
                                cursorBrush = SolidColor(MaterialTheme.colorScheme.onBackground),
                                decorationBox = { inner ->
                                    Box {
                                        if (searchQuery.isEmpty()) {
                                            Text("Search library…", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyLarge)
                                        }
                                        inner()
                                    }
                                }
                            )
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = {
                                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                    onSearchQueryChange("")
                                }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear search", tint = MaterialTheme.colorScheme.onBackground)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}


private val tvEpisodeNamePattern = Regex(
    """(?i)(?:\bs\d{1,2}\s*[._ -]*e\d{1,3}\b|\b\d{1,2}\s*x\s*\d{1,3}\b|\b(?:season|episode|ep)\s*[._ #:-]*\d{1,3}\b|\b\d{1,2}e\d{1,3}\b|^\s*(?:episode|ep)[ ._-]*\d{1,3}\b)"""
)
private val tvFolderPattern = Regex(
    """(?i)^(?:tv|tv shows?|shows?|series|web[ ._-]*series|episodes?|season[ ._-]*\d+|s\d{1,2}|anime series|cartoon series)$"""
)

private fun isLikelyTvEpisode(name: String, pathSegments: List<String>): Boolean {
    val stem = name.substringBeforeLast('.', name).lowercase(Locale.ROOT).replace('_', ' ')
    // Some downloads use leading episode numbers inside per-show folders.
    val numberedEpisode = Regex("""(?i)^\s*\[?\d{1,3}\]?\s*[._ -]+""").containsMatchIn(stem) &&
        pathSegments.any { it.contains("season") || it.contains("series") || it.contains("tv") || it.contains("anime") }
    return tvEpisodeNamePattern.containsMatchIn(stem) ||
        numberedEpisode ||
        pathSegments.any { segment -> tvFolderPattern.matches(segment) }
}


@Composable
private fun ResumePlaybackPill(
    backdrop: Backdrop,
    hasResumeHistory: Boolean,
    onClick: () -> Unit
) {
    GlassSurface(
        backdrop = backdrop,
        modifier = Modifier.clickable(
            interactionSource = null,
            indication = null,
            onClick = onClick
        ),
        cornerRadius = 50.dp,
        surfaceAlpha = 0.055f,
        navMaterial = true,
        clampNavLensToSurfaceHeight = true,
        dropShadowElevation = 12.dp
    ) {
        Row(
            Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            Icon(
                Icons.Default.PlayArrow,
                if (hasResumeHistory) "Resume video" else "Play video",
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.size(21.dp)
            )
            Text(
                if (hasResumeHistory) "Resume" else "Play",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}

private val nonMovieFolderPattern = Regex(
    """(?i)^(?:dcim|camera|screenshots?|screen[ ._-]*recordings?|screenrecordings?|recordings?|whatsapp(?:[ ._-]*videos?)?|telegram(?:[ ._-]*videos?)?|instagram|reels?|shorts?|clips?|statuses?|memes?)$"""
)
