package com.tejas.liquidplayer.player

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat

internal enum class AudioFocusEvent { Duck, TransientLoss, Gain, PermanentLoss }
internal enum class AudioFocusAction { Duck, Pause, RestoreVolume, Resume, PermanentLoss }

/** Pure focus transition logic, separated from the Android AudioManager bridge. */
internal class AudioFocusStateMachine {
    private var resumeOnGain = false

    fun transition(event: AudioFocusEvent, wasPlaying: Boolean = false): List<AudioFocusAction> = when (event) {
        AudioFocusEvent.Duck -> listOf(AudioFocusAction.Duck)
        AudioFocusEvent.TransientLoss -> {
            resumeOnGain = wasPlaying
            listOf(AudioFocusAction.Pause)
        }
        AudioFocusEvent.Gain -> {
            val actions = if (resumeOnGain) {
                listOf(AudioFocusAction.RestoreVolume, AudioFocusAction.Resume)
            } else {
                listOf(AudioFocusAction.RestoreVolume)
            }
            resumeOnGain = false
            actions
        }
        AudioFocusEvent.PermanentLoss -> {
            resumeOnGain = false
            listOf(AudioFocusAction.PermanentLoss)
        }
    }

    fun reset() { resumeOnGain = false }
}

/**
 * Owns foreground playback focus and the noisy-output receiver.
 * Transient loss retains the focus request so GAIN can resume only previously-playing media.
 */
internal class PlaybackAudioFocus(
    context: Context,
    private val wasPlaying: () -> Boolean,
    private val onTransientLoss: () -> Unit,
    private val onResumeAfterGain: () -> Unit,
    private val onDuck: () -> Unit,
    private val onRestoreVolume: () -> Unit,
    private val onFocusLost: () -> Unit
) {
    private val appContext = context.applicationContext
    private val audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val stateMachine = AudioFocusStateMachine()
    private var noisyReceiverRegistered = false
    private var requestActive = false

    private val noisyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY && hasFocus) {
                abandon()
                onFocusLost()
            }
        }
    }

    private val listener = AudioManager.OnAudioFocusChangeListener { change ->
        if (!requestActive) return@OnAudioFocusChangeListener
        when (change) {
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                stateMachine.transition(AudioFocusEvent.Duck).forEach(::dispatch)
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                hasFocus = false
                stateMachine.transition(AudioFocusEvent.TransientLoss, wasPlaying()).forEach(::dispatch)
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                hasFocus = true
                registerNoisyReceiver()
                stateMachine.transition(AudioFocusEvent.Gain).forEach(::dispatch)
            }
            AudioManager.AUDIOFOCUS_LOSS -> {
                val actions = stateMachine.transition(AudioFocusEvent.PermanentLoss)
                abandon()
                actions.forEach(::dispatch)
            }
        }
    }

    private val focusRequest: AudioFocusRequest? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MOVIE)
                    .build()
            )
            .setWillPauseWhenDucked(false)
            .setAcceptsDelayedFocusGain(false)
            .setOnAudioFocusChangeListener(listener, Handler(Looper.getMainLooper()))
            .build()
    } else null

    var hasFocus: Boolean = false
        private set

    fun request(): Boolean {
        val hadActiveRequest = requestActive
        val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioManager.requestAudioFocus(requireNotNull(focusRequest))
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(listener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN)
        }
        hasFocus = result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        requestActive = hasFocus || hadActiveRequest
        if (hasFocus) registerNoisyReceiver()
        else if (!requestActive) unregisterNoisyReceiver()
        return hasFocus
    }

    fun abandon() {
        requestActive = false
        hasFocus = false
        stateMachine.reset()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            focusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(listener)
        }
        unregisterNoisyReceiver()
    }

    fun close() = abandon()

    private fun dispatch(action: AudioFocusAction) {
        when (action) {
            AudioFocusAction.Duck -> onDuck()
            AudioFocusAction.Pause -> onTransientLoss()
            AudioFocusAction.RestoreVolume -> onRestoreVolume()
            AudioFocusAction.Resume -> onResumeAfterGain()
            AudioFocusAction.PermanentLoss -> onFocusLost()
        }
    }

    private fun registerNoisyReceiver() {
        if (noisyReceiverRegistered) return
        noisyReceiverRegistered = runCatching {
            ContextCompat.registerReceiver(
                appContext, noisyReceiver, IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY),
                ContextCompat.RECEIVER_NOT_EXPORTED
            )
        }.isSuccess
    }

    private fun unregisterNoisyReceiver() {
        if (!noisyReceiverRegistered) return
        runCatching { appContext.unregisterReceiver(noisyReceiver) }
        noisyReceiverRegistered = false
    }
}
