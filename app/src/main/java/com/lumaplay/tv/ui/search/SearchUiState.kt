package com.lumaplay.tv.ui.search

import com.lumaplay.tv.data.model.SearchResults

/**
 * Filter categories for universal offline search matching the reference Stitch design.
 */
enum class SearchCategory(val label: String) {
    ALL("All"),
    VIDEOS("Videos"),
    MUSIC("Music"),
    FOLDERS("Folders")
}

/**
 * Execution status for universal offline search.
 */
sealed interface SearchStatus {
    /** Initial idle state before a search query has been submitted. */
    data object Idle : SearchStatus

    /** Search query is currently executing off the main thread. */
    data object Loading : SearchStatus

    /** Search completed with at least one matching result across categories. */
    data class Success(val results: SearchResults) : SearchStatus

    /** Search completed with zero matching results across all categories. */
    data object Empty : SearchStatus

    /** Search operation failed with an error message. */
    data class Error(val message: String) : SearchStatus
}

/**
 * Complete UI state for Universal Offline Search.
 *
 * @property query The current raw query string typed by the user.
 * @property selectedCategory The active filter category (All, Videos, Music, Folders).
 * @property status The execution status of the search.
 */
data class SearchUiState(
    val query: String = "",
    val selectedCategory: SearchCategory = SearchCategory.ALL,
    val status: SearchStatus = SearchStatus.Idle
) {
    /**
     * True if no search has been initiated yet (initial state).
     */
    val isIdle: Boolean get() = status is SearchStatus.Idle

    /**
     * True if a search is actively in progress.
     */
    val isLoading: Boolean get() = status is SearchStatus.Loading

    /**
     * True if the search operation failed with an error.
     */
    val isError: Boolean get() = status is SearchStatus.Error

    /**
     * Raw, unfiltered results from the latest search if successful.
     */
    val rawResults: SearchResults
        get() = (status as? SearchStatus.Success)?.results ?: SearchResults.EMPTY

    /**
     * Results filtered by the [selectedCategory]:
     * - [SearchCategory.ALL]: all media types (videos, tracks, artists, albums, folders)
     * - [SearchCategory.VIDEOS]: videos only
     * - [SearchCategory.MUSIC]: tracks, artists, and albums only
     * - [SearchCategory.FOLDERS]: folders only
     */
    val filteredResults: SearchResults
        get() = when (selectedCategory) {
            SearchCategory.ALL -> rawResults
            SearchCategory.VIDEOS -> rawResults.copy(
                tracks = emptyList(),
                artists = emptyList(),
                albums = emptyList(),
                folders = emptyList()
            )
            SearchCategory.MUSIC -> rawResults.copy(
                videos = emptyList(),
                folders = emptyList()
            )
            SearchCategory.FOLDERS -> rawResults.copy(
                videos = emptyList(),
                tracks = emptyList(),
                artists = emptyList(),
                albums = emptyList()
            )
        }

    /**
     * True if the query completed successfully but has zero items in the active [selectedCategory].
     */
    val isFilteredEmpty: Boolean
        get() = when (status) {
            is SearchStatus.Empty -> true
            is SearchStatus.Success -> filteredResults.isEmpty
            else -> false
        }
}
