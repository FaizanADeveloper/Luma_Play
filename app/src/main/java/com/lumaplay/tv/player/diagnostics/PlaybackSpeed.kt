package com.lumaplay.tv.player.diagnostics

import java.util.Locale

/**
 * Playback speed options and cycle controller for LumaPlay video player.
 */
object PlaybackSpeed {
    val SUPPORTED_SPEEDS = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
    const val DEFAULT_SPEED = 1.0f

    fun nextSpeed(currentSpeed: Float): Float {
        val currentIndex = SUPPORTED_SPEEDS.indexOfFirst { kotlin.math.abs(it - currentSpeed) < 0.01f }
        return if (currentIndex >= 0 && currentIndex < SUPPORTED_SPEEDS.size - 1) {
            SUPPORTED_SPEEDS[currentIndex + 1]
        } else {
            SUPPORTED_SPEEDS[0]
        }
    }

    fun formatSpeed(speed: Float): String {
        return if (speed % 1f == 0f) {
            String.format(Locale.US, "%.1fx", speed)
        } else {
            String.format(Locale.US, "%sx", speed.toString().removeSuffix(".0"))
        }
    }
}
