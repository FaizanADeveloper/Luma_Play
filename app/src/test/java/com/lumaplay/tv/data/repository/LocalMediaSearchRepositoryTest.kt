package com.lumaplay.tv.data.repository

import android.net.FakeUri
import com.lumaplay.tv.data.model.AlbumItem
import com.lumaplay.tv.data.model.ArtistItem
import com.lumaplay.tv.data.model.AudioItem
import com.lumaplay.tv.data.model.SearchResults
import com.lumaplay.tv.data.model.VideoItem
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class LocalMediaSearchRepositoryTest {

    private fun createVideo(
        id: Long,
        title: String,
        folderName: String = "Internal Storage / Videos",
        filePath: String? = null
    ): VideoItem = VideoItem(
        id = id,
        contentUri = FakeUri("content://media/external/video/media/$id"),
        title = title,
        durationMs = 7_200_000L,
        width = 3840,
        height = 2160,
        sizeBytes = 5_000_000_000L,
        dateAdded = 1000L,
        folderName = folderName,
        filePath = filePath
    )

    private fun createTrack(
        id: Long,
        title: String,
        artist: String? = null,
        album: String? = null,
        folderName: String = "Internal Storage / Music",
        filePath: String? = null
    ): AudioItem = AudioItem(
        id = id,
        contentUri = FakeUri("content://media/external/audio/media/$id"),
        title = title,
        artist = artist,
        album = album,
        durationMs = 210_000L,
        sizeBytes = 8_000_000L,
        dateAdded = 2000L,
        folderName = folderName,
        filePath = filePath
    )

    private fun createArtist(
        name: String,
        trackCount: Int = 1,
        albumCount: Int = 1
    ): ArtistItem = ArtistItem(
        name = name,
        trackCount = trackCount,
        albumCount = albumCount
    )

    private fun createAlbum(
        id: Long,
        title: String,
        artist: String,
        trackCount: Int = 1
    ): AlbumItem = AlbumItem(
        id = id,
        title = title,
        artist = artist,
        trackCount = trackCount
    )

    private class FakeVideoRepository(
        var result: Result<List<VideoItem>> = Result.success(emptyList())
    ) : VideoRepository {
        var callCount = 0
        override suspend fun getVideos(): Result<List<VideoItem>> {
            callCount++
            return result
        }
    }

    private class FakeAudioRepository(
        var tracksResult: Result<List<AudioItem>> = Result.success(emptyList()),
        var artistsResult: Result<List<ArtistItem>> = Result.success(emptyList()),
        var albumsResult: Result<List<AlbumItem>> = Result.success(emptyList())
    ) : AudioRepository {
        var tracksCallCount = 0
        var artistsCallCount = 0
        var albumsCallCount = 0

        override suspend fun getAudioTracks(): Result<List<AudioItem>> {
            tracksCallCount++
            return tracksResult
        }

        override suspend fun getAlbums(): Result<List<AlbumItem>> {
            albumsCallCount++
            return albumsResult
        }

        override suspend fun getArtists(): Result<List<ArtistItem>> {
            artistsCallCount++
            return artistsResult
        }
    }

    // 1. Video title matching
    @Test
    fun testVideoTitleMatching() {
        runBlocking {
            val video1 = createVideo(1L, "Interstellar")
            val video2 = createVideo(2L, "Inception")
            val videoRepo = FakeVideoRepository(Result.success(listOf(video1, video2)))
            val audioRepo = FakeAudioRepository()
            val searchRepo = LocalMediaSearchRepository(videoRepo, audioRepo, Dispatchers.Unconfined)

            val result = searchRepo.search("Interstellar")

            assertTrue(result.isSuccess)
            val searchResults = result.getOrThrow()
            assertEquals(1, searchResults.videos.size)
            assertEquals("Interstellar", searchResults.videos[0].title)
        }
    }

    // 2. Clean-title matching
    @Test
    fun testCleanTitleMatching() {
        runBlocking {
            val video = createVideo(1L, "The.Dark.Knight.mkv")
            val videoRepo = FakeVideoRepository(Result.success(listOf(video)))
            val audioRepo = FakeAudioRepository()
            val searchRepo = LocalMediaSearchRepository(videoRepo, audioRepo, Dispatchers.Unconfined)

            val result = searchRepo.search("Dark.Knight")

            assertTrue(result.isSuccess)
            val searchResults = result.getOrThrow()
            assertEquals(1, searchResults.videos.size)
            assertEquals("The.Dark.Knight", searchResults.videos[0].cleanTitle)
        }
    }

    // 3. Folder matching
    @Test
    fun testFolderMatching() {
        runBlocking {
            val video1 = createVideo(1L, "Movie A", folderName = "Internal Storage / SciFi")
            val video2 = createVideo(2L, "Movie B", folderName = "Internal Storage / Comedy")
            val track1 = createTrack(1L, "Song A", folderName = "Internal Storage / SciFi Soundtrack")
            val videoRepo = FakeVideoRepository(Result.success(listOf(video1, video2)))
            val audioRepo = FakeAudioRepository(tracksResult = Result.success(listOf(track1)))
            val searchRepo = LocalMediaSearchRepository(videoRepo, audioRepo, Dispatchers.Unconfined)

            val result = searchRepo.search("SciFi")

            assertTrue(result.isSuccess)
            val searchResults = result.getOrThrow()
            assertEquals(2, searchResults.folders.size)
            assertTrue(searchResults.folders.contains("Internal Storage / SciFi"))
            assertTrue(searchResults.folders.contains("Internal Storage / SciFi Soundtrack"))
            assertEquals(1, searchResults.videos.size) // Movie A matched via folderName
        }
    }

    // 4. Music-track title matching
    @Test
    fun testMusicTrackTitleMatching() {
        runBlocking {
            val track1 = createTrack(1L, "Bohemian Rhapsody.mp3", artist = "Queen")
            val track2 = createTrack(2L, "Comfortably Numb.flac", artist = "Pink Floyd")
            val videoRepo = FakeVideoRepository()
            val audioRepo = FakeAudioRepository(tracksResult = Result.success(listOf(track1, track2)))
            val searchRepo = LocalMediaSearchRepository(videoRepo, audioRepo, Dispatchers.Unconfined)

            val result = searchRepo.search("Bohemian")

            assertTrue(result.isSuccess)
            val searchResults = result.getOrThrow()
            assertEquals(1, searchResults.tracks.size)
            assertEquals("Bohemian Rhapsody.mp3", searchResults.tracks[0].title)
            assertEquals("Bohemian Rhapsody", searchResults.tracks[0].cleanTitle)
        }
    }

    // 5. Artist matching
    @Test
    fun testArtistMatching() {
        runBlocking {
            val artist1 = createArtist("Pink Floyd", trackCount = 10, albumCount = 3)
            val artist2 = createArtist("Radiohead", trackCount = 5, albumCount = 2)
            val track = createTrack(1L, "Time", artist = "Pink Floyd")
            val videoRepo = FakeVideoRepository()
            val audioRepo = FakeAudioRepository(
                tracksResult = Result.success(listOf(track)),
                artistsResult = Result.success(listOf(artist1, artist2))
            )
            val searchRepo = LocalMediaSearchRepository(videoRepo, audioRepo, Dispatchers.Unconfined)

            val result = searchRepo.search("Pink Floyd")

            assertTrue(result.isSuccess)
            val searchResults = result.getOrThrow()
            assertEquals(1, searchResults.artists.size)
            assertEquals("Pink Floyd", searchResults.artists[0].name)
            assertEquals(1, searchResults.tracks.size)
        }
    }

    // 6. Album matching
    @Test
    fun testAlbumMatching() {
        runBlocking {
            val album1 = createAlbum(100L, "The Dark Side of the Moon", "Pink Floyd")
            val album2 = createAlbum(101L, "OK Computer", "Radiohead")
            val videoRepo = FakeVideoRepository()
            val audioRepo = FakeAudioRepository(
                albumsResult = Result.success(listOf(album1, album2))
            )
            val searchRepo = LocalMediaSearchRepository(videoRepo, audioRepo, Dispatchers.Unconfined)

            val result = searchRepo.search("Dark Side")

            assertTrue(result.isSuccess)
            val searchResults = result.getOrThrow()
            assertEquals(1, searchResults.albums.size)
            assertEquals("The Dark Side of the Moon", searchResults.albums[0].title)
        }
    }

    // 7. Case-insensitive queries
    @Test
    fun testCaseInsensitiveQueries() {
        runBlocking {
            val video = createVideo(1L, "Pulp Fiction")
            val track = createTrack(1L, "Misirlou", artist = "Dick Dale")
            val artist = createArtist("Dick Dale")
            val videoRepo = FakeVideoRepository(Result.success(listOf(video)))
            val audioRepo = FakeAudioRepository(
                tracksResult = Result.success(listOf(track)),
                artistsResult = Result.success(listOf(artist))
            )
            val searchRepo = LocalMediaSearchRepository(videoRepo, audioRepo, Dispatchers.Unconfined)

            val resultVideo = searchRepo.search("pUlP fIcTiOn")
            assertTrue(resultVideo.isSuccess)
            assertEquals(1, resultVideo.getOrThrow().videos.size)

            val resultAudio = searchRepo.search("dIcK dAlE")
            assertTrue(resultAudio.isSuccess)
            assertEquals(1, resultAudio.getOrThrow().tracks.size)
            assertEquals(1, resultAudio.getOrThrow().artists.size)
        }
    }

    // 8. Leading and trailing whitespace
    @Test
    fun testLeadingAndTrailingWhitespace() {
        runBlocking {
            val video = createVideo(1L, "The Matrix")
            val videoRepo = FakeVideoRepository(Result.success(listOf(video)))
            val audioRepo = FakeAudioRepository()
            val searchRepo = LocalMediaSearchRepository(videoRepo, audioRepo, Dispatchers.Unconfined)

            val result = searchRepo.search("   The Matrix   \t")

            assertTrue(result.isSuccess)
            val searchResults = result.getOrThrow()
            assertEquals(1, searchResults.videos.size)
            assertEquals("The Matrix", searchResults.videos[0].title)
        }
    }

    // 9. Blank queries return empty results without querying repositories
    @Test
    fun testBlankQueriesReturnEmptyWithoutQueryingRepositories() {
        runBlocking {
            val videoRepo = FakeVideoRepository()
            val audioRepo = FakeAudioRepository()
            val searchRepo = LocalMediaSearchRepository(videoRepo, audioRepo, Dispatchers.Unconfined)

            val emptyResult = searchRepo.search("")
            assertTrue(emptyResult.isSuccess)
            assertTrue(emptyResult.getOrThrow().isEmpty)
            assertEquals(0, emptyResult.getOrThrow().totalCount)

            val spacesResult = searchRepo.search("    ")
            assertTrue(spacesResult.isSuccess)
            assertTrue(spacesResult.getOrThrow().isEmpty)

            val whitespaceResult = searchRepo.search("\n\t  ")
            assertTrue(whitespaceResult.isSuccess)
            assertTrue(whitespaceResult.getOrThrow().isEmpty)

            // Verifies zero calls were made to underlying repositories
            assertEquals(0, videoRepo.callCount)
            assertEquals(0, audioRepo.tracksCallCount)
            assertEquals(0, audioRepo.artistsCallCount)
            assertEquals(0, audioRepo.albumsCallCount)
        }
    }

    // 10. Missing metadata handled safely
    @Test
    fun testMissingMetadataHandledSafely() {
        runBlocking {
            val videoWithNulls = VideoItem(
                id = 1L,
                contentUri = FakeUri("content://video/1"),
                title = "Ghost Protocol",
                durationMs = 0L,
                width = 0,
                height = 0,
                sizeBytes = 0L,
                dateAdded = 0L,
                folderName = "",
                mimeType = null,
                filePath = null
            )
            val trackWithNulls = AudioItem(
                id = 2L,
                contentUri = FakeUri("content://audio/2"),
                title = "Ghost Town",
                artist = null,
                album = null,
                folderName = "   ",
                filePath = null
            )
            val blankArtist = ArtistItem(
                name = "",
                trackCount = 0,
                albumCount = 0
            )
            val blankAlbum = AlbumItem(
                id = 10L,
                title = "",
                artist = "",
                trackCount = 0
            )

            val videoRepo = FakeVideoRepository(Result.success(listOf(videoWithNulls)))
            val audioRepo = FakeAudioRepository(
                tracksResult = Result.success(listOf(trackWithNulls)),
                artistsResult = Result.success(listOf(blankArtist)),
                albumsResult = Result.success(listOf(blankAlbum))
            )
            val searchRepo = LocalMediaSearchRepository(videoRepo, audioRepo, Dispatchers.Unconfined)

            val result = searchRepo.search("Ghost")

            assertTrue(result.isSuccess)
            val searchResults = result.getOrThrow()
            assertEquals(1, searchResults.videos.size)
            assertEquals("Ghost Protocol", searchResults.videos[0].title)
            assertEquals(1, searchResults.tracks.size)
            assertEquals("Ghost Town", searchResults.tracks[0].title)
            assertEquals(0, searchResults.artists.size)
            assertEquals(0, searchResults.albums.size)
            assertEquals(0, searchResults.folders.size)
        }
    }

    // 11. Duplicate folder handling
    @Test
    fun testDuplicateFolderHandling() {
        runBlocking {
            val folder = "Internal Storage / 4K UHD Movies"
            val videos = (1..5).map { createVideo(it.toLong(), "Movie $it", folderName = folder) }
            val tracks = (1..3).map { createTrack(it.toLong(), "Track $it", folderName = folder) }
            val videoRepo = FakeVideoRepository(Result.success(videos))
            val audioRepo = FakeAudioRepository(tracksResult = Result.success(tracks))
            val searchRepo = LocalMediaSearchRepository(videoRepo, audioRepo, Dispatchers.Unconfined)

            val result = searchRepo.search("4K UHD")

            assertTrue(result.isSuccess)
            val searchResults = result.getOrThrow()
            // Must contain exactly 1 folder entry, despite 8 items sharing the folder
            assertEquals(1, searchResults.folders.size)
            assertEquals(folder, searchResults.folders[0])
        }
    }

    // 12. Empty result sets
    @Test
    fun testEmptyResultSets() {
        runBlocking {
            val video = createVideo(1L, "Alien")
            val track = createTrack(1L, "Starman", artist = "David Bowie")
            val videoRepo = FakeVideoRepository(Result.success(listOf(video)))
            val audioRepo = FakeAudioRepository(tracksResult = Result.success(listOf(track)))
            val searchRepo = LocalMediaSearchRepository(videoRepo, audioRepo, Dispatchers.Unconfined)

            val result = searchRepo.search("Terminator")

            assertTrue(result.isSuccess)
            val searchResults = result.getOrThrow()
            assertTrue(searchResults.isEmpty)
            assertFalse(searchResults.isNotEmpty)
            assertEquals(0, searchResults.totalCount)
            assertEquals(0, searchResults.videos.size)
            assertEquals(0, searchResults.tracks.size)
            assertEquals(0, searchResults.artists.size)
            assertEquals(0, searchResults.albums.size)
            assertEquals(0, searchResults.folders.size)
        }
    }

    // 13. Repository failure handling
    @Test
    fun testRepositoryFailureHandling() {
        runBlocking {
            val diskError = IOException("Disk read error")

            // 13a. Video repository failure
            val videoFailRepo = FakeVideoRepository(Result.failure(diskError))
            val searchRepo1 = LocalMediaSearchRepository(videoFailRepo, FakeAudioRepository(), Dispatchers.Unconfined)
            val result1 = searchRepo1.search("Matrix")
            assertTrue(result1.isFailure)
            assertEquals("Disk read error", result1.exceptionOrNull()?.message)

            // 13b. Audio tracks repository failure
            val audioTracksFailRepo = FakeAudioRepository(tracksResult = Result.failure(diskError))
            val searchRepo2 = LocalMediaSearchRepository(FakeVideoRepository(), audioTracksFailRepo, Dispatchers.Unconfined)
            val result2 = searchRepo2.search("Matrix")
            assertTrue(result2.isFailure)
            assertEquals("Disk read error", result2.exceptionOrNull()?.message)

            // 13c. Artists repository failure
            val artistsFailRepo = FakeAudioRepository(artistsResult = Result.failure(diskError))
            val searchRepo3 = LocalMediaSearchRepository(FakeVideoRepository(), artistsFailRepo, Dispatchers.Unconfined)
            val result3 = searchRepo3.search("Matrix")
            assertTrue(result3.isFailure)
            assertEquals("Disk read error", result3.exceptionOrNull()?.message)

            // 13d. Albums repository failure
            val albumsFailRepo = FakeAudioRepository(albumsResult = Result.failure(diskError))
            val searchRepo4 = LocalMediaSearchRepository(FakeVideoRepository(), albumsFailRepo, Dispatchers.Unconfined)
            val result4 = searchRepo4.search("Matrix")
            assertTrue(result4.isFailure)
            assertEquals("Disk read error", result4.exceptionOrNull()?.message)
        }
    }

    // 14. Cancellation behavior where practical
    @Test(expected = CancellationException::class)
    fun testCancellationBehavior() {
        runBlocking {
            val job = Job()
            job.cancel()
            val searchRepo = LocalMediaSearchRepository(
                FakeVideoRepository(),
                FakeAudioRepository(),
                Dispatchers.Unconfined
            )

            withContext(job) {
                searchRepo.search("Interstellar")
            }
        }
    }
}
