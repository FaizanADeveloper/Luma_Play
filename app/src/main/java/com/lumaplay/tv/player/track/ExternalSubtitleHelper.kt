package com.lumaplay.tv.player.track

import android.net.Uri
import android.util.Log
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import com.lumaplay.tv.data.model.VideoItem
import java.io.File
import java.util.Locale

/**
 * Represents a discovered external subtitle track ready for Media3 playback.
 *
 * @property file The physical subtitle file, if discovered from filesystem storage.
 * @property uri The usable playback URI for Media3 (e.g. file:// or authorized content://).
 * @property languageCode The normalized ISO/BCP-47 language tag (e.g. "en", "eng", "es"), or null if unspecified.
 * @property languageName The human-readable display name (e.g. "English", "Spanish", "Unknown Language").
 * @property label The display label for player menus and OSD (e.g. "English (External SRT)").
 * @property isForced True if this subtitle is tagged as forced narrative.
 * @property isSdh True if this subtitle is tagged for deaf or hard-of-hearing (SDH/CC).
 * @property configuration The Media3 [MediaItem.SubtitleConfiguration] ready for ExoPlayer.
 */
data class DiscoveredSubtitle(
    val file: File?,
    val uri: Uri,
    val languageCode: String?,
    val languageName: String,
    val label: String,
    val isForced: Boolean = false,
    val isSdh: Boolean = false,
    val configuration: MediaItem.SubtitleConfiguration
)

/**
 * Parsed language and modifier metadata extracted from a subtitle filename suffix.
 *
 * @property languageCode Normalized language code (e.g. "en", "eng", "es", "pt-BR"), or null if unspecified.
 * @property isForced True if the subtitle name indicates forced narrative.
 * @property isSdh True if the subtitle name indicates subtitles for deaf and hard-of-hearing.
 */
data class SubtitleSuffixInfo(
    val languageCode: String?,
    val isForced: Boolean = false,
    val isSdh: Boolean = false
)

/**
 * Pure, resilient discovery and configuration helper for external sidecar subtitles (.srt).
 *
 * Adheres to Android scoped storage constraints:
 * - Scans authorized sibling directory files when physical paths or file:// URIs are accessible.
 * - Gracefully handles opaque content:// URIs where sibling discovery is prohibited by Android security,
 *   returning empty results rather than guessing unauthorized filesystem paths.
 * - Builds fully formed Media3 [MediaItem.SubtitleConfiguration] items compatible with [TrackHelper].
 */
object ExternalSubtitleHelper {

    private const val TAG = "ExternalSubtitleHelper"
    const val SUBTITLE_EXTENSION_SRT = "srt"
    const val MIME_TYPE_SRT = MimeTypes.APPLICATION_SUBRIP

    private val KNOWN_VIDEO_EXTENSIONS = setOf(
        "mp4", "mkv", "webm", "avi", "mov", "ts", "wmv", "m4v", "flv", "3gp",
        "mpg", "mpeg", "vob", "m2ts", "ogv", "divx", "asf", "rmvb"
    )

    private val LANGUAGE_NAME_TO_CODE = mapOf(
        "english" to "en",
        "spanish" to "es",
        "french" to "fr",
        "german" to "de",
        "italian" to "it",
        "japanese" to "ja",
        "korean" to "ko",
        "chinese" to "zh",
        "russian" to "ru",
        "portuguese" to "pt",
        "hindi" to "hi",
        "arabic" to "ar",
        "bengali" to "bn",
        "punjabi" to "pa",
        "tamil" to "ta",
        "telugu" to "te",
        "marathi" to "mr",
        "urdu" to "ur",
        "turkish" to "tr",
        "vietnamese" to "vi",
        "dutch" to "nl",
        "swedish" to "sv",
        "norwegian" to "no",
        "danish" to "da",
        "finnish" to "fi",
        "greek" to "el",
        "hebrew" to "he",
        "czech" to "cs",
        "hungarian" to "hu",
        "polish" to "pl",
        "romanian" to "ro",
        "thai" to "th",
        "indonesian" to "id",
        "filipino" to "fil",
        "tagalog" to "tl"
    )

    /**
     * Extracts the clean video basename without directory path and without video extension.
     *
     * Examples:
     * - "The.Matrix.1999.1080p.mkv" -> "The.Matrix.1999.1080p"
     * - "/storage/emulated/0/Movies/sample.mp4" -> "sample"
     * - "C:\\Videos\\Dune.Part.Two.avi" -> "Dune.Part.Two"
     * - "MovieWithoutExtension" -> "MovieWithoutExtension"
     */
    fun extractBaseName(fileNameOrTitle: String?): String {
        if (fileNameOrTitle.isNullOrBlank()) return ""
        val cleanName = fileNameOrTitle.trim()
            .substringAfterLast('/')
            .substringAfterLast('\\')

        val dotIndex = cleanName.lastIndexOf('.')
        if (dotIndex <= 0) {
            return cleanName
        }

        val ext = cleanName.substring(dotIndex + 1).lowercase(Locale.US)
        return if (ext in KNOWN_VIDEO_EXTENSIONS) {
            cleanName.substring(0, dotIndex)
        } else {
            cleanName
        }
    }

    /**
     * Resolves a token into a normalized language code (e.g. "en", "eng", "es", "pt-BR").
     * Returns null if the token does not represent a recognizable language.
     */
    fun resolveLanguageCode(token: String?): String? {
        if (token.isNullOrBlank()) return null
        val clean = token.trim()
        val lower = clean.lowercase(Locale.US)

        // 1. Full language name lookup (e.g. "English" -> "en")
        LANGUAGE_NAME_TO_CODE[lower]?.let { return it }

        // 2. ISO 639-1 (2-letter)
        if (clean.length == 2 && clean.all { it.isLetter() }) {
            val langName = TrackHelper.formatLanguage(lower)
            if (langName != "Unknown Language") {
                return lower
            }
        }

        // 3. ISO 639-2 (3-letter)
        if (clean.length == 3 && clean.all { it.isLetter() }) {
            val langName = TrackHelper.formatLanguage(lower)
            if (langName != "Unknown Language") {
                return lower
            }
        }

        // 4. BCP-47 tag (e.g. "en-US", "pt-BR")
        if (clean.contains('-') || clean.contains('_')) {
            val normalizedTag = clean.replace('_', '-')
            val langName = TrackHelper.formatLanguage(normalizedTag)
            if (langName != "Unknown Language") {
                return normalizedTag
            }
        }

        return null
    }

    /**
     * Parses the suffix between [videoBaseName] and the .srt extension to extract language and modifiers.
     *
     * Returns null if [subtitleFileName] does not match [videoBaseName] or if the suffix contains
     * unrelated non-language tokens (e.g. "Matrix.Reloaded.2003.srt" for video "Matrix.mkv").
     */
    fun parseSubtitleSuffix(videoBaseName: String, subtitleFileName: String): SubtitleSuffixInfo? {
        if (videoBaseName.isBlank() || subtitleFileName.isBlank()) return null
        if (!subtitleFileName.endsWith(".$SUBTITLE_EXTENSION_SRT", ignoreCase = true)) return null

        val candBase = subtitleFileName.substring(0, subtitleFileName.length - (SUBTITLE_EXTENSION_SRT.length + 1))

        // Condition A: Exact basename match (e.g. "Movie.srt" for video "Movie.mkv")
        if (candBase.equals(videoBaseName, ignoreCase = true)) {
            return SubtitleSuffixInfo(languageCode = null, isForced = false, isSdh = false)
        }

        // Condition B: Suffix match (e.g. "Movie.en.srt", "Movie_es_forced.srt")
        if (candBase.length > videoBaseName.length && candBase.startsWith(videoBaseName, ignoreCase = true)) {
            val separator = candBase[videoBaseName.length]
            if (separator != '.' && separator != '_' && separator != '-') {
                return null
            }

            val suffix = candBase.substring(videoBaseName.length + 1)
            val tokens = suffix.split('.', '_').filter { it.isNotBlank() }
            if (tokens.isEmpty()) return null

            var detectedLanguageCode: String? = null
            var isForced = false
            var isSdh = false

            for (token in tokens) {
                val lower = token.lowercase(Locale.US)
                when (lower) {
                    "forced" -> isForced = true
                    "sdh", "cc" -> isSdh = true
                    "hi" -> {
                        if (detectedLanguageCode != null) {
                            isSdh = true
                        } else {
                            detectedLanguageCode = "hi"
                        }
                    }
                    "default" -> { /* Recognized modifier */ }
                    else -> {
                        val langCode = resolveLanguageCode(token)
                        if (langCode != null) {
                            if (detectedLanguageCode == null) {
                                detectedLanguageCode = langCode
                            }
                        } else {
                            // Unrecognized token (e.g. movie release, year, unrelated title segment)
                            return null
                        }
                    }
                }
            }

            return SubtitleSuffixInfo(
                languageCode = detectedLanguageCode,
                isForced = isForced,
                isSdh = isSdh
            )
        }

        return null
    }

    /**
     * Discovers all matching external .srt files in [parentDirectory] for the given [videoFileNameOrTitle].
     *
     * Handles missing directory, unreadable permissions, duplicate candidates, and case differences gracefully.
     */
    fun findMatchingSubtitleFiles(
        videoFileNameOrTitle: String,
        parentDirectory: File?
    ): List<File> {
        if (parentDirectory == null || videoFileNameOrTitle.isBlank()) return emptyList()

        val canAccess = runCatching {
            parentDirectory.exists() && parentDirectory.isDirectory && parentDirectory.canRead()
        }.getOrDefault(false)

        if (!canAccess) return emptyList()

        val videoBaseName = extractBaseName(videoFileNameOrTitle)
        if (videoBaseName.isBlank()) return emptyList()

        val children = runCatching { parentDirectory.listFiles() }.getOrNull() ?: return emptyList()

        val matches = mutableListOf<Pair<File, SubtitleSuffixInfo>>()

        for (child in children) {
            val isReadableFile = runCatching { child.isFile && child.canRead() }.getOrDefault(false)
            if (!isReadableFile) continue

            val suffixInfo = parseSubtitleSuffix(videoBaseName, child.name)
            if (suffixInfo != null) {
                matches.add(child to suffixInfo)
            }
        }

        // Deduplicate by canonical path to handle case-insensitive filesystems or symlinks
        val deduplicated = matches.distinctBy { (file, _) ->
            runCatching { file.canonicalPath }.getOrDefault(file.absolutePath.lowercase(Locale.US))
        }

        // Sort: Exact match first, then alphabetically by language/modifier, then by filename
        val sorted = deduplicated.sortedWith(
            compareBy<Pair<File, SubtitleSuffixInfo>> { (_, info) ->
                // Exact match (no language, not forced) comes first
                if (info.languageCode == null && !info.isForced) 0 else 1
            }.thenBy { (_, info) ->
                // Then non-forced before forced
                if (info.isForced) 1 else 0
            }.thenBy { (_, info) ->
                // Then by language display name
                TrackHelper.formatLanguage(info.languageCode)
            }.thenBy { (file, _) ->
                file.name.lowercase(Locale.US)
            }
        )

        return sorted.map { it.first }
    }

    /**
     * Discovers all matching external .srt files for the given [videoFile].
     */
    fun findMatchingSubtitleFiles(videoFile: File): List<File> {
        val parent = videoFile.parentFile ?: return emptyList()
        return findMatchingSubtitleFiles(videoFile.name, parent)
    }

    /**
     * Constructs a Media3 [MediaItem.SubtitleConfiguration] for an external .srt subtitle.
     *
     * Configures appropriate MIME type, track ID format required by [TrackHelper.isExternalSubtitleFormat],
     * display label, language code, and selection/role flags.
     */
    fun buildSubtitleConfiguration(
        uri: Uri,
        languageCode: String? = null,
        isForced: Boolean = false,
        isSdh: Boolean = false,
        customLabel: String? = null,
        id: String? = null
    ): MediaItem.SubtitleConfiguration {
        val cleanLang = languageCode?.trim()?.lowercase(Locale.US)?.takeIf {
            it.isNotBlank() && it != "und" && it != "zxx"
        }

        val resolvedId = id ?: "external:$uri"

        val resolvedLabel = if (!customLabel.isNullOrBlank()) {
            customLabel
        } else {
            val langName = TrackHelper.formatLanguage(cleanLang)
            if (langName != "Unknown Language") {
                when {
                    isForced -> "$langName • Forced (External SRT)"
                    isSdh -> "$langName • SDH (External SRT)"
                    else -> "$langName (External SRT)"
                }
            } else {
                when {
                    isForced -> "External SRT (Forced)"
                    isSdh -> "External SRT (SDH)"
                    else -> "External SRT"
                }
            }
        }

        var selectionFlags = 0
        if (isForced) {
            selectionFlags = selectionFlags or C.SELECTION_FLAG_FORCED
        }

        var roleFlags = 0
        if (isSdh) {
            roleFlags = roleFlags or C.ROLE_FLAG_DESCRIBES_MUSIC_AND_SOUND
        } else {
            roleFlags = roleFlags or C.ROLE_FLAG_SUBTITLE
        }

        return MediaItem.SubtitleConfiguration.Builder(uri)
            .setMimeType(MIME_TYPE_SRT)
            .setId(resolvedId)
            .setLanguage(cleanLang)
            .setLabel(resolvedLabel)
            .setSelectionFlags(selectionFlags)
            .setRoleFlags(roleFlags)
            .build()
    }

    /**
     * Resolves a fallback [Uri] for JVM unit test environments where unmocked android.jar
     * returns null for `Uri.fromFile(file)`.
     */
    internal fun defaultUriResolver(file: File): Uri {
        val uri = runCatching { Uri.fromFile(file) }.getOrNull()
        if (uri != null) return uri

        // JVM unit test fallback using test FakeUri double if available on classpath
        return try {
            val fakeUriClass = Class.forName("android.net.FakeUri")
            val constructor = fakeUriClass.getConstructor(String::class.java)
            constructor.newInstance("file://${file.absolutePath.replace('\\', '/')}") as Uri
        } catch (_: Throwable) {
            Uri.parse("file://${file.absolutePath.replace('\\', '/')}")
        }
    }

    /**
     * Discovers matching external .srt subtitles for a [VideoItem] using authorized storage access.
     *
     * Storage resolution order:
     * 1. [VideoItem.filePath]: Physical canonical path from MediaStore or direct crawl.
     * 2. [VideoItem.contentUri] when scheme is "file".
     * 3. Opaque content:// URIs: Sibling discovery is not supported under Android scoped storage
     *    because a single document URI does not grant parent directory listing rights.
     *    Returns [emptyList] safely without guessing filesystem paths.
     *
     * @param video The [VideoItem] for which to discover subtitles.
     * @param uriResolver Custom URI provider for testing or specialized URI generation.
     */
    fun discoverSubtitles(
        video: VideoItem,
        uriResolver: (File) -> Uri = { defaultUriResolver(it) }
    ): List<DiscoveredSubtitle> {
        val parentDir: File?
        val titleOrName: String

        when {
            !video.filePath.isNullOrBlank() -> {
                val file = File(video.filePath)
                parentDir = file.parentFile
                titleOrName = if (video.title.isNotBlank()) video.title else file.name
            }
            video.contentUri.scheme == "file" || video.contentUri.toString().startsWith("file:", ignoreCase = true) -> {
                val uriStr = video.contentUri.toString()
                val path = if (video.contentUri.scheme == "file" && !video.contentUri.path.isNullOrBlank()) {
                    video.contentUri.path
                } else {
                    uriStr.removePrefix("file://").removePrefix("file:")
                }
                if (!path.isNullOrBlank()) {
                    val file = File(path)
                    parentDir = file.parentFile
                    titleOrName = if (video.title.isNotBlank()) video.title else file.name
                } else {
                    return emptyList()
                }
            }
            else -> {
                // Scoped storage limitation: Opaque content:// URIs do not permit directory enumeration
                // of sibling non-media files without SAF document tree authorization.
                runCatching {
                    Log.d(TAG, "Cannot crawl siblings for opaque content URI without filePath: ${video.contentUri}")
                }
                return emptyList()
            }
        }

        if (parentDir == null) return emptyList()

        return discoverSubtitles(
            videoBaseName = extractBaseName(titleOrName),
            parentDirectory = parentDir,
            uriResolver = uriResolver
        )
    }

    /**
     * Discovers matching subtitles in [parentDirectory] for [videoBaseName].
     */
    fun discoverSubtitles(
        videoBaseName: String,
        parentDirectory: File?,
        uriResolver: (File) -> Uri = { defaultUriResolver(it) }
    ): List<DiscoveredSubtitle> {
        if (videoBaseName.isBlank() || parentDirectory == null) return emptyList()

        val matchingFiles = findMatchingSubtitleFiles(videoBaseName, parentDirectory)
        if (matchingFiles.isEmpty()) return emptyList()

        val results = mutableListOf<DiscoveredSubtitle>()

        for (file in matchingFiles) {
            val suffixInfo = parseSubtitleSuffix(videoBaseName, file.name) ?: continue
            val uri = runCatching { uriResolver(file) }.getOrNull() ?: continue

            val langName = TrackHelper.formatLanguage(suffixInfo.languageCode)
            val config = buildSubtitleConfiguration(
                uri = uri,
                languageCode = suffixInfo.languageCode,
                isForced = suffixInfo.isForced,
                isSdh = suffixInfo.isSdh,
                id = "external:${file.name}"
            )

            results.add(
                DiscoveredSubtitle(
                    file = file,
                    uri = uri,
                    languageCode = suffixInfo.languageCode,
                    languageName = langName,
                    label = config.label?.toString() ?: "External SRT",
                    isForced = suffixInfo.isForced,
                    isSdh = suffixInfo.isSdh,
                    configuration = config
                )
            )
        }

        return results.distinctBy { it.uri.toString() }
    }

    /**
     * Discovers matching subtitles and returns pure Media3 [MediaItem.SubtitleConfiguration] objects.
     */
    fun discoverSubtitleConfigurations(
        video: VideoItem,
        uriResolver: (File) -> Uri = { defaultUriResolver(it) }
    ): List<MediaItem.SubtitleConfiguration> {
        return discoverSubtitles(video, uriResolver).map { it.configuration }
    }
}
