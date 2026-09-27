package com.lumaplay.tv.data.repository

import com.lumaplay.tv.data.model.VideoItem

/**
 * Repository interface for discovering and retrieving local videos.
 */
interface VideoRepository {
    /**
     * Scans local storage for accessible videos.
     * Executes off the main thread.
     */
    suspend fun getVideos(): Result<List<VideoItem>>
}
