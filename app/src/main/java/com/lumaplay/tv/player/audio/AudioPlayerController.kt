package com.lumaplay.tv.player.audio

import android.content.Context
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.lumaplay.tv.data.model.AudioItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Headless audio playback engine and queue controller.
 *
 * Coordinates a dedicated Media3 [ExoPlayer] instance with [PlaybackQueue],
 * exposing an observable [StateFlow] of [AudioPlayerState] to the presentation layer.
 *
 * Architectural Invariants:
 * - [PlaybackQueue] is the sole source of truth for queue ordering, index, shuffle, and repeat.
 * - Feeds [ExoPlayer] ONE active [MediaItem] at a time rather than maintaining an ExoPlayer playlist.
 * - Completely decoupled from video renderers, surfaces, and Activity window transparency.
 * - Handles audio focus and audio-becoming-noisy events automatically.
 */
class AudioPlayerController(
    context: Context
) {
    private val appContext = context.applicationContext
    private val controllerScope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())

    @Volatile
    private var isReleased = false

    private var playbackQueue: PlaybackQueue = PlaybackQueue.EMPTY
    private var positionPollingJob: Job? = null

    private val _state = MutableStateFlow(AudioPlayerState.IDLE)

    /**
     * Observable playback state stream for UI and ViewModel consumers.
     */
    val state: StateFlow<AudioPlayerState> = _state.asStateFlow()

    private val audioAttributes = AudioAttributes.Builder()
        .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
        .setUsage(C.USAGE_MEDIA)
        .build()

    private val exoPlayer: ExoPlayer = ExoPlayer.Builder(appContext)
        .setAudioAttributes(audioAttributes, /* handleAudioFocus = */ true)
        .setHandleAudioBecomingNoisy(true)
        .build()

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            if (isReleased) return
            updatePlaybackState()
            if (isPlaying) {
                startPositionPolling()
            } else {
                stopPositionPolling()
            }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (isReleased) return
            when (playbackState) {
                Player.STATE_ENDED -> {
                    handleNaturalCompletion()
                }
                Player.STATE_READY,
                Player.STATE_BUFFERING,
                Player.STATE_IDLE -> {
                    updatePlaybackState()
                }
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            if (isReleased) return
            stopPositionPolling()
            val errorMsg = error.localizedMessage ?: "Playback error (${error.errorCodeName})"
            _state.update { current ->
                current.copy(
                    status = AudioPlaybackStatus.ERROR,
                    isPlaying = false,
                    errorMessage = errorMsg
                )
            }
        }
    }

    init {
        exoPlayer.addListener(playerListener)
    }

    /**
     * Replaces the active queue and optionally begins playback at [startIndex].
     */
    fun setQueue(tracks: List<AudioItem>, startIndex: Int = 0, autoPlay: Boolean = true) {
        if (isReleased) return
        playbackQueue = PlaybackQueue.from(
            tracks = tracks,
            initialIndex = startIndex,
            isShuffled = playbackQueue.isShuffled,
            repeatMode = playbackQueue.repeatMode
        )
        loadAndPrepareCurrentTrack(playWhenReady = autoPlay)
    }

    /**
     * Plays a specific [track] if it exists in the active playback queue.
     */
    fun playTrack(track: AudioItem) {
        if (isReleased) return
        val newQueue = playbackQueue.playTrack(track)
        if (newQueue != playbackQueue) {
            playbackQueue = newQueue
            loadAndPrepareCurrentTrack(playWhenReady = true)
        } else {
            play()
        }
    }

    /**
     * Plays the track at [index] within the active queue permutation.
     */
    fun playTrackAt(index: Int) {
        if (isReleased) return
        val newQueue = playbackQueue.playTrackAt(index)
        if (newQueue.currentIndex != playbackQueue.currentIndex || exoPlayer.currentMediaItem == null) {
            playbackQueue = newQueue
            loadAndPrepareCurrentTrack(playWhenReady = true)
        } else {
            play()
        }
    }

    /**
     * Starts or resumes playback.
     */
    fun play() {
        if (isReleased || playbackQueue.isEmpty) return
        if (exoPlayer.playbackState == Player.STATE_ENDED) {
            exoPlayer.seekTo(0L)
        }
        exoPlayer.play()
        _state.update { it.copy(isPlaying = true) }
    }

    /**
     * Pauses playback.
     */
    fun pause() {
        if (isReleased) return
        exoPlayer.pause()
        stopPositionPolling()
        _state.update { it.copy(isPlaying = false) }
    }

    /**
     * Toggles between play and pause.
     */
    fun togglePlayPause() {
        if (isReleased) return
        if (exoPlayer.isPlaying) {
            pause()
        } else {
            play()
        }
    }

    /**
     * Advances to the next track according to queue policy.
     */
    fun next() {
        if (isReleased) return
        val nextQueue = playbackQueue.next()
        if (nextQueue != null && nextQueue.currentTrack != playbackQueue.currentTrack) {
            val wasPlaying = exoPlayer.playWhenReady || exoPlayer.isPlaying
            playbackQueue = nextQueue
            loadAndPrepareCurrentTrack(playWhenReady = wasPlaying)
        } else if (nextQueue != null && playbackQueue.repeatMode == AudioRepeatMode.ONE) {
            exoPlayer.seekTo(0L)
            exoPlayer.play()
            _state.update { it.copy(currentPositionMs = 0L, isPlaying = true) }
        } else {
            if (!playbackQueue.isEmpty) {
                _state.update { it.copy(status = AudioPlaybackStatus.ENDED, isPlaying = false) }
            }
        }
    }

    /**
     * Navigates backwards adhering to the 10-foot remote 3-second rewind rule.
     */
    fun previous() {
        if (isReleased) return
        val currentPos = exoPlayer.currentPosition.coerceAtLeast(0L)
        when (val result = playbackQueue.previous(currentPos)) {
            is PreviousResult.SeekToStart -> {
                exoPlayer.seekTo(0L)
                _state.update { it.copy(currentPositionMs = 0L) }
            }
            is PreviousResult.Moved -> {
                val wasPlaying = exoPlayer.playWhenReady || exoPlayer.isPlaying
                playbackQueue = result.queue
                loadAndPrepareCurrentTrack(playWhenReady = wasPlaying)
            }
            is PreviousResult.NoOp -> {
                // At beginning with repeat OFF or ONE: do not change queue/current track
            }
        }
    }

    /**
     * Seeks to [positionMs] within the active track.
     */
    fun seekTo(positionMs: Long) {
        if (isReleased) return
        val duration = exoPlayer.duration
        val maxDuration = if (duration > 0L) duration else Long.MAX_VALUE
        val clamped = positionMs.coerceIn(0L, maxDuration)
        exoPlayer.seekTo(clamped)
        _state.update { it.copy(currentPositionMs = clamped) }
    }

    /**
     * Toggles shuffle state on or off while preserving the currently playing track.
     */
    fun toggleShuffle() {
        if (isReleased) return
        playbackQueue = playbackQueue.toggleShuffle()
        updatePlaybackState()
    }

    /**
     * Sets explicit shuffle state.
     */
    fun setShuffle(enabled: Boolean) {
        if (isReleased) return
        playbackQueue = playbackQueue.setShuffle(enabled)
        updatePlaybackState()
    }

    /**
     * Sets the repeat mode ([AudioRepeatMode.OFF], [AudioRepeatMode.ALL], [AudioRepeatMode.ONE]).
     */
    fun setRepeatMode(mode: AudioRepeatMode) {
        if (isReleased) return
        playbackQueue = playbackQueue.setRepeatMode(mode)
        updatePlaybackState()
    }

    /**
     * Cycles through repeat modes: OFF -> ALL -> ONE -> OFF.
     */
    fun cycleRepeatMode() {
        if (isReleased) return
        playbackQueue = playbackQueue.cycleRepeatMode()
        updatePlaybackState()
    }

    /**
     * Releases player resources, listeners, and coroutine jobs.
     * This operation is idempotent.
     */
    fun release() {
        if (isReleased) return
        isReleased = true
        stopPositionPolling()
        controllerScope.cancel()
        exoPlayer.removeListener(playerListener)
        exoPlayer.stop()
        exoPlayer.clearMediaItems()
        exoPlayer.release()
        _state.update { AudioPlayerState.IDLE }
    }

    private fun loadAndPrepareCurrentTrack(playWhenReady: Boolean) {
        val track = playbackQueue.currentTrack
        if (track == null) {
            exoPlayer.stop()
            exoPlayer.clearMediaItems()
            stopPositionPolling()
            _state.update { current ->
                current.copy(
                    status = AudioPlaybackStatus.IDLE,
                    isPlaying = false,
                    currentTrack = null,
                    currentPositionMs = 0L,
                    durationMs = 0L,
                    queue = playbackQueue.activeTracks,
                    queueIndex = playbackQueue.currentIndex,
                    isShuffled = playbackQueue.isShuffled,
                    repeatMode = playbackQueue.repeatMode,
                    errorMessage = null
                )
            }
            return
        }

        _state.update { current ->
            current.copy(
                status = AudioPlaybackStatus.BUFFERING,
                isPlaying = playWhenReady,
                currentTrack = track,
                currentPositionMs = 0L,
                durationMs = track.durationMs.coerceAtLeast(0L),
                queue = playbackQueue.activeTracks,
                queueIndex = playbackQueue.currentIndex,
                isShuffled = playbackQueue.isShuffled,
                repeatMode = playbackQueue.repeatMode,
                errorMessage = null
            )
        }

        val mediaItem = track.toMediaItem()
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
        exoPlayer.playWhenReady = playWhenReady
    }

    private fun handleNaturalCompletion() {
        stopPositionPolling()
        if (isReleased) return

        when (playbackQueue.repeatMode) {
            AudioRepeatMode.ONE -> {
                exoPlayer.seekTo(0L)
                exoPlayer.play()
                _state.update { current ->
                    current.copy(
                        status = AudioPlaybackStatus.READY,
                        isPlaying = true,
                        currentPositionMs = 0L
                    )
                }
            }
            AudioRepeatMode.ALL -> {
                val nextQueue = playbackQueue.next()
                if (nextQueue != null) {
                    playbackQueue = nextQueue
                    loadAndPrepareCurrentTrack(playWhenReady = true)
                }
            }
            AudioRepeatMode.OFF -> {
                val nextQueue = playbackQueue.next()
                if (nextQueue != null) {
                    playbackQueue = nextQueue
                    loadAndPrepareCurrentTrack(playWhenReady = true)
                } else {
                    _state.update { current ->
                        current.copy(
                            status = AudioPlaybackStatus.ENDED,
                            isPlaying = false,
                            currentPositionMs = exoPlayer.duration.coerceAtLeast(0L)
                        )
                    }
                }
            }
        }
    }

    private fun updatePlaybackState() {
        if (isReleased) return
        val currentTrack = playbackQueue.currentTrack
        val isPlaying = exoPlayer.isPlaying
        val playerState = exoPlayer.playbackState

        val status = when {
            _state.value.status == AudioPlaybackStatus.ERROR -> AudioPlaybackStatus.ERROR
            playerState == Player.STATE_BUFFERING -> AudioPlaybackStatus.BUFFERING
            playerState == Player.STATE_READY -> AudioPlaybackStatus.READY
            playerState == Player.STATE_ENDED -> AudioPlaybackStatus.ENDED
            playerState == Player.STATE_IDLE -> AudioPlaybackStatus.IDLE
            else -> AudioPlaybackStatus.IDLE
        }

        val currentPosition = exoPlayer.currentPosition.coerceAtLeast(0L)
        val rawDuration = exoPlayer.duration
        val duration = if (rawDuration > 0L) {
            rawDuration
        } else {
            currentTrack?.durationMs?.coerceAtLeast(0L) ?: 0L
        }

        _state.update { current ->
            current.copy(
                status = status,
                isPlaying = isPlaying,
                currentTrack = currentTrack,
                currentPositionMs = currentPosition,
                durationMs = duration,
                queue = playbackQueue.activeTracks,
                queueIndex = playbackQueue.currentIndex,
                isShuffled = playbackQueue.isShuffled,
                repeatMode = playbackQueue.repeatMode,
                errorMessage = if (status != AudioPlaybackStatus.ERROR) null else current.errorMessage
            )
        }
    }

    private fun startPositionPolling() {
        positionPollingJob?.cancel()
        positionPollingJob = controllerScope.launch {
            while (isActive && exoPlayer.isPlaying && !isReleased) {
                val currentPos = exoPlayer.currentPosition.coerceAtLeast(0L)
                val rawDuration = exoPlayer.duration
                val duration = if (rawDuration > 0L) {
                    rawDuration
                } else {
                    playbackQueue.currentTrack?.durationMs?.coerceAtLeast(0L) ?: 0L
                }
                _state.update { current ->
                    current.copy(
                        currentPositionMs = currentPos,
                        durationMs = duration
                    )
                }
                delay(500L)
            }
        }
    }

    private fun stopPositionPolling() {
        positionPollingJob?.cancel()
        positionPollingJob = null
    }

    private fun AudioItem.toMediaItem(): MediaItem {
        val cleanArtist = artist?.takeIf {
            it.isNotBlank() && !it.equals("<unknown>", ignoreCase = true)
        }
        val cleanAlbum = album?.takeIf {
            it.isNotBlank() && !it.equals("<unknown>", ignoreCase = true)
        }

        val metadata = MediaMetadata.Builder()
            .setTitle(cleanTitle)
            .apply {
                cleanArtist?.let { setArtist(it) }
                cleanAlbum?.let { setAlbumTitle(it) }
                artworkUri?.let { setArtworkUri(it) }
            }
            .build()

        return MediaItem.Builder()
            .setMediaId(id.toString())
            .setUri(contentUri)
            .apply {
                mimeType?.let { setMimeType(it) }
            }
            .setMediaMetadata(metadata)
            .build()
    }
}
