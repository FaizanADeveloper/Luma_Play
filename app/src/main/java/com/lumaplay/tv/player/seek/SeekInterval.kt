package com.lumaplay.tv.player.seek

/**
 * Supported configurable skip/seek intervals for video playback.
 *
 * @property seconds Duration in seconds.
 * @property label User-facing short label for OSD buttons and hints.
 */
enum class SeekInterval(val seconds: Int, val label: String) {
    FIVE(5, "5s"),
    TEN(10, "10s"),
    FIFTEEN(15, "15s"),
    THIRTY(30, "30s"),
    SIXTY(60, "60s");

    val millis: Long
        get() = seconds * 1000L

    /**
     * Cycles to the next available interval.
     */
    fun next(): SeekInterval {
        val all = entries
        return all[(ordinal + 1) % all.size]
    }

    companion object {
        val DEFAULT = TEN

        /**
         * Resolves a [SeekInterval] from a seconds value, defaulting to [DEFAULT] (10s)
         * if the value is unrecognized.
         */
        fun fromSeconds(seconds: Int): SeekInterval {
            return entries.firstOrNull { it.seconds == seconds } ?: DEFAULT
        }
    }
}
