package com.lumaplay.tv.ui.details

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lumaplay.tv.data.model.TechnicalMediaSpecs
import com.lumaplay.tv.data.model.VideoItem
import com.lumaplay.tv.data.util.MediaSpecsExtractor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * UI state holder for the Video Details & Technical Specs screen.
 */
class VideoDetailsViewModel(application: Application) : AndroidViewModel(application) {

    private val _currentVideo = MutableStateFlow<VideoItem?>(null)
    val currentVideo: StateFlow<VideoItem?> = _currentVideo.asStateFlow()

    private val _specs = MutableStateFlow(TechnicalMediaSpecs.Empty)
    val specs: StateFlow<TechnicalMediaSpecs> = _specs.asStateFlow()

    private val _isLoadingSpecs = MutableStateFlow(false)
    val isLoadingSpecs: StateFlow<Boolean> = _isLoadingSpecs.asStateFlow()

    /**
     * Initializes or switches the active video for the details screen.
     * Launches asynchronous on-demand metadata extraction.
     */
    fun setVideo(video: VideoItem) {
        if (_currentVideo.value?.id == video.id && _specs.value.isExtracted) {
            return
        }

        _currentVideo.value = video
        _specs.value = TechnicalMediaSpecs.Empty
        _isLoadingSpecs.value = true

        viewModelScope.launch {
            val extractedSpecs = MediaSpecsExtractor.extractSpecs(
                context = getApplication(),
                video = video
            )
            _specs.value = extractedSpecs
            _isLoadingSpecs.value = false
        }
    }
}
