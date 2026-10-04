package com.lumaplay.tv.data.model

import java.util.Locale

/**
 * Detailed technical stream specifications extracted from local media container headers.
 *
 * All values represent verified, authentic stream metadata extracted via [android.media.MediaExtractor].
 * Any value not present in the media container is represented as null or an honest neutral state ("—").
 */
data class TechnicalMediaSpecs(
    // --- Video Stream Telemetry ---
    val videoMimeType: String? = null,
    val videoCodecName: String = "—",
    val videoProfile: String? = null,
    val videoLevel: String? = null,
    val frameRate: Float? = null,
    val streamBitrateBps: Long? = null,
    val calculatedAverageBitrateBps: Long? = null,
    val hdrFormat: String = "—",
    val colorStandard: String? = null,
    val colorTransfer: String? = null,

    // --- Audio Stream Telemetry ---
    val primaryAudioMimeType: String? = null,
    val primaryAudioCodecName: String = "—",
    val audioChannelCount: Int? = null,
    val audioChannelsDescription: String = "—",
    val audioSampleRateHz: Int? = null,
    val audioLanguage: String? = null,
    val audioTrackCount: Int = 0,
    val secondaryAudioSummary: String? = null,

    // --- Subtitle Streams ---
    val subtitleTrackCount: Int = 0,
    val subtitleTracks: List<SubtitleTrackSpec> = emptyList(),

    // --- Extraction Status ---
    val isExtracted: Boolean = false,
    val extractionError: String? = null
) {
    /**
     * Formatted string representing codec and profile (e.g. "HEVC / H.265 (Main 10)").
     */
    val formattedVideoCodecAndProfile: String
        get() {
            if (videoCodecName == "—") return "—"
            return when {
                !videoProfile.isNullOrBlank() && !videoLevel.isNullOrBlank() ->
                    "$videoCodecName ($videoProfile, L$videoLevel)"
                !videoProfile.isNullOrBlank() ->
                    "$videoCodecName ($videoProfile)"
                else -> videoCodecName
            }
        }

    /**
     * Formatted frame rate (e.g. "23.976 fps" or "60.0 fps").
     */
    val formattedFrameRate: String
        get() = when {
            frameRate != null && frameRate > 0f -> {
                if (frameRate % 1f == 0f) {
                    String.format(Locale.US, "%.0f fps", frameRate)
                } else {
                    String.format(Locale.US, "%.3f fps", frameRate)
                }
            }
            else -> "—"
        }

    /**
     * Bitrate display with clear truthfulness distinction between
     * stream bitrate and calculated average file bitrate.
     */
    val formattedBitrate: String
        get() = when {
            streamBitrateBps != null && streamBitrateBps > 0L -> {
                val mbps = streamBitrateBps / 1_000_000.0
                String.format(Locale.US, "%.1f Mbps (Stream bitrate)", mbps)
            }
            calculatedAverageBitrateBps != null && calculatedAverageBitrateBps > 0L -> {
                val mbps = calculatedAverageBitrateBps / 1_000_000.0
                String.format(Locale.US, "%.1f Mbps (Average file bitrate)", mbps)
            }
            else -> "—"
        }

    /**
     * Formatted audio sampling and bit depth (e.g. "48 kHz" or "44.1 kHz").
     */
    val formattedAudioSampling: String
        get() = when {
            audioSampleRateHz != null && audioSampleRateHz > 0 -> {
                val khz = audioSampleRateHz / 1000.0
                if (audioSampleRateHz % 1000 == 0) {
                    String.format(Locale.US, "%.0f kHz", khz)
                } else {
                    String.format(Locale.US, "%.1f kHz", khz)
                }
            }
            else -> "—"
        }

    companion object {
        val Empty = TechnicalMediaSpecs()
    }
}

/**
 * Embedded subtitle track specification.
 */
data class SubtitleTrackSpec(
    val trackIndex: Int,
    val language: String,
    val formatDescription: String
)
