package com.lumaplay.tv.data.repository

import com.lumaplay.tv.data.model.AlbumItem
import com.lumaplay.tv.data.model.ArtistItem
import com.lumaplay.tv.data.model.AudioItem

/**
 * Repository interface for discovering and retrieving local audio tracks, albums, and artists.
 */
interface AudioRepository {
    /**
     * Scans local storage for accessible audio tracks.
     * Executes off the main thread.
     */
    suspend fun getAudioTracks(): Result<List<AudioItem>>

    /**
     * Retrieves audio tracks grouped into albums.
     * Executes off the main thread.
     */
    suspend fun getAlbums(): Result<List<AlbumItem>>

    /**
     * Retrieves audio tracks grouped into artists.
     * Executes off the main thread.
     */
    suspend fun getArtists(): Result<List<ArtistItem>>
}

