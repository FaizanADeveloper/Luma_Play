package com.lumaplay.tv.player.seek

/**
 * Pure utility functions for calculating seek positions with safe boundary clamping.
 */
object SeekClampingHelper {

    /**
     * Clamps [targetMs] to [0L, durationMs] when [durationMs] > 0.
     * If [durationMs] is unknown or <= 0, clamps to [0L, Long.MAX_VALUE].
     */
    fun clampSeekPosition(targetMs: Long, durationMs: Long): Long {
        val lowerClamped = targetMs.coerceAtLeast(0L)
        return if (durationMs > 0L) {
            lowerClamped.coerceAtMost(durationMs)
        } else {
            lowerClamped
        }
    }

    /**
     * Calculates the backward seek position from [currentMs] by [intervalMs],
     * clamped at 0L.
     */
    fun calculateBackward(currentMs: Long, intervalMs: Long, durationMs: Long): Long {
        val target = currentMs - intervalMs
        return clampSeekPosition(target, durationMs)
    }

    /**
     * Calculates the forward seek position from [currentMs] by [intervalMs],
     * clamped at [durationMs] if known.
     */
    fun calculateForward(currentMs: Long, intervalMs: Long, durationMs: Long): Long {
        val target = currentMs + intervalMs
        return clampSeekPosition(target, durationMs)
    }
}
