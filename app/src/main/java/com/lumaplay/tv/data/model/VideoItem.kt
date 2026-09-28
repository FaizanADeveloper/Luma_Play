package com.lumaplay.tv.data.model

import android.net.Uri

/**
 * Represents a local video file discovered through MediaStore.
 *
 * @property id MediaStore numeric ID.
 * @property contentUri Content URI for streaming/playback via ExoPlayer.
 * @property title Display name or title of the video file.
 * @property durationMs Duration in milliseconds.
 * @property width Video frame width in pixels.
 * @property height Video frame height in pixels.
 * @property sizeBytes File size in bytes.
 * @property dateAdded Date added timestamp (seconds).
 */
data class VideoItem(
    val id: Long,
    val contentUri: Uri,
    val title: String,
    val durationMs: Long,
    val width: Int,
    val height: Int,
    val sizeBytes: Long,
    val dateAdded: Long,
    val folderName: String = "Internal Storage / Videos",
    val mimeType: String? = null,
    val playbackPositionMs: Long = 0L
) {
    /**
     * Clean display title without raw file extensions.
     */
    val cleanTitle: String
        get() = title
            .removeSuffix(".mkv")
            .removeSuffix(".mp4")
            .removeSuffix(".avi")
            .removeSuffix(".webm")
            .removeSuffix(".mov")
            .removeSuffix(".ts")

    /**
     * Formats duration into HH:mm:ss or mm:ss for 10-foot viewing.
     */
    val formattedDuration: String
        get() {
            if (durationMs <= 0) return "--:--"
            val totalSeconds = durationMs / 1000
            val hours = totalSeconds / 3600
            val minutes = (totalSeconds % 3600) / 60
            val seconds = totalSeconds % 60
            return if (hours > 0) {
                String.format(java.util.Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
            } else {
                String.format(java.util.Locale.US, "%02d:%02d", minutes, seconds)
            }
        }

    /**
     * Compact duration label (e.g. "2h 46m", "22m") matching Stitch badge specification.
     */
    val compactDuration: String
        get() {
            if (durationMs <= 0) return "--"
            val totalMinutes = durationMs / 60000
            val hours = totalMinutes / 60
            val minutes = totalMinutes % 60
            return if (hours > 0) {
                "${hours}h ${minutes}m"
            } else {
                "${minutes}m"
            }
        }

    /**
     * Resolution tag for LumaBadge (e.g. "4K UHD", "1080p", "720p", "SD").
     */
    val resolutionTag: String
        get() = when {
            width >= 3840 || height >= 2160 -> "4K UHD"
            width >= 1920 || height >= 1080 -> "1080p"
            width >= 1280 || height >= 720 -> "720p"
            width > 0 && height > 0 -> "SD"
            else -> "VIDEO"
        }

    /**
     * Container format tag derived from file name extension or MIME type.
     */
    val containerTag: String?
        get() {
            val ext = title.substringAfterLast('.', "").uppercase(java.util.Locale.US)
            return when {
                ext in listOf("MKV", "MP4", "WEBM", "AVI", "MOV", "TS") -> ext
                mimeType != null && mimeType.contains("matroska") -> "MKV"
                mimeType != null && mimeType.contains("mp4") -> "MP4"
                mimeType != null && mimeType.contains("webm") -> "WEBM"
                else -> null
            }
        }

    /**
     * Formats file size in MB or GB.
     */
    val formattedFileSize: String
        get() {
            if (sizeBytes <= 0) return "-- MB"
            val mb = sizeBytes / (1024.0 * 1024.0)
            return if (mb >= 1024.0) {
                String.format(java.util.Locale.US, "%.1f GB", mb / 1024.0)
            } else {
                String.format(java.util.Locale.US, "%.0f MB", mb)
            }
        }

    /**
     * Formats elapsed resume position into HH:mm:ss or mm:ss.
     */
    val formattedResumePosition: String
        get() {
            if (playbackPositionMs <= 0) return ""
            val totalSeconds = playbackPositionMs / 1000
            val hours = totalSeconds / 3600
            val minutes = (totalSeconds % 3600) / 60
            val seconds = totalSeconds % 60
            return if (hours > 0) {
                String.format(java.util.Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
            } else {
                String.format(java.util.Locale.US, "%d:%02d", minutes, seconds)
            }
        }
}
