package com.lumaplay.tv.data.repository

import com.lumaplay.tv.data.model.AlbumItem
import com.lumaplay.tv.data.model.AudioItem

/**
 * Repository interface for discovering and retrieving local audio tracks and albums.
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
}
