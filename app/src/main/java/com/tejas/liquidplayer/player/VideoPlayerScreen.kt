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
import android.app.PictureInPictureParams
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.media.MediaMetadataRetriever
import android.os.Build
import android.util.Rational
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.BackHandler
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow
import com.tejas.liquidplayer.data.TrackOption
import com.tejas.liquidplayer.data.VideoItem
import com.tejas.liquidplayer.data.formatDuration
import com.tejas.liquidplayer.ui.GlassIconButton
import com.tejas.liquidplayer.ui.GlassSurface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
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
    gesturesEnabled: Boolean,
    isInPictureInPictureMode: Boolean
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
    var controlsLocked by remember(video.uri) { mutableStateOf(false) }
    var manualRotation by remember(video.uri) { mutableStateOf(false) }
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
    var controlsInteractionToken by remember { mutableIntStateOf(0) }
    var adjustmentMode by remember { mutableIntStateOf(0) }
    var videoAspectRatio by remember(video.uri) { mutableFloatStateOf(16f / 9f) }
    var selectedAspectIndex by remember(video.uri) { mutableIntStateOf(0) }
    var showAspectRatioFeedback by remember { mutableStateOf(false) }
    var subtitleDelayUs by remember(video.uri) { mutableLongStateOf(0L) }
    val aspectRatios = remember { listOf("Original" to null, "16:9" to "16:9", "4:3" to "4:3", "1:1" to "1:1", "21:9" to "21:9", "2.35:1" to "2.35:1") }

    val persistProgress = remember(video.uri, controller, playerPreferences) {
        {
            val current = runCatching { controller.player.time }.getOrDefault(positionMs).coerceAtLeast(0L)
            val length = runCatching { controller.player.length }.getOrDefault(durationMs)
            val storedPosition = if (length > 0L && current >= length - 2_500L) 0L else current
            playerPreferences.edit().putLong(progressKey, storedPosition).apply()
        }
    }

    BackHandler(enabled = !isInPictureInPictureMode) {
        if (controlsLocked) {
            controlsLocked = false
            controlsVisible = true
            controlsInteractionToken++
        } else if (panel != PlayerPanel.None) panel = PlayerPanel.None else { persistProgress(); onClose() }
    }
    LaunchedEffect(controlsVisible, controlsLocked, panel, controlsInteractionToken, isInPictureInPictureMode) {
        if (controlsVisible && !controlsLocked && panel == PlayerPanel.None && !isInPictureInPictureMode) {
            delay(4_000L)
            controlsVisible = false
        }
    }
    LaunchedEffect(adjustmentMode, brightness, volume) {
        if (adjustmentMode != 0) { delay(900L); adjustmentMode = 0 }
    }
    LaunchedEffect(showAspectRatioFeedback) {
        if (showAspectRatioFeedback) { delay(1_200L); showAspectRatioFeedback = false }
    }
    LaunchedEffect(video.uri, activity, manualRotation) {
        val dimensions = withContext(Dispatchers.IO) {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(context, video.uri)
                val w = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
                val h = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
                val rotation = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull() ?: 0
                if (w > 0 && h > 0) if (rotation == 90 || rotation == 270) h to w else w to h else null
            } catch (_: Exception) { null } finally { runCatching { retriever.release() } }
        }
        if (dimensions != null) {
            val (w, h) = dimensions
            videoAspectRatio = w.toFloat() / h.toFloat()
            if (activity != null && !isInPictureInPictureMode && !manualRotation) {
                activity.requestedOrientation = if (h > w) ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT else ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            }
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
            if (panel == PlayerPanel.Subtitles) subtitleDelayUs = controller.currentSubtitleDelayUs()
            while (isActive && (panel == PlayerPanel.Audio || panel == PlayerPanel.Subtitles)) {
                audioTracks = controller.audioTracks()
                subtitleTracks = controller.subtitleTracks()
                selectedAudioTrackId = controller.currentAudioTrackId()
                selectedSubtitleTrackId = controller.currentSubtitleTrackId()
                if (panel == PlayerPanel.Subtitles) subtitleDelayUs = controller.currentSubtitleDelayUs()
                delay(300L)
            }
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize().background(Color.Black).pointerInput(video.uri) {
        awaitEachGesture { awaitFirstDown(pass = PointerEventPass.Initial); controlsInteractionToken++ }
    }) {
        val isLandscape = maxWidth > maxHeight
        val screenWidth = maxWidth
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
                            if (!controlsLocked) {
                                controlsVisible = !controlsVisible
                                controlsInteractionToken++
                                if (!controlsVisible) panel = PlayerPanel.None
                            }
                        },
                        onDoubleTap = if (gesturesEnabled && !controlsLocked) { offset ->
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
                .pointerInput(gesturesEnabled, video.uri, controlsLocked) {
                    if (gesturesEnabled && !controlsLocked) {
                        detectVerticalDragGestures(
                            onDragStart = { start ->
                                verticalGestureZone = when {
                                    start.x < size.width * 0.40f -> -1
                                    start.x > size.width * 0.60f -> 1
                                    else -> 0
                                }
                                adjustmentMode = verticalGestureZone
                                controlsInteractionToken++
                            },
                            onVerticalDrag = { change, dragAmount ->
                                controlsInteractionToken++
                                when (verticalGestureZone) {
                                    -1 -> {
                                        adjustmentMode = -1
                                        brightness = (brightness - dragAmount / size.height * 1.75f).coerceIn(0.05f, 1f)
                                        activity?.window?.let { window ->
                                            val params = window.attributes
                                            params.screenBrightness = brightness
                                            window.attributes = params
                                        }
                                    }
                                    1 -> {
                                        adjustmentMode = 1
                                        volume = (volume - (dragAmount / size.height * 275f).roundToInt()).coerceIn(0, 200)
                                        runCatching { controller.player.volume = volume }
                                    }
                                }
                                change.consume()
                            }
                        )
                    }
                }
        )


        if (adjustmentMode != 0 && !isInPictureInPictureMode) {
            Box(Modifier.align(if (adjustmentMode < 0) Alignment.CenterEnd else Alignment.CenterStart).padding(start = if (adjustmentMode > 0) 28.dp else 0.dp, end = if (adjustmentMode < 0) 28.dp else 0.dp)) {
                AdjustmentIndicator(
                    icon = if (adjustmentMode < 0) Icons.Default.Brightness6 else Icons.Default.VolumeUp,
                    value = if (adjustmentMode < 0) "${(brightness * 100).toInt()}%" else "${(volume / 2).coerceIn(0, 100)}%",
                    level = if (adjustmentMode < 0) brightness else (volume / 200f),
                    label = if (adjustmentMode < 0) "Brightness" else "Volume"
                )
            }
        }
        if (showAspectRatioFeedback && !isInPictureInPictureMode) {
            Box(Modifier.align(Alignment.Center)) {
                Text("Aspect ratio · ${aspectRatios[selectedAspectIndex].first}", color = Color.White, style = MaterialTheme.typography.labelLarge, modifier = Modifier.clip(RoundedCornerShape(50.dp)).background(Color.Black.copy(alpha = 0.78f)).padding(horizontal = 18.dp, vertical = 10.dp))
            }
        }

        if (controlsVisible && !controlsLocked && !isInPictureInPictureMode) {
            Column(
                Modifier.fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = if (isLandscape) 16.dp else 12.dp, vertical = 8.dp)
            ) {
                if (isLandscape) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PlainBackButton(onClick = { persistProgress(); onClose() })
                        GlassIconButton(
                            backdrop, Icons.Default.Lock, "Lock controls",
                            modifier = Modifier.size(width = 48.dp, height = 44.dp),
                            onClick = { controlsLocked = true; panel = PlayerPanel.None; controlsVisible = false; controlsInteractionToken++ }
                        )
                        Column(Modifier.weight(1f).padding(horizontal = 2.dp)) {
                            Text(video.name, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Medium)
                        }
                        GlassIconButton(backdrop, Icons.Default.Audiotrack, "Audio tracks", modifier = Modifier.size(width = 48.dp, height = 44.dp), onClick = { trackSelectionMessage = null; panel = togglePanel(panel, PlayerPanel.Audio) })
                        GlassIconButton(backdrop, Icons.Default.ClosedCaption, "Subtitles", modifier = Modifier.size(width = 48.dp, height = 44.dp), onClick = { trackSelectionMessage = null; panel = togglePanel(panel, PlayerPanel.Subtitles) })
                        // The menu moved from the transport row into the former top tools slot.
                        GlassIconButton(backdrop, Icons.Default.Menu, "Player tools", modifier = Modifier.size(width = 48.dp, height = 44.dp), onClick = { panel = togglePanel(panel, PlayerPanel.Tools) })
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PlainBackButton(onClick = { persistProgress(); onClose() })
                        Column(Modifier.weight(1f).padding(start = 2.dp)) {
                            Text(video.name, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Medium)
                        }
                        GlassIconButton(backdrop, Icons.Default.Audiotrack, "Audio tracks", modifier = Modifier.size(width = 48.dp, height = 44.dp), onClick = { trackSelectionMessage = null; panel = togglePanel(panel, PlayerPanel.Audio) })
                        GlassIconButton(backdrop, Icons.Default.ClosedCaption, "Subtitles", modifier = Modifier.size(width = 48.dp, height = 44.dp), onClick = { trackSelectionMessage = null; panel = togglePanel(panel, PlayerPanel.Subtitles) })
                        GlassIconButton(backdrop, Icons.Default.Menu, "Player tools", modifier = Modifier.size(width = 48.dp, height = 44.dp), onClick = { panel = togglePanel(panel, PlayerPanel.Tools) })
                    }
                }

                Spacer(Modifier.weight(1f))

                Row(Modifier.fillMaxWidth().height(36.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        formatDuration(positionMs),
                        modifier = Modifier.widthIn(min = 44.dp),
                        color = Color.White,
                        style = MaterialTheme.typography.labelMedium
                    )
                    LiquidSeekBar(
                        backdrop = backdrop,
                        positionMs = positionMs,
                        durationMs = durationMs,
                        onSeek = { target ->
                            positionMs = target
                            controlsInteractionToken++
                            controller.seekTo(target)
                        },
                        modifier = Modifier.weight(1f).padding(horizontal = if (isLandscape) 14.dp else 10.dp)
                    )
                    Text(
                        formatDuration(durationMs),
                        modifier = Modifier.widthIn(min = 44.dp),
                        color = Color.White,
                        style = MaterialTheme.typography.labelMedium,
                        textAlign = androidx.compose.ui.text.style.TextAlign.End
                    )
                }
                Spacer(Modifier.height(if (isLandscape) 12.dp else 8.dp))

                val currentVideoIndex = playlist.indexOfFirst { it.uri == video.uri }
                Box(Modifier.fillMaxWidth().height(54.dp)) {
                    GlassIconButton(
                        backdrop,
                        Icons.Default.ScreenRotation,
                        "Rotate screen",
                        modifier = Modifier.align(Alignment.CenterStart).size(width = 48.dp, height = 44.dp),
                        onClick = {
                            manualRotation = true
                            val targetOrientation = if (isLandscape) ActivityInfo.SCREEN_ORIENTATION_PORTRAIT else ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                            activity?.requestedOrientation = targetOrientation
                            controlsInteractionToken++
                        }
                    )
                    Row(
                        Modifier.align(Alignment.Center),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(if (isLandscape) 10.dp else 8.dp)
                    ) {
                        GlassIconButton(
                            backdrop, Icons.Default.SkipPrevious, "Previous video",
                            modifier = Modifier.size(width = if (isLandscape) 68.dp else 46.dp, height = 44.dp),
                            onClick = {
                                if (currentVideoIndex > 0) {
                                    persistProgress()
                                    onSelectVideo(playlist[currentVideoIndex - 1])
                                }
                            }
                        )
                        GlassIconButton(
                            backdrop,
                            if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            if (isPlaying) "Pause" else "Play",
                            modifier = Modifier.size(width = if (isLandscape) 80.dp else 58.dp, height = if (isLandscape) 52.dp else 48.dp),
                            selected = true,
                            onClick = { controller.togglePlayPause() }
                        )
                        GlassIconButton(
                            backdrop, Icons.Default.SkipNext, "Next video",
                            modifier = Modifier.size(width = if (isLandscape) 68.dp else 46.dp, height = 44.dp),
                            onClick = {
                                if (currentVideoIndex >= 0 && currentVideoIndex < playlist.lastIndex) {
                                    persistProgress()
                                    onSelectVideo(playlist[currentVideoIndex + 1])
                                }
                            }
                        )
                        GlassIconButton(
                            backdrop, Icons.Default.AspectRatio, "Aspect ratio",
                            modifier = Modifier.size(width = if (isLandscape) 68.dp else 46.dp, height = 44.dp),
                            onClick = {
                                selectedAspectIndex = (selectedAspectIndex + 1) % aspectRatios.size
                                controller.setAspectRatio(aspectRatios[selectedAspectIndex].second)
                                showAspectRatioFeedback = true
                            },
                            onLongClick = { panel = PlayerPanel.AspectRatio }
                        )
                    }
                    if (isLandscape) {
                        GlassIconButton(
                            backdrop,
                            Icons.Default.PictureInPictureAlt,
                            "Picture-in-picture",
                            modifier = Modifier.align(Alignment.CenterEnd).size(width = 48.dp, height = 44.dp),
                            onClick = {
                                persistProgress()
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && activity != null &&
                                    context.packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)) {
                                    val ratio = videoAspectRatio.coerceIn(0.42f, 2.39f)
                                    runCatching { activity.enterPictureInPictureMode(PictureInPictureParams.Builder().setAspectRatio(Rational((ratio * 1000).toInt().coerceAtLeast(1), 1000)).build()) }
                                }
                            }
                        )
                    }
                }
            }
        }

        if (controlsLocked && !isInPictureInPictureMode) {
            Box(Modifier.align(Alignment.CenterStart).padding(start = 24.dp)) {
                GlassIconButton(
                    backdrop,
                    Icons.Default.LockOpen,
                    "Unlock controls",
                    modifier = Modifier.size(width = 68.dp, height = 48.dp),
                    onClick = { controlsLocked = false; controlsVisible = true; controlsInteractionToken++ }
                )
            }
        }

        if (panel != PlayerPanel.None && !isInPictureInPictureMode) {
            Box(
                Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.20f)).clickable(
                    indication = null,
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                ) { panel = PlayerPanel.None },
                contentAlignment = if (isLandscape) Alignment.CenterEnd else Alignment.Center
            ) {
                GlassSurface(
                    backdrop,
                    modifier = Modifier.fillMaxHeight().width(if (isLandscape) 340.dp else (screenWidth - 24.dp).coerceAtLeast(280.dp)).padding(12.dp).clickable(
                        indication = null,
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                    ) {},
                    cornerRadius = 28.dp,
                    surfaceAlpha = 0.11f,
                    navMaterial = true,
                    flatNavMaterial = true
                ) {
                    Column(Modifier.fillMaxSize().padding(18.dp).verticalScroll(rememberScrollState())) {
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
                                Spacer(Modifier.height(14.dp))
                                Text("Subtitle delay", style = MaterialTheme.typography.titleMedium, color = Color.White)
                                Text("Positive values show subtitles later; negative values show them earlier.", style = MaterialTheme.typography.bodySmall, color = Color.LightGray, modifier = Modifier.padding(top = 4.dp, bottom = 8.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    TextButton(onClick = { val next = (subtitleDelayUs - 50_000L).coerceAtLeast(-10_000_000L); if (controller.setSubtitleDelayUs(next)) subtitleDelayUs = next }) { Text("−50 ms", color = Color.White) }
                                    Text("%+.0f ms".format(subtitleDelayUs / 1000.0), modifier = Modifier.weight(1f), color = Color.White, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                                    TextButton(onClick = { val next = (subtitleDelayUs + 50_000L).coerceAtMost(10_000_000L); if (controller.setSubtitleDelayUs(next)) subtitleDelayUs = next }) { Text("+50 ms", color = Color.White) }
                                }
                            }
                            PlayerPanel.Tools -> ToolsPanel(
                                onAudio = { panel = PlayerPanel.Audio },
                                onSubtitles = { panel = PlayerPanel.Subtitles },
                                onSpeed = { panel = PlayerPanel.Speed }
                            )
                            PlayerPanel.AspectRatio -> AspectRatioPanel { label, ratio ->
                                selectedAspectIndex = aspectRatios.indexOfFirst { it.first == label }.coerceAtLeast(0)
                                controller.setAspectRatio(ratio)
                                showAspectRatioFeedback = true
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
    onAudio: () -> Unit,
    onSubtitles: () -> Unit,
    onSpeed: () -> Unit
) {
    val tools = listOf(
        Triple(Icons.Default.PlaylistAdd, "Playing queue", onSpeed),
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
private fun AdjustmentIndicator(icon: ImageVector, value: String, level: Float, label: String) {
    Column(Modifier.width(72.dp).padding(horizontal = 10.dp, vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(icon, label, tint = Color.White, modifier = Modifier.size(23.dp))
        Text(value, color = Color.White, style = MaterialTheme.typography.labelMedium)
        Box(Modifier.width(3.dp).height(80.dp), contentAlignment = Alignment.BottomCenter) {
            Box(Modifier.fillMaxWidth().fillMaxHeight(level.coerceIn(0.02f, 1f)).clip(RoundedCornerShape(50.dp)).background(Color.White))
        }
    }
}

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



@Composable
private fun PlainBackButton(onClick: () -> Unit) {
    Box(
        Modifier.size(width = 36.dp, height = 44.dp)
            .clickable(interactionSource = null, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.ArrowBack,
            contentDescription = "Back",
            tint = Color.White,
            modifier = Modifier.size(27.dp)
        )
    }
}

@Composable
private fun LiquidSeekBar(
    backdrop: com.kyant.backdrop.Backdrop,
    positionMs: Long,
    durationMs: Long,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val thumbSize = 26.dp
    val density = LocalDensity.current
    val thumbDiameterPx = with(density) { thumbSize.toPx() }
    val progress = if (durationMs > 0L) {
        (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
    } else 0f

    BoxWithConstraints(modifier.fillMaxWidth().height(36.dp)) {
        val thumbTravel = (maxWidth - thumbSize).coerceAtLeast(0.dp)

        Canvas(
            Modifier.fillMaxSize()
                .pointerInput(durationMs, thumbDiameterPx) {
                    detectTapGestures { point ->
                        val usableWidth = (size.width - thumbDiameterPx).coerceAtLeast(1f)
                        val fraction = ((point.x - thumbDiameterPx / 2f) / usableWidth).coerceIn(0f, 1f)
                        onSeek((durationMs * fraction).toLong().coerceAtLeast(0L))
                    }
                }
                .pointerInput(durationMs, thumbDiameterPx) {
                    detectDragGestures(
                        onDragStart = { point ->
                            val usableWidth = (size.width - thumbDiameterPx).coerceAtLeast(1f)
                            val fraction = ((point.x - thumbDiameterPx / 2f) / usableWidth).coerceIn(0f, 1f)
                            onSeek((durationMs * fraction).toLong().coerceAtLeast(0L))
                        },
                        onDrag = { change, _ ->
                            val usableWidth = (size.width - thumbDiameterPx).coerceAtLeast(1f)
                            val fraction = ((change.position.x - thumbDiameterPx / 2f) / usableWidth).coerceIn(0f, 1f)
                            onSeek((durationMs * fraction).toLong().coerceAtLeast(0L))
                            change.consume()
                        }
                    )
                }
        ) {
            val radius = thumbDiameterPx / 2f
            val startX = 0f
            val endX = size.width
            val thumbCenterX = radius + (size.width - thumbDiameterPx).coerceAtLeast(0f) * progress
            val centerY = size.height / 2f
            val stroke = 5.dp.toPx()
            drawLine(
                color = Color.White.copy(alpha = 0.34f),
                start = Offset(startX, centerY),
                end = Offset(endX, centerY),
                strokeWidth = stroke,
                cap = StrokeCap.Round
            )
            drawLine(
                color = Color.White,
                start = Offset(startX, centerY),
                end = Offset(thumbCenterX, centerY),
                strokeWidth = stroke,
                cap = StrokeCap.Round
            )
        }

        Box(
            Modifier.align(Alignment.CenterStart)
                .offset(x = thumbTravel * progress)
                .size(thumbSize)
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { CircleShape },
                    effects = {
                        vibrancy()
                        lens(
                            refractionHeight = 10.dp.toPx(),
                            refractionAmount = 14.dp.toPx(),
                            depthEffect = true,
                            chromaticAberration = false
                        )
                    },
                    highlight = {
                        Highlight.Default.copy(
                            width = 1.2.dp,
                            blurRadius = 1.4.dp,
                            alpha = 0.85f
                        )
                    },
                    shadow = {
                        Shadow(
                            radius = 5.dp,
                            offset = DpOffset(0.dp, 1.dp),
                            color = Color.Black.copy(alpha = 0.28f),
                            alpha = 0.7f
                        )
                    },
                    innerShadow = {
                        InnerShadow(
                            radius = 2.dp,
                            offset = DpOffset(0.dp, (-0.5).dp),
                            color = Color.White.copy(alpha = 0.22f),
                            alpha = 0.8f
                        )
                    },
                    onDrawSurface = {
                        drawRect(Color.White.copy(alpha = 0.07f))
                        drawRect(Color.Black.copy(alpha = 0.08f))
                    }
                )
        )
    }
}
