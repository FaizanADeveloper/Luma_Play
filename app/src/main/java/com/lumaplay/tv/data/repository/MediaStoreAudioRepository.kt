package com.lumaplay.tv.data.repository

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import com.lumaplay.tv.data.model.AlbumItem
import com.lumaplay.tv.data.model.ArtistItem
import com.lumaplay.tv.data.model.AudioItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

/**
 * Resilient implementation of [AudioRepository] for Android TV.
 *
 * Discovers accessible audio files through MediaStore and direct filesystem crawling
 * of standard public directories (/Music, /Download, /Podcasts).
 *
 * Reconciles and deduplicates both discovery sources using canonical physical paths.
 * If MediaStore reports incomplete metadata or if files are discovered directly from disk,
 * genuine metadata is extracted via a single fast [MediaMetadataRetriever] header pass.
 * Never fabricates metadata.
 *
 * All operations execute off the main thread on [Dispatchers.IO].
 */
class MediaStoreAudioRepository(
    private val context: Context,
    private val contentResolver: ContentResolver = context.contentResolver
) : AudioRepository {

    companion object {
        private const val TAG = "MediaStoreAudioRepo"
        private val SUPPORTED_AUDIO_EXTENSIONS = setOf(
            "mp3", "flac", "aac", "m4a", "wav", "ogg", "opus", "wma"
        )

        internal fun groupTracksToAlbums(tracks: List<AudioItem>): List<AlbumItem> {
            // Group tracks by album identity:
            // Prefer MediaStore albumId when valid; fallback to normalized album + artist key
            val grouped = tracks.groupBy { track ->
                if (track.albumId != null && track.albumId > 0L) {
                    "id_${track.albumId}"
                } else {
                    "name_${track.displayAlbum.lowercase(Locale.US)}_${track.displayArtist.lowercase(Locale.US)}"
                }
            }

            return grouped.map { (groupKey, albumTracks) ->
                // Sort tracks by track sequence number ascending (nulls last), then title
                val sortedTracks = albumTracks.sortedWith(
                    compareBy<AudioItem> { it.trackNumber ?: Int.MAX_VALUE }
                        .thenBy { it.cleanTitle }
                )

                val firstTrack = sortedTracks.first()
                val albumId = firstTrack.albumId ?: (groupKey.hashCode().toLong() and 0x7FFFFFFF)
                val title = albumTracks.firstOrNull { !it.album.isNullOrBlank() }?.album ?: "Unknown Album"
                val artist = albumTracks.firstOrNull { !it.artist.isNullOrBlank() }?.artist ?: "Unknown Artist"
                val year = albumTracks.mapNotNull { it.year }.filter { it > 0 }.minOrNull()
                val artworkUri = albumTracks.mapNotNull { it.artworkUri }.firstOrNull()

                AlbumItem(
                    id = albumId,
                    title = title,
                    artist = artist,
                    trackCount = sortedTracks.size,
                    year = year,
                    artworkUri = artworkUri,
                    tracks = sortedTracks
                )
            }.sortedBy { it.displayTitle.lowercase(Locale.US) }
        }

        internal fun groupTracksToArtists(tracks: List<AudioItem>): List<ArtistItem> {
            // Group tracks by normalized artist identity (lowercase, trimmed).
            // Blank or unknown artists normalize to "unknown artist" via displayArtist.
            val grouped = tracks.groupBy { track ->
                track.displayArtist.lowercase(Locale.US)
            }

            return grouped.map { (_, artistTracks) ->
                // Sort artist tracks: album name ascending, track sequence number ascending, then clean title
                val sortedTracks = artistTracks.sortedWith(
                    compareBy<AudioItem> { it.displayAlbum.lowercase(Locale.US) }
                        .thenBy { it.trackNumber ?: Int.MAX_VALUE }
                        .thenBy { it.cleanTitle.lowercase(Locale.US) }
                )

                // Determine artist display name truthfully without fabricating fictional identities
                val canonicalName = artistTracks.firstOrNull {
                    !it.artist.isNullOrBlank() && !it.artist.equals("<unknown>", ignoreCase = true)
                }?.artist?.trim() ?: "Unknown Artist"

                // Calculate album count using stable MediaStore albumId where available;
                // fallback to normalized displayAlbum when albumId is missing
                val distinctAlbums = artistTracks.map { track ->
                    if (track.albumId != null && track.albumId > 0L) {
                        "id_${track.albumId}"
                    } else {
                        "title_${track.displayAlbum.lowercase(Locale.US)}"
                    }
                }.distinct()

                val artworkUri = artistTracks.mapNotNull { it.artworkUri }.firstOrNull()

                ArtistItem(
                    name = canonicalName,
                    trackCount = sortedTracks.size,
                    albumCount = distinctAlbums.size,
                    artworkUri = artworkUri,
                    tracks = sortedTracks
                )
            }.sortedBy { it.displayName.lowercase(Locale.US) }
        }
    }

    @Volatile
    private var cachedTracks: List<AudioItem>? = null

    override suspend fun getAudioTracks(): Result<List<AudioItem>> = withContext(Dispatchers.IO) {
        runCatching {
            // Deduplicated map keyed by canonical physical path (or synthetic key if path unavailable)
            val reconciledAudio = LinkedHashMap<String, AudioItem>()

            // -------------------------------------------------------------
            // SOURCE 1: MediaStore Query (Relaxed, Incomplete-Row Aware)
            // -------------------------------------------------------------
            queryMediaStore(reconciledAudio)

            // -------------------------------------------------------------
            // SOURCE 2: Direct Filesystem Discovery (Authoritative Fallback)
            // -------------------------------------------------------------
            scanPublicFilesystem(reconciledAudio)

            // Return sorted by dateAdded DESC (matching established LumaPlay sorting contract)
            val sorted = reconciledAudio.values.sortedByDescending { it.dateAdded }
            cachedTracks = sorted
            sorted
        }
    }

    override suspend fun getAlbums(): Result<List<AlbumItem>> = withContext(Dispatchers.IO) {
        runCatching {
            val tracks = cachedTracks ?: getAudioTracks().getOrThrow()
            groupTracksToAlbums(tracks)
        }
    }

    override suspend fun getArtists(): Result<List<ArtistItem>> = withContext(Dispatchers.IO) {
        runCatching {
            val tracks = cachedTracks ?: getAudioTracks().getOrThrow()
            groupTracksToArtists(tracks)
        }
    }



    private fun queryMediaStore(outMap: LinkedHashMap<String, AudioItem>) {
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.MIME_TYPE,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.TRACK,
            MediaStore.Audio.Media.YEAR,
            MediaStore.Audio.Media.BUCKET_DISPLAY_NAME
        )

        // Relaxed selection: match valid sizes or pending rows, and do not strictly require IS_MUSIC != 0
        // because non-standard directories or USB drives on Android TV may have null or 0 IS_MUSIC flags.
        val selection = "${MediaStore.Audio.Media.SIZE} > 0 OR ${MediaStore.Audio.Media.SIZE} IS NULL"
        val sortOrder = "${MediaStore.Audio.Media.DATE_ADDED} DESC"

        try {
            contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                null,
                sortOrder
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val nameColumn = cursor.getColumnIndex(MediaStore.Audio.Media.DISPLAY_NAME)
                val titleColumn = cursor.getColumnIndex(MediaStore.Audio.Media.TITLE)
                val artistColumn = cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST)
                val albumColumn = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM)
                val albumIdColumn = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID)
                val durationColumn = cursor.getColumnIndex(MediaStore.Audio.Media.DURATION)
                val sizeColumn = cursor.getColumnIndex(MediaStore.Audio.Media.SIZE)
                val dateAddedColumn = cursor.getColumnIndex(MediaStore.Audio.Media.DATE_ADDED)
                val mimeColumn = cursor.getColumnIndex(MediaStore.Audio.Media.MIME_TYPE)
                val dataColumn = cursor.getColumnIndex(MediaStore.Audio.Media.DATA)
                val trackColumn = cursor.getColumnIndex(MediaStore.Audio.Media.TRACK)
                val yearColumn = cursor.getColumnIndex(MediaStore.Audio.Media.YEAR)
                val bucketColumn = cursor.getColumnIndex(MediaStore.Audio.Media.BUCKET_DISPLAY_NAME)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val rawName = if (nameColumn != -1) cursor.getString(nameColumn) else null
                    val rawTitle = if (titleColumn != -1) cursor.getString(titleColumn) else null
                    var artist = if (artistColumn != -1) cursor.getString(artistColumn) else null
                    var album = if (albumColumn != -1) cursor.getString(albumColumn) else null
                    val albumId = if (albumIdColumn != -1 && !cursor.isNull(albumIdColumn)) cursor.getLong(albumIdColumn) else null
                    var duration = if (durationColumn != -1) cursor.getLong(durationColumn) else 0L
                    var size = if (sizeColumn != -1) cursor.getLong(sizeColumn) else 0L
                    var dateAdded = if (dateAddedColumn != -1) cursor.getLong(dateAddedColumn) else 0L
                    var mimeType = if (mimeColumn != -1) cursor.getString(mimeColumn) else null
                    val data = if (dataColumn != -1) cursor.getString(dataColumn) else null
                    var rawTrack = if (trackColumn != -1 && !cursor.isNull(trackColumn)) cursor.getInt(trackColumn) else null
                    var year = if (yearColumn != -1 && !cursor.isNull(yearColumn)) cursor.getInt(yearColumn) else null
                    val bucketName = if (bucketColumn != -1) cursor.getString(bucketColumn) else null

                    val physicalFile = if (!data.isNullOrBlank()) File(data) else null
                    val canonicalPath = physicalFile?.canonicalPath ?: data

                    // Verify accessibility and physical file size
                    if (physicalFile != null && physicalFile.exists()) {
                        if (size <= 0L) {
                            size = physicalFile.length()
                        }
                        if (dateAdded <= 0L) {
                            dateAdded = physicalFile.lastModified() / 1000L
                        }
                    }

                    // Ignore 0-byte ghost entries
                    if (size <= 0L && (physicalFile == null || !physicalFile.exists() || physicalFile.length() <= 0L)) {
                        continue
                    }

                    val title = rawTitle?.takeIf { it.isNotBlank() }
                        ?: rawName
                        ?: physicalFile?.name
                        ?: "Untitled Audio"

                    val folderName = when {
                        !bucketName.isNullOrBlank() -> "Internal Storage / $bucketName"
                        physicalFile?.parentFile?.name != null -> "Internal Storage / ${physicalFile.parentFile?.name}"
                        else -> "Internal Storage / Music"
                    }

                    val mediaStoreUri = ContentUris.withAppendedId(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        id
                    )

                    val usableUri = if (isContentUriReadable(mediaStoreUri)) {
                        mediaStoreUri
                    } else if (physicalFile != null && physicalFile.canRead()) {
                        Uri.fromFile(physicalFile)
                    } else {
                        mediaStoreUri
                    }

                    // Normalize track number (e.g. MediaStore may store 1001 for disc 1 track 1)
                    var trackNumber = rawTrack?.let { if (it >= 1000) it % 1000 else it }

                    // If MediaStore metadata is incomplete (duration missing, or artist/album null), extract from file
                    if ((duration <= 0L || artist.isNullOrBlank() || album.isNullOrBlank()) && physicalFile != null && physicalFile.exists()) {
                        val meta = extractMetadata(usableUri, physicalFile)
                        if (duration <= 0L && meta.durationMs > 0L) duration = meta.durationMs
                        if (artist.isNullOrBlank() && !meta.artist.isNullOrBlank()) artist = meta.artist
                        if (album.isNullOrBlank() && !meta.album.isNullOrBlank()) album = meta.album
                        if (trackNumber == null && meta.trackNumber != null) trackNumber = meta.trackNumber
                        if (year == null && meta.year != null) year = meta.year
                        if (mimeType.isNullOrBlank() && !meta.mimeType.isNullOrBlank()) mimeType = meta.mimeType
                    }

                    val key = canonicalPath ?: "mediastore_audio_$id"
                    outMap[key] = AudioItem(
                        id = id,
                        contentUri = usableUri,
                        title = title,
                        artist = artist,
                        album = album,
                        albumId = albumId,
                        durationMs = duration,
                        sizeBytes = size,
                        dateAdded = dateAdded,
                        trackNumber = trackNumber,
                        year = year,
                        mimeType = mimeType,
                        folderName = folderName,
                        filePath = canonicalPath
                    )
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "MediaStore audio query encountered an issue: ${e.message}")
        }
    }

    private fun scanPublicFilesystem(outMap: LinkedHashMap<String, AudioItem>) {
        val targetDirectories = linkedSetOf<File>()

        // 1. Standard Framework public directories
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)?.let { targetDirectories.add(it) }
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)?.let { targetDirectories.add(it) }
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PODCASTS)?.let { targetDirectories.add(it) }

        // 2. Canonical emulated storage paths
        targetDirectories.add(File("/storage/emulated/0/Music"))
        targetDirectories.add(File("/storage/emulated/0/Download"))
        targetDirectories.add(File("/storage/emulated/0/Podcasts"))

        // 3. Common symlink paths
        targetDirectories.add(File("/sdcard/Music"))
        targetDirectories.add(File("/sdcard/Download"))
        targetDirectories.add(File("/sdcard/Podcasts"))

        val discoveredFiles = mutableListOf<File>()
        val visitedDirs = mutableSetOf<String>()

        for (dir in targetDirectories) {
            crawlDirectory(dir, depth = 0, maxDepth = 4, visitedDirs = visitedDirs, results = discoveredFiles)
        }

        for (file in discoveredFiles) {
            val canonicalPath = runCatching { file.canonicalPath }.getOrDefault(file.absolutePath)

            val existing = outMap[canonicalPath]
            if (existing != null) {
                // Already discovered via MediaStore: reconcile any missing fields
                var updatedItem = existing
                if (updatedItem.sizeBytes <= 0L && file.length() > 0L) {
                    updatedItem = updatedItem.copy(sizeBytes = file.length())
                }
                if (updatedItem.durationMs <= 0L || updatedItem.artist.isNullOrBlank() || updatedItem.album.isNullOrBlank()) {
                    val meta = extractMetadata(updatedItem.contentUri, file)
                    updatedItem = updatedItem.copy(
                        durationMs = if (updatedItem.durationMs > 0L) updatedItem.durationMs else meta.durationMs,
                        artist = if (!updatedItem.artist.isNullOrBlank()) updatedItem.artist else meta.artist,
                        album = if (!updatedItem.album.isNullOrBlank()) updatedItem.album else meta.album,
                        trackNumber = updatedItem.trackNumber ?: meta.trackNumber,
                        year = updatedItem.year ?: meta.year,
                        mimeType = if (!updatedItem.mimeType.isNullOrBlank()) updatedItem.mimeType else meta.mimeType
                    )
                }
                outMap[canonicalPath] = updatedItem
            } else {
                // Discovered exclusively through filesystem
                val fileLength = file.length()
                if (fileLength <= 0L) continue

                val syntheticId = (canonicalPath.hashCode().toLong() and 0x7FFFFFFF)
                val fileUri = Uri.fromFile(file)
                val folderName = "Internal Storage / ${file.parentFile?.name ?: "Music"}"
                val fallbackMime = inferMimeType(file.extension)

                // Extract genuine metadata from file headers
                val meta = extractMetadata(fileUri, file)

                outMap[canonicalPath] = AudioItem(
                    id = syntheticId,
                    contentUri = fileUri,
                    title = meta.title ?: file.name,
                    artist = meta.artist,
                    album = meta.album,
                    albumId = null,
                    durationMs = meta.durationMs,
                    sizeBytes = fileLength,
                    dateAdded = file.lastModified() / 1000L,
                    trackNumber = meta.trackNumber,
                    year = meta.year,
                    mimeType = meta.mimeType ?: fallbackMime,
                    folderName = folderName,
                    filePath = canonicalPath
                )
            }
        }
    }

    private fun crawlDirectory(
        dir: File,
        depth: Int,
        maxDepth: Int,
        visitedDirs: MutableSet<String>,
        results: MutableList<File>
    ) {
        if (depth > maxDepth || !dir.exists() || !dir.isDirectory || !dir.canRead()) return

        val canonicalDir = runCatching { dir.canonicalPath }.getOrDefault(dir.absolutePath)
        if (!visitedDirs.add(canonicalDir)) return

        // Skip hidden directories and .nomedia directories
        if (dir.name.startsWith(".")) return
        if (File(dir, ".nomedia").exists()) return

        val files = dir.listFiles() ?: return
        for (f in files) {
            if (f.name.startsWith(".")) continue
            if (f.isDirectory) {
                crawlDirectory(f, depth + 1, maxDepth, visitedDirs, results)
            } else if (f.isFile && f.length() > 0L) {
                val ext = f.extension.lowercase(Locale.US)
                if (ext in SUPPORTED_AUDIO_EXTENSIONS) {
                    results.add(f)
                }
            }
        }
    }

    private data class ExtractedAudioMeta(
        val title: String? = null,
        val artist: String? = null,
        val album: String? = null,
        val durationMs: Long = 0L,
        val trackNumber: Int? = null,
        val year: Int? = null,
        val mimeType: String? = null
    )

    /**
     * Single MediaMetadataRetriever pass to extract authentic audio metadata from headers.
     */
    private fun extractMetadata(uri: Uri, file: File?): ExtractedAudioMeta {
        val retriever = MediaMetadataRetriever()
        var meta = ExtractedAudioMeta()
        try {
            var dataSourceSet = false

            // Try ContentResolver openFileDescriptor if content://
            if (uri.scheme == ContentResolver.SCHEME_CONTENT) {
                try {
                    contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                        retriever.setDataSource(pfd.fileDescriptor)
                        dataSourceSet = true
                    }
                } catch (_: Exception) {}
            }

            // Fallback to direct file path
            if (!dataSourceSet && file != null && file.exists() && file.canRead()) {
                try {
                    retriever.setDataSource(file.absolutePath)
                    dataSourceSet = true
                } catch (_: Exception) {}
            }

            if (!dataSourceSet) return meta

            val rawTitle = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
            val rawArtist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
            val rawAlbum = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            val durationMs = durationStr?.toLongOrNull() ?: 0L
            val trackStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER)
            val trackNumber = trackStr?.split("/")?.firstOrNull()?.trim()?.toIntOrNull()
            val yearStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_YEAR)
                ?: retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DATE)
            val year = yearStr?.trim()?.take(4)?.toIntOrNull()
            val mime = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE)

            meta = ExtractedAudioMeta(
                title = rawTitle?.takeIf { it.isNotBlank() },
                artist = rawArtist?.takeIf { it.isNotBlank() },
                album = rawAlbum?.takeIf { it.isNotBlank() },
                durationMs = durationMs,
                trackNumber = trackNumber,
                year = year,
                mimeType = mime
            )
        } catch (e: Exception) {
            Log.w(TAG, "MediaMetadataRetriever failed for $uri: ${e.message}")
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {}
        }
        return meta
    }

    private fun isContentUriReadable(uri: Uri): Boolean {
        return try {
            contentResolver.openFileDescriptor(uri, "r")?.use { true } ?: false
        } catch (_: Exception) {
            false
        }
    }

    private fun inferMimeType(extension: String): String? {
        return when (extension.lowercase(Locale.US)) {
            "mp3" -> "audio/mpeg"
            "flac" -> "audio/flac"
            "aac" -> "audio/aac"
            "m4a" -> "audio/mp4"
            "wav" -> "audio/x-wav"
            "ogg" -> "audio/ogg"
            "opus" -> "audio/opus"
            "wma" -> "audio/x-ms-wma"
            else -> null
        }
    }
}
