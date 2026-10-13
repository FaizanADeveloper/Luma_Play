package com.lumaplay.tv.player.audio

import android.net.FakeUri
import com.lumaplay.tv.data.model.AudioItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class AudioMetadataTest {

    @Test
    fun testCompleteMetadataMapping() {
        val track = AudioItem(
            id = 42L,
            contentUri = FakeUri("content://media/external/audio/media/42"),
            title = "Bohemian Rhapsody.mp3",
            artist = "Queen",
            album = "A Night at the Opera",
            albumId = 100L,
            durationMs = 354_000L,
            trackNumber = 4,
            year = 1975,
            mimeType = "audio/mpeg"
        )

        // Verify AudioItem clean metadata
        assertEquals("Bohemian Rhapsody", track.cleanTitle)
        assertEquals("Queen", track.displayArtist)
        assertEquals("A Night at the Opera", track.displayAlbum)

        // Convert to Media3 MediaItem via top-level internal extension
        val mediaItem = track.toMediaItem()
        assertEquals("42", mediaItem.mediaId)
        assertEquals(track.contentUri.toString(), mediaItem.localConfiguration?.uri.toString())
        assertEquals("audio/mpeg", mediaItem.localConfiguration?.mimeType)

        // Verify truthful MediaMetadata
        val metadata = mediaItem.mediaMetadata
        assertEquals("Bohemian Rhapsody", metadata.title.toString())
        assertEquals("Queen", metadata.artist.toString())
        assertEquals("A Night at the Opera", metadata.albumTitle.toString())
        assertEquals(4, metadata.trackNumber)
        assertEquals(1975, metadata.releaseYear)
        assertEquals(track.artworkUri, metadata.artworkUri)
    }

    @Test
    fun testTruthfulOmissionOfUnknownOrMissingArtist() {
        val trackNullArtist = AudioItem(
            id = 1L,
            contentUri = FakeUri("content://media/external/audio/media/1"),
            title = "Mystery Track.flac",
            artist = null
        )
        val mediaItemNull = trackNullArtist.toMediaItem()
        assertNull("Null artist must not be fabricated", mediaItemNull.mediaMetadata.artist)

        val trackUnknownArtist = AudioItem(
            id = 2L,
            contentUri = FakeUri("content://media/external/audio/media/2"),
            title = "Mystery Track.flac",
            artist = "<unknown>"
        )
        val mediaItemUnknown = trackUnknownArtist.toMediaItem()
        assertNull("<unknown> artist tag must be omitted from published metadata", mediaItemUnknown.mediaMetadata.artist)

        val trackBlankArtist = AudioItem(
            id = 3L,
            contentUri = FakeUri("content://media/external/audio/media/3"),
            title = "Mystery Track.flac",
            artist = "   "
        )
        val mediaItemBlank = trackBlankArtist.toMediaItem()
        assertNull("Blank artist tag must be omitted from published metadata", mediaItemBlank.mediaMetadata.artist)
    }

    @Test
    fun testTruthfulOmissionOfUnknownOrMissingAlbum() {
        val trackNullAlbum = AudioItem(
            id = 1L,
            contentUri = FakeUri("content://media/external/audio/media/1"),
            title = "Single Track.wav",
            album = null
        )
        val mediaItemNull = trackNullAlbum.toMediaItem()
        assertNull("Null album must not be fabricated", mediaItemNull.mediaMetadata.albumTitle)

        val trackUnknownAlbum = AudioItem(
            id = 2L,
            contentUri = FakeUri("content://media/external/audio/media/2"),
            title = "Single Track.wav",
            album = "<unknown>"
        )
        val mediaItemUnknown = trackUnknownAlbum.toMediaItem()
        assertNull("<unknown> album tag must be omitted from published metadata", mediaItemUnknown.mediaMetadata.albumTitle)

        val trackBlankAlbum = AudioItem(
            id = 3L,
            contentUri = FakeUri("content://media/external/audio/media/3"),
            title = "Single Track.wav",
            album = "  "
        )
        val mediaItemBlank = trackBlankAlbum.toMediaItem()
        assertNull("Blank album tag must be omitted from published metadata", mediaItemBlank.mediaMetadata.albumTitle)
    }

    @Test
    fun testOptionalNumericMetadataOmission() {
        val track = AudioItem(
            id = 10L,
            contentUri = FakeUri("content://media/external/audio/media/10"),
            title = "Simple Track",
            trackNumber = null,
            year = null,
            albumId = null
        )

        val mediaItem = track.toMediaItem()
        assertNull(mediaItem.mediaMetadata.trackNumber)
        assertNull(mediaItem.mediaMetadata.releaseYear)
        assertNull(mediaItem.mediaMetadata.artworkUri)
    }

    @Test
    fun testTitleExtensionStripping() {
        val extensions = listOf(".mp3", ".flac", ".aac", ".wav", ".ogg", ".m4a", ".opus", ".wma")
        for (ext in extensions) {
            val track = AudioItem(
                id = 1L,
                contentUri = FakeUri("content://media/1"),
                title = "Song Name$ext"
            )
            assertEquals("Extension $ext must be stripped", "Song Name", track.cleanTitle)
            assertEquals("Song Name", track.toMediaItem().mediaMetadata.title.toString())
        }
    }

    @Test
    fun testEmptyOrBlankTitleFallback() {
        val track = AudioItem(
            id = 1L,
            contentUri = FakeUri("content://media/1"),
            title = "   "
        )
        assertEquals("Unknown Title", track.cleanTitle)
        assertEquals("Unknown Title", track.toMediaItem().mediaMetadata.title.toString())
    }
}
