package com.lumaplay.tv.data.model

import android.content.ContentUris
import android.net.Uri
import java.util.Locale

/**
 * Represents a local audio track discovered through MediaStore or direct filesystem discovery.
 *
 * @property id MediaStore numeric ID (or synthetic positive hash for direct filesystem items).
 * @property contentUri Content URI (or file URI) for playback via ExoPlayer.
 * @property title Track title or display name.
 * @property artist Artist name, or null if unassigned/unknown.
 * @property album Album title, or null if unassigned/unknown.
 * @property albumId MediaStore album identifier, or null if unavailable.
 * @property durationMs Track duration in milliseconds.
 * @property sizeBytes File size in bytes.
 * @property dateAdded Date added timestamp (seconds).
 * @property trackNumber Track sequence number on the album (if available).
 * @property year Release year (if available).
 * @property mimeType Audio MIME type (e.g. "audio/mpeg", "audio/flac").
 * @property folderName Display folder containing the audio file.
 * @property filePath Absolute physical filesystem path if available.
 */
data class AudioItem(
    val id: Long,
    val contentUri: Uri,
    val title: String,
    val artist: String? = null,
    val album: String? = null,
    val albumId: Long? = null,
    val durationMs: Long = 0L,
    val sizeBytes: Long = 0L,
    val dateAdded: Long = 0L,
    val trackNumber: Int? = null,
    val year: Int? = null,
    val mimeType: String? = null,
    val folderName: String = "Internal Storage / Music",
    val filePath: String? = null
) {
    /**
     * Clean display title without raw file extensions.
     */
    val cleanTitle: String
        get() = title
            .removeSuffix(".mp3")
            .removeSuffix(".flac")
            .removeSuffix(".aac")
            .removeSuffix(".wav")
            .removeSuffix(".ogg")
            .removeSuffix(".m4a")
            .removeSuffix(".opus")
            .removeSuffix(".wma")
            .ifBlank { "Unknown Title" }

    /**
     * Display artist with truthful fallback. Never fabricates fictional artists.
     */
    val displayArtist: String
        get() = if (artist.isNullOrBlank() || artist.equals("<unknown>", ignoreCase = true)) {
            "Unknown Artist"
        } else {
            artist.trim()
        }

    /**
     * Display album with truthful fallback. Never fabricates fictional albums.
     */
    val displayAlbum: String
        get() = if (album.isNullOrBlank() || album.equals("<unknown>", ignoreCase = true)) {
            "Unknown Album"
        } else {
            album.trim()
        }

    /**
     * Standard MediaStore album artwork URI if [albumId] is valid.
     */
    val artworkUri: Uri?
        get() = albumId?.let { id ->
            if (id > 0) {
                runCatching {
                    ContentUris.withAppendedId(
                        Uri.parse("content://media/external/audio/albumart"),
                        id
                    )
                }.getOrNull()
            } else null
        }

    /**
     * Formats duration into mm:ss or HH:mm:ss for 10-foot viewing.
     */
    val formattedDuration: String
        get() {
            if (durationMs <= 0) return "--:--"
            val totalSeconds = durationMs / 1000
            val hours = totalSeconds / 3600
            val minutes = (totalSeconds % 3600) / 60
            val seconds = totalSeconds % 60
            return if (hours > 0) {
                String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
            } else {
                String.format(Locale.US, "%02d:%02d", minutes, seconds)
            }
        }

    /**
     * Audio format badge derived from file extension or MIME type (e.g. "FLAC", "MP3", "AAC", "WAV", "OGG").
     */
    val formatTag: String
        get() {
            val ext = (filePath ?: title).substringAfterLast('.', "").uppercase(Locale.US)
            return when {
                ext in listOf("FLAC", "MP3", "AAC", "WAV", "OGG", "M4A", "OPUS", "WMA") -> ext
                mimeType != null && mimeType.contains("flac") -> "FLAC"
                mimeType != null && mimeType.contains("mpeg") -> "MP3"
                mimeType != null && (mimeType.contains("mp4") || mimeType.contains("m4a")) -> "M4A"
                mimeType != null && mimeType.contains("aac") -> "AAC"
                mimeType != null && mimeType.contains("wav") -> "WAV"
                mimeType != null && mimeType.contains("ogg") -> "OGG"
                mimeType != null && mimeType.contains("opus") -> "OPUS"
                else -> "AUDIO"
            }
        }

    /**
     * Formats file size in MB or KB.
     */
    val formattedFileSize: String
        get() {
            if (sizeBytes <= 0) return "-- MB"
            val mb = sizeBytes / (1024.0 * 1024.0)
            return if (mb >= 1.0) {
                String.format(Locale.US, "%.1f MB", mb)
            } else {
                val kb = sizeBytes / 1024.0
                String.format(Locale.US, "%.0f KB", kb)
            }
        }
}
