package com.lumaplay.tv.navigation

import com.lumaplay.tv.data.model.VideoItem

/**
 * Type-safe navigation destinations for LumaPlay TV.
 */
sealed interface Screen {
    /** The main TV Home dashboard screen. */
    data object Home : Screen

    /** The full video library browse screen. */
    data object Videos : Screen

    /**
     * Video details screen for a single indexed video.
     * Wired to the player in B2-M1; a dedicated details screen
     * will be added in B2-M2.
     */
    data class VideoDetail(val video: VideoItem) : Screen

    /** Fullscreen hardware-accelerated video playback screen. */
    data class Player(val video: VideoItem) : Screen
}
