package com.tejas.liquidplayer.ui

import android.graphics.Bitmap
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.text.format.Formatter
import android.util.Size
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.Storage
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.documentfile.provider.DocumentFile
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.tejas.liquidplayer.ui.liquid.LiquidBottomTab
import com.tejas.liquidplayer.ui.liquid.LiquidBottomTabs
import com.tejas.liquidplayer.data.BrowseEntry
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
    selectedFolder: String?,
    onFolderSelected: (String) -> Unit,
    storageTreeUri: android.net.Uri?,
    browsePath: List<String>,
    browseEntries: List<BrowseEntry>,
    onChooseStorageFolder: () -> Unit,
    onBrowseBack: () -> Unit,
    onBrowseEntry: (BrowseEntry) -> Unit,
    onPlay: (VideoItem) -> Unit,
    onChooseVideoFile: () -> Unit,
    hardwareAcceleration: Boolean,
    onHardwareAccelerationChanged: (Boolean) -> Unit,
    gesturesEnabled: Boolean,
    onGesturesEnabledChanged: (Boolean) -> Unit,
    onRescan: () -> Unit
) {
    val backdrop = rememberLayerBackdrop()
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        // Capture a neutral black surface behind the actual Kyant0 backdrop controls.
        Box(
            Modifier.fillMaxSize().layerBackdrop(backdrop).background(Color.Black)
        )
        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()
        ) {
            Box(Modifier.weight(1f).fillMaxWidth()) {
                when (selectedTab) {
                    0 -> VideosScreen(
                        backdrop, videos, hasVideoPermission, onRequestMediaPermission,
                        selectedFolder, onClearFolder = { onFolderSelected("") }, onPlay = onPlay,
                        onChooseVideoFile = onChooseVideoFile
                    )
                    1 -> FoldersScreen(backdrop, videos, hasVideoPermission, onFolderSelected)
                    2 -> BrowseScreen(
                        backdrop = backdrop,
                        videos = videos,
                        treeUri = storageTreeUri,
                        path = browsePath,
                        entries = browseEntries,
                        onChooseFolder = onChooseStorageFolder,
                        onBack = onBrowseBack,
                        onEntry = onBrowseEntry,
                        onPlay = onPlay
                    )
                    else -> MoreScreen(
                        backdrop = backdrop,
                        hardwareAcceleration = hardwareAcceleration,
                        onHardwareAccelerationChanged = onHardwareAccelerationChanged,
                        gesturesEnabled = gesturesEnabled,
                        onGesturesEnabledChanged = onGesturesEnabledChanged,
                        onRescan = onRescan,
                        onChooseVideoFile = onChooseVideoFile
                    )
                }
            }
            BottomNavigation(backdrop, selectedTab, onTabSelected)
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
    var searchVisible by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf("All") }
    val filtered = videos.filter { item ->
        val name = item.name.lowercase(Locale.ROOT)
        val matchesQuery = query.isBlank() || name.contains(query.lowercase(Locale.ROOT))
        val matchesCategory = when (category) {
            "Movies" -> !Regex("s\\d{1,2}e\\d{1,2}", RegexOption.IGNORE_CASE).containsMatchIn(name)
            "TV Shows" -> Regex("s\\d{1,2}e\\d{1,2}", RegexOption.IGNORE_CASE).containsMatchIn(name)
            "Anime" -> listOf("anime", "jujutsu", "naruto", "one.piece", "bleach", "attack.on", "demon.slayer", "suzume", "spirited.away", "frieren", "ippo").any(name::contains)
            "Recent" -> true
            else -> true
        }
        matchesQuery && matchesCategory && (selectedFolder.isNullOrBlank() || item.folderName == selectedFolder)
    }.let { if (category == "Recent") it.sortedByDescending(VideoItem::dateAddedSeconds) else it }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(start = 22.dp, end = 18.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Videos", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            IconButton(onClick = { searchVisible = !searchVisible; if (!searchVisible) query = "" }) {
                Icon(Icons.Default.Search, contentDescription = "Search videos", tint = Color.White)
            }
            IconButton(onClick = { category = if (category == "All") "Recent" else "All" }) {
                Icon(Icons.Default.Tune, contentDescription = "Sort videos", tint = Color.White)
            }
        }
        if (searchVisible) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp),
                singleLine = true,
                placeholder = { Text("Search videos", color = Color.Gray) },
                shape = RoundedCornerShape(18.dp)
            )
        }
        if (!selectedFolder.isNullOrBlank()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Folder: $selectedFolder", color = Color.LightGray, modifier = Modifier.weight(1f))
                TextButton(onClick = onClearFolder) { Text("Clear", color = Color.White) }
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
                        selectedContainerColor = Color.White.copy(alpha = 0.16f),
                        selectedLabelColor = Color.White,
                        containerColor = Color(0xFF101010),
                        labelColor = Color.LightGray
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
                        GlassSurface(
                            backdrop,
                            modifier = Modifier.fillMaxWidth().clickable { onFolderSelected(name) },
                            cornerRadius = 22.dp,
                            surfaceAlpha = 0.045f
                        ) {
                            Row(
                                Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 13.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Folder, null, tint = Color(0xFFD2D2D2), modifier = Modifier.size(54.dp))
                                Spacer(Modifier.width(16.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(name, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleMedium)
                                    Text("${folderVideos.size} media files", color = Color.LightGray, style = MaterialTheme.typography.bodyMedium)
                                }
                                Icon(Icons.Default.ViewList, contentDescription = "Open folder", tint = Color.Gray)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BrowseScreen(
    backdrop: LayerBackdrop,
    videos: List<VideoItem>,
    treeUri: android.net.Uri?,
    path: List<String>,
    entries: List<BrowseEntry>,
    onChooseFolder: () -> Unit,
    onBack: () -> Unit,
    onEntry: (BrowseEntry) -> Unit,
    onPlay: (VideoItem) -> Unit
) {
    val context = LocalContext.current
    val storage = remember {
        runCatching {
            val stat = StatFs(Environment.getExternalStorageDirectory().absolutePath)
            stat.totalBytes to stat.availableBytes
        }.getOrDefault(0L to 0L)
    }
    val used = (storage.first - storage.second).coerceAtLeast(0L)
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(start = 22.dp, end = 18.dp, top = 8.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Browse", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            IconButton(onClick = onChooseFolder) { Icon(Icons.Default.FolderOpen, contentDescription = "Choose storage folder", tint = Color.White) }
        }
        GlassSurface(backdrop, Modifier.fillMaxWidth().padding(horizontal = 16.dp), cornerRadius = 26.dp, surfaceAlpha = 0.07f) {
            Column(Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Storage, null, tint = Color.White, modifier = Modifier.size(34.dp))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Internal storage", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
                        Text(Formatter.formatFileSize(context, storage.first) + " total", color = Color.LightGray, style = MaterialTheme.typography.bodySmall)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(Formatter.formatFileSize(context, used) + " used", style = MaterialTheme.typography.bodyMedium)
                        Text(Formatter.formatFileSize(context, storage.second) + " free", color = Color.LightGray, style = MaterialTheme.typography.bodySmall)
                    }
                }
                Spacer(Modifier.height(12.dp))
                Box(Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(50)).background(Color.White.copy(alpha = 0.12f))) {
                    Box(Modifier.fillMaxWidth(used.toFloat() / storage.first.coerceAtLeast(1L).toFloat()).height(5.dp).clip(RoundedCornerShape(50)).background(Color.White.copy(alpha = 0.8f)))
                }
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Browse folders and files", color = Color.LightGray, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                    Button(
                        onClick = onChooseFolder,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)
                    ) { Text(if (treeUri == null) "Open storage" else "Change folder") }
                }
            }
        }
        if (treeUri == null) {
            Text("Video folders", Modifier.padding(start = 20.dp, top = 18.dp, bottom = 8.dp), style = MaterialTheme.typography.titleMedium)
            val folderGroups = videos.groupBy { it.folderName }.toList().sortedBy { it.first }
            if (folderGroups.isEmpty()) {
                Text("Choose Open storage to browse directories on your device.", Modifier.padding(horizontal = 22.dp, vertical = 12.dp), color = Color.LightGray)
            } else {
                LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    items(folderGroups, key = { it.first }) { (name, grouped) ->
                        GlassSurface(backdrop, Modifier.fillMaxWidth().clickable { onPlay(grouped.first()) }, cornerRadius = 18.dp, surfaceAlpha = 0.035f) {
                            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Folder, null, tint = Color.LightGray, modifier = Modifier.size(28.dp))
                                Spacer(Modifier.width(12.dp))
                                Text(name, Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("${grouped.size}", color = Color.Gray)
                            }
                        }
                    }
                }
            }
        } else {
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                if (path.isNotEmpty()) {
                    TextButton(onClick = onBack) { Text("‹ Back", color = Color.White) }
                    Spacer(Modifier.width(8.dp))
                }
                Text((path.lastOrNull() ?: "Storage root"), color = Color.LightGray, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (entries.isEmpty()) {
                EmptyState(backdrop, "This folder is empty", "Choose another folder or open a video directly.", "Change folder", onChooseFolder)
            } else {
                LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(entries, key = { it.uri.toString() }) { entry ->
                        val icon = if (entry.isDirectory) Icons.Default.Folder else if (entry.mimeType?.startsWith("video/") == true) Icons.Default.Movie else Icons.Default.InsertDriveFile
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).clickable { onEntry(entry) }.padding(horizontal = 12.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(icon, null, tint = Color.LightGray, modifier = Modifier.size(30.dp))
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(entry.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(if (entry.isDirectory) "Folder" else if (entry.sizeBytes > 0L) Formatter.formatFileSize(context, entry.sizeBytes) else (entry.mimeType ?: "File"), color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MoreScreen(
    backdrop: LayerBackdrop,
    hardwareAcceleration: Boolean,
    onHardwareAccelerationChanged: (Boolean) -> Unit,
    gesturesEnabled: Boolean,
    onGesturesEnabledChanged: (Boolean) -> Unit,
    onRescan: () -> Unit,
    onChooseVideoFile: () -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        Text("More", Modifier.padding(start = 22.dp, end = 18.dp, top = 16.dp, bottom = 14.dp), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
        GlassSurface(backdrop, Modifier.fillMaxWidth().padding(horizontal = 16.dp), cornerRadius = 26.dp, surfaceAlpha = 0.065f) {
            Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.PlayArrow, null, tint = Color.White, modifier = Modifier.size(48.dp))
                Spacer(Modifier.width(14.dp))
                Column {
                    Text("Liquid Player", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    Text("A quiet, glassy player for your local media", color = Color.LightGray, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
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
                        ActionItem(Icons.Default.Storage, "Internal storage", "Browse device folders from the Browse tab", {})
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
    Column {
        Text(title, Modifier.padding(start = 4.dp, bottom = 8.dp), color = Color.LightGray, style = MaterialTheme.typography.titleSmall)
        GlassSurface(backdrop, Modifier.fillMaxWidth(), cornerRadius = 22.dp, surfaceAlpha = 0.045f) {
            Column {
                items.forEachIndexed { index, item ->
                    Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(item.icon, null, tint = Color.LightGray, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(item.title)
                            Text(item.description, color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                        }
                        Switch(
                            checked = item.checked,
                            onCheckedChange = item.onCheckedChange,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.Black,
                                checkedTrackColor = Color.White,
                                uncheckedThumbColor = Color.LightGray,
                                uncheckedTrackColor = Color.DarkGray
                            )
                        )
                    }
                    if (index != items.lastIndex) androidx.compose.foundation.Canvas(Modifier.fillMaxWidth().padding(start = 50.dp, end = 14.dp).height(0.5.dp)) {
                        drawLine(Color.White.copy(alpha = 0.10f), androidx.compose.ui.geometry.Offset.Zero, androidx.compose.ui.geometry.Offset(size.width, 0f), strokeWidth = 1f)
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsActionSection(backdrop: LayerBackdrop, title: String, items: List<ActionItem>) {
    Column {
        Text(title, Modifier.padding(start = 4.dp, bottom = 8.dp), color = Color.LightGray, style = MaterialTheme.typography.titleSmall)
        GlassSurface(backdrop, Modifier.fillMaxWidth(), cornerRadius = 22.dp, surfaceAlpha = 0.045f) {
            Column {
                items.forEach { item ->
                    Row(
                        Modifier.fillMaxWidth().clickable(onClick = item.onClick).padding(horizontal = 14.dp, vertical = 13.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(item.icon, null, tint = Color.LightGray, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(item.title)
                            Text(item.description, color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                        }
                        Text("›", color = Color.Gray, fontSize = 26.sp)
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
            runCatching {
                if (Build.VERSION.SDK_INT >= 29) {
                    context.contentResolver.loadThumbnail(item.uri, Size(320, 180), null)
                } else null
            }.getOrNull()
        }
    }
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).clickable(onClick = onClick).padding(horizontal = 6.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.width(112.dp).height(70.dp).clip(RoundedCornerShape(13.dp)).background(Color(0xFF171717)),
            contentAlignment = Alignment.Center
        ) {
            if (thumbnail != null) {
                Image(thumbnail!!.asImageBitmap(), contentDescription = null, modifier = Modifier.fillMaxSize())
            } else {
                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(30.dp))
            }
            Text(
                formatDuration(item.durationMs),
                Modifier.align(Alignment.BottomEnd).padding(4.dp).background(Color.Black.copy(alpha = 0.76f), RoundedCornerShape(5.dp)).padding(horizontal = 5.dp, vertical = 2.dp),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                fontSize = 10.sp
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(item.name, maxLines = 2, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyLarge)
            Text(
                listOf(
                    if (item.sizeBytes > 0L) Formatter.formatFileSize(context, item.sizeBytes) else "Local video",
                    if (item.durationMs > 0L) formatDuration(item.durationMs) else null,
                    item.mimeType?.substringAfter('/')?.uppercase(Locale.ROOT)
                ).filterNotNull().joinToString("  ·  "),
                color = Color.Gray,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(item.folderName, color = Color(0xFFAAAAAA), style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.width(5.dp))
        Icon(Icons.Default.MoreHoriz, contentDescription = "Video options", tint = Color.Gray, modifier = Modifier.size(22.dp))
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
        GlassSurface(backdrop, Modifier.fillMaxWidth(), cornerRadius = 28.dp, surfaceAlpha = 0.055f) {
            Column(Modifier.fillMaxWidth().padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Default.Movie, null, tint = Color.LightGray, modifier = Modifier.size(42.dp))
                Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Text(message, color = Color.LightGray, style = MaterialTheme.typography.bodyMedium)
                Button(onClick = onAction, colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)) { Text(action) }
                if (secondaryAction != null && onSecondaryAction != null) {
                    OutlinedButton(onClick = onSecondaryAction, colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)) { Text(secondaryAction) }
                }
            }
        }
    }
}

@Composable
private fun BottomNavigation(
    backdrop: LayerBackdrop,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit
) {
    val navItems = listOf(
        Icons.Default.Movie,
        Icons.Default.Folder,
        Icons.Default.Storage,
        Icons.Default.MoreHoriz
    )
    LiquidBottomTabs(
        selectedTabIndex = selectedTab,
        onTabSelected = onTabSelected,
        backdrop = backdrop,
        tabsCount = navItems.size,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        navItems.forEachIndexed { index, icon ->
            LiquidBottomTab(onClick = { onTabSelected(index) }) {
                Icon(
                    imageVector = icon,
                    contentDescription = listOf("Videos", "Folder", "Browse", "More")[index],
                    tint = if (selectedTab == index) Color.White else Color(0xFF9A9A9A),
                    modifier = Modifier.size(25.dp)
                )
            }
        }
    }
}
