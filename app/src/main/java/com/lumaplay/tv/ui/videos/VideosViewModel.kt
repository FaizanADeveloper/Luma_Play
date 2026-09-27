package com.lumaplay.tv.ui.videos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lumaplay.tv.data.model.VideoItem
import com.lumaplay.tv.data.repository.VideoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for the Video Library screen.
 *
 * Owns video scanning state and sort/filter operations.
 * Shares the same [VideoRepository] implementation as HomeViewModel
 * (backed by MediaStoreVideoRepository), so no additional MediaStore
 * round-trips are introduced.
 *
 * Sort and filter operations are applied in-memory on the already-loaded
 * list — no re-query is issued for each change.
 */
class VideosViewModel(
    private val videoRepository: VideoRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<VideosUiState>(VideosUiState.CheckingPermission)
    val uiState: StateFlow<VideosUiState> = _uiState.asStateFlow()

    /** Raw unfiltered, unsorted list from the last successful scan. */
    private var rawVideos: List<VideoItem> = emptyList()

    private val _sortOrder = MutableStateFlow(VideoSortOrder.DateAdded)
    val sortOrder: StateFlow<VideoSortOrder> = _sortOrder.asStateFlow()

    private val _activeFilter = MutableStateFlow(VideoFilter.All)
    val activeFilter: StateFlow<VideoFilter> = _activeFilter.asStateFlow()

    // -----------------------------------------------------------------
    // Permission handling — mirrors HomeViewModel behaviour
    // -----------------------------------------------------------------

    fun onPermissionResult(isGranted: Boolean) {
        if (isGranted) scanVideos() else _uiState.value = VideosUiState.PermissionRequired
    }

    fun onPermissionChecked(hasPermission: Boolean) {
        if (hasPermission) {
            if (_uiState.value is VideosUiState.CheckingPermission ||
                _uiState.value is VideosUiState.PermissionRequired) {
                scanVideos()
            }
        } else {
            _uiState.value = VideosUiState.PermissionRequired
        }
    }

    // -----------------------------------------------------------------
    // Scanning
    // -----------------------------------------------------------------

    fun scanVideos() {
        _uiState.value = VideosUiState.Loading
        viewModelScope.launch {
            videoRepository.getVideos()
                .onSuccess { videos ->
                    rawVideos = videos
                    applyFilterAndSort()
                }
                .onFailure { throwable ->
                    _uiState.value = VideosUiState.Error(
                        message = throwable.localizedMessage ?: "Failed to scan local video library."
                    )
                }
        }
    }

    // -----------------------------------------------------------------
    // Sort and filter — in-memory only, no re-query
    // -----------------------------------------------------------------

    fun setSortOrder(order: VideoSortOrder) {
        _sortOrder.value = order
        applyFilterAndSort()
    }

    fun setFilter(filter: VideoFilter) {
        _activeFilter.value = filter
        applyFilterAndSort()
    }

    private fun applyFilterAndSort() {
        val filtered = when (_activeFilter.value) {
            VideoFilter.All -> rawVideos
            VideoFilter.UHD4K -> rawVideos.filter { it.height >= 2160 || it.width >= 3840 }
            VideoFilter.HD1080 -> rawVideos.filter {
                (it.height >= 1080 || it.width >= 1920) && it.height < 2160 && it.width < 3840
            }
            VideoFilter.HD720 -> rawVideos.filter {
                (it.height >= 720 || it.width >= 1280) && it.height < 1080 && it.width < 1920
            }
            VideoFilter.SD -> rawVideos.filter {
                it.width > 0 && it.height > 0 && it.height < 720 && it.width < 1280
            }
        }

        val sorted = when (_sortOrder.value) {
            VideoSortOrder.DateAdded -> filtered.sortedByDescending { it.dateAdded }
            VideoSortOrder.Name -> filtered.sortedBy { it.title.lowercase() }
            VideoSortOrder.Duration -> filtered.sortedByDescending { it.durationMs }
            VideoSortOrder.FileSize -> filtered.sortedByDescending { it.sizeBytes }
            VideoSortOrder.Resolution -> filtered.sortedByDescending { it.width * it.height }
        }

        _uiState.value = if (sorted.isEmpty()) VideosUiState.Empty else VideosUiState.Success(sorted)
    }
}

// -----------------------------------------------------------------
// Sort and filter enums
// -----------------------------------------------------------------

/**
 * Sort options available on the Video Library screen.
 * All sorting is performed in-memory on indexed metadata fields.
 */
enum class VideoSortOrder(val label: String) {
    DateAdded("Date Added"),
    Name("Name"),
    Duration("Duration"),
    FileSize("File Size"),
    Resolution("Resolution")
}

/**
 * Filter options derived from resolution metadata available in [VideoItem].
 *
 * Only filters that can be honestly evaluated from MediaStore-indexed fields
 * (width × height) are included.  No HDR, codec or watched-state filters are
 * defined here because that data is not available from MediaStore without
 * opening each container.
 */
enum class VideoFilter(val label: String) {
    All("All Videos"),
    UHD4K("4K UHD"),
    HD1080("1080p"),
    HD720("720p"),
    SD("SD")
}
