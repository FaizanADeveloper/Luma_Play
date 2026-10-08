package com.lumaplay.tv.player.audio

/**
 * Repeat modes for audio playback and queue navigation.
 */
enum class AudioRepeatMode {
    /**
     * Playback advances sequentially and stops at the end of the queue.
     * Previous navigation does not wrap when positioned at the beginning.
     */
    OFF,

    /**
     * Playback loops continuously across the entire queue.
     * Next navigation wraps from the final track to the first.
     * Previous navigation wraps from the first track to the final.
     */
    ALL,

    /**
     * Completion of the active track repeats that track.
     * At the end of the queue, next navigation remains on or repeats the current track.
     */
    ONE
}
