package com.lumaplay.tv.player.track

import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MimeTypes
import androidx.media3.common.TrackGroup
import androidx.media3.common.TrackSelectionParameters
import androidx.media3.common.Tracks
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TrackHelperTest {

    @Test
    fun testLanguageNormalizationStandardCodes() {
        // ISO 639-1 (2-letter)
        assertEquals("English", TrackHelper.formatLanguage("en"))
        assertEquals("Spanish", TrackHelper.formatLanguage("es"))
        assertEquals("French", TrackHelper.formatLanguage("fr"))
        assertEquals("German", TrackHelper.formatLanguage("de"))
        assertEquals("Japanese", TrackHelper.formatLanguage("ja"))
        assertEquals("Hindi", TrackHelper.formatLanguage("hi"))

        // ISO 639-2 (3-letter)
        assertEquals("English", TrackHelper.formatLanguage("eng"))
        assertEquals("Spanish", TrackHelper.formatLanguage("spa"))
        assertEquals("French", TrackHelper.formatLanguage("fra"))
        assertEquals("French", TrackHelper.formatLanguage("fre"))
        assertEquals("German", TrackHelper.formatLanguage("deu"))
        assertEquals("German", TrackHelper.formatLanguage("ger"))
        assertEquals("Italian", TrackHelper.formatLanguage("ita"))
        assertEquals("Japanese", TrackHelper.formatLanguage("jpn"))
        assertEquals("Korean", TrackHelper.formatLanguage("kor"))
        assertEquals("Chinese", TrackHelper.formatLanguage("zho"))
        assertEquals("Chinese", TrackHelper.formatLanguage("chi"))
        assertEquals("Russian", TrackHelper.formatLanguage("rus"))
        assertEquals("Hindi", TrackHelper.formatLanguage("hin"))
        assertEquals("Portuguese", TrackHelper.formatLanguage("por"))
        assertEquals("Dutch", TrackHelper.formatLanguage("nld"))
        assertEquals("Dutch", TrackHelper.formatLanguage("dut"))
    }

    @Test
    fun testLanguageNormalizationBcp47RegionTags() {
        assertEquals("English (United States)", TrackHelper.formatLanguage("en-US"))
        assertEquals("Portuguese (Brazil)", TrackHelper.formatLanguage("pt-BR"))
        assertEquals("Spanish (Mexico)", TrackHelper.formatLanguage("es-MX"))
    }

    @Test
    fun testLanguageNormalizationUnknownAndEmptyTags() {
        assertEquals("Unknown Language", TrackHelper.formatLanguage(null))
        assertEquals("Unknown Language", TrackHelper.formatLanguage(""))
        assertEquals("Unknown Language", TrackHelper.formatLanguage("   "))
        assertEquals("Unknown Language", TrackHelper.formatLanguage("und"))
        assertEquals("Unknown Language", TrackHelper.formatLanguage("zxx"))
        assertEquals("Unknown Language", TrackHelper.formatLanguage("mul"))
        assertEquals("Unknown Language", TrackHelper.formatLanguage("qaa"))
        assertEquals("Unknown Language", TrackHelper.formatLanguage("xyz123"))
    }

    @Test
    fun testChannelLayoutFormatting() {
        assertEquals("", TrackHelper.formatChannelCount(0))
        assertEquals("", TrackHelper.formatChannelCount(-1))
        assertEquals("Mono (1.0)", TrackHelper.formatChannelCount(1))
        assertEquals("Stereo (2.0)", TrackHelper.formatChannelCount(2))
        assertEquals("2.1 Surround", TrackHelper.formatChannelCount(3))
        assertEquals("4.0 Surround", TrackHelper.formatChannelCount(4))
        assertEquals("5.0 Surround", TrackHelper.formatChannelCount(5))
        assertEquals("5.1 Surround", TrackHelper.formatChannelCount(6))
        assertEquals("6.1 Surround", TrackHelper.formatChannelCount(7))
        assertEquals("7.1 Surround", TrackHelper.formatChannelCount(8))
        assertEquals("16 Channels", TrackHelper.formatChannelCount(16))

        // Short channel layouts
        assertEquals("", TrackHelper.formatShortChannelLayout(0))
        assertEquals("1.0", TrackHelper.formatShortChannelLayout(1))
        assertEquals("2.0", TrackHelper.formatShortChannelLayout(2))
        assertEquals("5.1", TrackHelper.formatShortChannelLayout(6))
        assertEquals("7.1", TrackHelper.formatShortChannelLayout(8))
        assertEquals("16ch", TrackHelper.formatShortChannelLayout(16))
    }

    @Test
    fun testAudioCodecLabeling() {
        assertEquals("AAC", TrackHelper.formatAudioCodec(MimeTypes.AUDIO_AAC, null))
        assertEquals("Dolby Digital (AC-3)", TrackHelper.formatAudioCodec(MimeTypes.AUDIO_AC3, null))
        assertEquals("Dolby Digital Plus (E-AC-3)", TrackHelper.formatAudioCodec(MimeTypes.AUDIO_E_AC3, null))
        assertEquals("Dolby Digital Plus (Atmos)", TrackHelper.formatAudioCodec(MimeTypes.AUDIO_E_AC3, "joc"))
        assertEquals("Dolby Digital Plus (Atmos)", TrackHelper.formatAudioCodec("audio/eac3-joc", null))
        assertEquals("Dolby TrueHD", TrackHelper.formatAudioCodec(MimeTypes.AUDIO_TRUEHD, null))
        assertEquals("Dolby TrueHD (Atmos)", TrackHelper.formatAudioCodec(MimeTypes.AUDIO_TRUEHD, "atmos"))
        assertEquals("FLAC", TrackHelper.formatAudioCodec(MimeTypes.AUDIO_FLAC, null))
        assertEquals("MP3", TrackHelper.formatAudioCodec(MimeTypes.AUDIO_MPEG, null))
        assertEquals("Opus", TrackHelper.formatAudioCodec(MimeTypes.AUDIO_OPUS, null))
        assertEquals("Vorbis", TrackHelper.formatAudioCodec(MimeTypes.AUDIO_VORBIS, null))
        assertEquals("DTS", TrackHelper.formatAudioCodec(MimeTypes.AUDIO_DTS, null))
        assertEquals("DTS-HD", TrackHelper.formatAudioCodec(MimeTypes.AUDIO_DTS_HD, null))
        assertEquals("DTS Express", TrackHelper.formatAudioCodec(MimeTypes.AUDIO_DTS_EXPRESS, null))
        assertEquals("DTS:X", TrackHelper.formatAudioCodec(MimeTypes.AUDIO_DTS_X, null))
        assertEquals("PCM", TrackHelper.formatAudioCodec(MimeTypes.AUDIO_RAW, null))
        assertEquals("Unknown Codec", TrackHelper.formatAudioCodec(null, null))

        // Short codec labels
        assertEquals("AAC", TrackHelper.formatShortAudioCodec(MimeTypes.AUDIO_AAC, null))
        assertEquals("AC-3", TrackHelper.formatShortAudioCodec(MimeTypes.AUDIO_AC3, null))
        assertEquals("E-AC-3", TrackHelper.formatShortAudioCodec(MimeTypes.AUDIO_E_AC3, null))
        assertEquals("E-AC-3 Atmos", TrackHelper.formatShortAudioCodec(MimeTypes.AUDIO_E_AC3, "joc"))
        assertEquals("TrueHD", TrackHelper.formatShortAudioCodec(MimeTypes.AUDIO_TRUEHD, null))
        assertEquals("TrueHD Atmos", TrackHelper.formatShortAudioCodec(MimeTypes.AUDIO_TRUEHD, "atmos"))
        assertEquals("DTS", TrackHelper.formatShortAudioCodec(MimeTypes.AUDIO_DTS, null))
        assertEquals("DTS-HD", TrackHelper.formatShortAudioCodec(MimeTypes.AUDIO_DTS_HD, null))
        assertEquals("DTS Express", TrackHelper.formatShortAudioCodec(MimeTypes.AUDIO_DTS_EXPRESS, null))
        assertEquals("DTS:X", TrackHelper.formatShortAudioCodec(MimeTypes.AUDIO_DTS_X, null))
        assertEquals("PCM", TrackHelper.formatShortAudioCodec(MimeTypes.AUDIO_RAW, null))
    }

    @Test
    fun testSubtitleFormatLabeling() {
        assertEquals("SRT", TrackHelper.formatSubtitleFormat(MimeTypes.APPLICATION_SUBRIP))
        assertEquals("VTT", TrackHelper.formatSubtitleFormat(MimeTypes.TEXT_VTT))
        assertEquals("SSA/ASS", TrackHelper.formatSubtitleFormat(MimeTypes.TEXT_SSA))
        assertEquals("PGS", TrackHelper.formatSubtitleFormat(MimeTypes.APPLICATION_PGS))
        assertEquals("VobSub", TrackHelper.formatSubtitleFormat(MimeTypes.APPLICATION_VOBSUB))
        assertEquals("TTML", TrackHelper.formatSubtitleFormat(MimeTypes.APPLICATION_TTML))
        assertEquals("SRT", TrackHelper.formatSubtitleFormat(null, isExternal = true))
        assertEquals("Subtitles", TrackHelper.formatSubtitleFormat(null, isExternal = false))
    }

    @Test
    fun testAudioDisplayNameAndOsdSummary() {
        val format1 = Format.Builder()
            .setId("a1")
            .setLanguage("eng")
            .setSampleMimeType(MimeTypes.AUDIO_TRUEHD)
            .setChannelCount(8)
            .build()
        val item1 = AudioTrackItem(0, 0, format1, isSelected = true)

        assertEquals("English • Dolby TrueHD • 7.1 Surround", item1.displayName)
        assertEquals("English TrueHD 7.1", item1.osdSummary)

        val format2 = Format.Builder()
            .setId("a2")
            .setLanguage("spa")
            .setLabel("Director's Commentary")
            .setSampleMimeType(MimeTypes.AUDIO_AC3)
            .setChannelCount(2)
            .build()
        val item2 = AudioTrackItem(0, 1, format2, isSelected = false)

        assertEquals("Spanish • Director's Commentary • Dolby Digital (AC-3) • Stereo (2.0)", item2.displayName)
        assertEquals("Spanish AC-3 2.0", item2.osdSummary)

        // Missing metadata fallback
        val format3 = Format.Builder().build()
        val item3 = AudioTrackItem(1, 0, format3, isSelected = false)

        assertEquals("Audio Track 1", item3.displayName)
        assertEquals("Audio", item3.osdSummary)
    }

    @Test
    fun testSubtitleDisplayNameAndOsdSummary() {
        // Off sentinel
        val offItem = SubtitleTrackItem.off(isSelected = true)
        assertTrue(offItem.isOff)
        assertEquals("Off", offItem.displayName)
        assertEquals("Subtitles Off", offItem.osdSummary)

        // Embedded SRT
        val format1 = Format.Builder()
            .setLanguage("eng")
            .setSampleMimeType(MimeTypes.APPLICATION_SUBRIP)
            .build()
        val item1 = SubtitleTrackItem(0, 0, format1, isSelected = false)

        assertEquals("English [SRT]", item1.displayName)
        assertEquals("English [SRT]", item1.osdSummary)

        // Embedded with custom label
        val format2 = Format.Builder()
            .setLanguage("fre")
            .setLabel("Director's Notes")
            .setSampleMimeType(MimeTypes.TEXT_VTT)
            .build()
        val item2 = SubtitleTrackItem(0, 1, format2, isSelected = true)

        assertEquals("French • Director's Notes [VTT]", item2.displayName)
        assertEquals("French [VTT]", item2.osdSummary)

        // External SRT
        val format3 = Format.Builder()
            .setId("external:/storage/movie.srt")
            .setLanguage("eng")
            .setSampleMimeType(MimeTypes.APPLICATION_SUBRIP)
            .build()
        val item3 = SubtitleTrackItem(1, 0, format3, isSelected = false, isExternal = true)

        assertEquals("English (External SRT)", item3.displayName)
        assertEquals("English (External SRT)", item3.osdSummary)

        // Unknown language embedded
        val format4 = Format.Builder()
            .setSampleMimeType(MimeTypes.APPLICATION_SUBRIP)
            .build()
        val item4 = SubtitleTrackItem(0, 2, format4, isSelected = false)

        assertEquals("Subtitle Track 3 [SRT]", item4.displayName)
        assertEquals("Subtitles [SRT]", item4.osdSummary)
    }

    @Test
    fun testParseAudioTracksWithMultipleGroupsAndCorrectIndices() {
        val audioFormat1 = Format.Builder()
            .setId("a1")
            .setLanguage("eng")
            .setSampleMimeType(MimeTypes.AUDIO_AAC)
            .setChannelCount(2)
            .build()
        val audioFormat2 = Format.Builder()
            .setId("a2")
            .setLanguage("eng")
            .setSampleMimeType(MimeTypes.AUDIO_TRUEHD)
            .setChannelCount(8)
            .build()
        val audioGroup1 = TrackGroup("audio_group_0", audioFormat1, audioFormat2)

        val audioFormat3 = Format.Builder()
            .setId("a3")
            .setLanguage("spa")
            .setSampleMimeType(MimeTypes.AUDIO_AC3)
            .setChannelCount(6)
            .build()
        val audioGroup2 = TrackGroup("audio_group_1", audioFormat3)

        val videoFormat = Format.Builder()
            .setId("v1")
            .setSampleMimeType(MimeTypes.VIDEO_H264)
            .build()
        val videoGroup = TrackGroup("video_group_0", videoFormat)

        // Group 0: 2 audio tracks (track 1 selected)
        val media3Group0 = Tracks.Group(
            audioGroup1,
            false,
            intArrayOf(C.FORMAT_HANDLED, C.FORMAT_HANDLED),
            booleanArrayOf(false, true)
        )
        // Group 1: 1 video track
        val media3Group1 = Tracks.Group(
            videoGroup,
            false,
            intArrayOf(C.FORMAT_HANDLED),
            booleanArrayOf(true)
        )
        // Group 2: 1 audio track (track 0 unselected)
        val media3Group2 = Tracks.Group(
            audioGroup2,
            false,
            intArrayOf(C.FORMAT_HANDLED),
            booleanArrayOf(false)
        )

        val tracks = Tracks(listOf(media3Group0, media3Group1, media3Group2))

        val parsedAudio = TrackHelper.parseAudioTracks(tracks)
        assertEquals(3, parsedAudio.size)

        // Verify Group 0 Track 0
        assertEquals(0, parsedAudio[0].groupIndex)
        assertEquals(0, parsedAudio[0].trackIndex)
        assertEquals("a1", parsedAudio[0].format.id)
        assertFalse(parsedAudio[0].isSelected)
        assertTrue(parsedAudio[0].isSupported)

        // Verify Group 0 Track 1
        assertEquals(0, parsedAudio[1].groupIndex)
        assertEquals(1, parsedAudio[1].trackIndex)
        assertEquals("a2", parsedAudio[1].format.id)
        assertTrue(parsedAudio[1].isSelected)

        // Verify Group 2 Track 0 (retains actual groupIndex=2 in Tracks, not flattened 1)
        assertEquals(2, parsedAudio[2].groupIndex)
        assertEquals(0, parsedAudio[2].trackIndex)
        assertEquals("a3", parsedAudio[2].format.id)
        assertFalse(parsedAudio[2].isSelected)
    }

    @Test
    fun testParseSubtitleTracksWithOffSelectionState() {
        val subFormat1 = Format.Builder()
            .setId("s1")
            .setLanguage("eng")
            .setSampleMimeType(MimeTypes.APPLICATION_SUBRIP)
            .build()
        val subFormat2 = Format.Builder()
            .setId("s2")
            .setLanguage("fre")
            .setSampleMimeType(MimeTypes.TEXT_VTT)
            .build()
        val subGroup1 = TrackGroup("sub_group_eng", subFormat1)
        val subGroup2 = TrackGroup("sub_group_fre", subFormat2)

        // Case A: Group 0 is selected -> "Off" is not selected
        val media3GroupWithSelection1 = Tracks.Group(
            subGroup1,
            false,
            intArrayOf(C.FORMAT_HANDLED),
            booleanArrayOf(true)
        )
        val media3GroupWithSelection2 = Tracks.Group(
            subGroup2,
            false,
            intArrayOf(C.FORMAT_HANDLED),
            booleanArrayOf(false)
        )
        val tracksWithSelection = Tracks(listOf(media3GroupWithSelection1, media3GroupWithSelection2))
        val parsedWithSelection = TrackHelper.parseSubtitleTracks(tracksWithSelection, includeOffOption = true)

        assertEquals(3, parsedWithSelection.size)
        // Item 0 is Off
        assertTrue(parsedWithSelection[0].isOff)
        assertFalse(parsedWithSelection[0].isSelected)
        // Item 1 is English
        assertEquals("s1", parsedWithSelection[1].format?.id)
        assertEquals(0, parsedWithSelection[1].groupIndex)
        assertEquals(0, parsedWithSelection[1].trackIndex)
        assertTrue(parsedWithSelection[1].isSelected)
        // Item 2 is French
        assertEquals("s2", parsedWithSelection[2].format?.id)
        assertEquals(1, parsedWithSelection[2].groupIndex)
        assertEquals(0, parsedWithSelection[2].trackIndex)
        assertFalse(parsedWithSelection[2].isSelected)

        // Case B: No tracks selected -> "Off" is selected
        val media3GroupWithoutSelection1 = Tracks.Group(
            subGroup1,
            false,
            intArrayOf(C.FORMAT_HANDLED),
            booleanArrayOf(false)
        )
        val media3GroupWithoutSelection2 = Tracks.Group(
            subGroup2,
            false,
            intArrayOf(C.FORMAT_HANDLED),
            booleanArrayOf(false)
        )
        val tracksWithoutSelection = Tracks(listOf(media3GroupWithoutSelection1, media3GroupWithoutSelection2))
        val parsedWithoutSelection = TrackHelper.parseSubtitleTracks(tracksWithoutSelection, includeOffOption = true)

        assertEquals(3, parsedWithoutSelection.size)
        assertTrue(parsedWithoutSelection[0].isOff)
        assertTrue(parsedWithoutSelection[0].isSelected)
        assertFalse(parsedWithoutSelection[1].isSelected)
        assertFalse(parsedWithoutSelection[2].isSelected)
    }

    @Test
    fun testTrackSelectionParametersOverrides() {
        val initialParameters = TrackSelectionParameters.DEFAULT_WITHOUT_CONTEXT

        val audioFormat = Format.Builder().setId("a1").build()
        val audioGroup = TrackGroup("audio_group", audioFormat)
        val audioItem = AudioTrackItem(
            groupIndex = 0,
            trackIndex = 0,
            format = audioFormat,
            isSelected = false,
            mediaTrackGroup = audioGroup
        )

        val paramsWithAudio = TrackHelper.selectAudioTrack(initialParameters, audioItem)
        assertFalse(TrackHelper.isTrackTypeDisabled(paramsWithAudio, C.TRACK_TYPE_AUDIO))
        assertNotNull(paramsWithAudio.overrides[audioGroup])
        assertEquals(0, paramsWithAudio.overrides[audioGroup]?.trackIndices?.first())

        // Select subtitle
        val subFormat = Format.Builder().setId("s1").build()
        val subGroup = TrackGroup("sub_group", subFormat)
        val subItem = SubtitleTrackItem(
            groupIndex = 1,
            trackIndex = 0,
            format = subFormat,
            isSelected = false,
            mediaTrackGroup = subGroup
        )

        val paramsWithSub = TrackHelper.selectSubtitleTrack(paramsWithAudio, subItem)
        assertFalse(TrackHelper.isTrackTypeDisabled(paramsWithSub, C.TRACK_TYPE_TEXT))
        assertNotNull(paramsWithSub.overrides[subGroup])

        // Select subtitle OFF
        val offItem = SubtitleTrackItem.off(isSelected = false)
        val paramsWithSubOff = TrackHelper.selectSubtitleTrack(paramsWithSub, offItem)
        assertTrue(TrackHelper.isTrackTypeDisabled(paramsWithSubOff, C.TRACK_TYPE_TEXT))
        assertTrue(paramsWithSubOff.disabledTrackTypes.contains(C.TRACK_TYPE_TEXT))

        // Direct disable subtitles
        val paramsDisabledDirect = TrackHelper.disableSubtitles(paramsWithSub)
        assertTrue(TrackHelper.isTrackTypeDisabled(paramsDisabledDirect, C.TRACK_TYPE_TEXT))
    }
}
