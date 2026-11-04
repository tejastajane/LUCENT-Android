package com.tejas.liquidplayer

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class IncomingVideoIntentPolicyTest {
    @Test fun freshExternalViewIntentIsCaptured() {
        assertTrue(shouldCaptureIncomingVideo(false, false))
    }

    @Test fun recreatedActivityDoesNotReplayConsumedIntent() {
        assertFalse(shouldCaptureIncomingVideo(true, false))
    }

    @Test fun returningFromRecentsDoesNotReplayOriginalExternalIntent() {
        assertFalse(shouldCaptureIncomingVideo(false, true))
    }
}
