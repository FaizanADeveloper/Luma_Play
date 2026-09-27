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
    val dateAdded: Long
) {
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
                String.format("%d:%02d:%02d", hours, minutes, seconds)
            } else {
                String.format("%02d:%02d", minutes, seconds)
            }
        }

    /**
     * Resolution tag for LumaBadge (e.g. "4K", "1080p", "720p", "SD").
     */
    val resolutionTag: String
        get() = when {
            width >= 3840 || height >= 2160 -> "4K"
            width >= 1920 || height >= 1080 -> "1080p"
            width >= 1280 || height >= 720 -> "720p"
            width > 0 && height > 0 -> "SD"
            else -> "VIDEO"
        }
}
