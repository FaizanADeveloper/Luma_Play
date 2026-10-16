package com.lumaplay.tv.player.diagnostics

import androidx.media3.common.C
import androidx.media3.common.ColorInfo
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

/**
 * Classification of video/audio decoders based on recognized identifier conventions.
 */
enum class DecoderClassification {
    HARDWARE,
    SOFTWARE,
    UNKNOWN,
    UNAVAILABLE
}

/**
 * Immutable state model for real-time video playback telemetry and diagnostics.
 * Adheres strictly to LumaPlay playback truthfulness rules (no fabricated metrics).
 */
data class VideoTelemetryState(
    val videoDecoderName: String? = null,
    val audioDecoderName: String? = null,
    val videoWidth: Int = 0,
    val videoHeight: Int = 0,
    val frameRate: Float = -1f,
    val droppedFrames: Int = 0,
    val colorInfo: ColorInfo? = null,
    val audioMimeType: String? = null,
    val audioChannelCount: Int = 0,
    val audioSampleRate: Int = 0,
    val bufferedDurationMs: Long = 0L,
    val totalDurationMs: Long = 0L
) {
    val decoderClassification: DecoderClassification
        get() = classifyDecoder(videoDecoderName)

    val formattedVideoDecoder: String
        get() = when (decoderClassification) {
            DecoderClassification.HARDWARE -> "${videoDecoderName} (HW)"
            DecoderClassification.SOFTWARE -> "${videoDecoderName} (SW)"
            DecoderClassification.UNKNOWN -> videoDecoderName ?: "Unavailable"
            DecoderClassification.UNAVAILABLE -> "Unavailable"
        }

    val audioDecoderClassification: DecoderClassification
        get() = classifyDecoder(audioDecoderName)

    val formattedAudioDecoder: String
        get() = when (audioDecoderClassification) {
            DecoderClassification.HARDWARE -> "${audioDecoderName} (HW)"
            DecoderClassification.SOFTWARE -> "${audioDecoderName} (SW)"
            DecoderClassification.UNKNOWN -> audioDecoderName ?: "Unavailable"
            DecoderClassification.UNAVAILABLE -> "Unavailable"
        }

    val formattedResolutionAndFps: String
        get() {
            if (videoWidth <= 0 || videoHeight <= 0) return "Unavailable"
            val fpsString = formatFps(frameRate)
            return "${videoWidth}×${videoHeight} @ $fpsString"
        }

    val formattedDroppedFrames: String
        get() = "$droppedFrames dropped"

    val formattedColorInfo: String
        get() = formatColorInfo(colorInfo)

    val formattedAudioFormat: String
        get() = formatAudioInfo(audioMimeType, audioChannelCount, audioSampleRate)

    val formattedBufferAhead: String
        get() = if (bufferedDurationMs > 0) "${bufferedDurationMs} ms" else "0 ms"

    companion object {
        val EMPTY = VideoTelemetryState()

        fun formatFps(frameRate: Float): String {
            if (frameRate.isNaN() || frameRate.isInfinite() || frameRate <= 0f) {
                return "Unknown fps"
            }
            val df = DecimalFormat("0.###", DecimalFormatSymbols(Locale.US))
            return "${df.format(frameRate)} fps"
        }

        fun classifyDecoder(name: String?): DecoderClassification {
            if (name.isNullOrBlank()) return DecoderClassification.UNAVAILABLE
            val lower = name.lowercase(Locale.US)
            // Confidently recognized software decoders
            if (lower.startsWith("c2.android.") ||
                lower.startsWith("omx.google.") ||
                lower.startsWith("omx.ffmpeg.") ||
                lower.contains(".sw.") ||
                lower.contains("sw_codec")
            ) {
                return DecoderClassification.SOFTWARE
            }
            // Confidently recognized hardware decoder vendor/HAL prefixes
            if (lower.startsWith("c2.qti.") ||
                lower.startsWith("c2.mtk.") ||
                lower.startsWith("c2.exynos.") ||
                lower.startsWith("c2.goldfish.") ||
                lower.startsWith("c2.ranchu.") ||
                lower.startsWith("omx.qcom.") ||
                lower.startsWith("omx.mtk.") ||
                lower.startsWith("omx.nvidia.") ||
                lower.startsWith("omx.intel.") ||
                lower.startsWith("omx.exynos.") ||
                lower.startsWith("omx.amlogic.") ||
                lower.startsWith("omx.realtek.")
            ) {
                return DecoderClassification.HARDWARE
            }
            return DecoderClassification.UNKNOWN
        }

        fun formatColorInfo(colorInfo: ColorInfo?): String {
            if (colorInfo == null) return "Unavailable"
            val transfer = when (colorInfo.colorTransfer) {
                C.COLOR_TRANSFER_ST2084 -> "HDR10 (PQ)"
                C.COLOR_TRANSFER_HLG -> "HLG"
                C.COLOR_TRANSFER_SDR -> "SDR"
                else -> null
            }
            val space = when (colorInfo.colorSpace) {
                C.COLOR_SPACE_BT2020 -> "BT.2020"
                C.COLOR_SPACE_BT709 -> "BT.709"
                C.COLOR_SPACE_BT601 -> "BT.601"
                else -> null
            }
            return when {
                transfer != null && space != null -> "$transfer • $space"
                transfer != null -> transfer
                space != null -> space
                else -> "Unavailable"
            }
        }

        fun formatAudioInfo(mimeType: String?, channels: Int, sampleRate: Int): String {
            if (mimeType.isNullOrBlank()) return "Unavailable"
            val codecName = when {
                mimeType.contains("eac3-joc", ignoreCase = true) -> "Dolby Digital Plus with Atmos"
                mimeType.contains("eac3", ignoreCase = true) -> "Dolby Digital Plus (E-AC-3)"
                mimeType.contains("ac3", ignoreCase = true) -> "Dolby Digital (AC-3)"
                mimeType.contains("true-hd", ignoreCase = true) -> "Dolby TrueHD"
                mimeType.contains("dts-hd", ignoreCase = true) -> "DTS-HD"
                mimeType.contains("dts", ignoreCase = true) -> "DTS"
                mimeType.contains("mp4a-latm", ignoreCase = true) || mimeType.contains("aac", ignoreCase = true) -> "AAC"
                mimeType.contains("flac", ignoreCase = true) -> "FLAC"
                mimeType.contains("raw", ignoreCase = true) || mimeType.contains("pcm", ignoreCase = true) -> "PCM"
                mimeType.contains("opus", ignoreCase = true) -> "Opus"
                mimeType.contains("vorbis", ignoreCase = true) -> "Vorbis"
                else -> mimeType.substringAfterLast('/')
            }
            val channelString = when (channels) {
                1 -> "Mono"
                2 -> "Stereo"
                6 -> "5.1"
                8 -> "7.1"
                in 3..5, 7 -> "$channels ch"
                else -> ""
            }
            val sampleRateString = if (sampleRate > 0) {
                String.format(Locale.US, "%.1f kHz", sampleRate / 1000f)
            } else ""

            val details = listOf(channelString, sampleRateString).filter { it.isNotBlank() }.joinToString(" • ")
            return if (details.isNotBlank()) "$codecName ($details)" else codecName
        }
    }
}
