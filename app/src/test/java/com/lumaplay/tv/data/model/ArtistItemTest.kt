package com.lumaplay.tv.data.model

import android.net.FakeUri
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ArtistItemTest {

    @Test
    fun testDisplayNameWithValidName() {
        val artist = ArtistItem(
            name = "  Pink Floyd  ",
            trackCount = 5,
            albumCount = 2
        )
        assertEquals("Pink Floyd", artist.displayName)
    }

    @Test
    fun testDisplayNameWithBlankNameFallsBackToUnknownArtist() {
        val artistEmpty = ArtistItem(name = "", trackCount = 1, albumCount = 1)
        val artistSpaces = ArtistItem(name = "   ", trackCount = 1, albumCount = 1)
        assertEquals("Unknown Artist", artistEmpty.displayName)
        assertEquals("Unknown Artist", artistSpaces.displayName)
    }

    @Test
    fun testDisplayNameWithUnknownMarkerFallsBackToUnknownArtist() {
        val artistUpper = ArtistItem(name = "<UNKNOWN>", trackCount = 2, albumCount = 1)
        val artistLower = ArtistItem(name = "<unknown>", trackCount = 2, albumCount = 1)
        assertEquals("Unknown Artist", artistUpper.displayName)
        assertEquals("Unknown Artist", artistLower.displayName)
    }

    @Test
    fun testFormattedTrackCountPluralization() {
        val single = ArtistItem(name = "Artist", trackCount = 1, albumCount = 1)
        val plural = ArtistItem(name = "Artist", trackCount = 12, albumCount = 2)
        val zero = ArtistItem(name = "Artist", trackCount = 0, albumCount = 0)

        assertEquals("1 Track", single.formattedTrackCount)
        assertEquals("12 Tracks", plural.formattedTrackCount)
        assertEquals("0 Tracks", zero.formattedTrackCount)
    }

    @Test
    fun testFormattedAlbumCountPluralization() {
        val single = ArtistItem(name = "Artist", trackCount = 5, albumCount = 1)
        val plural = ArtistItem(name = "Artist", trackCount = 12, albumCount = 3)
        val zero = ArtistItem(name = "Artist", trackCount = 0, albumCount = 0)

        assertEquals("1 Album", single.formattedAlbumCount)
        assertEquals("3 Albums", plural.formattedAlbumCount)
        assertEquals("0 Albums", zero.formattedAlbumCount)
    }

    @Test
    fun testArtworkUriPreservation() {
        val artworkUri = FakeUri("content://media/external/audio/albumart/101")
        val artistWithArtwork = ArtistItem(
            name = "Daft Punk",
            trackCount = 10,
            albumCount = 2,
            artworkUri = artworkUri
        )
        val artistWithoutArtwork = ArtistItem(
            name = "Radiohead",
            trackCount = 4,
            albumCount = 1,
            artworkUri = null
        )

        assertEquals(artworkUri, artistWithArtwork.artworkUri)
        assertNull(artistWithoutArtwork.artworkUri)
    }
}
