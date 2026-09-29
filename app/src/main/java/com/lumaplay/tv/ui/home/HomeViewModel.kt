package com.lumaplay.tv.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lumaplay.tv.data.repository.VideoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel managing storage permissions state and video scanning for the TV Home screen.
 */
class HomeViewModel(
    private val videoRepository: VideoRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.CheckingPermission)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    @Volatile
    private var isScanInProgress = false

    fun onPermissionResult(isGranted: Boolean) {
        if (isGranted) {
            scanVideos()
        } else {
            _uiState.value = HomeUiState.PermissionRequired
        }
    }

    fun onPermissionChecked(hasPermission: Boolean) {
        if (hasPermission) {
            if (_uiState.value is HomeUiState.CheckingPermission || _uiState.value is HomeUiState.PermissionRequired) {
                scanVideos()
            }
        } else {
            _uiState.value = HomeUiState.PermissionRequired
        }
    }

    fun scanVideos() {
        if (isScanInProgress) return

        val currentState = _uiState.value
        if (currentState is HomeUiState.Success) {
            if (currentState.isRescanning) return
            _uiState.value = currentState.copy(isRescanning = true, rescanErrorMessage = null)
            isScanInProgress = true
            viewModelScope.launch {
                try {
                    videoRepository.getVideos()
                        .onSuccess { newVideos ->
                            _uiState.value = if (newVideos.isEmpty()) {
                                HomeUiState.Empty
                            } else {
                                HomeUiState.Success(
                                    videos = newVideos,
                                    isRescanning = false,
                                    lastScannedTimestampMs = System.currentTimeMillis(),
                                    rescanErrorMessage = null
                                )
                            }
                        }
                        .onFailure { error ->
                            // Preserve existing videos and previous successful scan time; show recoverable error
                            _uiState.value = currentState.copy(
                                isRescanning = false,
                                rescanErrorMessage = error.localizedMessage ?: "Failed to refresh video library"
                            )
                        }
                } finally {
                    isScanInProgress = false
                }
            }
        } else {
            _uiState.value = HomeUiState.Loading
            isScanInProgress = true
            viewModelScope.launch {
                try {
                    videoRepository.getVideos()
                        .onSuccess { videos ->
                            _uiState.value = if (videos.isEmpty()) {
                                HomeUiState.Empty
                            } else {
                                HomeUiState.Success(
                                    videos = videos,
                                    isRescanning = false,
                                    lastScannedTimestampMs = System.currentTimeMillis(),
                                    rescanErrorMessage = null
                                )
                            }
                        }
                        .onFailure { throwable ->
                            _uiState.value = HomeUiState.Error(
                                message = throwable.localizedMessage ?: "Failed to scan local video library."
                            )
                        }
                } finally {
                    isScanInProgress = false
                }
            }
        }
    }

}
