package com.tejas.liquidplayer.player

import android.graphics.Bitmap
import android.view.TextureView
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import kotlinx.coroutines.isActive
import android.os.SystemClock
import kotlin.math.roundToInt

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.tejas.liquidplayer.data.TrackOption
import com.tejas.liquidplayer.data.VideoItem
import com.tejas.liquidplayer.data.formatDuration
import com.tejas.liquidplayer.ui.GlassIconButton
import com.tejas.liquidplayer.ui.GlassSurface
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import org.videolan.libvlc.MediaPlayer
import org.videolan.libvlc.util.VLCVideoLayout

private enum class PlayerPanel { None, Audio, Subtitles, Tools, AspectRatio, Speed }

@Composable
fun VideoPlayerScreen(
    video: VideoItem,
    playlist: List<VideoItem>,
    onSelectVideo: (VideoItem) -> Unit,
    onClose: () -> Unit,
    hardwareAcceleration: Boolean,
    gesturesEnabled: Boolean
) {
    val context = LocalContext.current
    val activity = context.findActivity()
    val controller = remember(context, hardwareAcceleration) { LibVlcController(context, hardwareAcceleration) }
    val playerPreferences = remember(context) { context.getSharedPreferences("liquid_player", Context.MODE_PRIVATE) }
    val progressKey = remember(video.uri) { "playback_position_${video.uri.toString().hashCode()}" }
    val savedPositionMs = remember(video.uri, playerPreferences) { playerPreferences.getLong(progressKey, 0L) }
    val backdrop = rememberLayerBackdrop()
    val retryScope = rememberCoroutineScope()
    val cacheRetryAttempted = remember(controller, video.uri) { AtomicBoolean(false) }

    // Keep VLC's native TextureView outside Compose graphics/backdrop layers. A separate
    // low-opacity frame copy supplies a sample for glass controls without layering VLC output.
    var videoTextureView by remember { mutableStateOf<TextureView?>(null) }
    var backdropFrame by remember { mutableStateOf<Bitmap?>(null) }
    var backdropFrameVersion by remember { mutableIntStateOf(0) }
    var attachedController by remember { mutableStateOf<LibVlcController?>(null) }

    var panel by remember { mutableStateOf(PlayerPanel.None) }
    var controlsVisible by remember { mutableStateOf(true) }
    var isPlaying by remember { mutableStateOf(false) }
    var playbackError by remember(video.uri) { mutableStateOf<String?>(null) }
    var positionMs by remember(video.uri) { mutableLongStateOf(savedPositionMs) }
    var durationMs by remember(video.uri) { mutableLongStateOf(video.durationMs) }
    var audioTracks by remember(video.uri) { mutableStateOf<List<TrackOption>>(emptyList()) }
    var subtitleTracks by remember(video.uri) { mutableStateOf<List<TrackOption>>(emptyList()) }
    var selectedAudioTrackId by remember(video.uri) { mutableIntStateOf(-1) }
    var selectedSubtitleTrackId by remember(video.uri) { mutableIntStateOf(-1) }
    var trackSelectionMessage by remember(video.uri) { mutableStateOf<String?>(null) }
    var resumePositionApplied by remember(video.uri) { mutableStateOf(savedPositionMs < 1_500L) }
    var brightness by remember(video.uri) { mutableFloatStateOf(
        activity?.window?.attributes?.screenBrightness?.takeIf { it in 0.05f..1f } ?: 0.8f
    ) }
    var speed by remember(video.uri) { mutableFloatStateOf(1f) }
    var volume by remember(controller, video.uri) { mutableIntStateOf(runCatching { controller.player.volume }.getOrDefault(100).coerceIn(0, 200)) }
    var verticalGestureZone by remember { mutableIntStateOf(0) }

    val persistProgress = remember(video.uri, controller, playerPreferences) {
        {
            val current = runCatching { controller.player.time }.getOrDefault(positionMs).coerceAtLeast(0L)
            val length = runCatching { controller.player.length }.getOrDefault(durationMs)
            val storedPosition = if (length > 0L && current >= length - 2_500L) 0L else current
            playerPreferences.edit().putLong(progressKey, storedPosition).apply()
        }
    }

    val subtitlePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val added = controller.addExternalSubtitle(uri)
            trackSelectionMessage = if (added) "External subtitle added" else "VLC couldn't add this subtitle file."
            panel = PlayerPanel.Subtitles
        }
    }

    DisposableEffect(activity, controller) {
        val originalOrientation = activity?.requestedOrientation
        val windowController = activity?.let {
            WindowInsetsControllerCompat(it.window, it.window.decorView)
        }
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        windowController?.hide(WindowInsetsCompat.Type.systemBars())
        val originalBrightness = activity?.window?.attributes?.screenBrightness ?: -1f
        onDispose {
            windowController?.show(WindowInsetsCompat.Type.systemBars())
            activity?.window?.let { window ->
                val params = window.attributes
                params.screenBrightness = originalBrightness
                window.attributes = params
            }
            if (activity != null) activity.requestedOrientation = originalOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            controller.close()
        }
    }

    LaunchedEffect(video.uri, controller, attachedController) {
        if (attachedController !== controller) return@LaunchedEffect
        resumePositionApplied = savedPositionMs < 1_500L
        val immediateError = controller.play(video.uri)
        if (immediateError != null) {
            if (video.uri.scheme == "content" && cacheRetryAttempted.compareAndSet(false, true)) {
                playbackError = "Retrying video through a local cache…"
                controller.retryUsingCachedCopy(video.uri)?.let { playbackError = it }
            } else {
                playbackError = immediateError
            }
        }
        var lastSavedAt = SystemClock.elapsedRealtime()
        while (isActive) {
            val currentTime = runCatching { controller.player.time }.getOrDefault(0L).coerceAtLeast(0L)
            val mediaLength = runCatching { controller.player.length }.getOrDefault(0L)
            val playingNow = runCatching { controller.player.isPlaying }.getOrDefault(false)
            positionMs = currentTime
            if (mediaLength > 0L) durationMs = mediaLength
            isPlaying = playingNow
            if (!resumePositionApplied && playingNow && mediaLength > 0L) {
                if (savedPositionMs >= 1_500L && savedPositionMs < mediaLength - 2_500L) {
                    controller.seekTo(savedPositionMs)
                    positionMs = savedPositionMs
                }
                resumePositionApplied = true
            }
            val now = SystemClock.elapsedRealtime()
            if (now - lastSavedAt >= 1_000L) {
                val storedPosition = if (mediaLength > 0L && currentTime >= mediaLength - 2_500L) 0L else currentTime
                playerPreferences.edit().putLong(progressKey, storedPosition).apply()
                lastSavedAt = now
            }
            delay(350L)
        }
    }

    // Surface native decode/open failures instead of leaving users with a silent black player.
    DisposableEffect(controller, video.uri) {
        val mainHandler = Handler(Looper.getMainLooper())
        val listener = MediaPlayer.EventListener { event ->
            when (event.type) {
                MediaPlayer.Event.Opening ->
                    Log.d("LiquidPlayer", "libVLC opening ${video.uri}")
                MediaPlayer.Event.Playing -> mainHandler.post {
                    playbackError = null
                    val mediaLength = runCatching { controller.player.length }.getOrDefault(0L)
                    if (!resumePositionApplied && mediaLength > 0L) {
                        if (savedPositionMs >= 1_500L && savedPositionMs < mediaLength - 2_500L) {
                            controller.seekTo(savedPositionMs)
                            positionMs = savedPositionMs
                        }
                        resumePositionApplied = true
                    }
                }
                MediaPlayer.Event.EncounteredError -> {
                    val shouldRetry = video.uri.scheme == "content" &&
                        cacheRetryAttempted.compareAndSet(false, true)
                    mainHandler.post {
                        if (shouldRetry) {
                            playbackError = "Retrying video through a local cache…"
                            retryScope.launch {
                                controller.retryUsingCachedCopy(video.uri)?.let { playbackError = it }
                            }
                        } else {
                            playbackError = "libVLC failed to open this video even after the local-cache retry. Try another file; diagnostic details are logged."
                            Log.e("LiquidPlayer/VLC", "Playback failed for ${video.uri}")
                        }
                    }
                }
                else -> Unit
            }
        }
        controller.player.setEventListener(listener)
        onDispose {
            persistProgress()
            controller.player.setEventListener(null)
        }
    }

    LaunchedEffect(panel, controller, video.uri) {
        if (panel == PlayerPanel.Audio || panel == PlayerPanel.Subtitles) {
            // libVLC populates track descriptions asynchronously after opening. Keep the
            // chooser synchronized while open rather than taking a single early snapshot.
            while (isActive && (panel == PlayerPanel.Audio || panel == PlayerPanel.Subtitles)) {
                audioTracks = controller.audioTracks()
                subtitleTracks = controller.subtitleTracks()
                selectedAudioTrackId = controller.currentAudioTrackId()
                selectedSubtitleTrackId = controller.currentSubtitleTrackId()
                delay(300L)
            }
        }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        val frame = backdropFrame
        val frameRevision = backdropFrameVersion
        if (frame != null) {
            Image(
                bitmap = frame.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
                // Keep one stable drawing node. The revision invalidates its graphics layer
                // as the double-buffered frame changes, without remounting the glass source.
                modifier = Modifier.fillMaxSize().layerBackdrop(backdrop).graphicsLayer {
                    alpha = 1f
                    rotationZ = frameRevision * 0f
                }
            )
        }

        // VLC must render directly to its native TextureView. Wrapping it in layerBackdrop
        // or an alpha graphics layer can make video output black on real devices.
        AndroidView(
            factory = { ctx ->
                VLCVideoLayout(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    post {
                        controller.attach(this)
                        videoTextureView = findTextureViewDescendant()
                        attachedController = controller
                    }
                }
            },
            update = { layout ->
                if (attachedController !== controller) {
                    layout.post {
                        controller.attach(layout)
                        videoTextureView = layout.findTextureViewDescendant()
                        attachedController = controller
                    }
                } else if (videoTextureView == null) {
                    videoTextureView = layout.findTextureViewDescendant()
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Capture a small preview for backdrop sampling. layerBackdrop precedes output
        // opacity so the recorded pixels remain full-strength while the duplicate is subtle.
        LaunchedEffect(videoTextureView) {
            var firstBuffer: Bitmap? = null
            var secondBuffer: Bitmap? = null
            var useFirstBuffer = true
            while (isActive) {
                val texture = videoTextureView
                if (texture != null && texture.isAvailable && texture.width > 0 && texture.height > 0) {
                    val scale = minOf(1f, 640f / maxOf(texture.width, texture.height).toFloat())
                    val width = (texture.width * scale).toInt().coerceAtLeast(1)
                    val height = (texture.height * scale).toInt().coerceAtLeast(1)
                    if (firstBuffer == null || firstBuffer!!.width != width || firstBuffer!!.height != height) {
                        firstBuffer = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    }
                    if (secondBuffer == null || secondBuffer!!.width != width || secondBuffer!!.height != height) {
                        secondBuffer = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    }
                    val target = if (useFirstBuffer) firstBuffer!! else secondBuffer!!
                    val copied = runCatching { texture.getBitmap(target) }.getOrNull()
                    if (copied != null) {
                        backdropFrame = copied
                        backdropFrameVersion++
                        useFirstBuffer = !useFirstBuffer
                    }
                }
                // Keep the same spatial sample quality while reducing backdrop staleness from
                // five frames/second to twenty, using two buffers to avoid mutating the image
                // currently displayed by Compose.
                delay(50L)
            }
        }

        playbackError?.let { message ->
            Text(
                text = message,
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(24.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Black.copy(alpha = 0.82f))
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            )
        }

        // Tap the unobstructed video area to reveal/hide controls.
        Box(
            Modifier.fillMaxSize()
                .pointerInput(gesturesEnabled, video.uri) {
                    detectTapGestures(
                        onTap = {
                            controlsVisible = !controlsVisible
                            if (!controlsVisible) panel = PlayerPanel.None
                        },
                        onDoubleTap = if (gesturesEnabled) { offset ->
                            when {
                                offset.x < size.width * 0.30f -> controller.seekBy(-10_000L)
                                offset.x > size.width * 0.70f -> controller.seekBy(10_000L)
                                else -> controller.togglePlayPause()
                            }
                            controlsVisible = true
                            panel = PlayerPanel.None
                        } else null
                    )
                }
                .pointerInput(gesturesEnabled, video.uri) {
                    if (gesturesEnabled) {
                        detectVerticalDragGestures(
                            onDragStart = { start ->
                                verticalGestureZone = when {
                                    start.x < size.width * 0.40f -> -1 // left side: brightness
                                    start.x > size.width * 0.60f -> 1 // right side: volume
                                    else -> 0
                                }
                            },
                            onVerticalDrag = { change, dragAmount ->
                                when (verticalGestureZone) {
                                    -1 -> {
                                        brightness = (brightness - dragAmount / size.height * 1.4f).coerceIn(0.05f, 1f)
                                        activity?.window?.let { window ->
                                            val params = window.attributes
                                            params.screenBrightness = brightness
                                            window.attributes = params
                                        }
                                    }
                                    1 -> {
                                        volume = (volume - (dragAmount / size.height * 220f).roundToInt()).coerceIn(0, 200)
                                        runCatching { controller.player.volume = volume }
                                    }
                                }
                                change.consume()
                            }
                        )
                    }
                }
        )

        if (controlsVisible) {
            Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GlassIconButton(backdrop, Icons.Default.ArrowBack, "Back", onClick = { persistProgress(); onClose() })
                    Column(Modifier.weight(1f)) {
                        Text(video.name, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Medium)
                        Text("libVLC · Local playback", style = MaterialTheme.typography.labelSmall, color = Color.LightGray)
                    }
                    GlassIconButton(backdrop, Icons.Default.Audiotrack, "Audio tracks", onClick = { trackSelectionMessage = null; panel = togglePanel(panel, PlayerPanel.Audio) })
                    GlassIconButton(backdrop, Icons.Default.ClosedCaption, "Subtitles", onClick = { trackSelectionMessage = null; panel = togglePanel(panel, PlayerPanel.Subtitles) })
                    GlassIconButton(backdrop, Icons.Default.Tune, "Player tools", onClick = { panel = togglePanel(panel, PlayerPanel.Tools) })
                }

                Spacer(Modifier.weight(1f))

                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(formatDuration(positionMs), color = Color.White, style = MaterialTheme.typography.labelMedium)
                    Slider(
                        value = if (durationMs > 0L) (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f,
                        onValueChange = { fraction ->
                            val target = (durationMs * fraction).toLong()
                            positionMs = target
                            controller.seekTo(target)
                        },
                        modifier = Modifier.weight(1f).padding(horizontal = 10.dp).height(24.dp),
                        colors = SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = Color.White,
                            inactiveTrackColor = Color.White.copy(alpha = 0.38f),
                            activeTickColor = Color.Transparent,
                            inactiveTickColor = Color.Transparent
                        )
                    )
                    Text(formatDuration(durationMs), color = Color.White, style = MaterialTheme.typography.labelMedium)
                }

                Row(Modifier.fillMaxWidth().padding(bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    GlassIconButton(backdrop, Icons.Default.Lock, "Lock controls", onClick = { controlsVisible = false })
                    Spacer(Modifier.weight(1f))
                    val currentVideoIndex = playlist.indexOfFirst { it.uri == video.uri }
                    GlassIconButton(
                        backdrop, Icons.Default.SkipPrevious, "Previous video",
                        onClick = {
                            if (currentVideoIndex > 0) {
                                persistProgress()
                                onSelectVideo(playlist[currentVideoIndex - 1])
                            }
                        }
                    )
                    Spacer(Modifier.width(12.dp))
                    GlassIconButton(backdrop, if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, if (isPlaying) "Pause" else "Play", modifier = Modifier.size(60.dp), selected = true, onClick = { controller.togglePlayPause() })
                    Spacer(Modifier.width(12.dp))
                    GlassIconButton(
                        backdrop, Icons.Default.SkipNext, "Next video",
                        onClick = {
                            if (currentVideoIndex >= 0 && currentVideoIndex < playlist.lastIndex) {
                                persistProgress()
                                onSelectVideo(playlist[currentVideoIndex + 1])
                            }
                        }
                    )
                    Spacer(Modifier.weight(1f))
                    GlassIconButton(backdrop, Icons.Default.AspectRatio, "Aspect ratio", onClick = { panel = togglePanel(panel, PlayerPanel.AspectRatio) })
                }
            }
        }

        if (panel != PlayerPanel.None) {
            Box(
                Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.20f)).clickable(
                    indication = null,
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                ) { panel = PlayerPanel.None },
                contentAlignment = Alignment.CenterEnd
            ) {
                GlassSurface(
                    backdrop,
                    modifier = Modifier.fillMaxHeight().width(340.dp).padding(12.dp).clickable(
                        indication = null,
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                    ) {},
                    cornerRadius = 28.dp,
                    surfaceAlpha = 0.11f
                ) {
                    Column(Modifier.fillMaxSize().padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                when (panel) {
                                    PlayerPanel.Audio -> "Audio track"
                                    PlayerPanel.Subtitles -> "Subtitles"
                                    PlayerPanel.Tools -> "Player tools"
                                    PlayerPanel.AspectRatio -> "Aspect ratio"
                                    PlayerPanel.Speed -> "Playback speed"
                                    PlayerPanel.None -> ""
                                },
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { panel = PlayerPanel.None }) { Icon(Icons.Default.Close, "Close", tint = Color.White) }
                        }

                        when (panel) {
                            PlayerPanel.Audio -> {
                                trackSelectionMessage?.let { Text(it, color = Color.White, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp)) }
                                TrackPanel(
                                    emptyLabel = "Waiting for audio tracks…",
                                    tracks = audioTracks,
                                    selectedTrackId = selectedAudioTrackId,
                                    onSelect = { id ->
                                        val success = controller.selectAudioTrack(id)
                                        selectedAudioTrackId = controller.currentAudioTrackId()
                                        trackSelectionMessage = if (success) "Audio track selected" else "VLC could not select this audio track."
                                    }
                                )
                            }
                            PlayerPanel.Subtitles -> {
                                trackSelectionMessage?.let { Text(it, color = Color.White, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp)) }
                                Button(
                                    onClick = { subtitlePicker.launch(arrayOf("*/*")) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                                ) {
                                    Icon(Icons.Default.ClosedCaption, null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Open subtitle file")
                                }
                                TrackPanel(
                                    emptyLabel = "Waiting for subtitle tracks…",
                                    tracks = subtitleTracks,
                                    selectedTrackId = selectedSubtitleTrackId,
                                    onSelect = { id ->
                                        val success = controller.selectSubtitleTrack(id)
                                        selectedSubtitleTrackId = controller.currentSubtitleTrackId()
                                        trackSelectionMessage = if (success) "Subtitle track selected" else "VLC could not select this subtitle track."
                                    },
                                    onDisable = {
                                        val success = controller.selectSubtitleTrack(-1)
                                        selectedSubtitleTrackId = controller.currentSubtitleTrackId()
                                        trackSelectionMessage = if (success) "Subtitles disabled" else "VLC could not disable subtitles."
                                    }
                                )
                            }
                            PlayerPanel.Tools -> ToolsPanel(
                                onAspectRatio = { panel = PlayerPanel.AspectRatio },
                                onAudio = { panel = PlayerPanel.Audio },
                                onSubtitles = { panel = PlayerPanel.Subtitles },
                                onSpeed = { panel = PlayerPanel.Speed }
                            )
                            PlayerPanel.AspectRatio -> AspectRatioPanel { label, ratio ->
                                controller.setAspectRatio(ratio)
                                panel = PlayerPanel.None
                            }
                            PlayerPanel.Speed -> {
                                listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 1.75f, 2f).forEach { rate ->
                                    Row(
                                        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable {
                                            speed = rate
                                            controller.player.setRate(rate)
                                            panel = PlayerPanel.None
                                        }.padding(vertical = 12.dp, horizontal = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(if (rate == 1f) "Normal" else "$rate×", Modifier.weight(1f))
                                        if (speed == rate) Icon(Icons.Default.Check, null, tint = Color.White)
                                    }
                                }
                            }
                            PlayerPanel.None -> Unit
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TrackPanel(
    emptyLabel: String,
    tracks: List<TrackOption>,
    selectedTrackId: Int,
    onSelect: (Int) -> Unit,
    onDisable: (() -> Unit)? = null
) {
    if (tracks.isEmpty()) {
        Text(emptyLabel, color = Color.LightGray, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 14.dp))
    } else {
        Column(Modifier.fillMaxWidth()) {
            tracks.forEachIndexed { index, track ->
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { onSelect(track.id) }.padding(vertical = 12.dp, horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.size(18.dp).clip(RoundedCornerShape(50)).background(if (track.id == selectedTrackId) Color.White else Color.White.copy(alpha = 0.25f)))
                    Spacer(Modifier.width(12.dp))
                    Text(track.label, color = Color.White, modifier = Modifier.weight(1f), maxLines = 2, overflow = TextOverflow.Ellipsis)
                    if (track.id == selectedTrackId) Icon(Icons.Default.Check, contentDescription = "Selected track", tint = Color.White, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
    if (onDisable != null) {
        TextButton(onClick = onDisable) { Text("Disable subtitles", color = Color.White) }
    }
}

@Composable
private fun ToolsPanel(
    onAspectRatio: () -> Unit,
    onAudio: () -> Unit,
    onSubtitles: () -> Unit,
    onSpeed: () -> Unit
) {
    val tools = listOf(
        Triple(Icons.Default.PlaylistAdd, "Playing queue", onSpeed),
        Triple(Icons.Default.AspectRatio, "Aspect ratio", onAspectRatio),
        Triple(Icons.Default.Bookmark, "Bookmark", {}),
        Triple(Icons.Default.Audiotrack, "Audio tracks", onAudio),
        Triple(Icons.Default.ClosedCaption, "Subtitles", onSubtitles),
        Triple(Icons.Default.Speed, "Playback speed", onSpeed),
        Triple(Icons.Default.Favorite, "Favourite", {}),
        Triple(Icons.Default.Share, "Share", {}),
        Triple(Icons.Default.Info, "Information", {})
    )
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        tools.forEach { (icon, label, action) ->
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable(onClick = action).padding(horizontal = 8.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(icon, null, tint = Color.White, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(12.dp))
                Text(label, Modifier.weight(1f), color = Color.White)
                Text("›", color = Color.Gray, fontSize = 24.sp)
            }
        }
    }
}

@Composable
private fun AspectRatioPanel(onSelect: (String, String?) -> Unit) {
    val ratios = listOf(
        "Original" to null,
        "16:9" to "16:9",
        "4:3" to "4:3",
        "1:1" to "1:1",
        "21:9" to "21:9",
        "2.35:1" to "2.35:1"
    )
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        ratios.forEach { (label, ratio) ->
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable { onSelect(label, ratio) }.padding(horizontal = 10.dp, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.AspectRatio, null, tint = Color.LightGray, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(12.dp))
                Text(label, Modifier.weight(1f))
            }
        }
        Text("Ratios are applied by libVLC. Original restores the source aspect ratio.", color = Color.Gray, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 12.dp))
    }
}

@Composable
private fun neutralSliderColors() = SliderDefaults.colors(
    thumbColor = Color.White,
    activeTrackColor = Color.White,
    inactiveTrackColor = Color.White.copy(alpha = 0.3f),
    activeTickColor = Color.Transparent,
    inactiveTickColor = Color.Transparent
)

private fun togglePanel(current: PlayerPanel, requested: PlayerPanel): PlayerPanel =
    if (current == requested) PlayerPanel.None else requested

private fun ViewGroup.findTextureViewDescendant(): TextureView? {
    for (index in 0 until childCount) {
        val child = getChildAt(index)
        if (child is TextureView) return child
        if (child is ViewGroup) {
            child.findTextureViewDescendant()?.let { return it }
        }
    }
    return null
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is android.content.ContextWrapper -> baseContext.findActivity()
    else -> null
}

