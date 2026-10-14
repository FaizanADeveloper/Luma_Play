package com.lumaplay.tv.ui.music

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lumaplay.tv.data.model.AudioItem
import com.lumaplay.tv.data.repository.AudioRepository
import com.lumaplay.tv.player.audio.AudioPlayerController
import com.lumaplay.tv.player.audio.AudioPlayerState
import com.lumaplay.tv.player.audio.AudioRepeatMode
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Presentation state for local music library scanning and browsing.
 */
sealed interface MusicLibraryUiState {
    data object Loading : MusicLibraryUiState
    data class Success(val tracks: List<AudioItem>) : MusicLibraryUiState
    data object Empty : MusicLibraryUiState
    data class Error(val message: String) : MusicLibraryUiState
}

/**
 * Controller abstraction allowing [MusicPlayerViewModel] to remain cleanly decoupled
 * from direct Android framework / Media3 ExoPlayer engine instantiation in JVM unit tests.
 */
interface AudioPlaybackController {
    val state: StateFlow<AudioPlayerState>
    fun setQueue(tracks: List<AudioItem>, startIndex: Int = 0, autoPlay: Boolean = true)
    fun playTrack(track: AudioItem)
    fun playTrackAt(index: Int)
    fun play()
    fun pause()
    fun togglePlayPause()
    fun next()
    fun previous()
    fun seekTo(positionMs: Long)
    fun seekBy(deltaMs: Long)
    fun toggleShuffle()
    fun setShuffle(enabled: Boolean)
    fun setRepeatMode(mode: AudioRepeatMode)
    fun cycleRepeatMode()
    fun release()
}

/**
 * Production implementation of [AudioPlaybackController] that delegates directly
 * to the headless Media3 [AudioPlayerController].
 */
class RealAudioPlaybackController(
    private val controller: AudioPlayerController
) : AudioPlaybackController {
    override val state: StateFlow<AudioPlayerState> get() = controller.state

    override fun setQueue(tracks: List<AudioItem>, startIndex: Int, autoPlay: Boolean) {
        controller.setQueue(tracks = tracks, startIndex = startIndex, autoPlay = autoPlay)
    }

    override fun playTrack(track: AudioItem) = controller.playTrack(track)
    override fun playTrackAt(index: Int) = controller.playTrackAt(index)
    override fun play() = controller.play()
    override fun pause() = controller.pause()
    override fun togglePlayPause() = controller.togglePlayPause()
    override fun next() = controller.next()
    override fun previous() = controller.previous()
    override fun seekTo(positionMs: Long) = controller.seekTo(positionMs)
    override fun seekBy(deltaMs: Long) = controller.seekBy(deltaMs)
    override fun toggleShuffle() = controller.toggleShuffle()
    override fun setShuffle(enabled: Boolean) = controller.setShuffle(enabled)
    override fun setRepeatMode(mode: AudioRepeatMode) = controller.setRepeatMode(mode)
    override fun cycleRepeatMode() = controller.cycleRepeatMode()
    override fun release() = controller.release()
}

/**
 * Activity-scoped ViewModel for music library discovery and playback orchestration.
 *
 * Coordinates [AudioRepository] scanning with [AudioPlaybackController], exposing
 * reactive [StateFlow] streams for UI consumption without duplicating queue or playback state.
 */
class MusicPlayerViewModel(
    private val audioRepository: AudioRepository,
    private val controller: AudioPlaybackController,
    private val coroutineDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {

    /**
     * Production constructor taking the concrete [AudioPlayerController].
     */
    constructor(
        audioRepository: AudioRepository,
        audioPlayerController: AudioPlayerController
    ) : this(
        audioRepository = audioRepository,
        controller = RealAudioPlaybackController(audioPlayerController),
        coroutineDispatcher = Dispatchers.IO
    )

    private val _libraryState = MutableStateFlow<MusicLibraryUiState>(MusicLibraryUiState.Loading)

    /**
     * Observable stream representing music library discovery state.
     */
    val libraryState: StateFlow<MusicLibraryUiState> = _libraryState.asStateFlow()

    /**
     * Observable stream representing active playback status, track metadata, queue, and timeline.
     */
    val playbackState: StateFlow<AudioPlayerState> = controller.state

    @Volatile
    private var isScanInProgress = false

    /**
     * Called by MainActivity or lifecycle observers when storage permission state is verified.
     */
    fun onPermissionChecked(hasPermission: Boolean) {
        if (hasPermission) {
            scanMusic()
        }
    }

    /**
     * Triggers asynchronous audio track discovery from the configured repository.
     */
    fun scanMusic() {
        if (isScanInProgress) return
        isScanInProgress = true
        _libraryState.value = MusicLibraryUiState.Loading

        viewModelScope.launch(coroutineDispatcher) {
            try {
                audioRepository.getAudioTracks()
                    .onSuccess { tracks ->
                        _libraryState.value = if (tracks.isEmpty()) {
                            MusicLibraryUiState.Empty
                        } else {
                            MusicLibraryUiState.Success(tracks)
                        }
                    }
                    .onFailure { error ->
                        _libraryState.value = MusicLibraryUiState.Error(
                            message = error.localizedMessage ?: "Failed to scan audio library."
                        )
                    }
            } finally {
                isScanInProgress = false
            }
        }
    }

    /**
     * Begins playback of [track] using [trackList] as the active queue permutation.
     */
    fun playTrack(track: AudioItem, trackList: List<AudioItem>) {
        if (trackList.isEmpty()) return
        val targetIndex = trackList.indexOfFirst { it.id == track.id }
        val startIndex = if (targetIndex >= 0) targetIndex else 0
        controller.setQueue(tracks = trackList, startIndex = startIndex, autoPlay = true)
    }

    /**
     * Resumes or starts playback.
     */
    fun play() = controller.play()

    /**
     * Pauses playback.
     */
    fun pause() = controller.pause()

    /**
     * Toggles between play and pause.
     */
    fun togglePlayPause() = controller.togglePlayPause()

    /**
     * Advances to the next track in the queue according to repeat policy.
     */
    fun next() = controller.next()

    /**
     * Rewinds or navigates to the previous track according to repeat policy.
     */
    fun previous() = controller.previous()

    /**
     * Seeks to [positionMs] in the current track.
     */
    fun seekTo(positionMs: Long) = controller.seekTo(positionMs)

    /**
     * Seeks relative to the current position by [deltaMs].
     */
    fun seekBy(deltaMs: Long) = controller.seekBy(deltaMs)

    /**
     * Toggles shuffle state on or off.
     */
    fun toggleShuffle() = controller.toggleShuffle()

    /**
     * Sets explicit shuffle state.
     */
    fun setShuffle(enabled: Boolean) = controller.setShuffle(enabled)

    /**
     * Cycles through repeat modes: OFF -> ALL -> ONE -> OFF.
     */
    fun cycleRepeatMode() = controller.cycleRepeatMode()

    /**
     * Sets explicit repeat mode.
     */
    fun setRepeatMode(mode: AudioRepeatMode) = controller.setRepeatMode(mode)

    public override fun onCleared() {
        controller.release()
        super.onCleared()
    }
}
