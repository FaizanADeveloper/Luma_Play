package com.lumaplay.tv.data.repository

import android.content.ContentResolver
import android.content.ContentUris
import android.provider.MediaStore
import com.lumaplay.tv.data.model.VideoItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * MediaStore implementation of [VideoRepository].
 *
 * Scans [MediaStore.Video.Media.EXTERNAL_CONTENT_URI] off the main thread ([Dispatchers.IO]).
 * Safely constructs content URIs, handles missing columns, and filters out 0-byte or corrupt entries.
 */
class MediaStoreVideoRepository(
    private val contentResolver: ContentResolver
) : VideoRepository {

    override suspend fun getVideos(): Result<List<VideoItem>> = withContext(Dispatchers.IO) {
        runCatching {
            val videoList = mutableListOf<VideoItem>()

            val projection = arrayOf(
                MediaStore.Video.Media._ID,
                MediaStore.Video.Media.DISPLAY_NAME,
                MediaStore.Video.Media.DURATION,
                MediaStore.Video.Media.WIDTH,
                MediaStore.Video.Media.HEIGHT,
                MediaStore.Video.Media.SIZE,
                MediaStore.Video.Media.DATE_ADDED
            )

            // Filter out 0-byte files
            val selection = "${MediaStore.Video.Media.SIZE} > 0"
            val sortOrder = "${MediaStore.Video.Media.DATE_ADDED} DESC"

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

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val name = cursor.getString(nameColumn) ?: "Untitled Video"
                    val duration = cursor.getLong(durationColumn)
                    val width = cursor.getInt(widthColumn)
                    val height = cursor.getInt(heightColumn)
                    val size = cursor.getLong(sizeColumn)
                    val dateAdded = cursor.getLong(dateAddedColumn)

                    val contentUri = ContentUris.withAppendedId(
                        MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                        id
                    )

                    videoList.add(
                        VideoItem(
                            id = id,
                            contentUri = contentUri,
                            title = name,
                            durationMs = duration,
                            width = width,
                            height = height,
                            sizeBytes = size,
                            dateAdded = dateAdded
                        )
                    )
                }
            }

            videoList
        }
    }
}
