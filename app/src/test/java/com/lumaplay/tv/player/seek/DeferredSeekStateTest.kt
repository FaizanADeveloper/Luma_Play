package com.lumaplay.tv.player.seek

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DeferredSeekStateTest {

    @Test
    fun testIdleStateDefaults() {
        val idle = DeferredSeekState.IDLE
        assertFalse(idle.isScrubbing)
        assertEquals(0L, idle.previewPositionMs)
        assertEquals(0L, idle.originalPositionMs)
        assertEquals(0L, idle.durationMs)
        assertEquals(0f, idle.progress, 0.001f)
    }

    @Test
    fun testStartScrubbing() {
        val state = DeferredSeekState.startScrubbing(currentPositionMs = 25000L, durationMs = 100000L)
        assertTrue(state.isScrubbing)
        assertEquals(25000L, state.previewPositionMs)
        assertEquals(25000L, state.originalPositionMs)
        assertEquals(100000L, state.durationMs)
        assertEquals(0.25f, state.progress, 0.001f)
    }

    @Test
    fun testStartScrubbingClampedPosition() {
        val belowZero = DeferredSeekState.startScrubbing(currentPositionMs = -100L, durationMs = 50000L)
        assertEquals(0L, belowZero.previewPositionMs)

        val aboveDuration = DeferredSeekState.startScrubbing(currentPositionMs = 60000L, durationMs = 50000L)
        assertEquals(50000L, aboveDuration.previewPositionMs)
    }

    @Test
    fun testStepForwardDoesNotChangeOriginalPosition() {
        val initial = DeferredSeekState.startScrubbing(currentPositionMs = 20000L, durationMs = 60000L)
        val stepped = initial.stepForward(intervalMs = 10000L)

        assertTrue(stepped.isScrubbing)
        assertEquals(30000L, stepped.previewPositionMs)
        assertEquals(20000L, stepped.originalPositionMs) // Original position strictly preserved for cancel
        assertEquals(0.5f, stepped.progress, 0.001f)
    }

    @Test
    fun testStepBackwardDoesNotChangeOriginalPosition() {
        val initial = DeferredSeekState.startScrubbing(currentPositionMs = 20000L, durationMs = 60000L)
        val stepped = initial.stepBackward(intervalMs = 15000L)

        assertTrue(stepped.isScrubbing)
        assertEquals(5000L, stepped.previewPositionMs)
        assertEquals(20000L, stepped.originalPositionMs) // Original position strictly preserved for cancel
    }

    @Test
    fun testStepBoundaryClamping() {
        val initial = DeferredSeekState.startScrubbing(currentPositionMs = 5000L, durationMs = 60000L)
        val steppedBack = initial.stepBackward(intervalMs = 10000L)
        assertEquals(0L, steppedBack.previewPositionMs)

        val nearEnd = DeferredSeekState.startScrubbing(currentPositionMs = 55000L, durationMs = 60000L)
        val steppedForward = nearEnd.stepForward(intervalMs = 10000L)
        assertEquals(60000L, steppedForward.previewPositionMs)
    }

    @Test
    fun testFormattedPreviewPosition() {
        val stateMinutes = DeferredSeekState(previewPositionMs = 125000L, durationMs = 600000L)
        assertEquals("02:05", stateMinutes.formattedPreviewPosition)

        val stateHours = DeferredSeekState(previewPositionMs = 3665000L, durationMs = 7200000L)
        assertEquals("1:01:05", stateHours.formattedPreviewPosition)

        val stateZero = DeferredSeekState(previewPositionMs = 0L, durationMs = 600000L)
        assertEquals("00:00", stateZero.formattedPreviewPosition)
    }

    @Test
    fun testConfirmVersusCancelSemantics() {
        val initial = DeferredSeekState.startScrubbing(currentPositionMs = 15000L, durationMs = 60000L)
        val scrubbed = initial.stepForward(10000L).stepForward(10000L)
        assertEquals(35000L, scrubbed.previewPositionMs)
        assertEquals(15000L, scrubbed.originalPositionMs)

        // Confirm behavior: target is scrub preview position
        val confirmedTarget = scrubbed.previewPositionMs
        assertEquals(35000L, confirmedTarget)

        // Cancel behavior: target remains original playback position
        val cancelledTarget = scrubbed.originalPositionMs
        assertEquals(15000L, cancelledTarget)

        // Post-transition returns to idle
        val idleState = DeferredSeekState.IDLE
        assertFalse(idleState.isScrubbing)
        assertEquals(0L, idleState.previewPositionMs)
    }
}

