package com.lumaplay.tv.ui.videos

import com.lumaplay.tv.data.model.VideoItem

/**
 * UI state for the Video Library screen.
 *
 * Mirrors the HomeUiState pattern for consistency across library screens.
 */
sealed interface VideosUiState {
    /** Initial state while checking for storage permissions. */
    data object CheckingPermission : VideosUiState

    /** Storage permission has not been granted. */
    data object PermissionRequired : VideosUiState

    /** Scanning local storage for video files. */
    data object Loading : VideosUiState

    /** Scan completed successfully, but no video files were found. */
    data object Empty : VideosUiState

    /** Scan completed with discovered local video files. */
    data class Success(val videos: List<VideoItem>) : VideosUiState

    /** Scan failed with an error message. */
    data class Error(val message: String) : VideosUiState
}
