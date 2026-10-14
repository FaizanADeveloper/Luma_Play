package com.lumaplay.tv.ui.music

import android.net.FakeUri
import com.lumaplay.tv.data.model.AlbumItem
import com.lumaplay.tv.data.model.AudioItem
import com.lumaplay.tv.data.repository.AudioRepository
import com.lumaplay.tv.player.audio.AudioPlayerState
import com.lumaplay.tv.player.audio.AudioRepeatMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MusicPlayerViewModelTest {

    private fun createTrack(id: Long, title: String = "Track $id"): AudioItem {
        return AudioItem(
            id = id,
            contentUri = FakeUri("content://media/external/audio/media/$id"),
            title = title,
            artist = "Artist $id",
            album = "Album $id",
            durationMs = 210_000L
        )
    }

    private class FakeAudioRepository(
        var tracksResult: Result<List<AudioItem>> = Result.success(emptyList())
    ) : AudioRepository {
        var scanCallCount = 0

        override suspend fun getAudioTracks(): Result<List<AudioItem>> {
            scanCallCount++
            return tracksResult
        }

        override suspend fun getAlbums(): Result<List<AlbumItem>> {
            return Result.success(emptyList())
        }
    }

    private class FakeAudioPlaybackController : AudioPlaybackController {
        val _state = MutableStateFlow(AudioPlayerState.IDLE)
        override val state: StateFlow<AudioPlayerState> = _state.asStateFlow()

        var lastSetQueueTracks: List<AudioItem>? = null
        var lastSetQueueStartIndex: Int? = null
        var lastSetQueueAutoPlay: Boolean? = null

        var playTrackCalledWith: AudioItem? = null
        var playTrackAtCalledWith: Int? = null
        var playCalled = false
        var pauseCalled = false
        var togglePlayPauseCalled = false
        var nextCalled = false
        var previousCalled = false
        var seekToCalledWith: Long? = null
        var seekByCalledWith: Long? = null
        var toggleShuffleCalled = false
        var setShuffleCalledWith: Boolean? = null
        var setRepeatModeCalledWith: AudioRepeatMode? = null
        var cycleRepeatModeCalled = false
        var releaseCalled = false

        override fun setQueue(tracks: List<AudioItem>, startIndex: Int, autoPlay: Boolean) {
            lastSetQueueTracks = tracks
            lastSetQueueStartIndex = startIndex
            lastSetQueueAutoPlay = autoPlay
        }

        override fun playTrack(track: AudioItem) { playTrackCalledWith = track }
        override fun playTrackAt(index: Int) { playTrackAtCalledWith = index }
        override fun play() { playCalled = true }
        override fun pause() { pauseCalled = true }
        override fun togglePlayPause() { togglePlayPauseCalled = true }
        override fun next() { nextCalled = true }
        override fun previous() { previousCalled = true }
        override fun seekTo(positionMs: Long) { seekToCalledWith = positionMs }
        override fun seekBy(deltaMs: Long) { seekByCalledWith = deltaMs }
        override fun toggleShuffle() { toggleShuffleCalled = true }
        override fun setShuffle(enabled: Boolean) { setShuffleCalledWith = enabled }
        override fun setRepeatMode(mode: AudioRepeatMode) { setRepeatModeCalledWith = mode }
        override fun cycleRepeatMode() { cycleRepeatModeCalled = true }
        override fun release() { releaseCalled = true }
    }

    // 1. Initial library state is correct.
    @Test
    fun testInitialLibraryStateIsLoading() {
        val repository = FakeAudioRepository()
        val controller = FakeAudioPlaybackController()
        val viewModel = MusicPlayerViewModel(
            audioRepository = repository,
            controller = controller,
            coroutineDispatcher = Dispatchers.Unconfined
        )

        assertEquals(MusicLibraryUiState.Loading, viewModel.libraryState.value)
        assertEquals(0, repository.scanCallCount)
    }

    // 2. Successful scan exposes discovered tracks.
    @Test
    fun testSuccessfulScanExposesDiscoveredTracks() {
        val track1 = createTrack(1, "Track 1")
        val track2 = createTrack(2, "Track 2")
        val repository = FakeAudioRepository(Result.success(listOf(track1, track2)))
        val controller = FakeAudioPlaybackController()
        val viewModel = MusicPlayerViewModel(
            audioRepository = repository,
            controller = controller,
            coroutineDispatcher = Dispatchers.Unconfined
        )

        viewModel.scanMusic()

        assertEquals(1, repository.scanCallCount)
        val state = viewModel.libraryState.value
        assertTrue(state is MusicLibraryUiState.Success)
        val success = state as MusicLibraryUiState.Success
        assertEquals(2, success.tracks.size)
        assertEquals("Track 1", success.tracks[0].title)
        assertEquals("Track 2", success.tracks[1].title)
    }

    // 3. Empty repository result produces empty state.
    @Test
    fun testEmptyRepositoryResultProducesEmptyState() {
        val repository = FakeAudioRepository(Result.success(emptyList()))
        val controller = FakeAudioPlaybackController()
        val viewModel = MusicPlayerViewModel(
            audioRepository = repository,
            controller = controller,
            coroutineDispatcher = Dispatchers.Unconfined
        )

        viewModel.scanMusic()

        assertEquals(1, repository.scanCallCount)
        assertEquals(MusicLibraryUiState.Empty, viewModel.libraryState.value)
    }

    // 4. Repository failure produces error state.
    @Test
    fun testRepositoryFailureProducesErrorState() {
        val repository = FakeAudioRepository(Result.failure(IllegalStateException("Disk read failure")))
        val controller = FakeAudioPlaybackController()
        val viewModel = MusicPlayerViewModel(
            audioRepository = repository,
            controller = controller,
            coroutineDispatcher = Dispatchers.Unconfined
        )

        viewModel.scanMusic()

        assertEquals(1, repository.scanCallCount)
        val state = viewModel.libraryState.value
        assertTrue(state is MusicLibraryUiState.Error)
        val error = state as MusicLibraryUiState.Error
        assertEquals("Disk read failure", error.message)
    }

    // 5. playTrack() delegates the selected track and correct starting index to AudioPlayerController.
    @Test
    fun testPlayTrackDelegatesSelectedTrackAndIndex() {
        val track1 = createTrack(10, "Alpha")
        val track2 = createTrack(20, "Beta")
        val track3 = createTrack(30, "Gamma")
        val trackList = listOf(track1, track2, track3)

        val repository = FakeAudioRepository()
        val controller = FakeAudioPlaybackController()
        val viewModel = MusicPlayerViewModel(
            audioRepository = repository,
            controller = controller,
            coroutineDispatcher = Dispatchers.Unconfined
        )

        viewModel.playTrack(track2, trackList)

        assertEquals(trackList, controller.lastSetQueueTracks)
        assertEquals(1, controller.lastSetQueueStartIndex)
        assertEquals(true, controller.lastSetQueueAutoPlay)
    }

    @Test
    fun testPlayTrackWithMissingTrackFallsBackSafely() {
        val track1 = createTrack(10, "Alpha")
        val track2 = createTrack(20, "Beta")
        val unlistedTrack = createTrack(99, "Unlisted")
        val trackList = listOf(track1, track2)

        val repository = FakeAudioRepository()
        val controller = FakeAudioPlaybackController()
        val viewModel = MusicPlayerViewModel(
            audioRepository = repository,
            controller = controller,
            coroutineDispatcher = Dispatchers.Unconfined
        )

        viewModel.playTrack(unlistedTrack, trackList)

        assertEquals(trackList, controller.lastSetQueueTracks)
        assertEquals(0, controller.lastSetQueueStartIndex)
        assertEquals(true, controller.lastSetQueueAutoPlay)
    }

    @Test
    fun testPlayTrackWithEmptyListDoesNotCrashOrSetQueue() {
        val track = createTrack(1, "Track 1")
        val repository = FakeAudioRepository()
        val controller = FakeAudioPlaybackController()
        val viewModel = MusicPlayerViewModel(
            audioRepository = repository,
            controller = controller,
            coroutineDispatcher = Dispatchers.Unconfined
        )

        viewModel.playTrack(track, emptyList())

        assertNull(controller.lastSetQueueTracks)
        assertNull(controller.lastSetQueueStartIndex)
    }

    // 6. Transport methods delegate correctly.
    @Test
    fun testTransportMethodsDelegateCorrectly() {
        val repository = FakeAudioRepository()
        val controller = FakeAudioPlaybackController()
        val viewModel = MusicPlayerViewModel(
            audioRepository = repository,
            controller = controller,
            coroutineDispatcher = Dispatchers.Unconfined
        )

        assertFalse(controller.playCalled)
        viewModel.play()
        assertTrue(controller.playCalled)

        assertFalse(controller.pauseCalled)
        viewModel.pause()
        assertTrue(controller.pauseCalled)

        assertFalse(controller.togglePlayPauseCalled)
        viewModel.togglePlayPause()
        assertTrue(controller.togglePlayPauseCalled)

        assertFalse(controller.nextCalled)
        viewModel.next()
        assertTrue(controller.nextCalled)

        assertFalse(controller.previousCalled)
        viewModel.previous()
        assertTrue(controller.previousCalled)

        assertNull(controller.seekToCalledWith)
        viewModel.seekTo(45_000L)
        assertEquals(45_000L, controller.seekToCalledWith)

        assertNull(controller.seekByCalledWith)
        viewModel.seekBy(10_000L)
        assertEquals(10_000L, controller.seekByCalledWith)
        viewModel.seekBy(-10_000L)
        assertEquals(-10_000L, controller.seekByCalledWith)
    }

    // 7. Shuffle and repeat methods delegate correctly.
    @Test
    fun testShuffleAndRepeatMethodsDelegateCorrectly() {
        val repository = FakeAudioRepository()
        val controller = FakeAudioPlaybackController()
        val viewModel = MusicPlayerViewModel(
            audioRepository = repository,
            controller = controller,
            coroutineDispatcher = Dispatchers.Unconfined
        )

        assertFalse(controller.toggleShuffleCalled)
        viewModel.toggleShuffle()
        assertTrue(controller.toggleShuffleCalled)

        assertNull(controller.setShuffleCalledWith)
        viewModel.setShuffle(true)
        assertEquals(true, controller.setShuffleCalledWith)
        viewModel.setShuffle(false)
        assertEquals(false, controller.setShuffleCalledWith)

        assertFalse(controller.cycleRepeatModeCalled)
        viewModel.cycleRepeatMode()
        assertTrue(controller.cycleRepeatModeCalled)

        assertNull(controller.setRepeatModeCalledWith)
        viewModel.setRepeatMode(AudioRepeatMode.ONE)
        assertEquals(AudioRepeatMode.ONE, controller.setRepeatModeCalledWith)
        viewModel.setRepeatMode(AudioRepeatMode.ALL)
        assertEquals(AudioRepeatMode.ALL, controller.setRepeatModeCalledWith)
    }

    // 8. onPermissionChecked(false) does not trigger scanning.
    @Test
    fun testOnPermissionCheckedFalseDoesNotTriggerScan() {
        val repository = FakeAudioRepository()
        val controller = FakeAudioPlaybackController()
        val viewModel = MusicPlayerViewModel(
            audioRepository = repository,
            controller = controller,
            coroutineDispatcher = Dispatchers.Unconfined
        )

        viewModel.onPermissionChecked(false)

        assertEquals(0, repository.scanCallCount)
        assertEquals(MusicLibraryUiState.Loading, viewModel.libraryState.value)
    }

    // 9. onPermissionChecked(true) triggers scanning.
    @Test
    fun testOnPermissionCheckedTrueTriggersScan() {
        val track = createTrack(1, "Track")
        val repository = FakeAudioRepository(Result.success(listOf(track)))
        val controller = FakeAudioPlaybackController()
        val viewModel = MusicPlayerViewModel(
            audioRepository = repository,
            controller = controller,
            coroutineDispatcher = Dispatchers.Unconfined
        )

        viewModel.onPermissionChecked(true)

        assertEquals(1, repository.scanCallCount)
        assertTrue(viewModel.libraryState.value is MusicLibraryUiState.Success)
    }

    // 10. ViewModel cleanup calls AudioPlayerController.release().
    @Test
    fun testOnClearedReleasesAudioPlayerController() {
        val repository = FakeAudioRepository()
        val controller = FakeAudioPlaybackController()
        val viewModel = MusicPlayerViewModel(
            audioRepository = repository,
            controller = controller,
            coroutineDispatcher = Dispatchers.Unconfined
        )

        assertFalse(controller.releaseCalled)
        viewModel.onCleared()
        assertTrue(controller.releaseCalled)
    }
}
