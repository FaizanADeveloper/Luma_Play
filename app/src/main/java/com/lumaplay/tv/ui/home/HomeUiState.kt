package com.lumaplay.tv.ui.home

import com.lumaplay.tv.data.model.VideoItem

/**
 * UI State for the TV Home & Video Library screen.
 */
sealed interface HomeUiState {
    /** Initial state checking for storage permissions. */
    data object CheckingPermission : HomeUiState

    /** Storage permission has not been granted. */
    data object PermissionRequired : HomeUiState

    /** Scanning local storage for video files. */
    data object Loading : HomeUiState

    /** Scan completed successfully, but no video files were found. */
    data object Empty : HomeUiState

    /** Scan completed with discovered local video files. */
    data class Success(
        val videos: List<VideoItem>,
        val isRescanning: Boolean = false,
        val lastScannedTimestampMs: Long? = null,
        val rescanErrorMessage: String? = null
    ) : HomeUiState

    /** Scan failed with an error. */
    data class Error(val message: String) : HomeUiState
}
