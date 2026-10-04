package com.lumaplay.tv.data.repository

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import com.lumaplay.tv.data.model.VideoItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

/**
 * Resilient implementation of [VideoRepository] for Android TV.
 *
 * Combines MediaStore query results with direct filesystem discovery of standard
 * public video directories (/Movies, /Download, /DCIM).
 *
 * Reconciles and deduplicates both discovery sources using physical canonical paths.
 * MediaStore is treated as one source rather than the sole gatekeeper.
 *
 * If MediaStore reports incomplete metadata (e.g. is_pending=1, size=NULL, width=NULL,
 * duration=NULL), genuine values are resolved from the physical file length and a single,
 * fast [MediaExtractor] header pass.
 *
 * All operations execute off the main thread on [Dispatchers.IO].
 */
class MediaStoreVideoRepository(
    private val context: Context,
    private val contentResolver: ContentResolver = context.contentResolver
) : VideoRepository {

    companion object {
        private const val TAG = "MediaStoreVideoRepo"
        private val SUPPORTED_VIDEO_EXTENSIONS = setOf(
            "mp4", "mkv", "webm", "avi", "mov", "ts", "wmv", "m4v", "flv"
        )
    }

    override suspend fun getVideos(): Result<List<VideoItem>> = withContext(Dispatchers.IO) {
        runCatching {
            // Deduplicated map keyed by canonical physical path (or synthetic key if path unavailable)
            val reconciledVideos = LinkedHashMap<String, VideoItem>()

            // -------------------------------------------------------------
            // SOURCE 1: MediaStore Query (Relaxed, Incomplete-Row Aware)
            // -------------------------------------------------------------
            queryMediaStore(reconciledVideos)

            // -------------------------------------------------------------
            // SOURCE 2: Direct Filesystem Discovery (Authoritative Fallback)
            // -------------------------------------------------------------
            scanPublicFilesystem(reconciledVideos)

            // Return sorted by dateAdded DESC (matching established LumaPlay sorting contract)
            reconciledVideos.values.sortedByDescending { it.dateAdded }
        }
    }

    private fun queryMediaStore(outMap: LinkedHashMap<String, VideoItem>) {
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.WIDTH,
            MediaStore.Video.Media.HEIGHT,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Video.Media.MIME_TYPE,
            MediaStore.Video.Media.DATA
        )

        // Relax filter: do not exclude rows where size is NULL or 0
        val selection = "${MediaStore.Video.Media.SIZE} > 0 OR ${MediaStore.Video.Media.SIZE} IS NULL"
        val sortOrder = "${MediaStore.Video.Media.DATE_ADDED} DESC"

        try {
            contentResolver.query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                null,
                sortOrder
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
                val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
                val widthColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.WIDTH)
                val heightColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.HEIGHT)
                val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
                val dateAddedColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
                val bucketColumn = cursor.getColumnIndex(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)
                val mimeColumn = cursor.getColumnIndex(MediaStore.Video.Media.MIME_TYPE)
                val dataColumn = cursor.getColumnIndex(MediaStore.Video.Media.DATA)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val rawName = cursor.getString(nameColumn)
                    var duration = cursor.getLong(durationColumn)
                    var width = cursor.getInt(widthColumn)
                    var height = cursor.getInt(heightColumn)
                    var size = cursor.getLong(sizeColumn)
                    var dateAdded = cursor.getLong(dateAddedColumn)
                    val bucketName = if (bucketColumn != -1) cursor.getString(bucketColumn) else null
                    var mimeType = if (mimeColumn != -1) cursor.getString(mimeColumn) else null
                    val data = if (dataColumn != -1) cursor.getString(dataColumn) else null

                    val physicalFile = if (!data.isNullOrBlank()) File(data) else null
                    val canonicalPath = physicalFile?.canonicalPath ?: data

                    // If physical file exists, verify accessibility and true file size
                    if (physicalFile != null && physicalFile.exists()) {
                        if (size <= 0L) {
                            size = physicalFile.length()
                        }
                        if (dateAdded <= 0L) {
                            dateAdded = physicalFile.lastModified() / 1000L
                        }
                    }

                    // Ignore completely 0-byte ghost entries
                    if (size <= 0L && (physicalFile == null || !physicalFile.exists() || physicalFile.length() <= 0L)) {
                        continue
                    }

                    val displayName = rawName ?: physicalFile?.name ?: "Untitled Video"

                    val folderName = when {
                        !bucketName.isNullOrBlank() -> "Internal Storage / $bucketName"
                        physicalFile?.parentFile?.name != null -> "Internal Storage / ${physicalFile.parentFile?.name}"
                        else -> "Internal Storage / Videos"
                    }

                    val mediaStoreUri = ContentUris.withAppendedId(
                        MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                        id
                    )

                    // Preserving content:// URI when usable; fallback to Uri.fromFile only if unusable
                    val usableUri = if (isContentUriReadable(mediaStoreUri)) {
                        mediaStoreUri
                    } else if (physicalFile != null && physicalFile.canRead()) {
                        Uri.fromFile(physicalFile)
                    } else {
                        mediaStoreUri
                    }

                    // Check if MediaStore metadata is incomplete (width, height, or duration missing)
                    if ((width <= 0 || height <= 0 || duration <= 0L) && physicalFile != null && physicalFile.exists()) {
                        val basicInfo = extractBasicMetadata(usableUri, physicalFile)
                        if (width <= 0 && basicInfo.width > 0) width = basicInfo.width
                        if (height <= 0 && basicInfo.height > 0) height = basicInfo.height
                        if (duration <= 0L && basicInfo.durationMs > 0L) duration = basicInfo.durationMs
                        if (mimeType.isNullOrBlank() && basicInfo.mimeType != null) mimeType = basicInfo.mimeType
                    }

                    val key = canonicalPath ?: "mediastore_$id"
                    outMap[key] = VideoItem(
                        id = id,
                        contentUri = usableUri,
                        title = displayName,
                        durationMs = duration,
                        width = width,
                        height = height,
                        sizeBytes = size,
                        dateAdded = dateAdded,
                        folderName = folderName,
                        mimeType = mimeType,
                        filePath = canonicalPath
                    )
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "MediaStore query encountered an issue: ${e.message}")
        }
    }

    private fun scanPublicFilesystem(outMap: LinkedHashMap<String, VideoItem>) {
        val targetDirectories = linkedSetOf<File>()

        // 1. Standard Framework public directories
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)?.let { targetDirectories.add(it) }
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)?.let { targetDirectories.add(it) }
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)?.let { targetDirectories.add(it) }

        // 2. Canonical emulated storage paths
        targetDirectories.add(File("/storage/emulated/0/Movies"))
        targetDirectories.add(File("/storage/emulated/0/Download"))
        targetDirectories.add(File("/storage/emulated/0/DCIM"))

        // 3. Common symlink paths
        targetDirectories.add(File("/sdcard/Movies"))
        targetDirectories.add(File("/sdcard/Download"))
        targetDirectories.add(File("/sdcard/DCIM"))

        val discoveredFiles = mutableListOf<File>()
        val visitedDirs = mutableSetOf<String>()

        for (dir in targetDirectories) {
            crawlDirectory(dir, depth = 0, maxDepth = 4, visitedDirs = visitedDirs, results = discoveredFiles)
        }

        for (file in discoveredFiles) {
            val canonicalPath = runCatching { file.canonicalPath }.getOrDefault(file.absolutePath)

            val existing = outMap[canonicalPath]
            if (existing != null) {
                // Already discovered via MediaStore! Reconcile any missing metadata
                var updatedItem = existing

                if (updatedItem.sizeBytes <= 0L && file.length() > 0L) {
                    updatedItem = updatedItem.copy(sizeBytes = file.length())
                }
                if (updatedItem.width <= 0 || updatedItem.height <= 0 || updatedItem.durationMs <= 0L) {
                    val basicInfo = extractBasicMetadata(updatedItem.contentUri, file)
                    updatedItem = updatedItem.copy(
                        width = if (updatedItem.width > 0) updatedItem.width else basicInfo.width,
                        height = if (updatedItem.height > 0) updatedItem.height else basicInfo.height,
                        durationMs = if (updatedItem.durationMs > 0L) updatedItem.durationMs else basicInfo.durationMs,
                        mimeType = if (!updatedItem.mimeType.isNullOrBlank()) updatedItem.mimeType else basicInfo.mimeType
                    )
                }
                outMap[canonicalPath] = updatedItem
            } else {
                // Discovered exclusively through filesystem
                val fileLength = file.length()
                if (fileLength <= 0L) continue

                val syntheticId = (canonicalPath.hashCode().toLong() and 0x7FFFFFFF)
                val fileUri = Uri.fromFile(file)
                val folderName = "Internal Storage / ${file.parentFile?.name ?: "Movies"}"
                val fallbackMime = inferMimeType(file.extension)

                // Single lightweight MediaExtractor pass
                val basicInfo = extractBasicMetadata(fileUri, file)

                outMap[canonicalPath] = VideoItem(
                    id = syntheticId,
                    contentUri = fileUri,
                    title = file.name,
                    durationMs = basicInfo.durationMs,
                    width = basicInfo.width,
                    height = basicInfo.height,
                    sizeBytes = fileLength,
                    dateAdded = file.lastModified() / 1000L,
                    folderName = folderName,
                    mimeType = basicInfo.mimeType ?: fallbackMime,
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
                if (ext in SUPPORTED_VIDEO_EXTENSIONS) {
                    results.add(f)
                }
            }
        }
    }

    private data class BasicMetadata(
        val width: Int = 0,
        val height: Int = 0,
        val durationMs: Long = 0L,
        val mimeType: String? = null
    )

    /**
     * Single MediaExtractor pass to extract only basic container dimensions and duration.
     * Does NOT perform full Video Details audio/subtitle/HDR telemetry extraction.
     */
    private fun extractBasicMetadata(uri: Uri, file: File?): BasicMetadata {
        val extractor = MediaExtractor()
        var metadata = BasicMetadata()
        try {
            var dataSourceSet = false

            // Try ContentResolver openFileDescriptor if content://
            if (uri.scheme == ContentResolver.SCHEME_CONTENT) {
                try {
                    contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                        extractor.setDataSource(pfd.fileDescriptor)
                        dataSourceSet = true
                    }
                } catch (_: Exception) {}
            }

            // Fallback to direct file path
            if (!dataSourceSet && file != null && file.exists() && file.canRead()) {
                try {
                    extractor.setDataSource(file.absolutePath)
                    dataSourceSet = true
                } catch (_: Exception) {}
            }

            if (!dataSourceSet) return metadata

            val trackCount = extractor.trackCount
            for (i in 0 until trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = if (format.containsKey(MediaFormat.KEY_MIME)) {
                    format.getString(MediaFormat.KEY_MIME)
                } else null

                if (mime != null && mime.startsWith("video/")) {
                    val width = if (format.containsKey(MediaFormat.KEY_WIDTH)) {
                        format.getInteger(MediaFormat.KEY_WIDTH)
                    } else 0
                    val height = if (format.containsKey(MediaFormat.KEY_HEIGHT)) {
                        format.getInteger(MediaFormat.KEY_HEIGHT)
                    } else 0
                    val durationUs = if (format.containsKey(MediaFormat.KEY_DURATION)) {
                        format.getLong(MediaFormat.KEY_DURATION)
                    } else 0L

                    metadata = BasicMetadata(
                        width = width,
                        height = height,
                        durationMs = durationUs / 1000L,
                        mimeType = mime
                    )
                    break
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "MediaExtractor basic pass failed for $uri: ${e.message}")
        } finally {
            try {
                extractor.release()
            } catch (_: Exception) {}
        }
        return metadata
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
            "mp4" -> "video/mp4"
            "mkv" -> "video/x-matroska"
            "webm" -> "video/webm"
            "avi" -> "video/x-msvideo"
            "mov" -> "video/quicktime"
            "ts" -> "video/mp2t"
            "wmv" -> "video/x-ms-wmv"
            "m4v" -> "video/x-m4v"
            "flv" -> "video/x-flv"
            else -> null
        }
    }
}
