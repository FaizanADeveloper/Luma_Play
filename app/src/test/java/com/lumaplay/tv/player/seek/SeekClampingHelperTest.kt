package com.lumaplay.tv.player.seek

import org.junit.Assert.assertEquals
import org.junit.Test

class SeekClampingHelperTest {

    @Test
    fun testClampSeekPositionWithinBounds() {
        assertEquals(5000L, SeekClampingHelper.clampSeekPosition(5000L, 10000L))
        assertEquals(0L, SeekClampingHelper.clampSeekPosition(0L, 10000L))
        assertEquals(10000L, SeekClampingHelper.clampSeekPosition(10000L, 10000L))
    }

    @Test
    fun testClampSeekPositionBelowZero() {
        assertEquals(0L, SeekClampingHelper.clampSeekPosition(-5000L, 10000L))
        assertEquals(0L, SeekClampingHelper.clampSeekPosition(-1L, 60000L))
    }

    @Test
    fun testClampSeekPositionAboveDuration() {
        assertEquals(10000L, SeekClampingHelper.clampSeekPosition(15000L, 10000L))
        assertEquals(60000L, SeekClampingHelper.clampSeekPosition(999999L, 60000L))
    }

    @Test
    fun testClampSeekPositionWithZeroOrUnknownDuration() {
        assertEquals(0L, SeekClampingHelper.clampSeekPosition(-500L, 0L))
        assertEquals(5000L, SeekClampingHelper.clampSeekPosition(5000L, 0L))
        assertEquals(5000L, SeekClampingHelper.clampSeekPosition(5000L, -1L))
    }

    @Test
    fun testCalculateBackward() {
        // Normal backward step
        assertEquals(20000L, SeekClampingHelper.calculateBackward(30000L, 10000L, 60000L))
        // Clamping at zero
        assertEquals(0L, SeekClampingHelper.calculateBackward(5000L, 10000L, 60000L))
        assertEquals(0L, SeekClampingHelper.calculateBackward(0L, 10000L, 60000L))
    }

    @Test
    fun testCalculateForward() {
        // Normal forward step
        assertEquals(40000L, SeekClampingHelper.calculateForward(30000L, 10000L, 60000L))
        // Clamping at duration
        assertEquals(60000L, SeekClampingHelper.calculateForward(55000L, 10000L, 60000L))
        assertEquals(60000L, SeekClampingHelper.calculateForward(60000L, 10000L, 60000L))
    }
}
