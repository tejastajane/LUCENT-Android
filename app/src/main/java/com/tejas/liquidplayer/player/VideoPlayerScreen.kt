package com.tejas.liquidplayer.player

import android.graphics.Bitmap
import android.view.TextureView
import android.view.ViewTreeObserver
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.ContentScale
import kotlinx.coroutines.isActive
import android.os.SystemClock
import android.text.format.Formatter
import kotlin.math.roundToInt
import kotlin.math.abs

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.Context
import android.content.ClipData
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.media.MediaMetadataRetriever
import android.media.AudioManager
import android.os.Build
import android.util.Rational
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.BackHandler
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.wrapContentHeight
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Speed
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.foundation.focusable
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.lerp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow
import com.tejas.liquidplayer.ui.liquid.utils.DampedDragAnimation
import com.tejas.liquidplayer.data.TrackOption
import com.tejas.liquidplayer.data.VideoItem
import com.tejas.liquidplayer.data.formatDuration
import com.tejas.liquidplayer.data.mediaPreferenceId
import com.tejas.liquidplayer.ui.GlassIconButton
import com.tejas.liquidplayer.ui.KyantGlassSurface
import com.tejas.liquidplayer.ui.GlassSurface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import org.videolan.libvlc.MediaPlayer
import org.videolan.libvlc.util.VLCVideoLayout

private enum class PlayerPanel { None, Audio, Subtitles, Tools, AspectRatio, Speed, Bookmarks, Information }

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
    val lifecycleOwner = LocalLifecycleOwner.current
    val audioManager = remember(context) {
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }
    val systemVolumeMax = remember(audioManager) {
        audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
    }
    val controller = remember(context, hardwareAcceleration) { LibVlcController(context, hardwareAcceleration) }
    val playerPreferences = remember(context) { context.getSharedPreferences("liquid_player", Context.MODE_PRIVATE) }
    val lastBrightnessKey = "last_player_brightness"
    val preferenceSuffix = remember(video.uri) { mediaPreferenceId(video.uri.toString()) }
    val legacyPreferenceSuffix = remember(video.uri) { video.uri.toString().hashCode().toString() }
    val progressKey = remember(preferenceSuffix) { "playback_position_$preferenceSuffix" }
    val legacyProgressKey = remember(legacyPreferenceSuffix) { "playback_position_$legacyPreferenceSuffix" }
    val favouriteKey = remember(preferenceSuffix) { "favourite_$preferenceSuffix" }
    val legacyFavouriteKey = remember(legacyPreferenceSuffix) { "favourite_$legacyPreferenceSuffix" }
    val audioTrackIdKey = remember(preferenceSuffix) { "audio_track_id_$preferenceSuffix" }
    val legacyAudioTrackIdKey = remember(legacyPreferenceSuffix) { "audio_track_id_$legacyPreferenceSuffix" }
    val audioTrackLabelKey = remember(preferenceSuffix) { "audio_track_label_$preferenceSuffix" }
    val legacyAudioTrackLabelKey = remember(legacyPreferenceSuffix) { "audio_track_label_$legacyPreferenceSuffix" }
    val subtitleTrackIdKey = remember(preferenceSuffix) { "subtitle_track_id_$preferenceSuffix" }
    val legacySubtitleTrackIdKey = remember(legacyPreferenceSuffix) { "subtitle_track_id_$legacyPreferenceSuffix" }
    val subtitleTrackLabelKey = remember(preferenceSuffix) { "subtitle_track_label_$preferenceSuffix" }
    val legacySubtitleTrackLabelKey = remember(legacyPreferenceSuffix) { "subtitle_track_label_$legacyPreferenceSuffix" }
    val externalSubtitleUriKey = remember(preferenceSuffix) { "external_subtitle_uri_$preferenceSuffix" }
    val legacyExternalSubtitleUriKey = remember(legacyPreferenceSuffix) { "external_subtitle_uri_$legacyPreferenceSuffix" }
    val subtitleDelayKey = remember(preferenceSuffix) { "subtitle_delay_us_$preferenceSuffix" }
    val legacySubtitleDelayKey = remember(legacyPreferenceSuffix) { "subtitle_delay_us_$legacyPreferenceSuffix" }
    val savedAudioTrackId = remember(video.uri, playerPreferences) {
        playerPreferences.getInt(audioTrackIdKey, playerPreferences.getInt(legacyAudioTrackIdKey, Int.MIN_VALUE))
    }
    val savedAudioTrackLabel = remember(video.uri, playerPreferences) {
        playerPreferences.getString(audioTrackLabelKey, playerPreferences.getString(legacyAudioTrackLabelKey, null))
    }
    val savedSubtitleTrackId = remember(video.uri, playerPreferences) {
        playerPreferences.getInt(subtitleTrackIdKey, playerPreferences.getInt(legacySubtitleTrackIdKey, Int.MIN_VALUE))
    }
    val savedSubtitleTrackLabel = remember(video.uri, playerPreferences) {
        playerPreferences.getString(subtitleTrackLabelKey, playerPreferences.getString(legacySubtitleTrackLabelKey, null))
    }
    val savedExternalSubtitleUri = remember(video.uri, playerPreferences) {
        playerPreferences.getString(externalSubtitleUriKey, playerPreferences.getString(legacyExternalSubtitleUriKey, null))
    }
    val savedSubtitleDelayUs = remember(video.uri, playerPreferences) {
        playerPreferences.getLong(subtitleDelayKey, playerPreferences.getLong(legacySubtitleDelayKey, 0L))
    }
    var isFavourite by remember(video.uri, playerPreferences) {
        mutableStateOf(playerPreferences.getBoolean(favouriteKey, playerPreferences.getBoolean(legacyFavouriteKey, false)))
    }
    val bookmarksKey = remember(preferenceSuffix) { "bookmarks_$preferenceSuffix" }
    val legacyBookmarksKey = remember(legacyPreferenceSuffix) { "bookmarks_$legacyPreferenceSuffix" }
    var bookmarksMs by remember(video.uri, playerPreferences) {
        mutableStateOf(
            playerPreferences.getStringSet(bookmarksKey, playerPreferences.getStringSet(legacyBookmarksKey, emptySet()))
                ?.mapNotNull { it.toLongOrNull() }
                ?.toSet()
                ?: emptySet()
        )
    }
    val savedPositionMs = remember(video.uri, playerPreferences) {
        playerPreferences.getLong(progressKey, playerPreferences.getLong(legacyProgressKey, 0L))
    }
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
    val shouldSampleBackdrop = rememberUpdatedState(controlsVisible || panel != PlayerPanel.None)
    var controlsLocked by remember(video.uri) { mutableStateOf(false) }
    var showUnlockButton by remember(video.uri) { mutableStateOf(false) }
    var manualRotation by remember(video.uri) { mutableStateOf(false) }
    var isPlaying by remember { mutableStateOf(false) }
    var playbackError by remember(video.uri) { mutableStateOf<String?>(null) }
    var cacheRetryProgress by remember(video.uri) { mutableStateOf<Pair<Long, Long?>?>(null) }
    var cacheRetryJob by remember(video.uri) { mutableStateOf<kotlinx.coroutines.Job?>(null) }
    var positionMs by remember(video.uri) { mutableLongStateOf(savedPositionMs) }
    var durationMs by remember(video.uri) { mutableLongStateOf(video.durationMs) }
    val latestAudioFocusLoss = rememberUpdatedState {
        controller.player.pause()
        val current = runCatching { controller.player.time }
            .getOrDefault(positionMs)
            .coerceAtLeast(0L)
        val length = runCatching { controller.player.length }
            .getOrDefault(durationMs)
        val storedPosition = if (length > 0L && current >= length - 2_500L) 0L else current
        positionMs = storedPosition
        playerPreferences.edit()
            .putLong(progressKey, storedPosition)
            .remove(legacyProgressKey)
            .apply()
        isPlaying = false
        Toast.makeText(
            context,
            "Playback paused because another app, an audio-route change, or a phone call needs audio.",
            Toast.LENGTH_SHORT
        ).show()
    }
    val audioFocus = remember(context, controller) {
        PlaybackAudioFocus(context) { latestAudioFocusLoss.value() }
    }
    var showRemainingTime by remember(video.uri) { mutableStateOf(false) }
    var audioTracks by remember(video.uri) { mutableStateOf<List<TrackOption>>(emptyList()) }
    var subtitleTracks by remember(video.uri) { mutableStateOf<List<TrackOption>>(emptyList()) }
    var selectedAudioTrackId by remember(video.uri) { mutableIntStateOf(-1) }
    var selectedSubtitleTrackId by remember(video.uri) { mutableIntStateOf(-1) }
    var trackSelectionMessage by remember(video.uri) { mutableStateOf<String?>(null) }
    var resumePositionApplied by remember(video.uri) { mutableStateOf(savedPositionMs < 1_500L) }
    var brightness by remember(video.uri, playerPreferences) {
        mutableFloatStateOf(
            playerPreferences.getFloat(
                lastBrightnessKey,
                activity?.window?.attributes?.screenBrightness?.takeIf { it in 0f..1f } ?: 0.8f
            ).coerceIn(0f, 1f)
        )
    }
    var speed by remember(video.uri) { mutableFloatStateOf(1f) }
    // The Android media stream is the single source of truth for both gesture and hardware volume.
    var volume by remember(video.uri, audioManager) {
        mutableIntStateOf(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).coerceIn(0, systemVolumeMax))
    }
    var verticalGestureZone by remember { mutableIntStateOf(0) }
    var controlsInteractionToken by remember { mutableIntStateOf(0) }
    var adjustmentMode by remember { mutableIntStateOf(0) }
    var seekFeedbackDirection by remember { mutableIntStateOf(0) }
    var seekFeedbackAmountMs by remember { mutableLongStateOf(0L) }
    var seekFeedbackToken by remember { mutableIntStateOf(0) }
    var swipeSeekPreview by remember(video.uri) { mutableStateOf<HorizontalSeekPreview?>(null) }
    var swipeSeekPreviewPoint by remember(video.uri) { mutableStateOf(Offset.Zero) }
    var showSwipeSeekHint by remember(video.uri) { mutableStateOf(false) }
    var videoAspectRatio by remember(video.uri) { mutableFloatStateOf(16f / 9f) }
    var selectedAspectIndex by remember(video.uri) { mutableIntStateOf(0) }
    var showAspectRatioFeedback by remember { mutableStateOf(false) }
    var subtitleDelayUs by remember(video.uri) { mutableLongStateOf(savedSubtitleDelayUs) }
    val aspectRatios = remember { listOf("Original" to null, "16:9" to "16:9", "4:3" to "4:3", "1:1" to "1:1", "21:9" to "21:9", "2.35:1" to "2.35:1") }

    val persistProgress = remember(video.uri, controller, playerPreferences) {
        {
            val current = runCatching { controller.player.time }.getOrDefault(positionMs).coerceAtLeast(0L)
            val length = runCatching { controller.player.length }.getOrDefault(durationMs)
            val storedPosition = if (length > 0L && current >= length - 2_500L) 0L else current
            playerPreferences.edit().putLong(progressKey, storedPosition).remove(legacyProgressKey).apply()
        }
    }
    val latestPersistProgress = rememberUpdatedState(persistProgress)

    fun findSavedTrack(
        tracks: List<TrackOption>,
        preferredId: Int,
        preferredLabel: String?
    ): TrackOption? {
        if (preferredId != Int.MIN_VALUE) {
            tracks.firstOrNull { it.id == preferredId }?.let { return it }
        }
        val label = preferredLabel?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        return tracks.firstOrNull { it.label.trim().equals(label, ignoreCase = true) }
    }

    fun persistBookmarks(updated: Set<Long>) {
        bookmarksMs = updated.toSortedSet()
        playerPreferences.edit()
            .putStringSet(bookmarksKey, bookmarksMs.map { it.toString() }.toSet())
            .remove(legacyBookmarksKey)
            .apply()
    }

    fun togglePlayback() {
        val playingNow = runCatching { controller.player.isPlaying }.getOrDefault(false)
        if (playingNow) {
            controller.player.pause()
            persistProgress()
            isPlaying = false
            audioFocus.abandon()
        } else if (audioFocus.request()) {
            controller.player.play()
        } else {
            Toast.makeText(
                context,
                "LUCENT couldn't get audio focus. Another app or a phone call may be using audio.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    fun startCacheRetry() {
        cacheRetryJob?.cancel()
        playbackError = "Preparing a safe local copy for playback…"
        cacheRetryProgress = 0L to null
        cacheRetryJob = retryScope.launch {
            val error = controller.retryUsingCachedCopy(video.uri) { copiedBytes, expectedBytes ->
                cacheRetryProgress = copiedBytes to expectedBytes
            }
            cacheRetryProgress = null
            cacheRetryJob = null
            playbackError = error
        }
    }

    DisposableEffect(lifecycleOwner, activity, controller, audioFocus) {
        val observer = LifecycleEventObserver { _, event ->
            if (
                event == Lifecycle.Event.ON_STOP &&
                !(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N &&
                    activity?.isInPictureInPictureMode == true) &&
                runCatching { controller.player.isPlaying }.getOrDefault(false)
            ) {
                controller.player.pause()
                latestPersistProgress.value()
                isPlaying = false
                audioFocus.abandon()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    BackHandler(enabled = !isInPictureInPictureMode) {
        if (controlsLocked) {
            controlsLocked = false
            showUnlockButton = false
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
    LaunchedEffect(controlsLocked, showUnlockButton, controlsInteractionToken) {
        if (controlsLocked && showUnlockButton) {
            delay(2_500L)
            showUnlockButton = false
        }
    }
    LaunchedEffect(adjustmentMode, brightness, volume) {
        if (adjustmentMode != 0) { delay(900L); adjustmentMode = 0 }
    }
    LaunchedEffect(seekFeedbackDirection, seekFeedbackToken) {
        if (seekFeedbackDirection != 0) {
            delay(750L)
            seekFeedbackDirection = 0
            seekFeedbackAmountMs = 0L
        }
    }
    LaunchedEffect(showAspectRatioFeedback) {
        if (showAspectRatioFeedback) { delay(1_200L); showAspectRatioFeedback = false }
    }
    LaunchedEffect(video.uri, gesturesEnabled, controlsLocked) {
        if (
            gesturesEnabled &&
            !controlsLocked &&
            !playerPreferences.getBoolean("horizontal_seek_hint_seen", false)
        ) {
            playerPreferences.edit().putBoolean("horizontal_seek_hint_seen", true).apply()
            showSwipeSeekHint = true
            delay(6_000L)
            showSwipeSeekHint = false
        } else {
            showSwipeSeekHint = false
        }
    }
    // A new video inherits the last brightness selected in the player.
    LaunchedEffect(video.uri, activity) {
        activity?.window?.let { window ->
            val params = window.attributes
            params.screenBrightness = brightness.coerceIn(0f, 1f)
            window.attributes = params
        }
    }
    // Keep the display awake only while media is actively playing (including PiP).
    LaunchedEffect(activity, isPlaying) {
        activity?.window?.let { window ->
            if (isPlaying) window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            else window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
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
            if (added) {
                playerPreferences.edit()
                    .putString(externalSubtitleUriKey, uri.toString())
                    .remove(legacyExternalSubtitleUriKey)
                    .remove(subtitleTrackIdKey)
                    .remove(legacySubtitleTrackIdKey)
                    .remove(subtitleTrackLabelKey)
                    .remove(legacySubtitleTrackLabelKey)
                    .apply()
                selectedSubtitleTrackId = controller.currentSubtitleTrackId()
            }
            trackSelectionMessage = if (added) "External subtitle added" else "VLC couldn't add this subtitle file."
            panel = PlayerPanel.Subtitles
        }
    }

    DisposableEffect(activity, controller, audioFocus) {
        val originalOrientation = activity?.requestedOrientation
        val originalVolumeControlStream = activity?.volumeControlStream ?: AudioManager.USE_DEFAULT_STREAM_TYPE
        val originalKeepScreenOn = activity?.window?.let { window ->
            window.attributes.flags and WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON != 0
        } ?: false
        activity?.volumeControlStream = AudioManager.STREAM_MUSIC
        val decorView = activity?.window?.decorView
        val windowController = activity?.let {
            WindowInsetsControllerCompat(it.window, it.window.decorView)
        }
        windowController?.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        // Hide after attachment and whenever the shade/notification panel returns focus.
        decorView?.post { windowController?.hide(WindowInsetsCompat.Type.systemBars()) }
        val focusListener = ViewTreeObserver.OnWindowFocusChangeListener { hasFocus ->
            if (hasFocus) {
                decorView?.post { windowController?.hide(WindowInsetsCompat.Type.systemBars()) }
            }
        }
        if (decorView?.viewTreeObserver?.isAlive == true) {
            decorView.viewTreeObserver.addOnWindowFocusChangeListener(focusListener)
        }
        val originalBrightness = activity?.window?.attributes?.screenBrightness ?: -1f
        onDispose {
            if (decorView?.viewTreeObserver?.isAlive == true) {
                decorView.viewTreeObserver.removeOnWindowFocusChangeListener(focusListener)
            }
            windowController?.show(WindowInsetsCompat.Type.systemBars())
            if (activity != null) activity.volumeControlStream = originalVolumeControlStream
            activity?.window?.let { window ->
                if (originalKeepScreenOn) window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                else window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
            activity?.window?.let { window ->
                val params = window.attributes
                params.screenBrightness = originalBrightness
                window.attributes = params
            }
            if (activity != null) activity.requestedOrientation = originalOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            audioFocus.close()
            controller.close()
        }
    }

    LaunchedEffect(video.uri, controller, attachedController) {
        if (attachedController !== controller) return@LaunchedEffect
        if (!audioFocus.request()) {
            playbackError = "Audio focus is unavailable. Press Play when the other audio interruption has ended."
            return@LaunchedEffect
        }
        resumePositionApplied = savedPositionMs < 1_500L
        val immediateError = controller.play(video.uri)
        if (immediateError != null) {
            audioFocus.abandon()
            if (video.uri.scheme == "content" && cacheRetryAttempted.compareAndSet(false, true)) {
                if (audioFocus.request()) startCacheRetry() else playbackError = immediateError
            } else {
                playbackError = immediateError
            }
        }
        // Restore this video's selected streams after libVLC has opened its tracks.
        // Track IDs are preferred; track labels provide a fallback if libVLC reorders IDs.
        launch {
            var audioRestored = savedAudioTrackId == Int.MIN_VALUE
            var subtitleRestored =
                savedSubtitleTrackId == Int.MIN_VALUE && savedExternalSubtitleUri == null
            var delayRestored = savedSubtitleDelayUs == 0L
            var externalSubtitleAttempted = false

            repeat(40) {
                if (!isActive) return@launch
                val availableAudio = controller.audioTracks()
                val availableSubtitles = controller.subtitleTracks()
                audioTracks = availableAudio
                subtitleTracks = availableSubtitles
                val mediaReady = runCatching { controller.player.isPlaying }.getOrDefault(false) ||
                    availableAudio.isNotEmpty() || availableSubtitles.isNotEmpty()

                if (mediaReady) {
                    if (!audioRestored && availableAudio.isNotEmpty()) {
                        val match = findSavedTrack(availableAudio, savedAudioTrackId, savedAudioTrackLabel)
                        if (match != null) {
                            audioRestored = controller.currentAudioTrackId() == match.id ||
                                controller.selectAudioTrack(match.id)
                        }
                    }

                    if (!subtitleRestored) {
                        if (savedExternalSubtitleUri != null && !externalSubtitleAttempted) {
                            externalSubtitleAttempted = true
                            subtitleRestored = controller.addExternalSubtitle(Uri.parse(savedExternalSubtitleUri))
                        } else if (savedSubtitleTrackId == -1) {
                            subtitleRestored = controller.currentSubtitleTrackId() == -1 ||
                                controller.selectSubtitleTrack(-1)
                        } else if (availableSubtitles.isNotEmpty()) {
                            val match = findSavedTrack(
                                availableSubtitles,
                                savedSubtitleTrackId,
                                savedSubtitleTrackLabel
                            )
                            if (match != null) {
                                subtitleRestored = controller.currentSubtitleTrackId() == match.id ||
                                    controller.selectSubtitleTrack(match.id)
                            }
                        }
                    }

                    if (!delayRestored) {
                        delayRestored = controller.setSubtitleDelayUs(savedSubtitleDelayUs)
                    }
                }

                if (audioRestored && subtitleRestored && delayRestored) {
                    selectedAudioTrackId = controller.currentAudioTrackId()
                    selectedSubtitleTrackId = controller.currentSubtitleTrackId()
                    return@launch
                }
                delay(250L)
            }
            selectedAudioTrackId = controller.currentAudioTrackId()
            selectedSubtitleTrackId = controller.currentSubtitleTrackId()
        }

        var lastSavedAt = SystemClock.elapsedRealtime()
        while (isActive) {
            val currentTime = runCatching { controller.player.time }.getOrDefault(0L).coerceAtLeast(0L)
            val mediaLength = runCatching { controller.player.length }.getOrDefault(0L)
            val playingNow = runCatching { controller.player.isPlaying }.getOrDefault(false)
            positionMs = currentTime
            if (mediaLength > 0L) durationMs = mediaLength
            isPlaying = playingNow
            // Reflect physical volume-button changes in the on-screen gesture indicator.
            val systemVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).coerceIn(0, systemVolumeMax)
            if (systemVolume != volume) volume = systemVolume
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
                playerPreferences.edit().putLong(progressKey, storedPosition).remove(legacyProgressKey).apply()
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
                MediaPlayer.Event.EndReached -> {
                    mainHandler.post {
                        audioFocus.abandon()
                        isPlaying = false
                    }
                }
                MediaPlayer.Event.EncounteredError -> {
                    val shouldRetry = video.uri.scheme == "content" &&
                        cacheRetryAttempted.compareAndSet(false, true)
                    mainHandler.post {
                        if (shouldRetry && audioFocus.hasFocus) {
                            startCacheRetry()
                        } else {
                            audioFocus.abandon()
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

    BoxWithConstraints(Modifier.fillMaxSize().background(Color.Black).pointerInput(video.uri, controlsLocked) {
        awaitEachGesture {
            awaitFirstDown(pass = PointerEventPass.Initial)
            controlsInteractionToken++
            if (controlsLocked) showUnlockButton = true
        }
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
                if (!shouldSampleBackdrop.value) {
                    delay(500L)
                    continue
                }
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
                // Refresh decorative glass at 10 Hz, only while controls/panels are visible.
                delay(100L)
            }
        }

        playbackError?.let { message ->
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(24.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Black.copy(alpha = 0.86f))
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(message, color = Color.White, style = MaterialTheme.typography.bodyMedium)
                cacheRetryProgress?.let { (copiedBytes, expectedBytes) ->
                    val progressText = expectedBytes?.let {
                        "${Formatter.formatFileSize(context, copiedBytes)} / ${Formatter.formatFileSize(context, it)}"
                    } ?: "${Formatter.formatFileSize(context, copiedBytes)} copied"
                    Text(progressText, color = Color.LightGray, style = MaterialTheme.typography.labelSmall)
                    TextButton(
                        onClick = {
                            cacheRetryJob?.cancel()
                            cacheRetryJob = null
                            cacheRetryProgress = null
                            playbackError = "Local playback retry cancelled."
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = Color.White)
                    ) {
                        Text("Cancel")
                    }
                }
            }
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
                            val direction = when {
                                offset.x < size.width * 0.30f -> -1
                                offset.x > size.width * 0.70f -> 1
                                else -> 0
                            }
                            when (direction) {
                                -1 -> {
                                    controller.seekBy(-10_000L)
                                    seekFeedbackAmountMs = if (seekFeedbackDirection == -1) seekFeedbackAmountMs + 10_000L else 10_000L
                                    seekFeedbackDirection = -1
                                    seekFeedbackToken++
                                }
                                1 -> {
                                    controller.seekBy(10_000L)
                                    seekFeedbackAmountMs = if (seekFeedbackDirection == 1) seekFeedbackAmountMs + 10_000L else 10_000L
                                    seekFeedbackDirection = 1
                                    seekFeedbackToken++
                                }
                                else -> togglePlayback()
                            }
                            // Seeking must not force the transport controls on screen.
                            // Keep their current visibility exactly as it was before the double tap.
                            panel = PlayerPanel.None
                        } else null
                    )
                }
                .pointerInput(gesturesEnabled, video.uri, controlsLocked, durationMs) {
                    if (gesturesEnabled && !controlsLocked) {
                        // 0 = undecided, 1 = horizontal seek, 2 = vertical adjustment.
                        // One detector arbitrates both axes so the two gestures cannot race.
                        var dragAxis = 0
                        var dragStart = Offset.Zero
                        var accumulatedDragDistance = Offset.Zero
                        var seekStartMs = 0L
                        var seekTargetMs = 0L
                        var volumeAtDragStart = 0
                        var accumulatedVolumeDelta = 0f
                        var lastAppliedVolume = 0

                        fun syncVolumeAfterDrag() {
                            val finalVolume = audioManager
                                .getStreamVolume(AudioManager.STREAM_MUSIC)
                                .coerceIn(0, systemVolumeMax)
                            if (volume != finalVolume) volume = finalVolume
                        }

                        detectDragGestures(
                            onDragStart = { start ->
                                dragAxis = 0
                                dragStart = start
                                accumulatedDragDistance = Offset.Zero
                                adjustmentMode = 0
                                swipeSeekPreview = null
                                controlsInteractionToken++
                            },
                            onDragEnd = {
                                when (dragAxis) {
                                    1 -> {
                                        swipeSeekPreview?.let { preview ->
                                            if (preview.deltaMs != 0L) {
                                                controller.seekTo(preview.targetPositionMs)
                                                positionMs = preview.targetPositionMs
                                                seekFeedbackDirection = if (preview.deltaMs > 0L) 1 else -1
                                                seekFeedbackAmountMs = abs(preview.deltaMs)
                                                seekFeedbackToken++
                                            }
                                        }
                                        swipeSeekPreview = null
                                    }
                                    2 -> syncVolumeAfterDrag()
                                }
                            },
                            onDragCancel = {
                                swipeSeekPreview = null
                                if (dragAxis == 2) syncVolumeAfterDrag()
                            },
                            onDrag = { change, dragAmount ->
                                accumulatedDragDistance += dragAmount
                                if (dragAxis == 0) {
                                    when (
                                        resolvePlayerDragAxis(
                                            accumulatedDragDistance.x,
                                            accumulatedDragDistance.y
                                        )
                                    ) {
                                        PlayerDragAxis.Horizontal -> {
                                            dragAxis = 1
                                            seekStartMs = runCatching { controller.player.time }
                                                .getOrDefault(positionMs)
                                                .coerceIn(0L, durationMs.coerceAtLeast(0L))
                                            seekTargetMs = seekStartMs
                                        }
                                        PlayerDragAxis.Vertical -> {
                                            dragAxis = 2
                                            verticalGestureZone = when {
                                                dragStart.x < size.width * 0.40f -> -1
                                                dragStart.x > size.width * 0.60f -> 1
                                                else -> 0
                                            }
                                            adjustmentMode = verticalGestureZone
                                            if (verticalGestureZone == 1) {
                                                volumeAtDragStart = audioManager
                                                    .getStreamVolume(AudioManager.STREAM_MUSIC)
                                                    .coerceIn(0, systemVolumeMax)
                                                lastAppliedVolume = volumeAtDragStart
                                                accumulatedVolumeDelta = 0f
                                                if (volume != volumeAtDragStart) volume = volumeAtDragStart
                                            }
                                        }
                                        null -> Unit
                                    }
                                }

                                when (dragAxis) {
                                    1 -> {
                                        val dragDistance = accumulatedDragDistance.x
                                        seekTargetMs = horizontalSeekTargetMs(
                                            startPositionMs = seekStartMs,
                                            durationMs = durationMs,
                                            dragDistancePx = dragDistance,
                                            surfaceWidthPx = size.width.toFloat()
                                        )
                                        swipeSeekPreview = HorizontalSeekPreview(seekStartMs, seekTargetMs)
                                        swipeSeekPreviewPoint = change.position
                                        controlsInteractionToken++
                                    }
                                    2 -> {
                                        controlsInteractionToken++
                                        when (verticalGestureZone) {
                                            -1 -> {
                                                adjustmentMode = -1
                                                brightness = (
                                                    brightness - dragAmount.y / size.height * 1.75f
                                                ).coerceIn(0f, 1f)
                                                playerPreferences.edit().putFloat(lastBrightnessKey, brightness).apply()
                                                activity?.window?.let { window ->
                                                    val params = window.attributes
                                                    params.screenBrightness = brightness
                                                    window.attributes = params
                                                }
                                            }
                                            1 -> {
                                                adjustmentMode = 1
                                                // Accumulate fractional movement to keep low-speed volume changes precise.
                                                accumulatedVolumeDelta -=
                                                    dragAmount.y / size.height * systemVolumeMax * 1.75f
                                                val targetVolume = (
                                                    volumeAtDragStart + accumulatedVolumeDelta.roundToInt()
                                                ).coerceIn(0, systemVolumeMax)
                                                if (targetVolume != lastAppliedVolume) {
                                                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetVolume, 0)
                                                    lastAppliedVolume = targetVolume
                                                    volume = targetVolume
                                                }
                                            }
                                        }
                                    }
                                }
                                change.consume()
                            }
                        )
                    }
                }
        )


        if (seekFeedbackDirection != 0 && !isInPictureInPictureMode) {
            SeekGestureIndicator(
                direction = seekFeedbackDirection,
                amountMs = seekFeedbackAmountMs,
                modifier = Modifier.fillMaxSize()
            )
        }

        swipeSeekPreview?.takeIf { !isInPictureInPictureMode }?.let { preview ->
            val density = LocalDensity.current
            val previewX = (
                with(density) { swipeSeekPreviewPoint.x.toDp() } - 84.dp
            ).coerceIn(12.dp, (screenWidth - 168.dp).coerceAtLeast(12.dp))
            val previewY = (
                with(density) { swipeSeekPreviewPoint.y.toDp() } - 88.dp
            ).coerceIn(80.dp, (maxHeight - 180.dp).coerceAtLeast(80.dp))
            Column(
                Modifier.offset(x = previewX, y = previewY)
                    .clip(RoundedCornerShape(22.dp))
                    .background(Color.Black.copy(alpha = 0.76f))
                    .padding(horizontal = 22.dp, vertical = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    if (preview.deltaMs < 0L) "Rewind" else "Forward",
                    color = Color.White.copy(alpha = 0.82f),
                    style = MaterialTheme.typography.labelMedium
                )
                Text(
                    formatDuration(preview.targetPositionMs),
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "${if (preview.deltaMs < 0L) "−" else "+"}${abs(preview.deltaMs) / 1_000L}s",
                    color = Color.White.copy(alpha = 0.76f),
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }

        if (showSwipeSeekHint && !isInPictureInPictureMode && gesturesEnabled && !controlsLocked) {
            KyantGlassSurface(
                backdrop = backdrop,
                modifier = Modifier.align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 148.dp)
                    .widthIn(max = 340.dp),
                shape = RoundedCornerShape(18.dp)
            ) {
                Row(
                    Modifier.padding(start = 16.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Swipe left or right across the video to seek",
                        color = Color.White,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { showSwipeSeekHint = false }) {
                        Icon(Icons.Default.Close, contentDescription = "Dismiss seeking tip", tint = Color.White)
                    }
                }
            }
        }

        if (adjustmentMode != 0 && !isInPictureInPictureMode) {
            Box(Modifier.align(if (adjustmentMode < 0) Alignment.CenterEnd else Alignment.CenterStart).padding(start = if (adjustmentMode > 0) 28.dp else 0.dp, end = if (adjustmentMode < 0) 28.dp else 0.dp)) {
                AdjustmentIndicator(
                    icon = if (adjustmentMode < 0) Icons.Default.Brightness6 else Icons.Default.VolumeUp,
                    value = if (adjustmentMode < 0) "${(brightness * 100).roundToInt()}%" else "${(volume.toFloat() / systemVolumeMax * 100f).roundToInt()}%",
                    level = if (adjustmentMode < 0) brightness else (volume.toFloat() / systemVolumeMax),
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

                Row(Modifier.fillMaxWidth().height(48.dp), verticalAlignment = Alignment.CenterVertically) {
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
                        if (showRemainingTime) "-${formatDuration((durationMs - positionMs).coerceAtLeast(0L))}"
                        else formatDuration(durationMs),
                        modifier = Modifier.widthIn(min = 54.dp).clickable {
                            showRemainingTime = !showRemainingTime
                        },
                        color = Color.White,
                        style = MaterialTheme.typography.labelMedium,
                        textAlign = androidx.compose.ui.text.style.TextAlign.End
                    )
                }
                Spacer(Modifier.height(if (isLandscape) 12.dp else 8.dp))

                val currentVideoIndex = playlist.indexOfFirst { it.uri == video.uri }
                val rotateScreen: () -> Unit = {
                    manualRotation = true
                    val targetOrientation = if (isLandscape) {
                        ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                    } else {
                        ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                    }
                    activity?.requestedOrientation = targetOrientation
                    controlsInteractionToken++
                }
                val lockControls: () -> Unit = {
                    controlsLocked = true
                    showUnlockButton = true
                    panel = PlayerPanel.None
                    controlsVisible = false
                    controlsInteractionToken++
                }
                Box(Modifier.fillMaxWidth().height(54.dp)) {
                    if (isLandscape) {
                        // In landscape, rotation stays at the far left.
                        GlassIconButton(
                            backdrop,
                            Icons.Default.ScreenRotation,
                            "Rotate screen",
                            modifier = Modifier.align(Alignment.CenterStart).size(width = 48.dp, height = 44.dp),
                            onClick = rotateScreen
                        )
                    }

                    Row(
                        Modifier.align(Alignment.Center),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(if (isLandscape) 10.dp else 8.dp)
                    ) {
                        if (isLandscape) {
                            GlassIconButton(
                                backdrop,
                                Icons.Default.Lock,
                                "Lock controls",
                                modifier = Modifier.size(width = 68.dp, height = 44.dp),
                                onClick = lockControls
                            )
                        } else {
                            // Portrait has no lock control: rotation occupies the lock slot.
                            GlassIconButton(
                                backdrop,
                                Icons.Default.ScreenRotation,
                                "Rotate screen",
                                modifier = Modifier.size(width = 48.dp, height = 44.dp),
                                onClick = rotateScreen
                            )
                        }
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
                            onClick = { togglePlayback() }
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
                    if (
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                        activity != null &&
                        context.packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)
                    ) {
                        GlassIconButton(
                            backdrop,
                            Icons.Default.PictureInPictureAlt,
                            "Picture-in-picture",
                            modifier = Modifier.align(Alignment.CenterEnd).size(width = 48.dp, height = 44.dp),
                            onClick = {
                                persistProgress()
                                val ratio = videoAspectRatio.coerceIn(0.42f, 2.39f)
                                runCatching {
                                    activity.enterPictureInPictureMode(
                                        PictureInPictureParams.Builder()
                                            .setAspectRatio(
                                                Rational((ratio * 1000).toInt().coerceAtLeast(1), 1000)
                                            )
                                            .build()
                                    )
                                }
                            }
                        )
                    }
                }
            }
        }

        if (controlsLocked && showUnlockButton && !isInPictureInPictureMode) {
            Box(Modifier.align(Alignment.CenterStart).padding(start = 24.dp)) {
                GlassIconButton(
                    backdrop,
                    Icons.Default.LockOpen,
                    "Unlock controls",
                    modifier = Modifier.size(width = 48.dp, height = 44.dp),
                    onClick = {
                        controlsLocked = false
                        showUnlockButton = false
                        controlsVisible = true
                        controlsInteractionToken++
                    }
                )
            }
        }

        if (panel != PlayerPanel.None && !isInPictureInPictureMode) {
            BoxWithConstraints(
                Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.20f)).clickable(
                    indication = null,
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                ) { panel = PlayerPanel.None },
                contentAlignment = when {
                    isLandscape -> Alignment.CenterEnd
                    panel == PlayerPanel.AspectRatio -> Alignment.BottomCenter
                    else -> Alignment.TopCenter
                }
            ) {
                KyantGlassSurface(
                    backdrop = backdrop,
                    modifier = Modifier.width(if (isLandscape) 340.dp else (screenWidth - 24.dp).coerceAtLeast(280.dp))
                        .padding(12.dp)
                        .then(
                            if (isLandscape) Modifier.fillMaxHeight()
                            else Modifier.heightIn(max = (maxHeight - 24.dp).coerceAtLeast(0.dp)).wrapContentHeight()
                        )
                        .clickable(
                            indication = null,
                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                        ) {},
                    shape = RoundedCornerShape(28.dp)
                ) {
                    Column(
                        Modifier.fillMaxWidth()
                            .then(if (isLandscape) Modifier.fillMaxHeight() else Modifier.wrapContentHeight())
                            .padding(18.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                when (panel) {
                                    PlayerPanel.Audio -> "Audio track"
                                    PlayerPanel.Subtitles -> "Subtitles"
                                    PlayerPanel.Tools -> "Player tools"
                                    PlayerPanel.AspectRatio -> "Aspect ratio"
                                    PlayerPanel.Speed -> "Playback speed"
                                    PlayerPanel.Bookmarks -> "Bookmarks"
                                    PlayerPanel.Information -> "Information"
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
                                        if (success || selectedAudioTrackId == id) {
                                            val editor = playerPreferences.edit()
                                                .putInt(audioTrackIdKey, id)
                                                .remove(legacyAudioTrackIdKey)
                                                .remove(legacyAudioTrackLabelKey)
                                            audioTracks.firstOrNull { it.id == id }?.let {
                                                editor.putString(audioTrackLabelKey, it.label)
                                            }
                                            editor.apply()
                                        }
                                        trackSelectionMessage = if (success || selectedAudioTrackId == id) "Audio track selected" else "VLC could not select this audio track."
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
                                        if (success || selectedSubtitleTrackId == id) {
                                            val editor = playerPreferences.edit()
                                                .putInt(subtitleTrackIdKey, id)
                                                .remove(legacySubtitleTrackIdKey)
                                                .remove(legacySubtitleTrackLabelKey)
                                                .remove(externalSubtitleUriKey)
                                                .remove(legacyExternalSubtitleUriKey)
                                            subtitleTracks.firstOrNull { it.id == id }?.let {
                                                editor.putString(subtitleTrackLabelKey, it.label)
                                            }
                                            editor.apply()
                                        }
                                        trackSelectionMessage = if (success || selectedSubtitleTrackId == id) "Subtitle track selected" else "VLC could not select this subtitle track."
                                    },
                                    onDisable = {
                                        val success = controller.selectSubtitleTrack(-1)
                                        selectedSubtitleTrackId = controller.currentSubtitleTrackId()
                                        if (success || selectedSubtitleTrackId == -1) {
                                            playerPreferences.edit()
                                                .putInt(subtitleTrackIdKey, -1)
                                                .remove(legacySubtitleTrackIdKey)
                                                .remove(subtitleTrackLabelKey)
                                                .remove(legacySubtitleTrackLabelKey)
                                                .remove(externalSubtitleUriKey)
                                                .remove(legacyExternalSubtitleUriKey)
                                                .apply()
                                        }
                                        trackSelectionMessage = if (success || selectedSubtitleTrackId == -1) "Subtitles disabled" else "VLC could not disable subtitles."
                                    }
                                )
                                Spacer(Modifier.height(14.dp))
                                Text("Subtitle delay", style = MaterialTheme.typography.titleMedium, color = Color.White)
                                Text("Positive values show subtitles later; negative values show them earlier.", style = MaterialTheme.typography.bodySmall, color = Color.LightGray, modifier = Modifier.padding(top = 4.dp, bottom = 8.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    TextButton(onClick = {
                                        val next = (subtitleDelayUs - 100_000L).coerceAtLeast(-10_000_000L)
                                        if (controller.setSubtitleDelayUs(next)) {
                                            subtitleDelayUs = next
                                            playerPreferences.edit().putLong(subtitleDelayKey, next).remove(legacySubtitleDelayKey).apply()
                                        }
                                    }) { Text("−0.1 s", color = Color.White) }
                                    Text(String.format(java.util.Locale.US, "%+.1f s", subtitleDelayUs / 1_000_000.0), modifier = Modifier.weight(1f), color = Color.White, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                                    TextButton(onClick = {
                                        val next = (subtitleDelayUs + 100_000L).coerceAtMost(10_000_000L)
                                        if (controller.setSubtitleDelayUs(next)) {
                                            subtitleDelayUs = next
                                            playerPreferences.edit().putLong(subtitleDelayKey, next).remove(legacySubtitleDelayKey).apply()
                                        }
                                    }) { Text("+0.1 s", color = Color.White) }
                                }
                            }
                            PlayerPanel.Tools -> ToolsPanel(
                                isFavourite = isFavourite,
                                onBookmarks = { panel = PlayerPanel.Bookmarks },
                                onSpeed = { panel = PlayerPanel.Speed },
                                onFavourite = {
                                    isFavourite = !isFavourite
                                    playerPreferences.edit().putBoolean(favouriteKey, isFavourite).remove(legacyFavouriteKey).apply()
                                },
                                onShare = {
                                    runCatching {
                                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = video.mimeType?.takeIf { it.contains("/") } ?: "video/*"
                                            putExtra(Intent.EXTRA_STREAM, video.uri)
                                            clipData = ClipData.newUri(context.contentResolver, video.name, video.uri)
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        context.startActivity(Intent.createChooser(shareIntent, "Share video"))
                                    }
                                },
                                onInformation = { panel = PlayerPanel.Information }
                            )
                            PlayerPanel.AspectRatio -> AspectRatioPanel(selectedAspectIndex) { label, ratio ->
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
                            PlayerPanel.Bookmarks -> {
                                Button(
                                    onClick = {
                                        val current = runCatching { controller.player.time }
                                            .getOrDefault(positionMs).coerceAtLeast(0L)
                                        val alreadySaved = bookmarksMs.any { kotlin.math.abs(it - current) < 1_000L }
                                        if (!alreadySaved) persistBookmarks(bookmarksMs + current)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp)
                                ) {
                                    Icon(Icons.Default.Bookmark, null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Save current position")
                                }
                                if (bookmarksMs.isEmpty()) {
                                    Text("No bookmarks for this video yet.", color = Color.LightGray, style = MaterialTheme.typography.bodySmall)
                                } else {
                                    bookmarksMs.sorted().forEach { bookmark ->
                                        Row(
                                            Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                                                .clickable {
                                                    controller.seekTo(bookmark)
                                                    positionMs = bookmark
                                                    panel = PlayerPanel.None
                                                }
                                                .padding(start = 10.dp, top = 8.dp, bottom = 8.dp, end = 2.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(formatDuration(bookmark), Modifier.weight(1f), color = Color.White)
                                            IconButton(onClick = { persistBookmarks(bookmarksMs - bookmark) }) {
                                                Icon(Icons.Default.Delete, "Delete bookmark", tint = Color.LightGray)
                                            }
                                        }
                                    }
                                }
                            }
                            PlayerPanel.Information -> {
                                val details = listOf(
                                    "File name" to video.name,
                                    "Folder" to video.relativePath.ifBlank { "Unknown" },
                                    "Duration" to formatDuration(durationMs),
                                    "File size" to if (video.sizeBytes > 0L) Formatter.formatFileSize(context, video.sizeBytes) else "Unknown",
                                    "Format" to (video.mimeType ?: "Unknown")
                                )
                                details.forEach { (label, value) ->
                                    Column(Modifier.fillMaxWidth().padding(vertical = 9.dp)) {
                                        Text(label, color = Color.LightGray, style = MaterialTheme.typography.labelSmall)
                                        Text(value, color = Color.White, style = MaterialTheme.typography.bodyMedium)
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
    isFavourite: Boolean,
    onBookmarks: () -> Unit,
    onSpeed: () -> Unit,
    onFavourite: () -> Unit,
    onShare: () -> Unit,
    onInformation: () -> Unit
) {
    val tools = listOf(
        Triple(Icons.Default.Bookmark, "Bookmarks", onBookmarks),
        Triple(Icons.Default.Speed, "Playback speed", onSpeed),
        Triple(Icons.Default.Favorite, if (isFavourite) "Remove from favourites" else "Favourite", onFavourite),
        Triple(Icons.Default.Share, "Share", onShare),
        Triple(Icons.Default.Info, "Information", onInformation)
    )
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        tools.forEach { (icon, label, action) ->
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable(onClick = action).padding(horizontal = 8.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    icon,
                    null,
                    tint = if (isFavourite && label == "Remove from favourites") Color(0xFFFF453A) else Color.White,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.width(12.dp))
                Text(label, Modifier.weight(1f), color = Color.White)
                Text("›", color = Color.Gray, fontSize = 24.sp)
            }
        }
    }
}

@Composable
private fun AspectRatioPanel(
    selectedIndex: Int,
    onSelect: (String, String?) -> Unit
) {
    val ratios = listOf(
        "Original" to null,
        "16:9" to "16:9",
        "4:3" to "4:3",
        "1:1" to "1:1",
        "21:9" to "21:9",
        "2.35:1" to "2.35:1"
    )
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        ratios.forEachIndexed { index, (label, ratio) ->
            val isSelected = index == selectedIndex
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { onSelect(label, ratio) }
                    .semantics { selected = isSelected }
                    .padding(horizontal = 10.dp, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.AspectRatio, null, tint = Color.LightGray, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(12.dp))
                Text(label, Modifier.weight(1f))
                if (isSelected) {
                    Icon(Icons.Default.Check, contentDescription = "Selected", tint = Color.White)
                }
            }
        }
        Text("Ratios are applied by libVLC. Original restores the source aspect ratio.", color = Color.Gray, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 12.dp))
    }
}

@Composable
private fun SeekGestureIndicator(
    direction: Int,
    amountMs: Long,
    modifier: Modifier = Modifier
) {
    val isRewind = direction < 0
    Box(modifier) {
        Canvas(Modifier.fillMaxSize()) {
            // The shade is an oversized elliptical lobe. Its curved edge starts
            // inset from the top, reaches its maximum inward bulge at mid-height,
            // then returns to an inset point at the bottom. The outer area is shaded.
            val edgeInset = size.width * 0.24f
            val midInset = size.width * 0.39f
            val topCurveControl = size.width * 0.33f
            val shadePath = Path().apply {
                if (isRewind) {
                    moveTo(0f, 0f)
                    lineTo(edgeInset, 0f)
                    cubicTo(
                        topCurveControl, 0f,
                        midInset, size.height * 0.22f,
                        midInset, size.height * 0.50f
                    )
                    cubicTo(
                        midInset, size.height * 0.78f,
                        topCurveControl, size.height,
                        edgeInset, size.height
                    )
                    lineTo(0f, size.height)
                    close()
                } else {
                    moveTo(size.width, 0f)
                    lineTo(size.width - edgeInset, 0f)
                    cubicTo(
                        size.width - topCurveControl, 0f,
                        size.width - midInset, size.height * 0.22f,
                        size.width - midInset, size.height * 0.50f
                    )
                    cubicTo(
                        size.width - midInset, size.height * 0.78f,
                        size.width - topCurveControl, size.height,
                        size.width - edgeInset, size.height
                    )
                    lineTo(size.width, size.height)
                    close()
                }
            }
            val shadeBrush = if (isRewind) {
                Brush.horizontalGradient(
                    colors = listOf(Color.Black.copy(alpha = 0.30f), Color.Black.copy(alpha = 0.20f), Color.Transparent),
                    startX = 0f,
                    endX = size.width * 0.44f
                )
            } else {
                Brush.horizontalGradient(
                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.20f), Color.Black.copy(alpha = 0.30f)),
                    startX = size.width * 0.56f,
                    endX = size.width
                )
            }
            drawPath(shadePath, shadeBrush)
        }

        Column(
            modifier = Modifier
                .fillMaxWidth(0.30f)
                .fillMaxHeight()
                .align(if (isRewind) Alignment.CenterStart else Alignment.CenterEnd),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = if (isRewind) Icons.Default.FastRewind else Icons.Default.FastForward,
                contentDescription = if (isRewind) "Rewind ${amountMs / 1_000L} seconds" else "Forward ${amountMs / 1_000L} seconds",
                tint = Color.White,
                modifier = Modifier.size(27.dp)
            )
            Text(
                "${if (isRewind) "−" else "+"}${(amountMs / 1_000L).coerceAtLeast(0L)}s",
                color = Color.White,
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

@Composable
private fun AdjustmentIndicator(icon: ImageVector, value: String, level: Float, label: String) {
    Column(Modifier.width(72.dp).padding(horizontal = 10.dp, vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(icon, label, tint = Color.White, modifier = Modifier.size(23.dp))
        Text(value, color = Color.White, style = MaterialTheme.typography.labelMedium)
        Box(Modifier.width(3.dp).height(80.dp), contentAlignment = Alignment.BottomCenter) {
            Box(Modifier.fillMaxWidth().fillMaxHeight(level.coerceIn(0f, 1f)).clip(RoundedCornerShape(50.dp)).background(Color.White))
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
    // Main-branch capsule dimensions and pointer-to-time mapping.
    val thumbWidth = 30.dp
    val thumbHeight = 16.dp
    val thumbWidthPx = with(LocalDensity.current) { thumbWidth.toPx() }
    val progress = if (durationMs > 0L) {
        (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
    } else 0f
    val latestProgress = rememberUpdatedState(progress)
    val latestPosition = rememberUpdatedState(positionMs)
    val latestDuration = rememberUpdatedState(durationMs)
    val latestOnSeek = rememberUpdatedState(onSeek)
    val isLightTheme = MaterialTheme.colorScheme.background.luminance() > 0.5f
    val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr
    val animationScope = rememberCoroutineScope()

    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = "Playback position"
                stateDescription = "${formatDuration(latestPosition.value)} of ${formatDuration(latestDuration.value)}"
                progressBarRangeInfo = ProgressBarRangeInfo(progress, 0f..1f)
                setProgress { target ->
                    val total = latestDuration.value
                    if (total <= 0L) {
                        false
                    } else {
                        latestOnSeek.value(
                            (target.coerceIn(0f, 1f) * total.toFloat()).toLong()
                        )
                        true
                    }
                }
            }
            .focusable()
            .onKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown || latestDuration.value <= 0L) {
                    false
                } else {
                    val nextPosition = when (event.key) {
                        Key.DirectionLeft -> latestPosition.value - 10_000L
                        Key.DirectionRight -> latestPosition.value + 10_000L
                        else -> null
                    }
                    if (nextPosition == null) {
                        false
                    } else {
                        latestOnSeek.value(nextPosition.coerceIn(0L, latestDuration.value))
                        true
                    }
                }
            },
        contentAlignment = Alignment.CenterStart
    ) {
        val trackWidthPx = constraints.maxWidth.toFloat().coerceAtLeast(1f)
        val usableWidthPx = (trackWidthPx - thumbWidthPx).coerceAtLeast(1f)
        val thumbTravelPx = usableWidthPx

        val dampedDragAnimation = remember(animationScope) {
            DampedDragAnimation(
                animationScope = animationScope,
                initialValue = progress,
                valueRange = 0f..1f,
                visibilityThreshold = 0.001f,
                initialScale = 1f,
                pressedScale = 1.25f,
                onDragStarted = {},
                onDragStopped = {},
                onDrag = { _, _ -> }
            )
        }

        LaunchedEffect(dampedDragAnimation) {
            snapshotFlow { latestProgress.value }.collectLatest { latest ->
                if (dampedDragAnimation.targetValue != latest) {
                    dampedDragAnimation.updateValue(latest)
                }
            }
        }

        fun fractionAt(positionX: Float): Float {
            val fraction = (
                (positionX - thumbWidthPx / 2f) / usableWidthPx
            ).coerceIn(0f, 1f)
            return if (isLtr) fraction else 1f - fraction
        }

        val thumbOffsetPx = thumbTravelPx * dampedDragAnimation.progress
        // Filled progress stops at the back/tail edge of the moving capsule.
        val progressWidthPx = thumbOffsetPx.coerceIn(0f, trackWidthPx)

        // The capsule samples the player frame backdrop directly. The progress stroke
        // is deliberately not exported as a backdrop source, so it cannot refract inside the glass.
        Box(Modifier.fillMaxSize()) {
            Canvas(
                Modifier
                    .fillMaxSize()
                    .pointerInput(dampedDragAnimation, durationMs, usableWidthPx, isLtr) {
                        detectTapGestures { point ->
                            val target = fractionAt(point.x)
                            dampedDragAnimation.animateToValue(target)
                            latestOnSeek.value(
                                (target * latestDuration.value).toLong().coerceAtLeast(0L)
                            )
                        }
                    }
                    .pointerInput(dampedDragAnimation, durationMs, usableWidthPx, isLtr) {
                        detectDragGestures(
                            onDragStart = { point ->
                                val target = fractionAt(point.x)
                                dampedDragAnimation.press()
                                dampedDragAnimation.updateValue(target)
                                latestOnSeek.value(
                                    (target * latestDuration.value).toLong().coerceAtLeast(0L)
                                )
                            },
                            onDragEnd = { dampedDragAnimation.release() },
                            onDragCancel = { dampedDragAnimation.release() },
                            onDrag = { change, _ ->
                                val target = fractionAt(change.position.x)
                                dampedDragAnimation.updateValue(target)
                                latestOnSeek.value(
                                    (target * latestDuration.value).toLong().coerceAtLeast(0L)
                                )
                                change.consume()
                            }
                        )
                    }
            ) {
                val centerY = size.height / 2f
                val stroke = 5.dp.toPx()
                val startX = if (isLtr) 0f else size.width
                val endX = if (isLtr) size.width else 0f
                drawLine(
                    color = Color.White.copy(alpha = 0.30f),
                    start = androidx.compose.ui.geometry.Offset(startX, centerY),
                    end = androidx.compose.ui.geometry.Offset(endX, centerY),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round
                )
                val progressEndX = if (isLtr) {
                    progressWidthPx
                } else {
                    size.width - progressWidthPx
                }
                drawLine(
                    color = Color.White.copy(alpha = 0.88f),
                    start = androidx.compose.ui.geometry.Offset(startX, centerY),
                    end = androidx.compose.ui.geometry.Offset(progressEndX, centerY),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round
                )
            }
        }

        Box(
            Modifier
                .graphicsLayer {
                    translationX = if (isLtr) thumbOffsetPx else -thumbOffsetPx
                }
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { RoundedCornerShape(50.dp) },
                    effects = {
                        val press = dampedDragAnimation.pressProgress
                        vibrancy()
                        blur(8.dp.toPx() * (1f - press))
                        lens(
                            refractionHeight = minOf(
                                24.dp.toPx(),
                                (size.height / 2f - 1.dp.toPx()).coerceAtLeast(0f)
                            ),
                            refractionAmount = 24.dp.toPx(),
                            depthEffect = true,
                            chromaticAberration = false
                        )
                    },
                    highlight = {
                        Highlight.Plain.copy(
                            width = 1.1.dp,
                            blurRadius = 1.25.dp,
                            alpha = 0.9f
                        )
                    },
                    shadow = {
                        Shadow(
                            radius = 14.dp,
                            offset = DpOffset(0.dp, 2.dp),
                            color = Color.Black.copy(alpha = 0.42f),
                            alpha = 0.95f
                        )
                    },
                    innerShadow = {
                        InnerShadow(
                            radius = 2.dp,
                            offset = DpOffset(0.dp, (-0.75).dp),
                            color = Color.White.copy(alpha = 0.14f),
                            alpha = 0.85f
                        )
                    },
                    layerBlock = {
                        scaleX = dampedDragAnimation.scaleX
                        scaleY = dampedDragAnimation.scaleY
                        val velocity = dampedDragAnimation.velocity / 10f
                        scaleX /= 1f - (velocity * 0.75f).fastCoerceIn(-0.2f, 0.2f)
                        scaleY *= 1f - (velocity * 0.25f).fastCoerceIn(-0.2f, 0.2f)
                    },
                    onDrawSurface = {
                        // Same neutral surface paint as GlassSurface(navMaterial = true).
                        val surface = if (isLightTheme) {
                            Color.Black.copy(alpha = 0.04f)
                        } else {
                            Color.White.copy(alpha = 0.055f)
                        }
                        drawRect(surface)
                    }
                )
                .size(width = thumbWidth, height = thumbHeight)
        )
    }
}
