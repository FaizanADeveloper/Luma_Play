package com.lumaplay.tv.data.repository

import com.lumaplay.tv.data.model.AlbumItem
import com.lumaplay.tv.data.model.ArtistItem
import com.lumaplay.tv.data.model.AudioItem
import com.lumaplay.tv.data.model.SearchResults
import com.lumaplay.tv.data.model.VideoItem
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.coroutineContext

/**
 * Implementation of [SearchRepository] that searches local video and audio libraries
 * off the main thread.
 *
 * Discovers matching videos, music tracks, artists, albums, and distinct folders
 * using case-insensitive, trimmed partial text matching.
 */
class LocalMediaSearchRepository(
    private val videoRepository: VideoRepository,
    private val audioRepository: AudioRepository,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : SearchRepository {

    override suspend fun search(query: String): Result<SearchResults> = withContext(dispatcher) {
        val trimmedQuery = query.trim()
        if (trimmedQuery.isEmpty()) {
            return@withContext Result.success(SearchResults.EMPTY)
        }

        try {
            coroutineContext.ensureActive()

            val videosResult = videoRepository.getVideos()
            if (videosResult.isFailure) {
                return@withContext Result.failure(
                    videosResult.exceptionOrNull()
                        ?: IllegalStateException("Failed to retrieve videos")
                )
            }
            val videos = videosResult.getOrNull().orEmpty()

            coroutineContext.ensureActive()

            val tracksResult = audioRepository.getAudioTracks()
            if (tracksResult.isFailure) {
                return@withContext Result.failure(
                    tracksResult.exceptionOrNull()
                        ?: IllegalStateException("Failed to retrieve audio tracks")
                )
            }
            val tracks = tracksResult.getOrNull().orEmpty()

            coroutineContext.ensureActive()

            val artistsResult = audioRepository.getArtists()
            if (artistsResult.isFailure) {
                return@withContext Result.failure(
                    artistsResult.exceptionOrNull()
                        ?: IllegalStateException("Failed to retrieve artists")
                )
            }
            val artists = artistsResult.getOrNull().orEmpty()

            coroutineContext.ensureActive()

            val albumsResult = audioRepository.getAlbums()
            if (albumsResult.isFailure) {
                return@withContext Result.failure(
                    albumsResult.exceptionOrNull()
                        ?: IllegalStateException("Failed to retrieve albums")
                )
            }
            val albums = albumsResult.getOrNull().orEmpty()

            coroutineContext.ensureActive()

            // 1. Video matching: Title, Clean title, Folder name, Path metadata
            val matchingVideos = videos.filter { video ->
                video.title.containsQuery(trimmedQuery) ||
                    video.cleanTitle.containsQuery(trimmedQuery) ||
                    video.folderName.containsQuery(trimmedQuery) ||
                    video.filePath.containsQuery(trimmedQuery)
            }

            // 2. Music-track matching: Title, Clean title, Artist, Album
            val matchingTracks = tracks.filter { track ->
                track.title.containsQuery(trimmedQuery) ||
                    track.cleanTitle.containsQuery(trimmedQuery) ||
                    track.artist.containsQuery(trimmedQuery) ||
                    track.displayArtist.containsQuery(trimmedQuery) ||
                    track.album.containsQuery(trimmedQuery) ||
                    track.displayAlbum.containsQuery(trimmedQuery)
            }

            // 3. Artist matching: Artist name, Display name
            val matchingArtists = artists.filter { artist ->
                artist.name.containsQuery(trimmedQuery) ||
                    artist.displayName.containsQuery(trimmedQuery)
            }

            // 4. Album matching: Album title, Display title, Artist
            val matchingAlbums = albums.filter { album ->
                album.title.containsQuery(trimmedQuery) ||
                    album.displayTitle.containsQuery(trimmedQuery) ||
                    album.artist.containsQuery(trimmedQuery) ||
                    album.displayArtist.containsQuery(trimmedQuery)
            }

            // 5. Folder matching: Distinct folders derived from available local media metadata
            val folderCandidates = LinkedHashSet<String>()
            for (video in videos) {
                extractFolderName(video.folderName, video.filePath)?.let { folderCandidates.add(it) }
            }
            for (track in tracks) {
                extractFolderName(track.folderName, track.filePath)?.let { folderCandidates.add(it) }
            }

            val matchingFolders = folderCandidates.filter { folder ->
                folder.containsQuery(trimmedQuery)
            }

            coroutineContext.ensureActive()

            Result.success(
                SearchResults(
                    videos = matchingVideos,
                    tracks = matchingTracks,
                    artists = matchingArtists,
                    albums = matchingAlbums,
                    folders = matchingFolders
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            Result.failure(e)
        }
    }

    private fun extractFolderName(folderName: String?, filePath: String?): String? {
        if (!folderName.isNullOrBlank()) {
            return folderName.trim()
        }
        if (!filePath.isNullOrBlank()) {
            val parentName = File(filePath).parentFile?.name
            if (!parentName.isNullOrBlank()) {
                return parentName.trim()
            }
        }
        return null
    }

    private fun String?.containsQuery(query: String): Boolean {
        if (this.isNullOrBlank()) return false
        return this.contains(query, ignoreCase = true)
    }
}
