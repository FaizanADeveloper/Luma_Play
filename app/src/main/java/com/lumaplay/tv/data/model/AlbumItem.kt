package com.lumaplay.tv.data.model

import android.net.Uri

/**
 * Represents an indexed music album, grouped from discovered [AudioItem] tracks.
 *
 * @property id Stable identifier for the album (albumId from MediaStore, or synthetic hash).
 * @property title Album title.
 * @property artist Primary album artist name.
 * @property trackCount Number of discovered tracks belonging to this album.
 * @property year Release year if available.
 * @property artworkUri Album artwork URI (from MediaStore albumart or first track).
 * @property tracks The ordered list of audio tracks belonging to this album.
 */
data class AlbumItem(
    val id: Long,
    val title: String,
    val artist: String,
    val trackCount: Int,
    val year: Int? = null,
    val artworkUri: Uri? = null,
    val tracks: List<AudioItem> = emptyList()
) {
    /**
     * Display title with truthful fallback. Never fabricates fictional titles.
     */
    val displayTitle: String
        get() = if (title.isBlank() || title.equals("<unknown>", ignoreCase = true)) {
            "Unknown Album"
        } else {
            title.trim()
        }

    /**
     * Display artist with truthful fallback. Never fabricates fictional artists.
     */
    val displayArtist: String
        get() = if (artist.isBlank() || artist.equals("<unknown>", ignoreCase = true)) {
            "Unknown Artist"
        } else {
            artist.trim()
        }

    /**
     * Formatted release year label (e.g. "1973" or null if unavailable).
     */
    val formattedYear: String?
        get() = if (year != null && year > 0) year.toString() else null

    /**
     * Formatted track count label (e.g. "12 Tracks" or "1 Track").
     */
    val formattedTrackCount: String
        get() = "$trackCount Track${if (trackCount != 1) "s" else ""}"
}
