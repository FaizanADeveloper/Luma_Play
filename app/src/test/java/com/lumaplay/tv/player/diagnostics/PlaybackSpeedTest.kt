package com.lumaplay.tv.player.diagnostics

import org.junit.Assert.assertEquals
import org.junit.Test

class PlaybackSpeedTest {

    @Test
    fun testDefaultSpeed() {
        assertEquals(1.0f, PlaybackSpeed.DEFAULT_SPEED, 0.001f)
    }

    @Test
    fun testSpeedCyclingOrder() {
        var current = 1.0f
        val expectedSequence = listOf(1.25f, 1.5f, 2.0f, 0.5f, 0.75f, 1.0f)

        for (expected in expectedSequence) {
            current = PlaybackSpeed.nextSpeed(current)
            assertEquals(expected, current, 0.001f)
        }
    }

    @Test
    fun testUnsupportedSpeedFallsBackToFirstSpeed() {
        val next = PlaybackSpeed.nextSpeed(3.5f)
        assertEquals(0.5f, next, 0.001f)
    }

    @Test
    fun testSpeedFormatting() {
        assertEquals("0.5x", PlaybackSpeed.formatSpeed(0.5f))
        assertEquals("0.75x", PlaybackSpeed.formatSpeed(0.75f))
        assertEquals("1.0x", PlaybackSpeed.formatSpeed(1.0f))
        assertEquals("1.25x", PlaybackSpeed.formatSpeed(1.25f))
        assertEquals("1.5x", PlaybackSpeed.formatSpeed(1.5f))
        assertEquals("2.0x", PlaybackSpeed.formatSpeed(2.0f))
    }
}
