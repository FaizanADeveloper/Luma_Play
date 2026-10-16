package com.lumaplay.tv.player.seek

import android.content.Context
import android.content.SharedPreferences

/**
 * Lightweight SharedPreferences-backed persistence for playback preferences.
 *
 * Provides safe storage for user-configured skip intervals without requiring
 * external dependencies or premature implementation of full Phase 8 DataStore.
 */
class SeekPreferences(
    private val prefs: SharedPreferences
) {

    constructor(context: Context) : this(
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    )

    /**
     * Retrieves the stored [SeekInterval], falling back to [SeekInterval.DEFAULT] (10s).
     */
    fun getSeekInterval(): SeekInterval {
        val seconds = prefs.getInt(KEY_SEEK_INTERVAL_SECONDS, SeekInterval.DEFAULT.seconds)
        return SeekInterval.fromSeconds(seconds)
    }

    /**
     * Persists the given [SeekInterval].
     */
    fun setSeekInterval(interval: SeekInterval) {
        prefs.edit().putInt(KEY_SEEK_INTERVAL_SECONDS, interval.seconds).commit()
    }

    companion object {
        const val PREFS_NAME = "lumaplay_playback_preferences"
        const val KEY_SEEK_INTERVAL_SECONDS = "seek_interval_seconds"
    }
}
