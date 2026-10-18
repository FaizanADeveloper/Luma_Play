package com.lumaplay.tv.data.model

/**
 * Aggregated, categorized search results discovered across local media.
 *
 * @property videos Matching video items.
 * @property tracks Matching individual audio tracks.
 * @property artists Matching music artists.
 * @property albums Matching music albums.
 * @property folders Distinct matching folder names derived from media items.
 */
data class SearchResults(
    val videos: List<VideoItem> = emptyList(),
    val tracks: List<AudioItem> = emptyList(),
    val artists: List<ArtistItem> = emptyList(),
    val albums: List<AlbumItem> = emptyList(),
    val folders: List<String> = emptyList()
) {
    /**
     * Total number of matched items across all categories.
     */
    val totalCount: Int
        get() = videos.size + tracks.size + artists.size + albums.size + folders.size

    /**
     * Whether all result categories are empty.
     */
    val isEmpty: Boolean
        get() = totalCount == 0

    /**
     * Whether at least one category contains results.
     */
    val isNotEmpty: Boolean
        get() = !isEmpty

    companion object {
        val EMPTY = SearchResults()
    }
}
