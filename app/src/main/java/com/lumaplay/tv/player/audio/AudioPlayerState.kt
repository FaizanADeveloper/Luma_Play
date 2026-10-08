package com.lumaplay.tv.player.audio

import com.lumaplay.tv.data.model.AudioItem
import java.util.Locale

/**
 * High-level playback status states for the audio player.
 */
enum class AudioPlaybackStatus {
    IDLE,
    BUFFERING,
    READY,
    ENDED,
    ERROR
}

/**
 * Immutable state model for audio playback suitable for StateFlow emission.
 *
 * Exposes current transport status, active track, timeline progress,
 * queue context, shuffle/repeat modes, and pure derived metrics.
 */
data class AudioPlayerState(
    val status: AudioPlaybackStatus = AudioPlaybackStatus.IDLE,
    val isPlaying: Boolean = false,
    val currentTrack: AudioItem? = null,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val queue: List<AudioItem> = emptyList(),
    val queueIndex: Int = -1,
    val isShuffled: Boolean = false,
    val repeatMode: AudioRepeatMode = AudioRepeatMode.OFF,
    val errorMessage: String? = null
) {
    /**
     * Normalized progress value [0.0f, 1.0f] for seek bars and progress scrubbers.
     */
    val progress: Float
        get() = if (durationMs > 0L) {
            (currentPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }

    /**
     * Elapsed playback time formatted as HH:mm:ss or mm:ss.
     */
    val formattedCurrentPosition: String
        get() = formatTime(currentPositionMs)

    /**
     * Total track duration formatted as HH:mm:ss or mm:ss.
     */
    val formattedDuration: String
        get() = formatTime(durationMs)

    /**
     * Whether forward navigation to a next track is available in the current queue state.
     */
    val hasNext: Boolean
        get() = when {
            queue.isEmpty() -> false
            repeatMode == AudioRepeatMode.ALL -> true
            repeatMode == AudioRepeatMode.ONE -> true
            else -> queueIndex in 0 until (queue.size - 1)
        }

    /**
     * Whether backward navigation (previous track or seek to beginning) is available.
     */
    val hasPrevious: Boolean
        get() = when {
            queue.isEmpty() -> false
            currentPositionMs > 3000L -> true
            repeatMode == AudioRepeatMode.ALL -> true
            else -> queueIndex > 0
        }

    companion object {
        val IDLE = AudioPlayerState()

        private fun formatTime(timeMs: Long): String {
            if (timeMs <= 0L) return "00:00"
            val totalSeconds = timeMs / 1000
            val hours = totalSeconds / 3600
            val minutes = (totalSeconds % 3600) / 60
            val seconds = totalSeconds % 60
            return if (hours > 0) {
                String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
            } else {
                String.format(Locale.US, "%02d:%02d", minutes, seconds)
            }
        }
    }
}
