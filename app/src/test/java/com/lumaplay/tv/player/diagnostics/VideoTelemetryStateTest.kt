package com.lumaplay.tv.player.diagnostics

import androidx.media3.common.C
import androidx.media3.common.ColorInfo
import org.junit.Assert.assertEquals
import org.junit.Test

class VideoTelemetryStateTest {

    @Test
    fun testSoftwareDecoderClassification() {
        assertEquals(DecoderClassification.SOFTWARE, VideoTelemetryState.classifyDecoder("c2.android.avc.decoder"))
        assertEquals(DecoderClassification.SOFTWARE, VideoTelemetryState.classifyDecoder("omx.google.h264.decoder"))
        assertEquals(DecoderClassification.SOFTWARE, VideoTelemetryState.classifyDecoder("omx.ffmpeg.hevc.decoder"))
        assertEquals(DecoderClassification.SOFTWARE, VideoTelemetryState.classifyDecoder("c2.vda.sw.decoder"))
        assertEquals(DecoderClassification.SOFTWARE, VideoTelemetryState.classifyDecoder("libstagefright.sw_codec"))
    }

    @Test
    fun testHardwareDecoderClassification() {
        assertEquals(DecoderClassification.HARDWARE, VideoTelemetryState.classifyDecoder("c2.goldfish.hevc.decoder"))
        assertEquals(DecoderClassification.HARDWARE, VideoTelemetryState.classifyDecoder("c2.qti.avc.decoder"))
        assertEquals(DecoderClassification.HARDWARE, VideoTelemetryState.classifyDecoder("c2.mtk.hevc.decoder"))
        assertEquals(DecoderClassification.HARDWARE, VideoTelemetryState.classifyDecoder("omx.qcom.video.decoder.avc"))
        assertEquals(DecoderClassification.HARDWARE, VideoTelemetryState.classifyDecoder("omx.nvidia.h264.decode"))
        assertEquals(DecoderClassification.HARDWARE, VideoTelemetryState.classifyDecoder("omx.intel.video.decoder.avc"))
        assertEquals(DecoderClassification.HARDWARE, VideoTelemetryState.classifyDecoder("omx.amlogic.video.decoder.hevc"))
    }

    @Test
    fun testUnknownDecoderClassificationDoesNotAssumeHardware() {
        val unknownName = "custom.vendor.decoder.special"
        assertEquals(DecoderClassification.UNKNOWN, VideoTelemetryState.classifyDecoder(unknownName))

        val state = VideoTelemetryState(videoDecoderName = unknownName)
        assertEquals(unknownName, state.formattedVideoDecoder)
    }

    @Test
    fun testUnavailableDecoderClassification() {
        assertEquals(DecoderClassification.UNAVAILABLE, VideoTelemetryState.classifyDecoder(null))
        assertEquals(DecoderClassification.UNAVAILABLE, VideoTelemetryState.classifyDecoder(""))
        assertEquals(DecoderClassification.UNAVAILABLE, VideoTelemetryState.classifyDecoder("   "))

        val state = VideoTelemetryState(videoDecoderName = null)
        assertEquals("Unavailable", state.formattedVideoDecoder)
    }

    @Test
    fun testColorInfoFormattingHdr10() {
        val colorInfo = ColorInfo.Builder()
            .setColorTransfer(C.COLOR_TRANSFER_ST2084)
            .setColorSpace(C.COLOR_SPACE_BT2020)
            .build()

        val state = VideoTelemetryState(colorInfo = colorInfo)
        assertEquals("HDR10 (PQ) • BT.2020", state.formattedColorInfo)
    }

    @Test
    fun testColorInfoFormattingHlg() {
        val colorInfo = ColorInfo.Builder()
            .setColorTransfer(C.COLOR_TRANSFER_HLG)
            .setColorSpace(C.COLOR_SPACE_BT2020)
            .build()

        val state = VideoTelemetryState(colorInfo = colorInfo)
        assertEquals("HLG • BT.2020", state.formattedColorInfo)
    }

    @Test
    fun testColorInfoFormattingSdrExplicit() {
        val colorInfo = ColorInfo.Builder()
            .setColorTransfer(C.COLOR_TRANSFER_SDR)
            .setColorSpace(C.COLOR_SPACE_BT709)
            .build()

        val state = VideoTelemetryState(colorInfo = colorInfo)
        assertEquals("SDR • BT.709", state.formattedColorInfo)
    }

    @Test
    fun testColorInfoFormattingNullNeverInfersSdr() {
        val state = VideoTelemetryState(colorInfo = null)
        assertEquals("Unavailable", state.formattedColorInfo)
    }

    @Test
    fun testResolutionAndFrameRateFormatting() {
        val valid4k = VideoTelemetryState(
            videoWidth = 3840,
            videoHeight = 2160,
            frameRate = 23.976f
        )
        assertEquals("3840×2160 @ 23.976 fps", valid4k.formattedResolutionAndFps)

        val valid1080p = VideoTelemetryState(
            videoWidth = 1920,
            videoHeight = 1080,
            frameRate = 25f
        )
        assertEquals("1920×1080 @ 25 fps", valid1080p.formattedResolutionAndFps)

        val validWebm = VideoTelemetryState(
            videoWidth = 3840,
            videoHeight = 2160,
            frameRate = 59.940063f
        )
        assertEquals("3840×2160 @ 59.94 fps", validWebm.formattedResolutionAndFps)

        val unknownFps = VideoTelemetryState(
            videoWidth = 1920,
            videoHeight = 1080,
            frameRate = -1f
        )
        assertEquals("1920×1080 @ Unknown fps", unknownFps.formattedResolutionAndFps)

        val invalidDim = VideoTelemetryState(
            videoWidth = 0,
            videoHeight = 0,
            frameRate = 60f
        )
        assertEquals("Unavailable", invalidDim.formattedResolutionAndFps)

        val negativeDim = VideoTelemetryState(
            videoWidth = -1,
            videoHeight = 1080,
            frameRate = 24f
        )
        assertEquals("Unavailable", negativeDim.formattedResolutionAndFps)
    }

    @Test
    fun testFormatFpsRealisticFractional() {
        assertEquals("23.976 fps", VideoTelemetryState.formatFps(23.976f))
        assertEquals("29.97 fps", VideoTelemetryState.formatFps(29.97f))
        assertEquals("59.94 fps", VideoTelemetryState.formatFps(59.94f))
        assertEquals("59.94 fps", VideoTelemetryState.formatFps(59.940063f))
    }

    @Test
    fun testFormatFpsIntegerRates() {
        assertEquals("24 fps", VideoTelemetryState.formatFps(24f))
        assertEquals("25 fps", VideoTelemetryState.formatFps(25f))
        assertEquals("30 fps", VideoTelemetryState.formatFps(30f))
        assertEquals("50 fps", VideoTelemetryState.formatFps(50f))
        assertEquals("60 fps", VideoTelemetryState.formatFps(60f))
        assertEquals("120 fps", VideoTelemetryState.formatFps(120f))
    }

    @Test
    fun testFormatFpsUnavailableAndNegative() {
        assertEquals("Unknown fps", VideoTelemetryState.formatFps(-1f))
        assertEquals("Unknown fps", VideoTelemetryState.formatFps(0f))
        assertEquals("Unknown fps", VideoTelemetryState.formatFps(-23.976f))
        assertEquals("Unknown fps", VideoTelemetryState.formatFps(-60f))
        assertEquals("Unknown fps", VideoTelemetryState.formatFps(Float.NaN))
        assertEquals("Unknown fps", VideoTelemetryState.formatFps(Float.POSITIVE_INFINITY))
        assertEquals("Unknown fps", VideoTelemetryState.formatFps(Float.NEGATIVE_INFINITY))
    }

    @Test
    fun testDroppedFramesFormatting() {
        val stateZero = VideoTelemetryState(droppedFrames = 0)
        assertEquals("0 dropped", stateZero.formattedDroppedFrames)

        val stateDropped = VideoTelemetryState(droppedFrames = 15)
        assertEquals("15 dropped", stateDropped.formattedDroppedFrames)
    }

    @Test
    fun testAudioFormatDescription() {
        val atmos = VideoTelemetryState(
            audioMimeType = "audio/eac3-joc",
            audioChannelCount = 8,
            audioSampleRate = 48000
        )
        assertEquals("Dolby Digital Plus with Atmos (7.1 • 48.0 kHz)", atmos.formattedAudioFormat)

        val eac3 = VideoTelemetryState(
            audioMimeType = "audio/eac3",
            audioChannelCount = 6,
            audioSampleRate = 48000
        )
        assertEquals("Dolby Digital Plus (E-AC-3) (5.1 • 48.0 kHz)", eac3.formattedAudioFormat)

        val aacStereo = VideoTelemetryState(
            audioMimeType = "audio/mp4a-latm",
            audioChannelCount = 2,
            audioSampleRate = 44100
        )
        assertEquals("AAC (Stereo • 44.1 kHz)", aacStereo.formattedAudioFormat)

        val missingAudio = VideoTelemetryState(audioMimeType = null)
        assertEquals("Unavailable", missingAudio.formattedAudioFormat)
    }

    @Test
    fun testBufferAheadFormatting() {
        val stateBuffered = VideoTelemetryState(bufferedDurationMs = 2450L)
        assertEquals("2450 ms", stateBuffered.formattedBufferAhead)

        val stateEmpty = VideoTelemetryState(bufferedDurationMs = 0L)
        assertEquals("0 ms", stateEmpty.formattedBufferAhead)
    }
}
