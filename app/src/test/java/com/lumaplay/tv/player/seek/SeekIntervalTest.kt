package com.lumaplay.tv.player.seek

import org.junit.Assert.assertEquals
import org.junit.Test

class SeekIntervalTest {

    @Test
    fun testAllIntervalValuesAndLabels() {
        assertEquals(5, SeekInterval.FIVE.seconds)
        assertEquals("5s", SeekInterval.FIVE.label)
        assertEquals(5000L, SeekInterval.FIVE.millis)

        assertEquals(10, SeekInterval.TEN.seconds)
        assertEquals("10s", SeekInterval.TEN.label)
        assertEquals(10000L, SeekInterval.TEN.millis)

        assertEquals(15, SeekInterval.FIFTEEN.seconds)
        assertEquals("15s", SeekInterval.FIFTEEN.label)
        assertEquals(15000L, SeekInterval.FIFTEEN.millis)

        assertEquals(30, SeekInterval.THIRTY.seconds)
        assertEquals("30s", SeekInterval.THIRTY.label)
        assertEquals(30000L, SeekInterval.THIRTY.millis)

        assertEquals(60, SeekInterval.SIXTY.seconds)
        assertEquals("60s", SeekInterval.SIXTY.label)
        assertEquals(60000L, SeekInterval.SIXTY.millis)
    }

    @Test
    fun testDefaultIntervalIsTenSeconds() {
        assertEquals(SeekInterval.TEN, SeekInterval.DEFAULT)
        assertEquals(10, SeekInterval.DEFAULT.seconds)
    }

    @Test
    fun testCycleSequence() {
        assertEquals(SeekInterval.TEN, SeekInterval.FIVE.next())
        assertEquals(SeekInterval.FIFTEEN, SeekInterval.TEN.next())
        assertEquals(SeekInterval.THIRTY, SeekInterval.FIFTEEN.next())
        assertEquals(SeekInterval.SIXTY, SeekInterval.THIRTY.next())
        assertEquals(SeekInterval.FIVE, SeekInterval.SIXTY.next())
    }

    @Test
    fun testFromSecondsResolution() {
        assertEquals(SeekInterval.FIVE, SeekInterval.fromSeconds(5))
        assertEquals(SeekInterval.TEN, SeekInterval.fromSeconds(10))
        assertEquals(SeekInterval.FIFTEEN, SeekInterval.fromSeconds(15))
        assertEquals(SeekInterval.THIRTY, SeekInterval.fromSeconds(30))
        assertEquals(SeekInterval.SIXTY, SeekInterval.fromSeconds(60))

        // Fallback for unrecognized values
        assertEquals(SeekInterval.DEFAULT, SeekInterval.fromSeconds(0))
        assertEquals(SeekInterval.DEFAULT, SeekInterval.fromSeconds(42))
        assertEquals(SeekInterval.DEFAULT, SeekInterval.fromSeconds(-10))
    }
}
