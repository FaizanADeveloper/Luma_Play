package com.lumaplay.tv.data.util

import android.content.ContentResolver
import android.content.Context
import android.media.MediaExtractor
import android.media.MediaFormat
import android.os.ParcelFileDescriptor
import android.util.Log
import com.lumaplay.tv.data.model.SubtitleTrackSpec
import com.lumaplay.tv.data.model.TechnicalMediaSpecs
import com.lumaplay.tv.data.model.VideoItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.util.Locale

/**
 * Asynchronous, non-blocking technical media metadata extractor.
 *
 * Uses Android framework's [MediaExtractor] and [MediaFormat] to extract genuine
 * stream specifications from container headers. Never fabricates values.
 */
object MediaSpecsExtractor {

    private const val TAG = "MediaSpecsExtractor"

    suspend fun extractSpecs(context: Context, video: VideoItem): TechnicalMediaSpecs =
        withContext(Dispatchers.IO) {
            runCatching {
                val extractor = MediaExtractor()
                try {
                    var dataSourceSet = false

                    // 1. Attempt ContentResolver openFileDescriptor for content:// URIs
                    if (video.contentUri.scheme == ContentResolver.SCHEME_CONTENT) {
                        try {
                            context.contentResolver.openFileDescriptor(video.contentUri, "r")?.use { pfd ->
                                extractor.setDataSource(pfd.fileDescriptor)
                                dataSourceSet = true
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "ContentResolver PFD failed for ${video.cleanTitle}: ${e.message}")
                        }
                    }

                    // 2. Direct file path fallback (for file:// URIs, direct filesystem discoveries, or pending MediaStore rows)
                    if (!dataSourceSet) {
                        val path = video.filePath
                            ?: (if (video.contentUri.scheme == "file") video.contentUri.path else null)
                        if (path != null) {
                            val file = File(path)
                            if (file.exists() && file.canRead()) {
                                extractor.setDataSource(file.absolutePath)
                                dataSourceSet = true
                            }
                        }
                    }

                    if (!dataSourceSet) {
                        return@withContext TechnicalMediaSpecs(
                            isExtracted = true,
                            extractionError = "Cannot open media data source"
                        )
                    }

                    val specs = parseExtractorTracks(extractor, video)
                    if (specs.frameRate == null || specs.frameRate <= 0f) {
                        val ebmlFps = extractEbmlDefaultDurationFps(context, video)
                        if (ebmlFps != null) {
                            return@withContext specs.copy(frameRate = ebmlFps)
                        }
                    }
                    specs
                } finally {
                    extractor.release()
                }
            }.getOrElse { exception ->
                Log.w(TAG, "Failed to extract deep specs for ${video.cleanTitle}: ${exception.message}")
                // Provide fallback with calculated average bitrate from MediaStore if available
                val calcBitrate = calculateAverageBitrate(video.sizeBytes, video.durationMs)
                TechnicalMediaSpecs(
                    calculatedAverageBitrateBps = calcBitrate,
                    isExtracted = true,
                    extractionError = exception.message
                )
            }
        }

    private fun parseExtractorTracks(
        extractor: MediaExtractor,
        video: VideoItem
    ): TechnicalMediaSpecs {
        val trackCount = extractor.trackCount

        var videoMime: String? = null
        var videoCodecName = "—"
        var videoProfile: String? = null
        var videoLevel: String? = null
        var frameRate: Float? = null
        var streamBitrate: Long? = null
        var hdrFormat = "—"
        var colorStandardStr: String? = null
        var colorTransferStr: String? = null

        var primaryAudioMime: String? = null
        var primaryAudioCodecName = "—"
        var audioChannels: Int? = null
        var audioChannelsDesc = "—"
        var audioSampleRate: Int? = null
        var audioLanguage: String? = null
        var audioTrackCount = 0
        val secondaryAudioTracks = mutableListOf<String>()

        val subtitleTracks = mutableListOf<SubtitleTrackSpec>()

        for (i in 0 until trackCount) {
            val format = extractor.getTrackFormat(i)
            val mime = format.optString(MediaFormat.KEY_MIME) ?: continue

            when {
                // --- Video Track ---
                mime.startsWith("video/") && videoMime == null -> {
                    videoMime = mime
                    videoCodecName = mapVideoMimeToCodecName(mime)

                    // Profile / Level
                    val profileInt = format.optInt(MediaFormat.KEY_PROFILE)
                    val levelInt = format.optInt(MediaFormat.KEY_LEVEL)
                    if (profileInt != null) {
                        videoProfile = mapProfile(mime, profileInt)
                    }
                    if (levelInt != null) {
                        videoLevel = mapLevel(levelInt)
                    }

                    // Frame rate (can be stored as Float or Int)
                    frameRate = format.optFloat(MediaFormat.KEY_FRAME_RATE)
                        ?: format.optInt(MediaFormat.KEY_FRAME_RATE)?.toFloat()

                    // Stream bitrate
                    streamBitrate = format.optLong(MediaFormat.KEY_BIT_RATE)
                        ?: format.optInt(MediaFormat.KEY_BIT_RATE)?.toLong()

                    // Color Standard & Transfer (HDR detection)
                    val colorTransfer = format.optInt(MediaFormat.KEY_COLOR_TRANSFER)
                    val colorStandard = format.optInt(MediaFormat.KEY_COLOR_STANDARD)

                    if (colorTransfer != null) {
                        colorTransferStr = mapColorTransfer(colorTransfer)
                        hdrFormat = when (colorTransfer) {
                            MediaFormat.COLOR_TRANSFER_ST2084 -> "HDR10 (PQ)"
                            MediaFormat.COLOR_TRANSFER_HLG -> "HLG"
                            MediaFormat.COLOR_TRANSFER_SDR_VIDEO -> "SDR (Rec. 709)"
                            else -> "—"
                        }
                    }
                    if (colorStandard != null) {
                        colorStandardStr = mapColorStandard(colorStandard)
                    }
                }

                // --- Audio Track ---
                mime.startsWith("audio/") -> {
                    audioTrackCount++
                    val channels = format.optInt(MediaFormat.KEY_CHANNEL_COUNT)
                    val sampleRate = format.optInt(MediaFormat.KEY_SAMPLE_RATE)
                    val lang = format.optString(MediaFormat.KEY_LANGUAGE)
                    val codec = mapAudioMimeToCodecName(mime)

                    if (primaryAudioMime == null) {
                        primaryAudioMime = mime
                        primaryAudioCodecName = codec
                        audioChannels = channels
                        audioChannelsDesc = mapAudioChannels(channels)
                        audioSampleRate = sampleRate
                        audioLanguage = lang
                    } else {
                        val trackDesc = buildString {
                            append(codec)
                            if (channels != null) append(" ${mapAudioChannels(channels)}")
                            if (!lang.isNullOrBlank()) append(" ($lang)")
                        }
                        secondaryAudioTracks.add(trackDesc)
                    }
                }

                // --- Subtitle Track ---
                mime.startsWith("text/") || mime.startsWith("application/") -> {
                    val lang = format.optString(MediaFormat.KEY_LANGUAGE) ?: "Undetermined"
                    val formatName = mapSubtitleMimeToFormat(mime)
                    subtitleTracks.add(
                        SubtitleTrackSpec(
                            trackIndex = subtitleTracks.size + 1,
                            language = normalizeLanguageCode(lang),
                            formatDescription = formatName
                        )
                    )
                }
            }
        }

        // Calculate fallback average bitrate if stream bitrate was omitted
        val calcAverageBitrate = calculateAverageBitrate(video.sizeBytes, video.durationMs)

        return TechnicalMediaSpecs(
            videoMimeType = videoMime,
            videoCodecName = videoCodecName,
            videoProfile = videoProfile,
            videoLevel = videoLevel,
            frameRate = frameRate,
            streamBitrateBps = streamBitrate,
            calculatedAverageBitrateBps = calcAverageBitrate,
            hdrFormat = hdrFormat,
            colorStandard = colorStandardStr,
            colorTransfer = colorTransferStr,
            primaryAudioMimeType = primaryAudioMime,
            primaryAudioCodecName = primaryAudioCodecName,
            audioChannelCount = audioChannels,
            audioChannelsDescription = audioChannelsDesc,
            audioSampleRateHz = audioSampleRate,
            audioLanguage = audioLanguage?.let { normalizeLanguageCode(it) },
            audioTrackCount = audioTrackCount,
            secondaryAudioSummary = if (secondaryAudioTracks.isNotEmpty()) {
                secondaryAudioTracks.joinToString(" • ")
            } else null,
            subtitleTrackCount = subtitleTracks.size,
            subtitleTracks = subtitleTracks,
            isExtracted = true
        )
    }

    private fun calculateAverageBitrate(sizeBytes: Long, durationMs: Long): Long? {
        if (sizeBytes <= 0 || durationMs <= 0) return null
        val durationSec = durationMs / 1000.0
        return ((sizeBytes * 8) / durationSec).toLong()
    }

    private fun mapVideoMimeToCodecName(mime: String): String = when (mime.lowercase(Locale.US)) {
        "video/hevc" -> "HEVC / H.265"
        "video/avc" -> "AVC / H.264"
        "video/x-vnd.on2.vp9" -> "VP9"
        "video/x-vnd.on2.vp8" -> "VP8"
        "video/av01" -> "AV1"
        "video/mp4v-es" -> "MPEG-4"
        "video/3gpp" -> "H.263"
        else -> mime.removePrefix("video/").uppercase(Locale.US)
    }

    private fun mapAudioMimeToCodecName(mime: String): String = when (mime.lowercase(Locale.US)) {
        "audio/mp4a-latm" -> "AAC"
        "audio/ac3" -> "Dolby Digital (AC-3)"
        "audio/eac3" -> "Dolby Digital Plus (E-AC-3)"
        "audio/eac3-joc" -> "Dolby Digital Plus (E-AC-3 JOC)"
        "audio/true-hd" -> "Dolby TrueHD"
        "audio/vnd.dts" -> "DTS"
        "audio/vnd.dts.hd" -> "DTS-HD"
        "audio/flac" -> "FLAC"
        "audio/opus" -> "Opus"
        "audio/vorbis" -> "Vorbis"
        "audio/raw" -> "PCM"
        else -> mime.removePrefix("audio/").uppercase(Locale.US)
    }

    private fun mapSubtitleMimeToFormat(mime: String): String = when (mime.lowercase(Locale.US)) {
        "text/vtt" -> "WebVTT"
        "application/x-subrip", "text/x-subrip" -> "SRT (Text)"
        "application/pgs" -> "PGS (Graphic)"
        "text/x-ssa" -> "SSA"
        "text/x-ass" -> "ASS"
        else -> mime.substringAfterLast('/').uppercase(Locale.US)
    }

    private fun mapAudioChannels(channels: Int?): String = when (channels) {
        1 -> "1.0ch Mono"
        2 -> "2.0ch Stereo"
        6 -> "5.1 Surround"
        8 -> "7.1 Surround"
        null -> "—"
        else -> "${channels}ch"
    }

    private fun mapColorTransfer(transfer: Int): String = when (transfer) {
        MediaFormat.COLOR_TRANSFER_ST2084 -> "SMPTE ST 2084 (PQ)"
        MediaFormat.COLOR_TRANSFER_HLG -> "ARIB STD-B67 (HLG)"
        MediaFormat.COLOR_TRANSFER_SDR_VIDEO -> "SDR (BT.709)"
        else -> "Unknown ($transfer)"
    }

    private fun mapColorStandard(standard: Int): String = when (standard) {
        MediaFormat.COLOR_STANDARD_BT2020 -> "BT.2020 Wide Gamut"
        MediaFormat.COLOR_STANDARD_BT709 -> "BT.709 Standard"
        MediaFormat.COLOR_STANDARD_BT601_PAL, MediaFormat.COLOR_STANDARD_BT601_NTSC -> "BT.601"
        else -> "Standard ($standard)"
    }

    private fun mapProfile(mime: String, profile: Int): String? = when {
        mime.equals("video/hevc", ignoreCase = true) -> when (profile) {
            1 -> "Main"
            2 -> "Main 10"
            4 -> "Main Still"
            else -> "Profile $profile"
        }
        mime.equals("video/avc", ignoreCase = true) -> when (profile) {
            1 -> "Baseline"
            2 -> "Main"
            4 -> "Extended"
            8 -> "High"
            16 -> "High 10"
            else -> "Profile $profile"
        }
        else -> null
    }

    private fun mapLevel(level: Int): String? = when (level) {
        1 -> "1.0"
        2 -> "1.1"
        4 -> "1.2"
        8 -> "1.3"
        16 -> "2.0"
        32 -> "2.1"
        64 -> "2.2"
        128 -> "3.0"
        256 -> "3.1"
        512 -> "3.2"
        1024 -> "4.0"
        2048 -> "4.1"
        4096 -> "4.2"
        8192 -> "5.0"
        16384 -> "5.1"
        32768 -> "5.2"
        65536 -> "6.0"
        131072 -> "6.1"
        262144 -> "6.2"
        else -> null
    }

    private fun normalizeLanguageCode(code: String): String = when (code.lowercase(Locale.US)) {
        "eng", "en" -> "English"
        "spa", "es" -> "Spanish"
        "fra", "fre", "fr" -> "French"
        "deu", "ger", "de" -> "German"
        "ita", "it" -> "Italian"
        "jpn", "ja" -> "Japanese"
        "zho", "chi", "zh" -> "Chinese"
        "rus", "ru" -> "Russian"
        "hin", "hi" -> "Hindi"
        "kor", "ko" -> "Korean"
        "por", "pt" -> "Portuguese"
        "ara", "ar" -> "Arabic"
        "und" -> "Undetermined"
        else -> code.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.US) else it.toString() }
    }

    // --- Safe Format Accessor Helpers ---
    private fun MediaFormat.optString(key: String): String? =
        if (containsKey(key)) runCatching { getString(key) }.getOrNull() else null

    private fun MediaFormat.optInt(key: String): Int? =
        if (containsKey(key)) runCatching { getInteger(key) }.getOrNull() else null

    private fun MediaFormat.optLong(key: String): Long? =
        if (containsKey(key)) runCatching { getLong(key) }.getOrNull() else null

    private fun MediaFormat.optFloat(key: String): Float? =
        if (containsKey(key)) runCatching { getFloat(key) }.getOrNull() else null

    // =========================================================================
    // Bounded Matroska / WebM EBML Parser (DefaultDuration fallback)
    // =========================================================================

    private const val EBML_ID_HEADER = 0x1A45DFA3L
    private const val EBML_ID_SEGMENT = 0x18538067L
    private const val EBML_ID_TRACKS = 0x1654AE6BL
    private const val EBML_ID_TRACK_ENTRY = 0xAEL
    private const val EBML_ID_TRACK_TYPE = 0x83L
    private const val EBML_ID_DEFAULT_DURATION = 0x23E383L
    private const val EBML_ID_CLUSTER = 0x1F43B675L

    /**
     * Fallback for Matroska/WebM containers where Android framework MediaExtractor
     * does not populate [MediaFormat.KEY_FRAME_RATE].
     *
     * Parses standard EBML element IDs and variable-length integer sizes to locate
     * TrackEntry's DefaultDuration (0x23E383) element for video tracks (TrackType = 1).
     *
     * Calculates FPS as 1_000_000_000f / defaultDurationNs.
     * Validates that the FPS is positive and in a plausible range (10..240 fps).
     * Strictly bounded to 8MB max read to avoid scanning huge files or stalling IO.
     */
    private fun extractEbmlDefaultDurationFps(context: Context, video: VideoItem): Float? {
        var pfd: ParcelFileDescriptor? = null
        var inputStream: InputStream? = null
        return try {
            if (video.contentUri.scheme == ContentResolver.SCHEME_CONTENT) {
                try {
                    pfd = context.contentResolver.openFileDescriptor(video.contentUri, "r")
                    if (pfd != null) {
                        inputStream = FileInputStream(pfd.fileDescriptor)
                    }
                } catch (e: Exception) {
                    Log.d(TAG, "ContentResolver PFD open failed for EBML: ${e.message}")
                }
            }
            if (inputStream == null) {
                val path = video.filePath
                    ?: (if (video.contentUri.scheme == "file") video.contentUri.path else null)
                if (path != null) {
                    val file = File(path)
                    if (file.exists() && file.canRead()) {
                        inputStream = FileInputStream(file)
                    }
                }
            }
            if (inputStream == null) return null

            val boundedStream = BoundedCountingInputStream(
                inner = BufferedInputStream(inputStream, 8192),
                maxBytes = 8 * 1024 * 1024L
            )
            parseEbmlFps(boundedStream)
        } catch (e: Exception) {
            Log.d(TAG, "EBML parsing skipped or failed for ${video.cleanTitle}: ${e.message}")
            null
        } finally {
            try { inputStream?.close() } catch (_: Exception) {}
            try { pfd?.close() } catch (_: Exception) {}
        }
    }

    private fun parseEbmlFps(stream: BoundedCountingInputStream): Float? {
        // 1. Verify 4-byte EBML Header magic: 0x1A 0x45 0xDF 0xA3
        val headerId = readElementId(stream) ?: return null
        if (headerId != EBML_ID_HEADER) return null

        val headerSize = readElementSize(stream) ?: return null
        if (headerSize > 0) {
            if (!skipBytes(stream, headerSize)) return null
        }

        // 2. Expect Segment master element (0x18538067)
        val segmentId = readElementId(stream) ?: return null
        if (segmentId != EBML_ID_SEGMENT) return null

        val segmentSize = readElementSize(stream) ?: return null
        // Note: segmentSize may be -1 (unknown size), which is common in Matroska/WebM

        // 3. Scan Segment children until Tracks element (0x1654AE6B) or Cluster (0x1F43B675)
        while (true) {
            val childId = readElementId(stream) ?: break
            val childSize = readElementSize(stream) ?: break

            when (childId) {
                EBML_ID_TRACKS -> {
                    return parseTracksElement(stream, childSize)
                }
                EBML_ID_CLUSTER -> {
                    // Cluster indicates media data start; no more header tracks
                    break
                }
                else -> {
                    if (childSize > 0) {
                        if (!skipBytes(stream, childSize)) break
                    } else if (childSize < 0) {
                        break
                    }
                }
            }
        }
        return null
    }

    private fun parseTracksElement(stream: BoundedCountingInputStream, tracksSize: Long): Float? {
        val endPos = if (tracksSize > 0) stream.bytesRead + tracksSize else Long.MAX_VALUE
        while (stream.bytesRead < endPos) {
            val trackElemId = readElementId(stream) ?: break
            val trackElemSize = readElementSize(stream) ?: break

            if (trackElemId == EBML_ID_TRACK_ENTRY) {
                val entryFps = parseTrackEntry(stream, trackElemSize)
                if (entryFps != null) return entryFps
            } else {
                if (trackElemSize > 0) {
                    if (!skipBytes(stream, trackElemSize)) break
                }
            }
        }
        return null
    }

    private fun parseTrackEntry(stream: BoundedCountingInputStream, entrySize: Long): Float? {
        val entryEndPos = if (entrySize > 0) stream.bytesRead + entrySize else Long.MAX_VALUE
        var trackType: Long? = null
        var defaultDurationNs: Long? = null

        while (stream.bytesRead < entryEndPos) {
            val elemId = readElementId(stream) ?: break
            val elemSize = readElementSize(stream) ?: break

            when (elemId) {
                EBML_ID_TRACK_TYPE -> {
                    if (elemSize in 1..8) {
                        trackType = readUnsignedInt(stream, elemSize.toInt())
                    } else if (elemSize > 0) {
                        skipBytes(stream, elemSize)
                    }
                }
                EBML_ID_DEFAULT_DURATION -> {
                    if (elemSize in 1..8) {
                        defaultDurationNs = readUnsignedInt(stream, elemSize.toInt())
                    } else if (elemSize > 0) {
                        skipBytes(stream, elemSize)
                    }
                }
                else -> {
                    if (elemSize > 0) {
                        if (!skipBytes(stream, elemSize)) break
                    }
                }
            }

            // If we have confirmed this is a video track (TrackType = 1) and have DefaultDuration
            if (trackType == 1L && defaultDurationNs != null && defaultDurationNs > 0) {
                val fps = 1_000_000_000.0 / defaultDurationNs.toDouble()
                if (fps in 10.0..240.0) {
                    val remaining = entryEndPos - stream.bytesRead
                    if (remaining > 0) skipBytes(stream, remaining)
                    return fps.toFloat()
                }
            }
        }

        // Secondary check at end of TrackEntry (if TrackType came after DefaultDuration)
        if (trackType == 1L && defaultDurationNs != null && defaultDurationNs > 0) {
            val fps = 1_000_000_000.0 / defaultDurationNs.toDouble()
            if (fps in 10.0..240.0) {
                return fps.toFloat()
            }
        }

        return null
    }

    private fun readElementId(input: InputStream): Long? {
        val b0 = input.read()
        if (b0 == -1) return null

        val length = when {
            (b0 and 0x80) != 0 -> 1
            (b0 and 0x40) != 0 -> 2
            (b0 and 0x20) != 0 -> 3
            (b0 and 0x10) != 0 -> 4
            else -> return null
        }

        var id = (b0 and 0xFF).toLong()
        for (i in 1 until length) {
            val next = input.read()
            if (next == -1) return null
            id = (id shl 8) or (next and 0xFF).toLong()
        }
        return id
    }

    private fun readElementSize(input: InputStream): Long? {
        val b0 = input.read()
        if (b0 == -1) return null

        var length = 0
        var mask = 0
        for (i in 0..7) {
            val bit = 1 shl (7 - i)
            if ((b0 and bit) != 0) {
                length = i + 1
                mask = bit - 1
                break
            }
        }
        if (length == 0) return null

        var size = (b0 and mask).toLong()
        for (i in 1 until length) {
            val next = input.read()
            if (next == -1) return null
            size = (size shl 8) or (next and 0xFF).toLong()
        }

        val totalDataBits = (8 - length) + (length - 1) * 8
        val unknownMarker = if (totalDataBits >= 63) -1L else (1L shl totalDataBits) - 1L
        if (size == unknownMarker) {
            return -1L
        }

        return size
    }

    private fun readUnsignedInt(input: InputStream, size: Int): Long? {
        if (size <= 0 || size > 8) return null
        var value = 0L
        for (i in 0 until size) {
            val b = input.read()
            if (b == -1) return null
            value = (value shl 8) or (b and 0xFF).toLong()
        }
        return value
    }

    private fun skipBytes(input: InputStream, count: Long): Boolean {
        if (count <= 0) return true
        var remaining = count
        while (remaining > 0) {
            val skipped = input.skip(remaining)
            if (skipped <= 0) {
                if (input.read() == -1) return false
                remaining--
            } else {
                remaining -= skipped
            }
        }
        return true
    }

    private class BoundedCountingInputStream(
        private val inner: InputStream,
        private val maxBytes: Long
    ) : InputStream() {
        var bytesRead: Long = 0
            private set

        override fun read(): Int {
            if (bytesRead >= maxBytes) return -1
            val b = inner.read()
            if (b != -1) bytesRead++
            return b
        }

        override fun read(b: ByteArray, off: Int, len: Int): Int {
            if (bytesRead >= maxBytes) return -1
            val toRead = minOf(len.toLong(), maxBytes - bytesRead).toInt()
            val read = inner.read(b, off, toRead)
            if (read != -1) bytesRead += read
            return read
        }

        override fun skip(n: Long): Long {
            if (bytesRead >= maxBytes || n <= 0) return 0
            val toSkip = minOf(n, maxBytes - bytesRead)
            val skipped = inner.skip(toSkip)
            if (skipped > 0) bytesRead += skipped
            return skipped
        }

        override fun close() {
            inner.close()
        }
    }
}
