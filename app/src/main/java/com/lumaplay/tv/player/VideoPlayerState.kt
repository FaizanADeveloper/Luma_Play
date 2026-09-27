package com.lumaplay.tv.player

/**
 * State holder for fullscreen video playback.
 *
 * Tracks transport progress, buffering, error messages, and OSD visibility.
 */
data class VideoPlayerState(
    val isPlaying: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val isBuffering: Boolean = true,
    val isEnded: Boolean = false,
    val errorMessage: String? = null,
    val showControls: Boolean = true
) {
    /**
     * Normalized progress value [0.0f, 1.0f] for seek bars and progress indicators.
     */
    val progress: Float
        get() = if (durationMs > 0) {
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
     * Total video duration formatted as HH:mm:ss or mm:ss.
     */
    val formattedDuration: String
        get() = formatTime(durationMs)

    private fun formatTime(timeMs: Long): String {
        if (timeMs <= 0) return "00:00"
        val totalSeconds = timeMs / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return if (hours > 0) {
            String.format("%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%02d:%02d", minutes, seconds)
        }
    }
}
