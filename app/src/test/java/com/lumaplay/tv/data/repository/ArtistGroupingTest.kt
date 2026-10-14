package com.lumaplay.tv.data.repository

import android.net.FakeUri
import com.lumaplay.tv.data.model.AudioItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ArtistGroupingTest {

    private fun createTrack(
        id: Long,
        title: String,
        artist: String?,
        album: String? = "Default Album",
        albumId: Long? = 100L,
        trackNumber: Int? = 1
    ): AudioItem {
        return AudioItem(
            id = id,
            contentUri = FakeUri("content://media/external/audio/media/$id"),
            title = title,
            artist = artist,
            album = album,
            albumId = albumId,
            durationMs = 180_000L,
            trackNumber = trackNumber
        )
    }

    @Test
    fun testGroupTracksToArtistsNormalizesNamesAndDeduplicates() {
        val tracks = listOf(
            createTrack(1, "One More Time", "Daft Punk"),
            createTrack(2, "Harder Better Faster Stronger", "daft punk"),
            createTrack(3, "Get Lucky", " Daft Punk "),
            createTrack(4, "Karma Police", "Radiohead")
        )

        val artists = MediaStoreAudioRepository.groupTracksToArtists(tracks)

        assertEquals(2, artists.size)
        val daftPunk = artists.first { it.displayName.equals("Daft Punk", ignoreCase = true) }
        assertEquals("Daft Punk", daftPunk.displayName)
        assertEquals(3, daftPunk.trackCount)
        assertEquals(3, daftPunk.tracks.size)

        val radiohead = artists.first { it.displayName == "Radiohead" }
        assertEquals(1, radiohead.trackCount)
    }

    @Test
    fun testArtistsSortedDeterministicallyCaseInsensitive() {
        val tracks = listOf(
            createTrack(1, "Track A", "Radiohead"),
            createTrack(2, "Track B", "arctic monkeys"),
            createTrack(3, "Track C", "Daft Punk")
        )

        val artists = MediaStoreAudioRepository.groupTracksToArtists(tracks)

        assertEquals(3, artists.size)
        assertEquals("arctic monkeys", artists[0].name)
        assertEquals("Daft Punk", artists[1].name)
        assertEquals("Radiohead", artists[2].name)
    }

    @Test
    fun testBlankAndUnknownArtistMetadataHandledTruthfully() {
        val tracks = listOf(
            createTrack(1, "Untitled 1", null),
            createTrack(2, "Untitled 2", ""),
            createTrack(3, "Untitled 3", "   "),
            createTrack(4, "Untitled 4", "<unknown>"),
            createTrack(5, "Known Track", "Muse")
        )

        val artists = MediaStoreAudioRepository.groupTracksToArtists(tracks)

        assertEquals(2, artists.size)
        val unknownArtist = artists.first { it.displayName == "Unknown Artist" }
        assertEquals("Unknown Artist", unknownArtist.displayName)
        assertEquals(4, unknownArtist.trackCount)

        val muse = artists.first { it.displayName == "Muse" }
        assertEquals("Muse", muse.displayName)
        assertEquals(1, muse.trackCount)
    }

    @Test
    fun testArtistOrderedTracksByAlbumThenTrackNumberThenTitle() {
        val tracks = listOf(
            createTrack(1, "Around the World", "Daft Punk", album = "Homework", trackNumber = 7),
            createTrack(2, "Da Funk", "Daft Punk", album = "Homework", trackNumber = 4),
            createTrack(3, "One More Time", "Daft Punk", album = "Discovery", trackNumber = 1),
            createTrack(4, "Aerodynamic", "Daft Punk", album = "Discovery", trackNumber = 2)
        )

        val artists = MediaStoreAudioRepository.groupTracksToArtists(tracks)
        val daftPunk = artists.first()

        assertEquals(4, daftPunk.tracks.size)
        // Ordered by album ("Discovery" before "Homework"), then track number
        assertEquals("One More Time", daftPunk.tracks[0].cleanTitle)
        assertEquals("Aerodynamic", daftPunk.tracks[1].cleanTitle)
        assertEquals("Da Funk", daftPunk.tracks[2].cleanTitle)
        assertEquals("Around the World", daftPunk.tracks[3].cleanTitle)
    }

    @Test
    fun testArtistAlbumCountCalculationWithStableIds() {
        val tracks = listOf(
            createTrack(1, "Track 1", "Artist", album = "Album A", albumId = 101L),
            createTrack(2, "Track 2", "Artist", album = "Album A", albumId = 101L),
            createTrack(3, "Track 3", "Artist", album = "Album B", albumId = 102L),
            createTrack(4, "Track 4", "Artist", album = "Album C", albumId = 103L)
        )

        val artists = MediaStoreAudioRepository.groupTracksToArtists(tracks)
        val artist = artists.first()

        assertEquals(4, artist.trackCount)
        assertEquals(3, artist.albumCount)
        assertEquals("3 Albums", artist.formattedAlbumCount)
    }

    @Test
    fun testArtistAlbumCountCalculationFallbackWithoutIds() {
        val tracks = listOf(
            createTrack(1, "Track 1", "Artist", album = "Greatest Hits", albumId = null),
            createTrack(2, "Track 2", "Artist", album = "Greatest Hits", albumId = null),
            createTrack(3, "Track 3", "Artist", album = "Live in Paris", albumId = null)
        )

        val artists = MediaStoreAudioRepository.groupTracksToArtists(tracks)
        val artist = artists.first()

        assertEquals(3, artist.trackCount)
        assertEquals(2, artist.albumCount)
        assertEquals("2 Albums", artist.formattedAlbumCount)
    }

    @Test
    fun testArtistRepresentativeArtworkSelectedFromFirstValidTrack() {
        val artworkTrack = AudioItem(
            id = 10,
            contentUri = FakeUri("content://media/external/audio/media/10"),
            title = "Track With Art",
            artist = "Pink Floyd",
            albumId = 555L
        )
        val noArtworkTrack = AudioItem(
            id = 11,
            contentUri = FakeUri("content://media/external/audio/media/11"),
            title = "Track Without Art",
            artist = "Pink Floyd",
            albumId = null
        )

        val artists = MediaStoreAudioRepository.groupTracksToArtists(listOf(noArtworkTrack, artworkTrack))
        val pinkFloyd = artists.first()

        // The representative artist artworkUri must match the resolved artwork of the first valid track.
        // In pure JVM unit tests on unmocked android.jar, AudioItem.artworkUri safely returns null via runCatching.
        assertEquals(artworkTrack.artworkUri, pinkFloyd.artworkUri)
        assertNull(MediaStoreAudioRepository.groupTracksToArtists(listOf(noArtworkTrack)).first().artworkUri)
    }

    @Test
    fun testEmptyTracksReturnsEmptyArtists() {
        val artists = MediaStoreAudioRepository.groupTracksToArtists(emptyList())
        assertTrue(artists.isEmpty())
    }
}
