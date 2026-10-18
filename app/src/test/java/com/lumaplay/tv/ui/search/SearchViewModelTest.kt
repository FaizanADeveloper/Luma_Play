package com.lumaplay.tv.ui.search

import android.net.FakeUri
import com.lumaplay.tv.data.model.AlbumItem
import com.lumaplay.tv.data.model.ArtistItem
import com.lumaplay.tv.data.model.AudioItem
import com.lumaplay.tv.data.model.SearchResults
import com.lumaplay.tv.data.model.VideoItem
import com.lumaplay.tv.data.repository.SearchRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class SearchViewModelTest {

    private fun createVideo(id: Long, title: String): VideoItem = VideoItem(
        id = id,
        contentUri = FakeUri("content://video/$id"),
        title = title,
        durationMs = 60_000L,
        width = 1920,
        height = 1080,
        sizeBytes = 1_000_000L,
        dateAdded = 1000L,
        folderName = "Internal Storage / Videos"
    )

    private fun createTrack(id: Long, title: String, artist: String = "Artist"): AudioItem = AudioItem(
        id = id,
        contentUri = FakeUri("content://audio/$id"),
        title = title,
        artist = artist,
        album = "Album",
        durationMs = 180_000L,
        folderName = "Internal Storage / Music"
    )

    private fun createArtist(name: String): ArtistItem = ArtistItem(
        name = name,
        trackCount = 1,
        albumCount = 1
    )

    private fun createAlbum(id: Long, title: String, artist: String): AlbumItem = AlbumItem(
        id = id,
        title = title,
        artist = artist,
        trackCount = 1
    )

    private class FakeSearchRepository(
        var result: Result<SearchResults> = Result.success(SearchResults.EMPTY)
    ) : SearchRepository {
        val queries = mutableListOf<String>()
        var delayMs: Long = 0L

        override suspend fun search(query: String): Result<SearchResults> {
            queries.add(query)
            if (delayMs > 0L) {
                delay(delayMs)
            }
            return result
        }
    }

    // 1. Initial state
    @Test
    fun testInitialStateIsIdleWithEmptyQueryAndAllCategory() {
        val repo = FakeSearchRepository()
        val viewModel = SearchViewModel(repo, Dispatchers.Unconfined, debounceMs = 0L)

        val state = viewModel.uiState.value
        assertEquals("", state.query)
        assertEquals(SearchCategory.ALL, state.selectedCategory)
        assertEquals(SearchStatus.Idle, state.status)
        assertTrue(state.isIdle)
        assertFalse(state.isLoading)
        assertFalse(state.isError)
        assertEquals(0, repo.queries.size)
    }

    // 2. Query changes
    @Test
    fun testQueryChangesImmediatelyReflectedInUiState() {
        val repo = FakeSearchRepository()
        val viewModel = SearchViewModel(repo, Dispatchers.Unconfined, debounceMs = 0L)

        viewModel.onQueryChanged("Interstellar")
        assertEquals("Interstellar", viewModel.uiState.value.query)
    }

    // 3. Successful results
    @Test
    fun testSuccessfulResultsExposedInUiState() {
        runBlocking {
            val video = createVideo(1L, "Inception")
            val track = createTrack(1L, "Time", "Hans Zimmer")
            val artist = createArtist("Hans Zimmer")
            val album = createAlbum(1L, "Inception OST", "Hans Zimmer")
            val results = SearchResults(
                videos = listOf(video),
                tracks = listOf(track),
                artists = listOf(artist),
                albums = listOf(album),
                folders = listOf("Soundtracks")
            )
            val repo = FakeSearchRepository(Result.success(results))
            val viewModel = SearchViewModel(repo, Dispatchers.Unconfined, debounceMs = 0L)

            viewModel.onQueryChanged("Inception")
            delay(50)

            val state = viewModel.uiState.value
            assertTrue(state.status is SearchStatus.Success)
            assertFalse(state.isIdle)
            assertFalse(state.isLoading)
            val success = state.status as SearchStatus.Success
            assertEquals(1, success.results.videos.size)
            assertEquals(1, success.results.tracks.size)
            assertEquals(1, success.results.artists.size)
            assertEquals(1, success.results.albums.size)
            assertEquals(1, success.results.folders.size)
            assertEquals(5, state.rawResults.totalCount)
            assertEquals(5, state.filteredResults.totalCount)
            assertFalse(state.isFilteredEmpty)
        }
    }

    // 4. Empty results
    @Test
    fun testEmptyResultsTransitionToEmptyStatus() {
        runBlocking {
            val repo = FakeSearchRepository(Result.success(SearchResults.EMPTY))
            val viewModel = SearchViewModel(repo, Dispatchers.Unconfined, debounceMs = 0L)

            viewModel.onQueryChanged("NonExistentMedia")
            delay(50)

            val state = viewModel.uiState.value
            assertEquals(SearchStatus.Empty, state.status)
            assertTrue(state.isFilteredEmpty)
            assertEquals(0, state.filteredResults.totalCount)
        }
    }

    // 5. Repository failures
    @Test
    fun testRepositoryFailureTransitionsToErrorStatus() {
        runBlocking {
            val diskError = IOException("Storage I/O failure")
            val repo = FakeSearchRepository(Result.failure(diskError))
            val viewModel = SearchViewModel(repo, Dispatchers.Unconfined, debounceMs = 0L)

            viewModel.onQueryChanged("Gladiator")
            delay(50)

            val state = viewModel.uiState.value
            assertTrue(state.status is SearchStatus.Error)
            assertTrue(state.isError)
            val error = state.status as SearchStatus.Error
            assertEquals("Storage I/O failure", error.message)
        }
    }

    // 6. Blank queries
    @Test
    fun testBlankQueriesDoNotTriggerRepositorySearch() {
        runBlocking {
            val repo = FakeSearchRepository()
            val viewModel = SearchViewModel(repo, Dispatchers.Unconfined, debounceMs = 0L)

            viewModel.onQueryChanged("   ")
            delay(50)

            assertEquals(SearchStatus.Idle, viewModel.uiState.value.status)
            assertEquals(0, repo.queries.size)
        }
    }

    // 7. Clear-query behavior
    @Test
    fun testClearQueryResetsQueryAndStatusToIdle() {
        runBlocking {
            val results = SearchResults(videos = listOf(createVideo(1L, "Alien")))
            val repo = FakeSearchRepository(Result.success(results))
            val viewModel = SearchViewModel(repo, Dispatchers.Unconfined, debounceMs = 0L)

            viewModel.onQueryChanged("Alien")
            delay(50)
            assertTrue(viewModel.uiState.value.status is SearchStatus.Success)

            viewModel.onClearQuery()
            delay(50)

            val state = viewModel.uiState.value
            assertEquals("", state.query)
            assertEquals(SearchStatus.Idle, state.status)
            assertTrue(state.isIdle)
        }
    }

    // 8. Category filtering: Videos
    @Test
    fun testCategoryFilteringVideos() {
        val results = SearchResults(
            videos = listOf(createVideo(1L, "Dune")),
            tracks = listOf(createTrack(1L, "Paul's Dream")),
            artists = listOf(createArtist("Hans Zimmer")),
            albums = listOf(createAlbum(1L, "Dune Sketchbook", "Hans Zimmer")),
            folders = listOf("SciFi")
        )
        val state = SearchUiState(
            query = "Dune",
            selectedCategory = SearchCategory.VIDEOS,
            status = SearchStatus.Success(results)
        )

        val filtered = state.filteredResults
        assertEquals(1, filtered.videos.size)
        assertEquals(0, filtered.tracks.size)
        assertEquals(0, filtered.artists.size)
        assertEquals(0, filtered.albums.size)
        assertEquals(0, filtered.folders.size)
        assertEquals(1, filtered.totalCount)
    }

    // 9. Category filtering: Music
    @Test
    fun testCategoryFilteringMusic() {
        val results = SearchResults(
            videos = listOf(createVideo(1L, "Dune")),
            tracks = listOf(createTrack(1L, "Paul's Dream")),
            artists = listOf(createArtist("Hans Zimmer")),
            albums = listOf(createAlbum(1L, "Dune Sketchbook", "Hans Zimmer")),
            folders = listOf("SciFi")
        )
        val state = SearchUiState(
            query = "Dune",
            selectedCategory = SearchCategory.MUSIC,
            status = SearchStatus.Success(results)
        )

        val filtered = state.filteredResults
        assertEquals(0, filtered.videos.size)
        assertEquals(1, filtered.tracks.size)
        assertEquals(1, filtered.artists.size)
        assertEquals(1, filtered.albums.size)
        assertEquals(0, filtered.folders.size)
        assertEquals(3, filtered.totalCount)
    }

    // 10. Category filtering: Folders
    @Test
    fun testCategoryFilteringFolders() {
        val results = SearchResults(
            videos = listOf(createVideo(1L, "Dune")),
            tracks = listOf(createTrack(1L, "Paul's Dream")),
            artists = listOf(createArtist("Hans Zimmer")),
            albums = listOf(createAlbum(1L, "Dune Sketchbook", "Hans Zimmer")),
            folders = listOf("SciFi")
        )
        val state = SearchUiState(
            query = "Dune",
            selectedCategory = SearchCategory.FOLDERS,
            status = SearchStatus.Success(results)
        )

        val filtered = state.filteredResults
        assertEquals(0, filtered.videos.size)
        assertEquals(0, filtered.tracks.size)
        assertEquals(0, filtered.artists.size)
        assertEquals(0, filtered.albums.size)
        assertEquals(1, filtered.folders.size)
        assertEquals(1, filtered.totalCount)
    }

    // 11. Switching categories without unnecessarily rerunning the search
    @Test
    fun testSwitchingCategoriesDoesNotRerunSearch() {
        runBlocking {
            val results = SearchResults(videos = listOf(createVideo(1L, "Heat")))
            val repo = FakeSearchRepository(Result.success(results))
            val viewModel = SearchViewModel(repo, Dispatchers.Unconfined, debounceMs = 0L)

            viewModel.onQueryChanged("Heat")
            delay(50)
            assertEquals(1, repo.queries.size)

            viewModel.onCategorySelected(SearchCategory.VIDEOS)
            viewModel.onCategorySelected(SearchCategory.MUSIC)
            viewModel.onCategorySelected(SearchCategory.FOLDERS)
            viewModel.onCategorySelected(SearchCategory.ALL)
            delay(50)

            // Repository query count must remain exactly 1
            assertEquals(1, repo.queries.size)
        }
    }

    // 12. isFilteredEmpty logic
    @Test
    fun testIsFilteredEmptyWhenSelectedCategoryHasZeroMatches() {
        val results = SearchResults(
            videos = listOf(createVideo(1L, "Top Gun")),
            tracks = emptyList(),
            artists = emptyList(),
            albums = emptyList(),
            folders = emptyList()
        )
        val successState = SearchUiState(
            query = "Top Gun",
            selectedCategory = SearchCategory.ALL,
            status = SearchStatus.Success(results)
        )

        assertFalse(successState.isFilteredEmpty)

        val videosState = successState.copy(selectedCategory = SearchCategory.VIDEOS)
        assertFalse(videosState.isFilteredEmpty)

        val musicState = successState.copy(selectedCategory = SearchCategory.MUSIC)
        assertTrue(musicState.isFilteredEmpty)

        val foldersState = successState.copy(selectedCategory = SearchCategory.FOLDERS)
        assertTrue(foldersState.isFilteredEmpty)
    }

    // 13. Debounce behavior
    @Test
    fun testDebounceSuppressesRapidQueries() {
        runBlocking {
            val repo = FakeSearchRepository(Result.success(SearchResults.EMPTY))
            // 80ms debounce
            val viewModel = SearchViewModel(repo, Dispatchers.IO, debounceMs = 80L)

            viewModel.onQueryChanged("A")
            delay(20)
            viewModel.onQueryChanged("AB")
            delay(20)
            viewModel.onQueryChanged("ABC")
            delay(150) // Let debounce expire for "ABC"

            assertEquals(1, repo.queries.size)
            assertEquals("ABC", repo.queries[0])
        }
    }

    // 14. Rapid queries and stale-result prevention
    @Test
    fun testRapidQueriesPreventStaleResults() {
        runBlocking {
            val slowResults = SearchResults(videos = listOf(createVideo(1L, "Slow Result")))
            val fastResults = SearchResults(videos = listOf(createVideo(2L, "Fast Result")))

            val repo = object : SearchRepository {
                override suspend fun search(query: String): Result<SearchResults> {
                    return if (query == "Slow") {
                        delay(120)
                        Result.success(slowResults)
                    } else {
                        delay(20)
                        Result.success(fastResults)
                    }
                }
            }

            val viewModel = SearchViewModel(repo, Dispatchers.IO, debounceMs = 0L)

            viewModel.onQueryChanged("Slow")
            delay(30)
            viewModel.onQueryChanged("Fast")
            delay(200)

            val state = viewModel.uiState.value
            assertEquals("Fast", state.query)
            val success = state.status as SearchStatus.Success
            assertEquals("Fast Result", success.results.videos[0].title)
        }
    }

    // 15. Whitespace trimming and distinctUntilChanged
    @Test
    fun testTrailingWhitespaceDoesNotTriggerRedundantSearch() {
        runBlocking {
            val repo = FakeSearchRepository(Result.success(SearchResults.EMPTY))
            val viewModel = SearchViewModel(repo, Dispatchers.Unconfined, debounceMs = 0L)

            viewModel.onQueryChanged("Avatar")
            delay(50)
            assertEquals(1, repo.queries.size)

            viewModel.onQueryChanged("Avatar ")
            delay(50)

            // Trimmed query is unchanged, so distinctUntilChanged prevents duplicate search
            assertEquals(1, repo.queries.size)
        }
    }
}
