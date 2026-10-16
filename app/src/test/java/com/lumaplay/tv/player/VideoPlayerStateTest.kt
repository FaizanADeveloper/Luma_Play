package com.lumaplay.tv.player

import com.lumaplay.tv.player.diagnostics.AspectRatioMode
import com.lumaplay.tv.player.diagnostics.PlaybackSpeed
import com.lumaplay.tv.player.diagnostics.VideoTelemetryState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoPlayerStateTest {

    @Test
    fun testDefaultStateValues() {
        val state = VideoPlayerState()
        assertFalse(state.isPlaying)
        assertEquals(0L, state.currentPositionMs)
        assertEquals(0L, state.durationMs)
        assertEquals(0L, state.bufferedPositionMs)
        assertTrue(state.isBuffering)
        assertFalse(state.isEnded)
        assertNull(state.errorMessage)
        assertTrue(state.showControls)
        assertEquals(PlaybackSpeed.DEFAULT_SPEED, state.playbackSpeed, 0.001f)
        assertEquals(AspectRatioMode.FIT, state.aspectRatioMode)
        assertFalse(state.showStats)
        assertEquals(VideoTelemetryState.EMPTY, state.telemetry)
    }

    @Test
    fun testProgressCalculation() {
        val state = VideoPlayerState(
            currentPositionMs = 30_000L,
            durationMs = 60_000L
        )
        assertEquals(0.5f, state.progress, 0.001f)

        val clampedState = VideoPlayerState(
            currentPositionMs = 90_000L,
            durationMs = 60_000L
        )
        assertEquals(1.0f, clampedState.progress, 0.001f)

        val zeroDuration = VideoPlayerState(
            currentPositionMs = 30_000L,
            durationMs = 0L
        )
        assertEquals(0.0f, zeroDuration.progress, 0.001f)
    }

    @Test
    fun testBufferedProgressCalculation() {
        val state = VideoPlayerState(
            bufferedPositionMs = 45_000L,
            durationMs = 60_000L
        )
        assertEquals(0.75f, state.bufferedProgress, 0.001f)

        val clampedState = VideoPlayerState(
            bufferedPositionMs = 120_000L,
            durationMs = 60_000L
        )
        assertEquals(1.0f, clampedState.bufferedProgress, 0.001f)

        val zeroDuration = VideoPlayerState(
            bufferedPositionMs = 10_000L,
            durationMs = 0L
        )
        assertEquals(0.0f, zeroDuration.bufferedProgress, 0.001f)
    }

    @Test
    fun testTimeFormattingMinutesSeconds() {
        val state = VideoPlayerState(
            currentPositionMs = 125_000L, // 2m 5s
            durationMs = 630_000L // 10m 30s
        )
        assertEquals("02:05", state.formattedCurrentPosition)
        assertEquals("10:30", state.formattedDuration)
    }

    @Test
    fun testTimeFormattingHours() {
        val state = VideoPlayerState(
            currentPositionMs = 3665_000L, // 1h 1m 5s
            durationMs = 7320_000L // 2h 2m 0s
        )
        assertEquals("1:01:05", state.formattedCurrentPosition)
        assertEquals("2:02:00", state.formattedDuration)
    }

    @Test
    fun testTimeFormattingZeroOrNegative() {
        val state = VideoPlayerState(
            currentPositionMs = 0L,
            durationMs = -1L
        )
        assertEquals("00:00", state.formattedCurrentPosition)
        assertEquals("00:00", state.formattedDuration)
    }
}
