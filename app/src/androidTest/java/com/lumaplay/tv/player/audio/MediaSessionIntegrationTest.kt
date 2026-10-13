package com.lumaplay.tv.player.audio

import android.net.Uri
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.test.platform.app.InstrumentationRegistry
import com.lumaplay.tv.data.model.AudioItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.TimeUnit

class MediaSessionIntegrationTest {

    private fun createTestTrack(id: Long, title: String = "Track $id", durationMs: Long = 240_000L): AudioItem {
        return AudioItem(
            id = id,
            contentUri = Uri.parse("content://media/external/audio/media/$id"),
            title = title,
            artist = "Artist $id",
            album = "Album $id",
            durationMs = durationMs
        )
    }

    @Test
    fun testMediaSessionCreationAndTokenAvailability() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext

        var controller: AudioPlayerController? = null
        instrumentation.runOnMainSync {
            controller = AudioPlayerController(context)
        }

        val nonNullController = checkNotNull(controller)
        try {
            val token = nonNullController.sessionToken
            assertNotNull("Session token must be available", token)
            assertEquals("com.lumaplay.tv", token.packageName)
            assertNotNull("MediaSession must not be null", nonNullController.mediaSession)
            assertFalse("Controller must start unreleased", nonNullController.state.value.isPlaying)
        } finally {
            instrumentation.runOnMainSync {
                nonNullController.release()
            }
        }
    }

    @Test
    fun testMediaControllerConnectionAndAdvertisedCommands() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext

        var controller: AudioPlayerController? = null
        instrumentation.runOnMainSync {
            controller = AudioPlayerController(context)
        }

        val nonNullController = checkNotNull(controller)
        var mediaController: MediaController? = null
        try {
            val token = nonNullController.sessionToken
            val controllerFuture = MediaController.Builder(context, token).buildAsync()
            mediaController = controllerFuture.get(10, TimeUnit.SECONDS)

            assertNotNull("MediaController must connect to MediaSession", mediaController)
            instrumentation.runOnMainSync {
                val mc = checkNotNull(mediaController)
                assertTrue("COMMAND_PLAY_PAUSE must be available", mc.isCommandAvailable(Player.COMMAND_PLAY_PAUSE))
                assertTrue("COMMAND_PREPARE must be available", mc.isCommandAvailable(Player.COMMAND_PREPARE))
                assertTrue("COMMAND_STOP must be available", mc.isCommandAvailable(Player.COMMAND_STOP))
                assertTrue("COMMAND_SEEK_TO_NEXT must be available", mc.isCommandAvailable(Player.COMMAND_SEEK_TO_NEXT))
                assertTrue("COMMAND_SEEK_TO_PREVIOUS must be available", mc.isCommandAvailable(Player.COMMAND_SEEK_TO_PREVIOUS))
                assertTrue("COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM must be available", mc.isCommandAvailable(Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM))
            }
        } finally {
            instrumentation.runOnMainSync {
                mediaController?.release()
                nonNullController.release()
            }
        }
    }

    @Test
    fun testMediaControllerNextAndPreviousRouting() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext

        var controller: AudioPlayerController? = null
        instrumentation.runOnMainSync {
            controller = AudioPlayerController(context)
        }

        val nonNullController = checkNotNull(controller)
        var mediaController: MediaController? = null
        try {
            val t1 = createTestTrack(1, "Track 1")
            val t2 = createTestTrack(2, "Track 2")
            val t3 = createTestTrack(3, "Track 3")

            instrumentation.runOnMainSync {
                nonNullController.setQueue(listOf(t1, t2, t3), startIndex = 0, autoPlay = false)
            }

            assertEquals(0, nonNullController.state.value.queueIndex)
            assertEquals(t1, nonNullController.state.value.currentTrack)

            val token = nonNullController.sessionToken
            val controllerFuture = MediaController.Builder(context, token).buildAsync()
            mediaController = controllerFuture.get(10, TimeUnit.SECONDS)

            // Test seekToNext via MediaController
            instrumentation.runOnMainSync {
                mediaController.seekToNext()
            }
            instrumentation.waitForIdleSync()

            Thread.sleep(500)
            instrumentation.runOnMainSync {
                assertEquals(1, nonNullController.state.value.queueIndex)
                assertEquals(t2, nonNullController.state.value.currentTrack)
            }

            // Test seekToPrevious via MediaController (within 3 seconds -> rewinds to track 1)
            instrumentation.runOnMainSync {
                mediaController.seekToPrevious()
            }
            instrumentation.waitForIdleSync()

            Thread.sleep(500)
            instrumentation.runOnMainSync {
                assertEquals(0, nonNullController.state.value.queueIndex)
                assertEquals(t1, nonNullController.state.value.currentTrack)
            }
        } finally {
            instrumentation.runOnMainSync {
                mediaController?.release()
                nonNullController.release()
            }
        }
    }

    @Test
    fun testMediaControllerPlayPauseHandling() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext

        var controller: AudioPlayerController? = null
        instrumentation.runOnMainSync {
            controller = AudioPlayerController(context)
        }

        val nonNullController = checkNotNull(controller)
        var mediaController: MediaController? = null
        try {
            val t1 = createTestTrack(1, "Track 1")
            instrumentation.runOnMainSync {
                nonNullController.setQueue(listOf(t1), startIndex = 0, autoPlay = false)
            }
            assertFalse(nonNullController.state.value.isPlaying)

            val token = nonNullController.sessionToken
            val controllerFuture = MediaController.Builder(context, token).buildAsync()
            mediaController = controllerFuture.get(10, TimeUnit.SECONDS)

            instrumentation.runOnMainSync {
                mediaController.play()
            }
            instrumentation.waitForIdleSync()
            Thread.sleep(300)

            instrumentation.runOnMainSync {
                assertTrue(nonNullController.state.value.isPlaying)
            }

            instrumentation.runOnMainSync {
                mediaController.pause()
            }
            instrumentation.waitForIdleSync()
            Thread.sleep(300)

            instrumentation.runOnMainSync {
                assertFalse(nonNullController.state.value.isPlaying)
            }
        } finally {
            instrumentation.runOnMainSync {
                mediaController?.release()
                nonNullController.release()
            }
        }
    }

    @Test
    fun testMediaControllerPreviousBeforeAndAfterThreshold() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext

        var controller: AudioPlayerController? = null
        instrumentation.runOnMainSync {
            controller = AudioPlayerController(context)
        }

        val nonNullController = checkNotNull(controller)
        var mediaController: MediaController? = null
        try {
            val t1 = createTestTrack(1, "Track 1")
            val t2 = createTestTrack(2, "Track 2")

            instrumentation.runOnMainSync {
                nonNullController.setQueue(listOf(t1, t2), startIndex = 1, autoPlay = false)
            }

            assertEquals(1, nonNullController.state.value.queueIndex)
            assertEquals(t2, nonNullController.state.value.currentTrack)

            val token = nonNullController.sessionToken
            val controllerFuture = MediaController.Builder(context, token).buildAsync()
            mediaController = controllerFuture.get(10, TimeUnit.SECONDS)

            // 1. Position <= 3000ms: seekToPrevious moves to preceding track
            instrumentation.runOnMainSync {
                mediaController.seekToPrevious()
            }
            instrumentation.waitForIdleSync()
            Thread.sleep(500)

            instrumentation.runOnMainSync {
                assertEquals(0, nonNullController.state.value.queueIndex)
                assertEquals(t1, nonNullController.state.value.currentTrack)
            }

            // Move forward to Track 2 again
            instrumentation.runOnMainSync {
                mediaController.seekToNext()
            }
            instrumentation.waitForIdleSync()
            Thread.sleep(500)

            instrumentation.runOnMainSync {
                assertEquals(1, nonNullController.state.value.queueIndex)
                assertEquals(t2, nonNullController.state.value.currentTrack)
                // Simulate track having played past 3000ms
                nonNullController.seekTo(5000L)
            }

            // 2. Position > 3000ms: seekToPrevious rewinds current track to 0 without moving queue
            instrumentation.runOnMainSync {
                mediaController.seekToPrevious()
            }
            instrumentation.waitForIdleSync()
            Thread.sleep(500)

            instrumentation.runOnMainSync {
                assertEquals(1, nonNullController.state.value.queueIndex)
                assertEquals(t2, nonNullController.state.value.currentTrack)
                assertEquals(0L, nonNullController.state.value.currentPositionMs)
            }
        } finally {
            instrumentation.runOnMainSync {
                mediaController?.release()
                nonNullController.release()
            }
        }
    }

    @Test
    fun testSafeIdempotentRelease() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext

        var controller: AudioPlayerController? = null
        instrumentation.runOnMainSync {
            controller = AudioPlayerController(context)
        }

        val nonNullController = checkNotNull(controller)
        instrumentation.runOnMainSync {
            nonNullController.release()
            // Repeated calls must be completely safe and idempotent
            nonNullController.release()
            nonNullController.release()
        }

        assertEquals(AudioPlayerState.IDLE, nonNullController.state.value)
    }
}
