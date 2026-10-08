package com.lumaplay.tv.player.audio

import com.lumaplay.tv.data.model.AudioItem
import kotlin.random.Random

/**
 * Result of a previous-track navigation request, adhering to the 10-foot remote playback rule:
 * - If current position > 3000 ms, seek current track back to 0.
 * - If current position <= 3000 ms, jump to the previous queue item (wrapping if [AudioRepeatMode.ALL]).
 * - If at the beginning with [AudioRepeatMode.OFF] or [AudioRepeatMode.ONE], no-op.
 */
sealed interface PreviousResult {
    val queue: PlaybackQueue

    /**
     * The active track should be rewound to 0 ms without changing the active queue track.
     */
    data class SeekToStart(override val queue: PlaybackQueue) : PreviousResult

    /**
     * The queue successfully navigated to the preceding track.
     */
    data class Moved(override val queue: PlaybackQueue, val track: AudioItem) : PreviousResult

    /**
     * No backward navigation could be performed (e.g. empty queue or start of queue without repeat ALL).
     */
    data class NoOp(override val queue: PlaybackQueue) : PreviousResult
}

/**
 * Pure, deterministic playback queue state machine.
 *
 * Maintains original track ordering alongside an active playback index mapping so that
 * shuffle toggling never mutates, loses, or duplicates items from the source library.
 *
 * Contains zero dependencies on Android SDK, ExoPlayer, Compose, or MediaSession.
 *
 * @property originalTracks The immutable master sequence of tracks in the queue.
 * @property activeIndices The active playback order mapping (indices into [originalTracks]).
 * @property currentIndex The pointer to the currently playing item within [activeIndices].
 * @property isShuffled Whether the active playback order is currently randomized.
 * @property repeatMode The active repeat policy ([AudioRepeatMode.OFF], [AudioRepeatMode.ALL], [AudioRepeatMode.ONE]).
 */
data class PlaybackQueue(
    val originalTracks: List<AudioItem> = emptyList(),
    val activeIndices: List<Int> = originalTracks.indices.toList(),
    val currentIndex: Int = if (originalTracks.isEmpty()) -1 else 0,
    val isShuffled: Boolean = false,
    val repeatMode: AudioRepeatMode = AudioRepeatMode.OFF
) {
    /**
     * Total number of items in the queue.
     */
    val size: Int
        get() = activeIndices.size

    /**
     * Whether the queue contains no items.
     */
    val isEmpty: Boolean
        get() = activeIndices.isEmpty()

    /**
     * The ordered list of tracks reflecting the current active playback permutation.
     */
    val activeTracks: List<AudioItem>
        get() = activeIndices.mapNotNull { originalTracks.getOrNull(it) }

    /**
     * The currently playing track, or null if the queue is empty or unpositioned.
     */
    val currentTrack: AudioItem?
        get() {
            val originalIndex = activeIndices.getOrNull(currentIndex) ?: return null
            return originalTracks.getOrNull(originalIndex)
        }

    /**
     * Whether forward navigation to a next track is logically available.
     */
    val hasNext: Boolean
        get() = when {
            isEmpty -> false
            repeatMode == AudioRepeatMode.ALL -> true
            repeatMode == AudioRepeatMode.ONE -> true
            else -> currentIndex in 0 until activeIndices.lastIndex
        }

    /**
     * Whether backward navigation (previous track or seek to beginning) is logically available.
     */
    fun hasPrevious(currentPositionMs: Long = 0L): Boolean {
        if (isEmpty) return false
        if (currentPositionMs > 3000L) return true
        if (repeatMode == AudioRepeatMode.ALL) return true
        return currentIndex > 0
    }

    /**
     * Moves to the next active queue item when one exists.
     *
     * Invariants at the end of the queue:
     * - [AudioRepeatMode.OFF] -> returns null (end of playback).
     * - [AudioRepeatMode.ALL] -> wraps to the first track (index 0).
     * - [AudioRepeatMode.ONE] -> remains on / repeats the current track.
     */
    fun next(): PlaybackQueue? {
        if (isEmpty) return null

        return if (currentIndex < activeIndices.lastIndex) {
            copy(currentIndex = currentIndex + 1)
        } else {
            when (repeatMode) {
                AudioRepeatMode.OFF -> null
                AudioRepeatMode.ALL -> copy(currentIndex = 0)
                AudioRepeatMode.ONE -> copy(currentIndex = currentIndex)
            }
        }
    }

    /**
     * Evaluates backward navigation using the TV remote 3-second playback rule:
     * - If [currentPositionMs] > 3000 ms: returns [PreviousResult.SeekToStart] without changing track.
     * - If [currentPositionMs] <= 3000 ms: moves to the previous track ([PreviousResult.Moved]).
     *   Wrapping from the first track to the final track is permitted only when [repeatMode] is [AudioRepeatMode.ALL].
     * - If at the beginning with [AudioRepeatMode.OFF] or [AudioRepeatMode.ONE], returns [PreviousResult.NoOp].
     */
    fun previous(currentPositionMs: Long = 0L): PreviousResult {
        if (isEmpty) return PreviousResult.NoOp(this)

        if (currentPositionMs > 3000L) {
            return PreviousResult.SeekToStart(this)
        }

        val movedQueue = moveToPrevious()
        return if (movedQueue != null && movedQueue.currentTrack != null) {
            PreviousResult.Moved(movedQueue, movedQueue.currentTrack!!)
        } else {
            PreviousResult.NoOp(this)
        }
    }

    /**
     * Pure decremental queue navigation without time evaluation.
     *
     * Returns:
     * - Preceding queue state if [currentIndex] > 0.
     * - Last queue track if [currentIndex] == 0 and [repeatMode] is [AudioRepeatMode.ALL].
     * - null if at the start and repeat is [AudioRepeatMode.OFF] or [AudioRepeatMode.ONE].
     */
    fun moveToPrevious(): PlaybackQueue? {
        if (isEmpty) return null

        return if (currentIndex > 0) {
            copy(currentIndex = currentIndex - 1)
        } else {
            when (repeatMode) {
                AudioRepeatMode.ALL -> copy(currentIndex = activeIndices.lastIndex)
                AudioRepeatMode.OFF, AudioRepeatMode.ONE -> null
            }
        }
    }

    /**
     * Pure track completion transition:
     * - In [AudioRepeatMode.ONE], repeats the active track (returns this).
     * - In [AudioRepeatMode.ALL], advances to next or wraps to index 0.
     * - In [AudioRepeatMode.OFF], advances to next or returns null when finished.
     */
    fun onTrackCompleted(): PlaybackQueue? {
        if (isEmpty) return null
        return when (repeatMode) {
            AudioRepeatMode.ONE -> this
            AudioRepeatMode.ALL, AudioRepeatMode.OFF -> next()
        }
    }

    /**
     * Direct navigation to an item at [index] within the active playback permutation.
     */
    fun playTrackAt(index: Int): PlaybackQueue {
        if (isEmpty || index !in activeIndices.indices) return this
        return copy(currentIndex = index)
    }

    /**
     * Direct navigation to a specific [track] if it exists in the active playback queue.
     */
    fun playTrack(track: AudioItem): PlaybackQueue {
        val targetIndex = activeTracks.indexOf(track)
        return if (targetIndex >= 0) copy(currentIndex = targetIndex) else this
    }

    /**
     * Updates shuffle state.
     *
     * Invariants:
     * - When enabling shuffle, the currently active track is preserved and pinned as the initial
     *   element of the randomized permutation; all other tracks are shuffled ahead of it.
     * - When disabling shuffle, original sequential ordering is restored and [currentIndex]
     *   points to the original index of the active track.
     * - No track is ever lost or duplicated.
     */
    fun setShuffle(enabled: Boolean, random: Random = Random.Default): PlaybackQueue {
        if (isShuffled == enabled) return this
        if (originalTracks.isEmpty()) return copy(isShuffled = enabled)

        val currentOriginalIndex = activeIndices.getOrNull(currentIndex)

        return if (enabled) {
            if (currentOriginalIndex != null) {
                val otherIndices = (originalTracks.indices - currentOriginalIndex).shuffled(random)
                val newActiveIndices = listOf(currentOriginalIndex) + otherIndices
                copy(
                    activeIndices = newActiveIndices,
                    currentIndex = 0,
                    isShuffled = true
                )
            } else {
                copy(
                    activeIndices = originalTracks.indices.shuffled(random),
                    currentIndex = -1,
                    isShuffled = true
                )
            }
        } else {
            val restoredIndices = originalTracks.indices.toList()
            val newIndex = if (currentOriginalIndex != null) {
                restoredIndices.indexOf(currentOriginalIndex)
            } else {
                -1
            }
            copy(
                activeIndices = restoredIndices,
                currentIndex = newIndex,
                isShuffled = false
            )
        }
    }

    /**
     * Toggles the shuffle state.
     */
    fun toggleShuffle(random: Random = Random.Default): PlaybackQueue = setShuffle(!isShuffled, random)

    /**
     * Sets the active [AudioRepeatMode].
     */
    fun setRepeatMode(mode: AudioRepeatMode): PlaybackQueue = copy(repeatMode = mode)

    /**
     * Cycles through repeat modes: OFF -> ALL -> ONE -> OFF.
     */
    fun cycleRepeatMode(): PlaybackQueue = copy(
        repeatMode = when (repeatMode) {
            AudioRepeatMode.OFF -> AudioRepeatMode.ALL
            AudioRepeatMode.ALL -> AudioRepeatMode.ONE
            AudioRepeatMode.ONE -> AudioRepeatMode.OFF
        }
    )

    companion object {
        val EMPTY = PlaybackQueue()

        /**
         * Creates a [PlaybackQueue] with deterministic initial positioning.
         */
        fun from(
            tracks: List<AudioItem>,
            initialIndex: Int = 0,
            isShuffled: Boolean = false,
            repeatMode: AudioRepeatMode = AudioRepeatMode.OFF,
            random: Random = Random.Default
        ): PlaybackQueue {
            if (tracks.isEmpty()) {
                return PlaybackQueue(
                    originalTracks = emptyList(),
                    activeIndices = emptyList(),
                    currentIndex = -1,
                    isShuffled = false,
                    repeatMode = repeatMode
                )
            }

            val clampedIndex = initialIndex.coerceIn(0, tracks.lastIndex)
            return if (!isShuffled) {
                PlaybackQueue(
                    originalTracks = tracks,
                    activeIndices = tracks.indices.toList(),
                    currentIndex = clampedIndex,
                    isShuffled = false,
                    repeatMode = repeatMode
                )
            } else {
                val otherIndices = (tracks.indices - clampedIndex).shuffled(random)
                val activeIndices = listOf(clampedIndex) + otherIndices
                PlaybackQueue(
                    originalTracks = tracks,
                    activeIndices = activeIndices,
                    currentIndex = 0,
                    isShuffled = true,
                    repeatMode = repeatMode
                )
            }
        }
    }
}
