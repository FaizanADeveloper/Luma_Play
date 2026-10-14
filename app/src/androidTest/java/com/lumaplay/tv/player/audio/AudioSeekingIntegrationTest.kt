package com.lumaplay.tv.player.audio

import android.net.Uri
import androidx.test.platform.app.InstrumentationRegistry
import com.lumaplay.tv.data.model.AudioItem
import org.junit.Assert.assertEquals
import org.junit.Test

class AudioSeekingIntegrationTest {

    private fun createTestTrack(id: Long, durationMs: Long = 180_000L): AudioItem {
        return AudioItem(
            id = id,
            contentUri = Uri.parse("content://media/external/audio/media/$id"),
            title = "Integration Seek Track $id",
            artist = "Test Artist",
            album = "Test Album",
            durationMs = durationMs
        )
    }

    @Test
    fun testSeekByForwardAndBackwardOnRealController() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext

        var controller: AudioPlayerController? = null
        instrumentation.runOnMainSync {
            controller = AudioPlayerController(context)
        }

        val nonNullController = checkNotNull(controller)
        try {
            val track = createTestTrack(100, durationMs = 120_000L)
            instrumentation.runOnMainSync {
                nonNullController.setQueue(listOf(track), startIndex = 0, autoPlay = false)
            }

            // Initially at position 0
            assertEquals(0L, nonNullController.state.value.currentPositionMs)

            // Seek forward +10s
            instrumentation.runOnMainSync {
                nonNullController.seekBy(10_000L)
            }
            instrumentation.waitForIdleSync()
            assertEquals(10_000L, nonNullController.state.value.currentPositionMs)

            // Seek forward another +10s -> 20s
            instrumentation.runOnMainSync {
                nonNullController.seekBy(10_000L)
            }
            instrumentation.waitForIdleSync()
            assertEquals(20_000L, nonNullController.state.value.currentPositionMs)

            // Seek backward -10s -> 10s
            instrumentation.runOnMainSync {
                nonNullController.seekBy(-10_000L)
            }
            instrumentation.waitForIdleSync()
            assertEquals(10_000L, nonNullController.state.value.currentPositionMs)
        } finally {
            instrumentation.runOnMainSync {
                nonNullController.release()
            }
        }
    }

    @Test
    fun testSeekByClampingAtZeroAndDuration() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext

        var controller: AudioPlayerController? = null
        instrumentation.runOnMainSync {
            controller = AudioPlayerController(context)
        }

        val nonNullController = checkNotNull(controller)
        try {
            val track = createTestTrack(200, durationMs = 30_000L)
            instrumentation.runOnMainSync {
                nonNullController.setQueue(listOf(track), startIndex = 0, autoPlay = false)
            }

            // Seek backward from 0 -> clamped at 0
            instrumentation.runOnMainSync {
                nonNullController.seekBy(-10_000L)
            }
            instrumentation.waitForIdleSync()
            assertEquals(0L, nonNullController.state.value.currentPositionMs)

            // Seek forward past duration (30s duration, seeking +40s) -> clamped at 30_000ms
            instrumentation.runOnMainSync {
                nonNullController.seekBy(40_000L)
            }
            instrumentation.waitForIdleSync()
            assertEquals(30_000L, nonNullController.state.value.currentPositionMs)
        } finally {
            instrumentation.runOnMainSync {
                nonNullController.release()
            }
        }
    }

    @Test
    fun testSeekByWithNoTrackOrInvalidDuration() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext

        var controller: AudioPlayerController? = null
        instrumentation.runOnMainSync {
            controller = AudioPlayerController(context)
        }

        val nonNullController = checkNotNull(controller)
        try {
            // No track loaded: seeking must be completely safe and no-op
            instrumentation.runOnMainSync {
                nonNullController.seekBy(10_000L)
            }
            instrumentation.waitForIdleSync()
            assertEquals(0L, nonNullController.state.value.currentPositionMs)

            // Track with 0 duration: seeking must be safe and no-op
            val zeroDurationTrack = createTestTrack(300, durationMs = 0L)
            instrumentation.runOnMainSync {
                nonNullController.setQueue(listOf(zeroDurationTrack), startIndex = 0, autoPlay = false)
                nonNullController.seekBy(10_000L)
            }
            instrumentation.waitForIdleSync()
            assertEquals(0L, nonNullController.state.value.currentPositionMs)
        } finally {
            instrumentation.runOnMainSync {
                nonNullController.release()
            }
        }
    }
}
