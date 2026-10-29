package com.tejas.liquidplayer.ui

import android.graphics.Bitmap
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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.tejas.liquidplayer.ui.liquid.LiquidBottomTab
import com.tejas.liquidplayer.ui.liquid.LiquidBottomTabs
import com.tejas.liquidplayer.data.VideoItem
import com.tejas.liquidplayer.data.formatDuration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

@Composable
fun MainLibraryUi(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    videos: List<VideoItem>,
    hasVideoPermission: Boolean,
    onRequestMediaPermission: () -> Unit,
    onPlay: (VideoItem) -> Unit,
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
            Column(Modifier.fillMaxSize().statusBarsPadding()) {
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
                        0 -> VideosScreen(backdrop, videos, hasVideoPermission, onRequestMediaPermission, selectedFolder, onClearFolder = { selectedFolder = null }, onPlay = onPlay, onChooseVideoFile = onChooseVideoFile)
                        1 -> FoldersScreen(backdrop, videos, hasVideoPermission, onFolderSelected = { folder -> selectedFolder = folder; onTabSelected(0) })
                        3 -> SearchResultsScreen(backdrop, videos, searchQuery, hasVideoPermission, onRequestMediaPermission, onPlay)
                        else -> SettingsScreen(
                            backdrop = backdrop,
                            hardwareAcceleration = hardwareAcceleration,
                            onHardwareAccelerationChanged = onHardwareAccelerationChanged,
                            gesturesEnabled = gesturesEnabled,
                            onGesturesEnabledChanged = onGesturesEnabledChanged,
                            isDarkTheme = isDarkTheme,
                            onThemeChanged = onThemeChanged,
                            onRescan = onRescan,
                            onChooseVideoFile = onChooseVideoFile
                        )
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
    }
}

@Composable
private fun SearchResultsScreen(
    backdrop: LayerBackdrop,
    videos: List<VideoItem>,
    query: String,
    hasPermission: Boolean,
    onRequestPermission: () -> Unit,
    onPlay: (VideoItem) -> Unit
) {
    val filtered = remember(videos, query) { videos.filter { it.name.contains(query, ignoreCase = true) || it.relativePath.contains(query, ignoreCase = true) } }
    Column(Modifier.fillMaxSize()) {
        Text("Search results", Modifier.padding(start = 22.dp, end = 18.dp, top = 14.dp, bottom = 8.dp), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
        if (!hasPermission) EmptyState(backdrop, "Video permission needed", "Allow access to search local videos.", "Allow access", onRequestPermission)
        else if (filtered.isEmpty()) EmptyState(backdrop, "No matches", "Try another title or folder name.", "No results", {})
        else LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 150.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            items(filtered, key = { it.id }) { item -> VideoRow(item, onClick = { onPlay(item) }) }
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
    onChooseVideoFile: () -> Unit
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
            "Movies" -> !looksLikeTvEpisode && !isAnime && !isNonMovieFolder
            "TV Shows" -> looksLikeTvEpisode
            "Anime" -> isAnime
            "Recent" -> true
            else -> true
        }
        matchesCategory && (selectedFolder.isNullOrBlank() || item.folderName == selectedFolder)
    }.let { items ->
        if (category == "Recent") items.sortedByDescending(VideoItem::dateAddedSeconds) else items
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(start = 22.dp, end = 18.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Videos", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            IconButton(onClick = { category = if (category == "All") "Recent" else "All" }) {
                Icon(Icons.Default.Tune, contentDescription = "Sort videos", tint = MaterialTheme.colorScheme.onBackground)
            }
        }
        if (!selectedFolder.isNullOrBlank()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Folder: $selectedFolder", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                TextButton(onClick = onClearFolder) { Text("Clear", color = MaterialTheme.colorScheme.onBackground) }
            }
        }
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("All", "Movies", "TV Shows", "Anime", "Recent").forEach { label ->
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
                title = if (videos.isEmpty()) "No videos found" else "Nothing matches",
                message = if (videos.isEmpty()) "Videos on your device will appear here after a media scan." else "Try another filter or search term.",
                action = "Choose a video file",
                onAction = onChooseVideoFile
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                items(filtered, key = { it.id }) { item -> VideoRow(item, onClick = { onPlay(item) }) }
            }
        }
    }
}

@Composable
private fun FoldersScreen(
    backdrop: LayerBackdrop,
    videos: List<VideoItem>,
    hasPermission: Boolean,
    onFolderSelected: (String) -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        Text("Folder", Modifier.padding(start = 22.dp, end = 18.dp, top = 16.dp, bottom = 18.dp), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
        if (!hasPermission) {
            EmptyState(backdrop, title = "Video permission needed", message = "Allow access to group videos by their storage folders.", action = "Go to Videos", onAction = { onFolderSelected("") })
        } else {
            val folders = videos.groupBy { it.folderName }.toList().sortedBy { it.first.lowercase(Locale.ROOT) }
            if (folders.isEmpty()) {
                EmptyState(backdrop, title = "No folders yet", message = "Folders containing video files will appear here.", action = "Refresh library", onAction = { onFolderSelected("") })
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(folders, key = { it.first }) { (name, folderVideos) ->
                        Column(Modifier.fillMaxWidth().clickable { onFolderSelected(name) }) {
                            Row(
                                Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 13.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Folder, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(54.dp))
                                Spacer(Modifier.width(16.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(name, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleMedium)
                                    Text("${folderVideos.size} media files", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                                }
                                Icon(Icons.Default.ViewList, contentDescription = "Open folder", tint = MaterialTheme.colorScheme.onSurfaceVariant)
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
    backdrop: LayerBackdrop,
    hardwareAcceleration: Boolean,
    onHardwareAccelerationChanged: (Boolean) -> Unit,
    gesturesEnabled: Boolean,
    onGesturesEnabledChanged: (Boolean) -> Unit,
    isDarkTheme: Boolean,
    onThemeChanged: (Boolean) -> Unit,
    onRescan: () -> Unit,
    onChooseVideoFile: () -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        Text("Settings", Modifier.padding(start = 22.dp, end = 18.dp, top = 16.dp, bottom = 14.dp), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.PlayArrow, null, tint = MaterialTheme.colorScheme.onBackground, modifier = Modifier.size(48.dp))
                Spacer(Modifier.width(14.dp))
                Column {
                    Text("Liquid Player", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    Text("A quiet, glassy player for your local media", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                SettingsSection(backdrop, "Appearance", listOf(
                    SettingItem(Icons.Default.Settings, "Dark theme", "Use the black AMOLED appearance", isDarkTheme, onThemeChanged)
                ))
            }
            item {
                SettingsSection(backdrop, "Playback settings", listOf(
                    SettingItem(Icons.Default.Tune, "Hardware acceleration", "Use libVLC's hardware video decoder", hardwareAcceleration, onHardwareAccelerationChanged),
                    SettingItem(Icons.Default.Settings, "Playback gestures", "Enable double-tap seeking and touch shortcuts", gesturesEnabled, onGesturesEnabledChanged)
                ))
            }
            item {
                SettingsActionSection(
                    backdrop, "Your library",
                    listOf(
                        ActionItem(Icons.Default.History, "Scan media", "Refresh videos indexed on this device", onRescan),
                        ActionItem(Icons.Default.FolderOpen, "Open video file", "Pick a file using Android's file browser", onChooseVideoFile)
                    )
                )
            }
            item {
                SettingsActionSection(
                    backdrop, "Player features",
                    listOf(
                        ActionItem(Icons.Default.Audiotrack, "Audio tracks", "Choose an audio language while playing", {}),
                        ActionItem(Icons.Default.ClosedCaption, "Subtitles", "Embedded and external subtitle tracks", {}),
                    )
                )
            }
            item {
                SettingsActionSection(
                    backdrop, "About & support",
                    listOf(
                        ActionItem(Icons.Default.Info, "About Liquid Player", "Kotlin · Jetpack Compose · libVLC", {}),
                        ActionItem(Icons.Default.Favorite, "Designed around Liquid Glass", "Neutral transparent glass, no blue tint", {}),
                        ActionItem(Icons.Default.Bookmark, "Project source", "Open the Liquid Player GitHub repository", {})
                    )
                )
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
private fun SettingsSection(backdrop: LayerBackdrop, title: String, items: List<SettingItem>) {
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
                        Switch(
                            checked = item.checked,
                            onCheckedChange = item.onCheckedChange,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.background,
                                checkedTrackColor = MaterialTheme.colorScheme.onBackground,
                                uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                uncheckedTrackColor = Color.DarkGray
                            )
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
private fun SettingsActionSection(backdrop: LayerBackdrop, title: String, items: List<ActionItem>) {
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

@Composable
private fun VideoRow(item: VideoItem, onClick: () -> Unit) {
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
        val saved = preferences.getLong("playback_position_${item.uri.toString().hashCode()}", 0L)
        if (item.durationMs > 0L) (saved.toFloat() / item.durationMs.toFloat()).coerceIn(0f, 1f) else 0f
    }
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).clickable(onClick = onClick).padding(horizontal = 6.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
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
        Spacer(Modifier.width(5.dp))
        Icon(Icons.Default.MoreHoriz, contentDescription = "Video options", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun EmptyState(
    backdrop: LayerBackdrop,
    title: String,
    message: String,
    action: String,
    onAction: () -> Unit,
    secondaryAction: String? = null,
    onSecondaryAction: (() -> Unit)? = null
) {
    Box(Modifier.fillMaxSize().padding(22.dp), contentAlignment = Alignment.Center) {
        Column(Modifier.fillMaxWidth()) {
            Column(Modifier.fillMaxWidth().padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Default.Movie, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(42.dp))
                Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                Button(onClick = onAction, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onBackground, contentColor = MaterialTheme.colorScheme.background)) { Text(action) }
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
    val activeLabelColor = if (isLightTheme) Color(0xFF1D1D1F) else Color.White
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
                            modifier = Modifier.size(26.dp).graphicsLayer { alpha = 1f - collapseProgress }
                        )
                        Text(
                            label,
                            color = if (selectedTab == index) activeLabelColor else MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            maxLines = 1,
                            modifier = Modifier.graphicsLayer { alpha = 1f - collapseProgress }
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
        navMaterial = true
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
