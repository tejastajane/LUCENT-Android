package com.tejas.liquidplayer.player

import org.junit.Assert.assertEquals
import org.junit.Test

class AudioFocusStateMachineTest {
    @Test fun notificationDuckDoesNotPauseOrResume() {
        val machine = AudioFocusStateMachine()
        assertEquals(listOf(AudioFocusAction.Duck), machine.transition(AudioFocusEvent.Duck, true))
        assertEquals(listOf(AudioFocusAction.RestoreVolume), machine.transition(AudioFocusEvent.Gain))
    }

    @Test fun transientLossPausesAndGainResumesPreviouslyPlayingMedia() {
        val machine = AudioFocusStateMachine()
        assertEquals(listOf(AudioFocusAction.Pause), machine.transition(AudioFocusEvent.TransientLoss, true))
        assertEquals(
            listOf(AudioFocusAction.RestoreVolume, AudioFocusAction.Resume),
            machine.transition(AudioFocusEvent.Gain)
        )
    }

    @Test fun transientLossDoesNotResumeMediaThatWasAlreadyPaused() {
        val machine = AudioFocusStateMachine()
        assertEquals(listOf(AudioFocusAction.Pause), machine.transition(AudioFocusEvent.TransientLoss, false))
        assertEquals(listOf(AudioFocusAction.RestoreVolume), machine.transition(AudioFocusEvent.Gain))
    }

    @Test fun permanentLossNeverAutoResumes() {
        val machine = AudioFocusStateMachine()
        machine.transition(AudioFocusEvent.TransientLoss, true)
        assertEquals(listOf(AudioFocusAction.PermanentLoss), machine.transition(AudioFocusEvent.PermanentLoss))
        assertEquals(listOf(AudioFocusAction.RestoreVolume), machine.transition(AudioFocusEvent.Gain))
    }
}
