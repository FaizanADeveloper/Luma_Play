package com.lumaplay.tv.player.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Focused unit tests for audio playback timeline seeking calculations and boundary clamping.
 */
class AudioSeekingTest {

    private val sampleDurationMs = 240_000L // 4 minutes

    @Test
    fun testForwardSeekingAdvancesPositionByDelta() {
        val currentPosition = 30_000L
        val delta = 10_000L // +10s
        val target = calculateSeekTarget(currentPosition, delta, sampleDurationMs)

        assertEquals("Forward seek must advance position by 10s", 40_000L, target)
    }

    @Test
    fun testBackwardSeekingRewindsPositionByDelta() {
        val currentPosition = 50_000L
        val delta = -10_000L // -10s
        val target = calculateSeekTarget(currentPosition, delta, sampleDurationMs)

        assertEquals("Backward seek must decrease position by 10s", 40_000L, target)
    }

    @Test
    fun testClampingAtZeroWhenRewindingNearStart() {
        // Position at 5s, rewind 10s -> must clamp to 0
        val targetFrom5s = calculateSeekTarget(5_000L, -10_000L, sampleDurationMs)
        assertEquals("Rewinding past start must clamp to 0 ms", 0L, targetFrom5s)

        // Position at 0s, rewind 10s -> must clamp to 0
        val targetFrom0s = calculateSeekTarget(0L, -10_000L, sampleDurationMs)
        assertEquals("Rewinding at 0 ms must remain at 0 ms", 0L, targetFrom0s)
    }

    @Test
    fun testClampingAtTrackDurationWhenSeekingNearEnd() {
        // Position at 235s on 240s track, forward 10s -> must clamp to 240s
        val currentPosition = 235_000L
        val delta = 10_000L
        val target = calculateSeekTarget(currentPosition, delta, sampleDurationMs)

        assertEquals("Seeking past duration must clamp to track duration", sampleDurationMs, target)

        // Position at exactly duration, forward 10s -> must remain at duration
        val targetAtEnd = calculateSeekTarget(sampleDurationMs, delta, sampleDurationMs)
        assertEquals("Seeking at duration must remain at track duration", sampleDurationMs, targetAtEnd)
    }

    @Test
    fun testUnknownOrInvalidDurationBehaviorReturnsNull() {
        // Zero duration (duration not yet loaded or unknown)
        val targetZeroDuration = calculateSeekTarget(30_000L, 10_000L, 0L)
        assertNull("Zero duration must prevent seeking by returning null", targetZeroDuration)

        // Negative duration (invalid or error state)
        val targetNegativeDuration = calculateSeekTarget(30_000L, 10_000L, -1L)
        assertNull("Negative duration must prevent seeking by returning null", targetNegativeDuration)

        val targetLargeNegativeDuration = calculateSeekTarget(30_000L, 10_000L, -999_999L)
        assertNull("Large negative duration must prevent seeking by returning null", targetLargeNegativeDuration)
    }

    @Test
    fun testNegativeCurrentPositionCoercedToZero() {
        // Edge case: player reports negative position (e.g. pre-buffering)
        val target = calculateSeekTarget(-500L, 10_000L, sampleDurationMs)
        assertEquals("Negative current position must be treated as 0 ms before adding delta", 10_000L, target)
    }

    @Test
    fun testZeroDeltaLeavesPositionUnchanged() {
        val target = calculateSeekTarget(42_000L, 0L, sampleDurationMs)
        assertEquals("Zero delta must leave position unchanged", 42_000L, target)
    }
}
