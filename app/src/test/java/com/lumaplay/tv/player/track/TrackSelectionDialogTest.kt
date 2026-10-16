package com.lumaplay.tv.player.track

import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MimeTypes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for TrackSelectionDialog UI state, track counts, and selection queries.
 */
class TrackSelectionDialogTest {

    private fun createAudioTrack(
        groupIndex: Int,
        trackIndex: Int,
        language: String? = "en",
        mimeType: String = MimeTypes.AUDIO_AAC,
        channelCount: Int = 2,
        isSelected: Boolean = false,
        isSupported: Boolean = true,
        label: String? = null
    ): AudioTrackItem {
        val format = Format.Builder()
            .setId("audio-$groupIndex-$trackIndex")
            .setLanguage(language)
            .setSampleMimeType(mimeType)
            .setChannelCount(channelCount)
            .setLabel(label)
            .build()
        return AudioTrackItem(
            groupIndex = groupIndex,
            trackIndex = trackIndex,
            format = format,
            isSelected = isSelected,
            isSupported = isSupported
        )
    }

    private fun createSubtitleTrack(
        groupIndex: Int,
        trackIndex: Int,
        language: String? = "en",
        mimeType: String = MimeTypes.APPLICATION_SUBRIP,
        isSelected: Boolean = false,
        isSupported: Boolean = true,
        isExternal: Boolean = false,
        label: String? = null,
        selectionFlags: Int = 0,
        roleFlags: Int = 0
    ): SubtitleTrackItem {
        val format = Format.Builder()
            .setId("sub-$groupIndex-$trackIndex")
            .setLanguage(language)
            .setSampleMimeType(mimeType)
            .setLabel(label)
            .setSelectionFlags(selectionFlags)
            .setRoleFlags(roleFlags)
            .build()
        return SubtitleTrackItem(
            groupIndex = groupIndex,
            trackIndex = trackIndex,
            format = format,
            isSelected = isSelected,
            isSupported = isSupported,
            isExternal = isExternal
        )
    }

    @Test
    fun testDefaultStateValues() {
        val defaultState = TrackSelectionUiState()

        assertFalse(defaultState.isVisible)
        assertEquals(TrackType.AUDIO, defaultState.trackType)
        assertTrue(defaultState.audioTracks.isEmpty())
        assertTrue(defaultState.subtitleTracks.isEmpty())
        assertNull(defaultState.selectedAudioTrack)
        assertNull(defaultState.selectedSubtitleTrack)
        assertTrue(defaultState.isSubtitlesOff)
        assertEquals(0, defaultState.realSubtitleCount)
        assertEquals(0, defaultState.activeTrackCount)
        assertFalse(defaultState.hasTracks)
    }

    @Test
    fun testAudioTrackSelectionAndActiveQuery() {
        val track1 = createAudioTrack(groupIndex = 0, trackIndex = 0, language = "en", isSelected = false)
        val track2 = createAudioTrack(groupIndex = 0, trackIndex = 1, language = "es", isSelected = true)
        val track3 = createAudioTrack(groupIndex = 1, trackIndex = 0, language = "ja", isSelected = false)

        val state = TrackSelectionUiState(
            isVisible = true,
            trackType = TrackType.AUDIO,
            audioTracks = listOf(track1, track2, track3)
        )

        assertTrue(state.isVisible)
        assertEquals(TrackType.AUDIO, state.trackType)
        assertEquals(3, state.activeTrackCount)
        assertTrue(state.hasTracks)

        val selected = state.selectedAudioTrack
        assertNotNull(selected)
        assertEquals(0, selected?.groupIndex)
        assertEquals(1, selected?.trackIndex)
        assertEquals("Spanish", selected?.languageName)
    }

    @Test
    fun testAudioTrackNoneSelected() {
        val track1 = createAudioTrack(groupIndex = 0, trackIndex = 0, isSelected = false)
        val track2 = createAudioTrack(groupIndex = 0, trackIndex = 1, isSelected = false)

        val state = TrackSelectionUiState(
            trackType = TrackType.AUDIO,
            audioTracks = listOf(track1, track2)
        )

        assertNull(state.selectedAudioTrack)
        assertEquals(2, state.activeTrackCount)
        assertTrue(state.hasTracks)
    }

    @Test
    fun testAudioSingleTrackAvailable() {
        val singleTrack = createAudioTrack(
            groupIndex = 0,
            trackIndex = 0,
            language = "en",
            mimeType = MimeTypes.AUDIO_TRUEHD,
            channelCount = 8,
            isSelected = true
        )

        val state = TrackSelectionUiState(
            trackType = TrackType.AUDIO,
            audioTracks = listOf(singleTrack)
        )

        assertEquals(1, state.activeTrackCount)
        assertTrue(state.hasTracks)
        assertEquals("TrueHD", state.selectedAudioTrack?.shortCodecLabel)
        assertEquals("7.1 Surround", state.selectedAudioTrack?.channelLayout)
    }

    @Test
    fun testAudioEmptyState() {
        val state = TrackSelectionUiState(
            trackType = TrackType.AUDIO,
            audioTracks = emptyList()
        )

        assertEquals(0, state.activeTrackCount)
        assertFalse(state.hasTracks)
        assertNull(state.selectedAudioTrack)
    }

    @Test
    fun testSubtitleOffState() {
        val offTrack = SubtitleTrackItem.off(isSelected = true)
        val sub1 = createSubtitleTrack(groupIndex = 0, trackIndex = 0, language = "en", isSelected = false)
        val sub2 = createSubtitleTrack(groupIndex = 0, trackIndex = 1, language = "fr", isSelected = false)

        val state = TrackSelectionUiState(
            isVisible = true,
            trackType = TrackType.SUBTITLE,
            subtitleTracks = listOf(offTrack, sub1, sub2)
        )

        assertTrue(state.isSubtitlesOff)
        assertNull(state.selectedSubtitleTrack)
        assertEquals(2, state.realSubtitleCount)
        assertEquals(2, state.activeTrackCount)
        assertTrue(state.hasTracks)
    }

    @Test
    fun testSubtitleSelectedActiveTrack() {
        val offTrack = SubtitleTrackItem.off(isSelected = false)
        val sub1 = createSubtitleTrack(
            groupIndex = 0,
            trackIndex = 0,
            language = "en",
            mimeType = MimeTypes.APPLICATION_SUBRIP,
            isSelected = true
        )
        val sub2 = createSubtitleTrack(
            groupIndex = 0,
            trackIndex = 1,
            language = "de",
            mimeType = MimeTypes.TEXT_VTT,
            isSelected = false
        )

        val state = TrackSelectionUiState(
            isVisible = true,
            trackType = TrackType.SUBTITLE,
            subtitleTracks = listOf(offTrack, sub1, sub2)
        )

        assertFalse(state.isSubtitlesOff)
        val selected = state.selectedSubtitleTrack
        assertNotNull(selected)
        assertEquals(0, selected?.groupIndex)
        assertEquals(0, selected?.trackIndex)
        assertEquals("English", selected?.languageName)
        assertEquals("SRT", selected?.formatTag)
        assertEquals(2, state.realSubtitleCount)
        assertEquals(2, state.activeTrackCount)
    }

    @Test
    fun testExternalSubtitleTrackInState() {
        val offTrack = SubtitleTrackItem.off(isSelected = false)
        val embedded = createSubtitleTrack(groupIndex = 0, trackIndex = 0, language = "en", isExternal = false)
        val external = createSubtitleTrack(
            groupIndex = 1,
            trackIndex = 0,
            language = "en",
            isExternal = true,
            isSelected = true,
            label = "movie.en.srt"
        )

        val state = TrackSelectionUiState(
            trackType = TrackType.SUBTITLE,
            subtitleTracks = listOf(offTrack, embedded, external)
        )

        assertFalse(state.isSubtitlesOff)
        val selected = state.selectedSubtitleTrack
        assertNotNull(selected)
        assertTrue(selected?.isExternal == true)
        assertEquals("movie.en.srt", selected?.customLabel)
        assertEquals(2, state.realSubtitleCount)
    }

    @Test
    fun testSubtitleEmptyState() {
        val state = TrackSelectionUiState(
            trackType = TrackType.SUBTITLE,
            subtitleTracks = emptyList()
        )

        assertTrue(state.isSubtitlesOff)
        assertNull(state.selectedSubtitleTrack)
        assertEquals(0, state.realSubtitleCount)
        assertEquals(0, state.activeTrackCount)
        assertFalse(state.hasTracks)
    }

    @Test
    fun testSubtitleOnlyOffOptionPresent() {
        val offTrack = SubtitleTrackItem.off(isSelected = true)
        val state = TrackSelectionUiState(
            trackType = TrackType.SUBTITLE,
            subtitleTracks = listOf(offTrack)
        )

        assertTrue(state.isSubtitlesOff)
        assertNull(state.selectedSubtitleTrack)
        assertEquals(0, state.realSubtitleCount)
        assertEquals(0, state.activeTrackCount)
        assertFalse(state.hasTracks)
    }

    @Test
    fun testTrackTypeToggleChangesActiveTrackCount() {
        val audioTracks = listOf(
            createAudioTrack(groupIndex = 0, trackIndex = 0),
            createAudioTrack(groupIndex = 0, trackIndex = 1),
            createAudioTrack(groupIndex = 0, trackIndex = 2)
        )
        val subtitleTracks = listOf(
            SubtitleTrackItem.off(isSelected = true),
            createSubtitleTrack(groupIndex = 0, trackIndex = 0)
        )

        val audioModeState = TrackSelectionUiState(
            trackType = TrackType.AUDIO,
            audioTracks = audioTracks,
            subtitleTracks = subtitleTracks
        )

        assertEquals(3, audioModeState.activeTrackCount)
        assertTrue(audioModeState.hasTracks)

        val subtitleModeState = audioModeState.copy(trackType = TrackType.SUBTITLE)

        assertEquals(1, subtitleModeState.activeTrackCount) // Real subtitle count
        assertTrue(subtitleModeState.hasTracks)
    }

    @Test
    fun testUnsupportedAudioTrackHandling() {
        val supportedTrack = createAudioTrack(groupIndex = 0, trackIndex = 0, isSupported = true, isSelected = true)
        val unsupportedTrack = createAudioTrack(groupIndex = 1, trackIndex = 0, isSupported = false, isSelected = false)

        val state = TrackSelectionUiState(
            trackType = TrackType.AUDIO,
            audioTracks = listOf(supportedTrack, unsupportedTrack)
        )

        assertEquals(2, state.audioTracks.size)
        assertTrue(state.audioTracks[0].isSupported)
        assertFalse(state.audioTracks[1].isSupported)
        assertEquals(supportedTrack, state.selectedAudioTrack)
    }

    @Test
    fun testSubtitleModifiersFlagDetection() {
        val forcedTrack = createSubtitleTrack(
            groupIndex = 0,
            trackIndex = 0,
            language = "en",
            selectionFlags = C.SELECTION_FLAG_FORCED
        )
        val sdhTrack = createSubtitleTrack(
            groupIndex = 0,
            trackIndex = 1,
            language = "en",
            roleFlags = C.ROLE_FLAG_DESCRIBES_MUSIC_AND_SOUND
        )

        assertTrue((forcedTrack.format?.selectionFlags?.and(C.SELECTION_FLAG_FORCED) ?: 0) != 0)
        assertTrue((sdhTrack.format?.roleFlags?.and(C.ROLE_FLAG_DESCRIBES_MUSIC_AND_SOUND) ?: 0) != 0)
    }
}
