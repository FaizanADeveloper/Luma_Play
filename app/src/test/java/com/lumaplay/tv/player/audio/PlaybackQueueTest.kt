package com.lumaplay.tv.player.audio

import android.net.FakeUri
import android.net.Uri
import com.lumaplay.tv.data.model.AudioItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class PlaybackQueueTest {

    private fun createTrack(id: Long, title: String = "Track $id"): AudioItem {
        return AudioItem(
            id = id,
            contentUri = FakeUri("content://media/external/audio/media/$id"),
            title = title,
            artist = "Artist $id",
            album = "Album $id",
            durationMs = 240_000L
        )
    }

    // 1. Empty queue
    @Test
    fun testEmptyQueueInvariants() {
        val queue = PlaybackQueue.EMPTY
        assertTrue(queue.isEmpty)
        assertEquals(0, queue.size)
        assertEquals(-1, queue.currentIndex)
        assertNull(queue.currentTrack)
        assertTrue(queue.activeTracks.isEmpty())
        assertFalse(queue.hasNext)
        assertFalse(queue.hasPrevious(0L))
        assertFalse(queue.hasPrevious(5000L))

        assertNull(queue.next())
        val prevResult = queue.previous(0L)
        assertTrue(prevResult is PreviousResult.NoOp)
        assertNull(queue.moveToPrevious())
        assertNull(queue.onTrackCompleted())

        // Shuffle empty queue remains safely empty
        val shuffled = queue.toggleShuffle()
        assertTrue(shuffled.isEmpty)
        assertEquals(-1, shuffled.currentIndex)
        assertNull(shuffled.currentTrack)
    }

    // 2. Single-track queue
    @Test
    fun testSingleTrackQueue() {
        val track = createTrack(1)
        val queue = PlaybackQueue.from(listOf(track))

        assertFalse(queue.isEmpty)
        assertEquals(1, queue.size)
        assertEquals(0, queue.currentIndex)
        assertEquals(track, queue.currentTrack)

        // With repeat OFF: cannot advance
        assertFalse(queue.hasNext)
        assertNull(queue.next())

        // With repeat ALL: wraps to itself
        val allQueue = queue.setRepeatMode(AudioRepeatMode.ALL)
        assertTrue(allQueue.hasNext)
        val nextAll = allQueue.next()
        assertNotNull(nextAll)
        assertEquals(0, nextAll?.currentIndex)
        assertEquals(track, nextAll?.currentTrack)

        // Previous at position > 3000ms: seeks to start
        val seekResult = queue.previous(4000L)
        assertTrue(seekResult is PreviousResult.SeekToStart)
        assertEquals(track, seekResult.queue.currentTrack)

        // Previous at position <= 3000ms with OFF: NoOp
        val prevOffResult = queue.previous(1500L)
        assertTrue(prevOffResult is PreviousResult.NoOp)

        // Previous at position <= 3000ms with ALL: wraps to track 0
        val prevAllResult = allQueue.previous(1500L)
        assertTrue(prevAllResult is PreviousResult.Moved)
        assertEquals(track, (prevAllResult as PreviousResult.Moved).track)
    }

    // 3. Multi-track sequential queue
    @Test
    fun testMultiTrackSequentialQueue() {
        val t1 = createTrack(1)
        val t2 = createTrack(2)
        val t3 = createTrack(3)
        val queue = PlaybackQueue.from(listOf(t1, t2, t3))

        assertEquals(3, queue.size)
        assertEquals(0, queue.currentIndex)
        assertEquals(t1, queue.currentTrack)
        assertEquals(listOf(t1, t2, t3), queue.activeTracks)
    }

    // 4. next() in normal sequential mode
    @Test
    fun testNextInNormalSequentialMode() {
        val t1 = createTrack(1)
        val t2 = createTrack(2)
        val t3 = createTrack(3)
        val queue = PlaybackQueue.from(listOf(t1, t2, t3))

        assertTrue(queue.hasNext)
        val q2 = queue.next()
        assertNotNull(q2)
        assertEquals(1, q2?.currentIndex)
        assertEquals(t2, q2?.currentTrack)

        assertTrue(q2!!.hasNext)
        val q3 = q2.next()
        assertNotNull(q3)
        assertEquals(2, q3?.currentIndex)
        assertEquals(t3, q3?.currentTrack)
    }

    // 5. next() at end with OFF
    @Test
    fun testNextAtEndWithOff() {
        val t1 = createTrack(1)
        val t2 = createTrack(2)
        val queue = PlaybackQueue.from(listOf(t1, t2), initialIndex = 1, repeatMode = AudioRepeatMode.OFF)

        assertFalse(queue.hasNext)
        assertNull(queue.next())
        assertNull(queue.onTrackCompleted())
    }

    // 6. next() at end with ALL
    @Test
    fun testNextAtEndWithAll() {
        val t1 = createTrack(1)
        val t2 = createTrack(2)
        val queue = PlaybackQueue.from(listOf(t1, t2), initialIndex = 1, repeatMode = AudioRepeatMode.ALL)

        assertTrue(queue.hasNext)
        val wrappedQueue = queue.next()
        assertNotNull(wrappedQueue)
        assertEquals(0, wrappedQueue?.currentIndex)
        assertEquals(t1, wrappedQueue?.currentTrack)

        // onTrackCompleted also wraps to first track
        val autoAdvanced = queue.onTrackCompleted()
        assertNotNull(autoAdvanced)
        assertEquals(0, autoAdvanced?.currentIndex)
        assertEquals(t1, autoAdvanced?.currentTrack)
    }

    // 7. ONE repeat semantics
    @Test
    fun testOneRepeatSemantics() {
        val t1 = createTrack(1)
        val t2 = createTrack(2)
        val queue = PlaybackQueue.from(listOf(t1, t2), initialIndex = 0, repeatMode = AudioRepeatMode.ONE)

        // In ONE mode, onTrackCompleted repeats the active track
        val repeated = queue.onTrackCompleted()
        assertNotNull(repeated)
        assertEquals(0, repeated?.currentIndex)
        assertEquals(t1, repeated?.currentTrack)

        // Manual next advances when another track exists
        val nextManual = queue.next()
        assertNotNull(nextManual)
        assertEquals(1, nextManual?.currentIndex)
        assertEquals(t2, nextManual?.currentTrack)

        // At the end with ONE mode, next remains on current track
        val atEndOne = nextManual!!.next()
        assertNotNull(atEndOne)
        assertEquals(1, atEndOne?.currentIndex)
        assertEquals(t2, atEndOne?.currentTrack)
    }

    // 8. previous() when position > 3000 ms
    @Test
    fun testPreviousWhenPositionGreaterThan3000Ms() {
        val t1 = createTrack(1)
        val t2 = createTrack(2)
        val queue = PlaybackQueue.from(listOf(t1, t2), initialIndex = 1)

        val result = queue.previous(currentPositionMs = 5000L)
        assertTrue(result is PreviousResult.SeekToStart)
        assertEquals(1, result.queue.currentIndex)
        assertEquals(t2, result.queue.currentTrack)
    }

    // 9. previous() when position <= 3000 ms
    @Test
    fun testPreviousWhenPositionLessThanOrEqualTo3000Ms() {
        val t1 = createTrack(1)
        val t2 = createTrack(2)
        val queue = PlaybackQueue.from(listOf(t1, t2), initialIndex = 1)

        val result = queue.previous(currentPositionMs = 2500L)
        assertTrue(result is PreviousResult.Moved)
        val moved = result as PreviousResult.Moved
        assertEquals(0, moved.queue.currentIndex)
        assertEquals(t1, moved.track)
        assertEquals(t1, moved.queue.currentTrack)
    }

    // 10. previous() at first track with OFF
    @Test
    fun testPreviousAtFirstTrackWithOff() {
        val t1 = createTrack(1)
        val t2 = createTrack(2)
        val queue = PlaybackQueue.from(listOf(t1, t2), initialIndex = 0, repeatMode = AudioRepeatMode.OFF)

        val result = queue.previous(currentPositionMs = 1500L)
        assertTrue(result is PreviousResult.NoOp)
        assertEquals(0, result.queue.currentIndex)
        assertEquals(t1, result.queue.currentTrack)
    }

    // 11. previous() at first track with ALL
    @Test
    fun testPreviousAtFirstTrackWithAll() {
        val t1 = createTrack(1)
        val t2 = createTrack(2)
        val t3 = createTrack(3)
        val queue = PlaybackQueue.from(listOf(t1, t2, t3), initialIndex = 0, repeatMode = AudioRepeatMode.ALL)

        val result = queue.previous(currentPositionMs = 1500L)
        assertTrue(result is PreviousResult.Moved)
        val moved = result as PreviousResult.Moved
        assertEquals(2, moved.queue.currentIndex)
        assertEquals(t3, moved.track)
        assertEquals(t3, moved.queue.currentTrack)
    }

    // 12. shuffle preserves every track exactly once
    @Test
    fun testShufflePreservesEveryTrackExactlyOnce() {
        val tracks = (1L..10L).map { createTrack(it) }
        val queue = PlaybackQueue.from(tracks, initialIndex = 3)

        val shuffled = queue.setShuffle(true, Random(12345))
        assertTrue(shuffled.isShuffled)
        assertEquals(tracks.size, shuffled.size)
        assertEquals(tracks.size, shuffled.activeTracks.size)

        // Every track is in activeTracks exactly once
        assertEquals(tracks.toSet(), shuffled.activeTracks.toSet())
    }

    // 13. shuffle preserves the currently playing track
    @Test
    fun testShufflePreservesCurrentlyPlayingTrack() {
        val tracks = (1L..5L).map { createTrack(it) }
        val targetTrack = tracks[2] // ID 3
        val queue = PlaybackQueue.from(tracks, initialIndex = 2)

        assertEquals(targetTrack, queue.currentTrack)

        val shuffled = queue.setShuffle(true, Random(999))
        assertEquals(targetTrack, shuffled.currentTrack)
        // Current track is at active index 0
        assertEquals(0, shuffled.currentIndex)
        assertEquals(targetTrack, shuffled.activeTracks[0])
    }

    // 14. shuffle OFF restores original order
    @Test
    fun testShuffleOffRestoresOriginalOrder() {
        val tracks = (1L..5L).map { createTrack(it) }
        val queue = PlaybackQueue.from(tracks, initialIndex = 1)
        val playingTrackBeforeShuffle = queue.currentTrack // ID 2

        val shuffled = queue.setShuffle(true, Random(42))
        // Advance in shuffled queue
        val advancedShuffled = shuffled.next()!!
        val activePlayingTrack = advancedShuffled.currentTrack

        val restored = advancedShuffled.setShuffle(false)
        assertFalse(restored.isShuffled)
        assertEquals(tracks, restored.activeTracks)
        assertEquals(activePlayingTrack, restored.currentTrack)
        assertEquals(tracks.indexOf(activePlayingTrack), restored.currentIndex)
    }

    // 15. toggling shuffle does not lose or duplicate tracks
    @Test
    fun testTogglingShuffleDoesNotLoseOrDuplicateTracks() {
        val tracks = (1L..8L).map { createTrack(it) }
        var queue = PlaybackQueue.from(tracks, initialIndex = 4)

        for (seed in 1..5) {
            queue = queue.toggleShuffle(Random(seed))
            assertTrue(queue.isShuffled)
            assertEquals(tracks.size, queue.activeTracks.size)
            assertEquals(tracks.toSet(), queue.activeTracks.toSet())

            queue = queue.toggleShuffle()
            assertFalse(queue.isShuffled)
            assertEquals(tracks, queue.activeTracks)
        }
    }

    // 16. repeat mode transitions
    @Test
    fun testRepeatModeTransitions() {
        val queue = PlaybackQueue.from(listOf(createTrack(1), createTrack(2)))
        assertEquals(AudioRepeatMode.OFF, queue.repeatMode)

        val qAll = queue.cycleRepeatMode()
        assertEquals(AudioRepeatMode.ALL, qAll.repeatMode)

        val qOne = qAll.cycleRepeatMode()
        assertEquals(AudioRepeatMode.ONE, qOne.repeatMode)

        val qOff = qOne.cycleRepeatMode()
        assertEquals(AudioRepeatMode.OFF, qOff.repeatMode)
    }

    // 17. relevant boundary cases
    @Test
    fun testBoundaryCases() {
        val t1 = createTrack(1)
        val t2 = createTrack(2)
        val queue = PlaybackQueue.from(listOf(t1, t2))

        // playTrackAt out of bounds
        val unchanged1 = queue.playTrackAt(-5)
        assertEquals(0, unchanged1.currentIndex)
        val unchanged2 = queue.playTrackAt(99)
        assertEquals(0, unchanged2.currentIndex)

        // playTrack valid and invalid
        val played2 = queue.playTrack(t2)
        assertEquals(1, played2.currentIndex)
        assertEquals(t2, played2.currentTrack)

        val nonExistent = createTrack(999)
        val notPlayed = queue.playTrack(nonExistent)
        assertEquals(0, notPlayed.currentIndex)

        // hasPrevious boundary checks
        assertFalse(queue.hasPrevious(0L))
        assertFalse(queue.hasPrevious(3000L))
        assertTrue(queue.hasPrevious(3001L))

        val atSecond = queue.playTrackAt(1)
        assertTrue(atSecond.hasPrevious(0L))
    }

    // 18. AudioPlayerState metrics
    @Test
    fun testAudioPlayerStateDerivedMetrics() {
        val t1 = createTrack(1)
        val state = AudioPlayerState(
            status = AudioPlaybackStatus.READY,
            isPlaying = true,
            currentTrack = t1,
            currentPositionMs = 65_000L, // 01:05
            durationMs = 130_000L,      // 02:10
            queue = listOf(t1),
            queueIndex = 0,
            repeatMode = AudioRepeatMode.OFF
        )

        assertEquals(0.5f, state.progress, 0.001f)
        assertEquals("01:05", state.formattedCurrentPosition)
        assertEquals("02:10", state.formattedDuration)
        assertFalse(state.hasNext)
        assertTrue(state.hasPrevious) // At 65s (>3s), previous rewinds to start

        // At beginning (<= 3s) with queueIndex 0 and repeat OFF, hasPrevious is false
        val atStart = state.copy(currentPositionMs = 1500L)
        assertFalse(atStart.hasPrevious)

        val longState = state.copy(currentPositionMs = 3665_000L, durationMs = 7320_000L)
        assertEquals("1:01:05", longState.formattedCurrentPosition)
        assertEquals("2:02:00", longState.formattedDuration)
    }
}
