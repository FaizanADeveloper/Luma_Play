package com.lumaplay.tv.player.track

import androidx.media3.common.Format
import androidx.media3.common.TrackGroup

/**
 * Represents a selectable audio track in the media player.
 *
 * Preserves the exact Media3 [groupIndex] in `Tracks.groups` and [trackIndex] in [mediaTrackGroup],
 * preventing flattened-list index corruption.
 *
 * @property groupIndex Index of the group within Media3's `Tracks.groups` collection.
 * @property trackIndex Index of the track within the specific [TrackGroup].
 * @property format The underlying Media3 [Format] containing codec, channel, and language metadata.
 * @property isSelected Whether this track is currently active/selected by the player.
 * @property isSupported Whether the device decoder supports this track format.
 * @property mediaTrackGroup The Media3 [TrackGroup] required to construct a `TrackSelectionOverride`.
 */
data class AudioTrackItem(
    val groupIndex: Int,
    val trackIndex: Int,
    val format: Format,
    val isSelected: Boolean,
    val isSupported: Boolean = true,
    val mediaTrackGroup: TrackGroup? = null
) {
    /**
     * Normalized, human-readable language display name (e.g. "English", "French", "Japanese").
     * Returns "Unknown Language" if language tag is absent, blank, or undetermined ("und").
     */
    val languageName: String
        get() = TrackHelper.formatLanguage(format.language)

    /**
     * Channel layout description (e.g. "Mono", "Stereo (2.0)", "5.1 Surround", "7.1 Surround").
     * Returns empty string if channel count is non-positive or unknown.
     */
    val channelLayout: String
        get() = TrackHelper.formatChannelCount(format.channelCount)

    /**
     * Human-readable audio codec label (e.g. "AAC", "Dolby Digital Plus (E-AC-3)", "Dolby TrueHD", "FLAC", "DTS").
     */
    val codecLabel: String
        get() = TrackHelper.formatAudioCodec(format.sampleMimeType, format.codecs)

    /**
     * Short codec label for compact badges and OSD summaries (e.g. "AAC", "E-AC-3", "TrueHD", "DTS").
     */
    val shortCodecLabel: String
        get() = TrackHelper.formatShortAudioCodec(format.sampleMimeType, format.codecs)

    /**
     * Clean label from track metadata, if provided and not duplicating language/title.
     */
    val customLabel: String?
        get() = format.label?.takeIf { it.isNotBlank() }

    /**
     * Complete user-facing display name for 10-foot TV menus and dialogs.
     * Examples:
     * - "English • Dolby TrueHD • 7.1 Surround"
     * - "Spanish • Director's Commentary • Stereo (2.0)"
     * - "Audio Track 1 • AAC • Stereo (2.0)"
     */
    val displayName: String
        get() = TrackHelper.formatAudioDisplayName(this)

    /**
     * Compact summary for the bottom OSD pill button (matching Stitch reference: "English TrueHD 7.1").
     */
    val osdSummary: String
        get() = TrackHelper.formatAudioOsdSummary(this)
}

/**
 * Represents a selectable subtitle/text track in the media player.
 *
 * Preserves the exact Media3 [groupIndex] and [trackIndex], or uses [OFF_INDEX] for the special "Off" sentinel.
 *
 * @property groupIndex Index of the group within Media3's `Tracks.groups` collection (-1 for OFF).
 * @property trackIndex Index of the track within the specific [TrackGroup] (-1 for OFF).
 * @property format The underlying Media3 [Format] metadata, or null for OFF / missing metadata.
 * @property isSelected Whether this subtitle track is currently active/selected.
 * @property isSupported Whether the device renderer supports this subtitle format.
 * @property mediaTrackGroup The Media3 [TrackGroup] for overrides, or null for OFF.
 * @property isExternal True if loaded from an external sidecar subtitle file (e.g. .srt).
 */
data class SubtitleTrackItem(
    val groupIndex: Int,
    val trackIndex: Int,
    val format: Format?,
    val isSelected: Boolean,
    val isSupported: Boolean = true,
    val mediaTrackGroup: TrackGroup? = null,
    val isExternal: Boolean = false
) {
    /**
     * Returns true if this item represents the "Subtitles Off" option.
     */
    val isOff: Boolean
        get() = groupIndex == OFF_INDEX && trackIndex == OFF_INDEX

    /**
     * Normalized, human-readable language display name (e.g. "English", "Spanish").
     * Returns "Off" when [isOff] is true, or "Unknown Language" when language is absent.
     */
    val languageName: String
        get() = if (isOff) "Off" else TrackHelper.formatLanguage(format?.language)

    /**
     * Subtitle format tag (e.g. "SRT", "VTT", "SSA/ASS", "PGS", "VobSub").
     */
    val formatTag: String
        get() = if (isOff) "" else TrackHelper.formatSubtitleFormat(format?.sampleMimeType, isExternal)

    /**
     * Clean label from track metadata, if provided and not duplicating language/title.
     */
    val customLabel: String?
        get() = format?.label?.takeIf { it.isNotBlank() }

    /**
     * Complete user-facing display name for 10-foot TV menus.
     * Examples:
     * - "Off"
     * - "English [SRT]"
     * - "English (External SRT)"
     * - "French • Director's Notes [VTT]"
     * - "Subtitle Track 1 [SRT]"
     */
    val displayName: String
        get() = TrackHelper.formatSubtitleDisplayName(this)

    /**
     * Compact summary for the bottom OSD pill button (matching Stitch reference: "English [SRT]" or "Subtitles Off").
     */
    val osdSummary: String
        get() = TrackHelper.formatSubtitleOsdSummary(this)

    companion object {
        const val OFF_INDEX = -1

        /**
         * Factory function creating the special "Subtitles Off" item.
         */
        fun off(isSelected: Boolean = false): SubtitleTrackItem = SubtitleTrackItem(
            groupIndex = OFF_INDEX,
            trackIndex = OFF_INDEX,
            format = null,
            isSelected = isSelected,
            isSupported = true,
            mediaTrackGroup = null,
            isExternal = false
        )
    }
}
