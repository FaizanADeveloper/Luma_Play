package com.lumaplay.tv.data.repository

import com.lumaplay.tv.data.model.SearchResults

/**
 * Repository interface for universal offline search across local media.
 */
interface SearchRepository {
    /**
     * Searches local media (videos, music tracks, artists, albums, and folders)
     * matching the given [query].
     *
     * Executes off the main thread.
     *
     * @param query The search query string.
     * @return [Result] containing [SearchResults] if successful, or the underlying failure.
     */
    suspend fun search(query: String): Result<SearchResults>
}
