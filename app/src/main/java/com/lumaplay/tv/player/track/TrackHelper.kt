package com.lumaplay.tv.player.track

import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MimeTypes
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.TrackSelectionParameters
import androidx.media3.common.Tracks
import java.util.Locale

/**
 * Pure, JVM-testable utilities for Media3 track inspection, labeling, and selection parameters.
 *
 * Implements Android TV 10-foot labeling standards adhering to Google Stitch specifications
 * without hardcoded assumptions or fabricated metadata.
 */
object TrackHelper {

    private const val UNKNOWN_LANGUAGE = "Unknown Language"
    private const val UNKNOWN_CODEC = "Unknown Codec"

    // Common ISO 639-2 (bibliographic and terminology) mappings to ensure consistent display names
    // even on platforms with incomplete 3-letter Locale resolution.
    private val ISO_639_2_TO_ENGLISH = mapOf(
        "eng" to "English",
        "spa" to "Spanish",
        "fra" to "French",
        "fre" to "French",
        "deu" to "German",
        "ger" to "German",
        "ita" to "Italian",
        "jpn" to "Japanese",
        "kor" to "Korean",
        "zho" to "Chinese",
        "chi" to "Chinese",
        "rus" to "Russian",
        "por" to "Portuguese",
        "hin" to "Hindi",
        "ara" to "Arabic",
        "ben" to "Bengali",
        "pan" to "Punjabi",
        "tam" to "Tamil",
        "tel" to "Telugu",
        "mar" to "Marathi",
        "kan" to "Kannada",
        "mal" to "Malayalam",
        "guj" to "Gujarati",
        "urd" to "Urdu",
        "tur" to "Turkish",
        "vie" to "Vietnamese",
        "pol" to "Polish",
        "nld" to "Dutch",
        "dut" to "Dutch",
        "swe" to "Swedish",
        "nor" to "Norwegian",
        "dan" to "Danish",
        "fin" to "Finnish",
        "ell" to "Greek",
        "gre" to "Greek",
        "heb" to "Hebrew",
        "ces" to "Czech",
        "cze" to "Czech",
        "hun" to "Hungarian",
        "ind" to "Indonesian",
        "tha" to "Thai",
        "ukr" to "Ukrainian",
        "fil" to "Filipino",
        "tgl" to "Tagalog",
        "ron" to "Romanian",
        "rum" to "Romanian",
        "bul" to "Bulgarian",
        "cat" to "Catalan",
        "hrv" to "Croatian",
        "srp" to "Serbian",
        "slv" to "Slovenian",
        "slk" to "Slovak",
        "slo" to "Slovak",
        "fas" to "Persian",
        "per" to "Persian",
        "msa" to "Malay",
        "may" to "Malay",
        "lat" to "Latin"
    )

    /**
     * Resolves a language code/tag into a clean, capitalized display name in English.
     *
     * Handles:
     * - Null, blank, "und" (undetermined), "zxx" (no linguistic content), "mul" (multiple)
     * - ISO 639-1 two-letter codes (e.g. "en", "es", "fr")
     * - ISO 639-2 three-letter codes (e.g. "eng", "spa", "fra", "deu")
     * - BCP-47 region tags (e.g. "en-US", "pt-BR", "es-419")
     */
    fun formatLanguage(languageTag: String?): String {
        if (languageTag.isNullOrBlank()) {
            return UNKNOWN_LANGUAGE
        }
        val clean = languageTag.trim()
        val lower = clean.lowercase(Locale.US)

        if (lower in setOf("und", "zxx", "mul", "qaa", "mis")) {
            return UNKNOWN_LANGUAGE
        }

        // Direct 3-letter lookup
        ISO_639_2_TO_ENGLISH[lower]?.let { return it }

        // BCP-47 Locale resolution
        return try {
            val normalizedTag = clean.replace('_', '-')
            val locale = Locale.forLanguageTag(normalizedTag)

            val displayName = if (clean.contains('-') || clean.contains('_')) {
                locale.getDisplayName(Locale.US)
            } else {
                locale.getDisplayLanguage(Locale.US)
            }

            if (displayName.isNotBlank() && !displayName.equals(clean, ignoreCase = true) && !displayName.equals("und", ignoreCase = true)) {
                displayName.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.US) else it.toString() }
            } else {
                // If Locale couldn't resolve 3-letter tag, check if it's alphanumeric tag
                if (clean.length in 2..3 && clean.all { it.isLetter() }) {
                    clean.uppercase(Locale.US)
                } else {
                    UNKNOWN_LANGUAGE
                }
            }
        } catch (_: Exception) {
            UNKNOWN_LANGUAGE
        }
    }

    /**
     * Converts a raw channel count into a readable 10-foot TV audio layout description.
     */
    fun formatChannelCount(channelCount: Int): String {
        return when (channelCount) {
            1 -> "Mono (1.0)"
            2 -> "Stereo (2.0)"
            3 -> "2.1 Surround"
            4 -> "4.0 Surround"
            5 -> "5.0 Surround"
            6 -> "5.1 Surround"
            7 -> "6.1 Surround"
            8 -> "7.1 Surround"
            in 9..32 -> "$channelCount Channels"
            else -> ""
        }
    }

    /**
     * Compact channel notation for OSD badges and tight spaces (e.g. "5.1", "7.1", "2.0").
     */
    fun formatShortChannelLayout(channelCount: Int): String {
        return when (channelCount) {
            1 -> "1.0"
            2 -> "2.0"
            3 -> "2.1"
            4 -> "4.0"
            5 -> "5.0"
            6 -> "5.1"
            7 -> "6.1"
            8 -> "7.1"
            in 9..32 -> "${channelCount}ch"
            else -> ""
        }
    }

    /**
     * Resolves human-readable full audio codec name from MIME type and optional codec string.
     */
    fun formatAudioCodec(mimeType: String?, codecs: String?): String {
        val mimeLower = mimeType?.lowercase(Locale.US)?.trim() ?: ""
        val codecLower = codecs?.lowercase(Locale.US)?.trim() ?: ""

        return when {
            mimeLower.contains("aac") || mimeLower == MimeTypes.AUDIO_AAC -> "AAC"
            mimeLower == MimeTypes.AUDIO_E_AC3 || mimeLower.contains("eac3") -> {
                if (codecLower.contains("joc") || mimeLower.contains("joc")) {
                    "Dolby Digital Plus (Atmos)"
                } else {
                    "Dolby Digital Plus (E-AC-3)"
                }
            }
            mimeLower == MimeTypes.AUDIO_AC3 || mimeLower.contains("ac3") -> "Dolby Digital (AC-3)"
            mimeLower == MimeTypes.AUDIO_TRUEHD || mimeLower.contains("true-hd") -> {
                if (codecLower.contains("atmos")) {
                    "Dolby TrueHD (Atmos)"
                } else {
                    "Dolby TrueHD"
                }
            }
            mimeLower == MimeTypes.AUDIO_FLAC || mimeLower.contains("flac") -> "FLAC"
            mimeLower == MimeTypes.AUDIO_MPEG || mimeLower.contains("mp3") || mimeLower.contains("mpeg") -> "MP3"
            mimeLower == MimeTypes.AUDIO_OPUS || mimeLower.contains("opus") -> "Opus"
            mimeLower == MimeTypes.AUDIO_VORBIS || mimeLower.contains("vorbis") -> "Vorbis"
            mimeLower == MimeTypes.AUDIO_DTS_X || mimeLower.contains("dts.uhd") -> "DTS:X"
            mimeLower == MimeTypes.AUDIO_DTS_EXPRESS || mimeLower.contains("profile=lbr") -> "DTS Express"
            mimeLower == MimeTypes.AUDIO_DTS_HD || mimeLower.contains("dts.hd") -> "DTS-HD"
            mimeLower == MimeTypes.AUDIO_DTS || mimeLower.endsWith(".dts") -> "DTS"
            mimeLower == MimeTypes.AUDIO_RAW || mimeLower.contains("pcm") -> "PCM"
            mimeLower == MimeTypes.AUDIO_ALAC || mimeLower.contains("alac") -> "ALAC"
            mimeLower == MimeTypes.AUDIO_WAV || mimeLower.contains("wav") -> "WAV"
            mimeLower.startsWith("audio/") -> {
                mimeLower.removePrefix("audio/").replace("x-", "").uppercase(Locale.US)
            }
            codecLower.isNotEmpty() -> {
                when {
                    codecLower.contains("aac") -> "AAC"
                    codecLower.contains("ec-3") || codecLower.contains("eac3") -> "Dolby Digital Plus (E-AC-3)"
                    codecLower.contains("ac-3") || codecLower.contains("ac3") -> "Dolby Digital (AC-3)"
                    codecLower.contains("truehd") -> "Dolby TrueHD"
                    codecLower.contains("dts") -> "DTS"
                    codecLower.contains("flac") -> "FLAC"
                    else -> codecLower.uppercase(Locale.US)
                }
            }
            else -> UNKNOWN_CODEC
        }
    }

    /**
     * Compact codec abbreviation for OSD pills and technical badges.
     */
    fun formatShortAudioCodec(mimeType: String?, codecs: String?): String {
        val mimeLower = mimeType?.lowercase(Locale.US)?.trim() ?: ""
        val codecLower = codecs?.lowercase(Locale.US)?.trim() ?: ""

        return when {
            mimeLower.contains("aac") || mimeLower == MimeTypes.AUDIO_AAC -> "AAC"
            mimeLower == MimeTypes.AUDIO_E_AC3 || mimeLower.contains("eac3") -> {
                if (codecLower.contains("joc") || mimeLower.contains("joc")) "E-AC-3 Atmos" else "E-AC-3"
            }
            mimeLower == MimeTypes.AUDIO_AC3 || mimeLower.contains("ac3") -> "AC-3"
            mimeLower == MimeTypes.AUDIO_TRUEHD || mimeLower.contains("true-hd") -> {
                if (codecLower.contains("atmos")) "TrueHD Atmos" else "TrueHD"
            }
            mimeLower == MimeTypes.AUDIO_FLAC || mimeLower.contains("flac") -> "FLAC"
            mimeLower == MimeTypes.AUDIO_MPEG || mimeLower.contains("mp3") || mimeLower.contains("mpeg") -> "MP3"
            mimeLower == MimeTypes.AUDIO_OPUS || mimeLower.contains("opus") -> "Opus"
            mimeLower == MimeTypes.AUDIO_VORBIS || mimeLower.contains("vorbis") -> "Vorbis"
            mimeLower == MimeTypes.AUDIO_DTS_X || mimeLower.contains("dts.uhd") -> "DTS:X"
            mimeLower == MimeTypes.AUDIO_DTS_EXPRESS || mimeLower.contains("profile=lbr") -> "DTS Express"
            mimeLower == MimeTypes.AUDIO_DTS_HD || mimeLower.contains("dts.hd") -> "DTS-HD"
            mimeLower == MimeTypes.AUDIO_DTS || mimeLower.endsWith(".dts") -> "DTS"
            mimeLower == MimeTypes.AUDIO_RAW || mimeLower.contains("pcm") -> "PCM"
            codecLower.isNotEmpty() -> codecLower.substringBefore('.').uppercase(Locale.US)
            mimeLower.startsWith("audio/") -> mimeLower.removePrefix("audio/").replace("x-", "").uppercase(Locale.US)
            else -> ""
        }
    }

    /**
     * Formats subtitle format tags (e.g. "SRT", "VTT", "SSA/ASS", "PGS", "VobSub").
     */
    fun formatSubtitleFormat(mimeType: String?, isExternal: Boolean = false): String {
        val mimeLower = mimeType?.lowercase(Locale.US)?.trim() ?: ""

        return when {
            mimeLower.contains("subrip") || mimeLower == MimeTypes.APPLICATION_SUBRIP -> "SRT"
            mimeLower.contains("vtt") || mimeLower == MimeTypes.TEXT_VTT -> "VTT"
            mimeLower.contains("ssa") || mimeLower.contains("ass") || mimeLower == MimeTypes.TEXT_SSA -> "SSA/ASS"
            mimeLower.contains("pgs") || mimeLower == MimeTypes.APPLICATION_PGS -> "PGS"
            mimeLower.contains("vobsub") || mimeLower == MimeTypes.APPLICATION_VOBSUB -> "VobSub"
            mimeLower.contains("ttml") || mimeLower == MimeTypes.APPLICATION_TTML -> "TTML"
            mimeLower.contains("dvbsubs") || mimeLower == MimeTypes.APPLICATION_DVBSUBS -> "DVB"
            mimeLower.isNotBlank() -> mimeLower.substringAfterLast('/').removePrefix("x-").uppercase(Locale.US)
            isExternal -> "SRT"
            else -> "Subtitles"
        }
    }

    /**
     * Composes a complete user-friendly display title for audio tracks in menus and dialogs.
     */
    fun formatAudioDisplayName(item: AudioTrackItem): String {
        val parts = mutableListOf<String>()

        val lang = item.languageName
        val hasValidLang = lang != UNKNOWN_LANGUAGE
        val custom = item.customLabel?.takeIf { !it.equals(lang, ignoreCase = true) }

        if (hasValidLang) {
            parts.add(lang)
        } else if (custom == null) {
            parts.add("Audio Track ${item.trackIndex + 1}")
        }

        if (custom != null) {
            parts.add(custom)
        }

        if (item.codecLabel != UNKNOWN_CODEC) {
            parts.add(item.codecLabel)
        }

        if (item.channelLayout.isNotBlank()) {
            parts.add(item.channelLayout)
        }

        return if (parts.isNotEmpty()) parts.joinToString(" • ") else "Audio Track ${item.trackIndex + 1}"
    }

    /**
     * Formats compact summary for the audio pill in the player OSD (e.g. "English TrueHD 7.1").
     */
    fun formatAudioOsdSummary(item: AudioTrackItem): String {
        val lang = if (item.languageName != UNKNOWN_LANGUAGE) item.languageName else "Audio"
        val codec = item.shortCodecLabel
        val channels = formatShortChannelLayout(item.format.channelCount)

        val suffix = listOf(codec, channels).filter { it.isNotBlank() }.joinToString(" ")
        return if (suffix.isNotBlank()) "$lang $suffix" else lang
    }

    /**
     * Composes a complete user-friendly display title for subtitle tracks in menus and dialogs.
     */
    fun formatSubtitleDisplayName(item: SubtitleTrackItem): String {
        if (item.isOff) return "Off"

        val lang = item.languageName
        val hasValidLang = lang != UNKNOWN_LANGUAGE
        val formatTag = item.formatTag
        val custom = item.customLabel?.takeIf { !it.equals(lang, ignoreCase = true) }

        return when {
            item.isExternal -> {
                if (hasValidLang) {
                    "$lang (External $formatTag)"
                } else {
                    "External $formatTag"
                }
            }
            custom != null -> {
                if (hasValidLang) {
                    "$lang • $custom [$formatTag]"
                } else {
                    "$custom [$formatTag]"
                }
            }
            hasValidLang -> {
                "$lang [$formatTag]"
            }
            else -> {
                "Subtitle Track ${item.trackIndex + 1} [$formatTag]"
            }
        }
    }

    /**
     * Formats compact summary for the subtitle pill in the player OSD (e.g. "English [SRT]" or "Subtitles Off").
     */
    fun formatSubtitleOsdSummary(item: SubtitleTrackItem): String {
        if (item.isOff) return "Subtitles Off"
        val lang = if (item.languageName != UNKNOWN_LANGUAGE) item.languageName else "Subtitles"
        val tag = item.formatTag

        return when {
            item.isExternal -> "$lang (External $tag)"
            tag.isNotBlank() && tag != "Subtitles" -> "$lang [$tag]"
            else -> lang
        }
    }

    /**
     * Parses all available audio tracks from Media3 [Tracks], preserving exact group and track indices.
     */
    fun parseAudioTracks(tracks: Tracks): List<AudioTrackItem> {
        val items = mutableListOf<AudioTrackItem>()
        for (groupIndex in 0 until tracks.groups.size) {
            val group = tracks.groups[groupIndex]
            if (group.type == C.TRACK_TYPE_AUDIO) {
                val mediaTrackGroup = group.mediaTrackGroup
                for (trackIndex in 0 until group.length) {
                    val format = group.getTrackFormat(trackIndex)
                    val isSelected = group.isTrackSelected(trackIndex)
                    val isSupported = group.isTrackSupported(trackIndex)
                    items.add(
                        AudioTrackItem(
                            groupIndex = groupIndex,
                            trackIndex = trackIndex,
                            format = format,
                            isSelected = isSelected,
                            isSupported = isSupported,
                            mediaTrackGroup = mediaTrackGroup
                        )
                    )
                }
            }
        }
        return items
    }

    /**
     * Parses all available text/subtitle tracks from Media3 [Tracks], optionally prepending the "Off" option.
     *
     * Correctly handles multiple track groups and tracks within groups without flattened-list index confusion.
     */
    fun parseSubtitleTracks(
        tracks: Tracks,
        includeOffOption: Boolean = true
    ): List<SubtitleTrackItem> {
        val items = mutableListOf<SubtitleTrackItem>()
        var anySubtitleSelected = false

        for (groupIndex in 0 until tracks.groups.size) {
            val group = tracks.groups[groupIndex]
            if (group.type == C.TRACK_TYPE_TEXT) {
                val mediaTrackGroup = group.mediaTrackGroup
                for (trackIndex in 0 until group.length) {
                    val format = group.getTrackFormat(trackIndex)
                    val isSelected = group.isTrackSelected(trackIndex)
                    if (isSelected) {
                        anySubtitleSelected = true
                    }
                    val isSupported = group.isTrackSupported(trackIndex)
                    val isExternal = isExternalSubtitleFormat(format)
                    items.add(
                        SubtitleTrackItem(
                            groupIndex = groupIndex,
                            trackIndex = trackIndex,
                            format = format,
                            isSelected = isSelected,
                            isSupported = isSupported,
                            mediaTrackGroup = mediaTrackGroup,
                            isExternal = isExternal
                        )
                    )
                }
            }
        }

        if (includeOffOption) {
            // "Off" is selected if no subtitle track in any group is selected
            items.add(0, SubtitleTrackItem.off(isSelected = !anySubtitleSelected))
        }

        return items
    }

    /**
     * Determines whether a subtitle track format originated from an external sidecar file.
     */
    fun isExternalSubtitleFormat(format: Format?): Boolean {
        if (format == null) return false
        val id = format.id ?: ""
        val label = format.label ?: ""
        return id.startsWith("external:", ignoreCase = true) ||
            label.contains("external", ignoreCase = true)
    }

    /**
     * Returns a new [TrackSelectionParameters] overriding audio track selection to the specified [track].
     */
    fun selectAudioTrack(
        currentParameters: TrackSelectionParameters,
        track: AudioTrackItem
    ): TrackSelectionParameters {
        val trackGroup = track.mediaTrackGroup ?: return currentParameters
        val override = TrackSelectionOverride(trackGroup, track.trackIndex)
        return currentParameters.buildUpon()
            .setOverrideForType(override)
            .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, false)
            .build()
    }

    /**
     * Returns a new [TrackSelectionParameters] overriding subtitle track selection.
     * If the track represents [SubtitleTrackItem.isOff], text tracks are disabled.
     */
    fun selectSubtitleTrack(
        currentParameters: TrackSelectionParameters,
        track: SubtitleTrackItem
    ): TrackSelectionParameters {
        if (track.isOff || track.mediaTrackGroup == null) {
            return disableSubtitles(currentParameters)
        }
        val override = TrackSelectionOverride(track.mediaTrackGroup, track.trackIndex)
        return currentParameters.buildUpon()
            .setOverrideForType(override)
            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
            .build()
    }

    /**
     * Returns a new [TrackSelectionParameters] with all subtitle/text tracks disabled.
     */
    fun disableSubtitles(
        currentParameters: TrackSelectionParameters
    ): TrackSelectionParameters {
        return currentParameters.buildUpon()
            .clearOverridesOfType(C.TRACK_TYPE_TEXT)
            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
            .build()
    }

    /**
     * Checks whether a specific track type is currently disabled in [parameters].
     */
    fun isTrackTypeDisabled(
        parameters: TrackSelectionParameters,
        trackType: Int
    ): Boolean {
        return parameters.disabledTrackTypes.contains(trackType)
    }
}
