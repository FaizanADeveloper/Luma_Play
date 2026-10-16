package com.lumaplay.tv.player.seek

/**
 * State model for YouTube-style deferred timeline scrubbing.
 *
 * Allows the user to navigate the timeline and inspect preview timestamps/thumbnails
 * without continuously seeking and flushing the playback pipeline.
 *
 * @property isScrubbing True when the timeline is active and receiving seek adjustments.
 * @property previewPositionMs Current scrub preview position along the timeline.
 * @property originalPositionMs The playback position when scrubbing began (for cancellation).
 * @property durationMs Total duration of the media in milliseconds.
 */
data class DeferredSeekState(
    val isScrubbing: Boolean = false,
    val previewPositionMs: Long = 0L,
    val originalPositionMs: Long = 0L,
    val durationMs: Long = 0L
) {

    /**
     * Normalized progress value [0.0f, 1.0f] for the scrubbing thumb position.
     */
    val progress: Float
        get() = if (durationMs > 0L) {
            (previewPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }

    /**
     * Preview position formatted as HH:mm:ss or mm:ss.
     */
    val formattedPreviewPosition: String
        get() = formatTime(previewPositionMs)

    /**
     * Steps forward by [intervalMs], clamped to [durationMs].
     */
    fun stepForward(intervalMs: Long): DeferredSeekState {
        val nextPos = SeekClampingHelper.calculateForward(previewPositionMs, intervalMs, durationMs)
        return copy(previewPositionMs = nextPos)
    }

    /**
     * Steps backward by [intervalMs], clamped to 0.
     */
    fun stepBackward(intervalMs: Long): DeferredSeekState {
        val nextPos = SeekClampingHelper.calculateBackward(previewPositionMs, intervalMs, durationMs)
        return copy(previewPositionMs = nextPos)
    }

    companion object {
        val IDLE = DeferredSeekState()

        /**
         * Initializes a new scrubbing session from the current playback position.
         */
        fun startScrubbing(currentPositionMs: Long, durationMs: Long): DeferredSeekState {
            val clamped = SeekClampingHelper.clampSeekPosition(currentPositionMs, durationMs)
            return DeferredSeekState(
                isScrubbing = true,
                previewPositionMs = clamped,
                originalPositionMs = clamped,
                durationMs = durationMs
            )
        }

        private fun formatTime(timeMs: Long): String {
            if (timeMs <= 0L) return "00:00"
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
}
