package com.lumaplay.tv.data.model

import android.net.Uri

/**
 * Represents an indexed music artist, grouped from discovered [AudioItem] tracks.
 *
 * @property name Artist name as discovered from track metadata.
 * @property trackCount Number of discovered tracks attributed to this artist.
 * @property albumCount Number of distinct discovered albums attributed to this artist.
 * @property artworkUri Representative artist/album artwork URI if available.
 * @property tracks The ordered list of audio tracks belonging to this artist.
 */
data class ArtistItem(
    val name: String,
    val trackCount: Int,
    val albumCount: Int,
    val artworkUri: Uri? = null,
    val tracks: List<AudioItem> = emptyList()
) {
    /**
     * Display artist name with truthful fallback. Never fabricates fictional artists.
     */
    val displayName: String
        get() = if (name.isBlank() || name.equals("<unknown>", ignoreCase = true)) {
            "Unknown Artist"
        } else {
            name.trim()
        }

    /**
     * Formatted track count label (e.g. "12 Tracks" or "1 Track").
     */
    val formattedTrackCount: String
        get() = "$trackCount Track${if (trackCount != 1) "s" else ""}"

    /**
     * Formatted album count label (e.g. "3 Albums" or "1 Album").
     */
    val formattedAlbumCount: String
        get() = "$albumCount Album${if (albumCount != 1) "s" else ""}"
}
