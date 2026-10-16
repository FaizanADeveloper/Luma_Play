package com.lumaplay.tv.player.track

import android.net.FakeUri
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MimeTypes
import com.lumaplay.tv.data.model.VideoItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class ExternalSubtitleHelperTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    // -------------------------------------------------------------
    // 1. Basename Extraction Tests
    // -------------------------------------------------------------

    @Test
    fun testExtractBaseNameWithVariousExtensions() {
        assertEquals("The.Matrix.1999.1080p", ExternalSubtitleHelper.extractBaseName("The.Matrix.1999.1080p.mkv"))
        assertEquals("SampleVideo", ExternalSubtitleHelper.extractBaseName("SampleVideo.mp4"))
        assertEquals("Avatar.2009", ExternalSubtitleHelper.extractBaseName("Avatar.2009.avi"))
        assertEquals("Nature", ExternalSubtitleHelper.extractBaseName("Nature.webm"))
        assertEquals("HomeMovie", ExternalSubtitleHelper.extractBaseName("HomeMovie.mov"))
        assertEquals("StreamSegment", ExternalSubtitleHelper.extractBaseName("StreamSegment.ts"))
        assertEquals("LegacyVideo", ExternalSubtitleHelper.extractBaseName("LegacyVideo.wmv"))
        assertEquals("MobileVideo", ExternalSubtitleHelper.extractBaseName("MobileVideo.m4v"))
        assertEquals("FlashVideo", ExternalSubtitleHelper.extractBaseName("FlashVideo.flv"))
    }

    @Test
    fun testExtractBaseNameWithPaths() {
        assertEquals("Interstellar", ExternalSubtitleHelper.extractBaseName("/storage/emulated/0/Movies/Interstellar.mp4"))
        assertEquals("Dune.Part.Two", ExternalSubtitleHelper.extractBaseName("C:\\Videos\\4K\\Dune.Part.Two.mkv"))
    }

    @Test
    fun testExtractBaseNameEdgeCases() {
        assertEquals("MovieWithoutExtension", ExternalSubtitleHelper.extractBaseName("MovieWithoutExtension"))
        assertEquals("", ExternalSubtitleHelper.extractBaseName(null))
        assertEquals("", ExternalSubtitleHelper.extractBaseName(""))
        assertEquals("", ExternalSubtitleHelper.extractBaseName("   "))
        assertEquals(".hidden", ExternalSubtitleHelper.extractBaseName(".hidden"))
    }

    // -------------------------------------------------------------
    // 2. Language Code Resolution Tests
    // -------------------------------------------------------------

    @Test
    fun testResolveLanguageCodeIsoAndNames() {
        // ISO 639-1
        assertEquals("en", ExternalSubtitleHelper.resolveLanguageCode("en"))
        assertEquals("es", ExternalSubtitleHelper.resolveLanguageCode("es"))
        assertEquals("fr", ExternalSubtitleHelper.resolveLanguageCode("fr"))
        assertEquals("de", ExternalSubtitleHelper.resolveLanguageCode("de"))
        assertEquals("ja", ExternalSubtitleHelper.resolveLanguageCode("ja"))

        // ISO 639-2
        assertEquals("eng", ExternalSubtitleHelper.resolveLanguageCode("eng"))
        assertEquals("spa", ExternalSubtitleHelper.resolveLanguageCode("spa"))
        assertEquals("fra", ExternalSubtitleHelper.resolveLanguageCode("fra"))
        assertEquals("deu", ExternalSubtitleHelper.resolveLanguageCode("deu"))

        // Full names
        assertEquals("en", ExternalSubtitleHelper.resolveLanguageCode("english"))
        assertEquals("en", ExternalSubtitleHelper.resolveLanguageCode("English"))
        assertEquals("es", ExternalSubtitleHelper.resolveLanguageCode("Spanish"))
        assertEquals("fr", ExternalSubtitleHelper.resolveLanguageCode("French"))
        assertEquals("de", ExternalSubtitleHelper.resolveLanguageCode("German"))
        assertEquals("ja", ExternalSubtitleHelper.resolveLanguageCode("Japanese"))

        // BCP-47
        assertEquals("en-US", ExternalSubtitleHelper.resolveLanguageCode("en-US"))
        assertEquals("pt-BR", ExternalSubtitleHelper.resolveLanguageCode("pt-BR"))
        assertEquals("pt-BR", ExternalSubtitleHelper.resolveLanguageCode("pt_BR"))
    }

    @Test
    fun testResolveLanguageCodeRejectsNonLanguages() {
        assertNull(ExternalSubtitleHelper.resolveLanguageCode("reloaded"))
        assertNull(ExternalSubtitleHelper.resolveLanguageCode("1080p"))
        assertNull(ExternalSubtitleHelper.resolveLanguageCode("2024"))
        assertNull(ExternalSubtitleHelper.resolveLanguageCode("trailer"))
        assertNull(ExternalSubtitleHelper.resolveLanguageCode("xyz123"))
        assertNull(ExternalSubtitleHelper.resolveLanguageCode("und"))
        assertNull(ExternalSubtitleHelper.resolveLanguageCode(null))
        assertNull(ExternalSubtitleHelper.resolveLanguageCode(""))
    }

    // -------------------------------------------------------------
    // 3. Suffix Parsing Tests
    // -------------------------------------------------------------

    @Test
    fun testParseSubtitleSuffixExactMatch() {
        val info1 = ExternalSubtitleHelper.parseSubtitleSuffix("Movie", "Movie.srt")
        assertNotNull(info1)
        assertNull(info1?.languageCode)
        assertFalse(info1?.isForced == true)
        assertFalse(info1?.isSdh == true)

        // Case difference
        val info2 = ExternalSubtitleHelper.parseSubtitleSuffix("movie", "MOVIE.SRT")
        assertNotNull(info2)
        assertNull(info2?.languageCode)
    }

    @Test
    fun testParseSubtitleSuffixWithLanguage() {
        val infoEn = ExternalSubtitleHelper.parseSubtitleSuffix("Movie", "Movie.en.srt")
        assertNotNull(infoEn)
        assertEquals("en", infoEn?.languageCode)
        assertFalse(infoEn?.isForced == true)

        val infoSpa = ExternalSubtitleHelper.parseSubtitleSuffix("Movie", "Movie_spa.srt")
        assertNotNull(infoSpa)
        assertEquals("spa", infoSpa?.languageCode)

        val infoPtBr = ExternalSubtitleHelper.parseSubtitleSuffix("Movie", "Movie.pt-BR.srt")
        assertNotNull(infoPtBr)
        assertEquals("pt-BR", infoPtBr?.languageCode)

        val infoSpanish = ExternalSubtitleHelper.parseSubtitleSuffix("Movie", "Movie.Spanish.srt")
        assertNotNull(infoSpanish)
        assertEquals("es", infoSpanish?.languageCode)
    }

    @Test
    fun testParseSubtitleSuffixWithModifiers() {
        val infoForced = ExternalSubtitleHelper.parseSubtitleSuffix("Movie", "Movie.en.forced.srt")
        assertNotNull(infoForced)
        assertEquals("en", infoForced?.languageCode)
        assertTrue(infoForced?.isForced == true)

        val infoForcedOnly = ExternalSubtitleHelper.parseSubtitleSuffix("Movie", "Movie.forced.srt")
        assertNotNull(infoForcedOnly)
        assertNull(infoForcedOnly?.languageCode)
        assertTrue(infoForcedOnly?.isForced == true)

        val infoSdh = ExternalSubtitleHelper.parseSubtitleSuffix("Movie", "Movie.eng.sdh.srt")
        assertNotNull(infoSdh)
        assertEquals("eng", infoSdh?.languageCode)
        assertTrue(infoSdh?.isSdh == true)

        val infoCc = ExternalSubtitleHelper.parseSubtitleSuffix("Movie", "Movie.cc.srt")
        assertNotNull(infoCc)
        assertNull(infoCc?.languageCode)
        assertTrue(infoCc?.isSdh == true)
    }

    @Test
    fun testParseSubtitleSuffixRejectsUnrelatedFiles() {
        // Different movie entirely
        assertNull(ExternalSubtitleHelper.parseSubtitleSuffix("Movie", "OtherMovie.srt"))

        // Unrelated suffix segments (e.g. Sequel or trailer)
        assertNull(ExternalSubtitleHelper.parseSubtitleSuffix("Matrix", "Matrix.Reloaded.2003.srt"))
        assertNull(ExternalSubtitleHelper.parseSubtitleSuffix("Dune", "Dune.Part.Two.srt"))
        assertNull(ExternalSubtitleHelper.parseSubtitleSuffix("Movie", "Movie.Trailer.srt"))

        // Non-SRT extensions
        assertNull(ExternalSubtitleHelper.parseSubtitleSuffix("Movie", "Movie.txt"))
        assertNull(ExternalSubtitleHelper.parseSubtitleSuffix("Movie", "Movie.vtt"))
        assertNull(ExternalSubtitleHelper.parseSubtitleSuffix("Movie", "Movie.sub"))
        assertNull(ExternalSubtitleHelper.parseSubtitleSuffix("Movie", "Movie.mp4"))
    }

    // -------------------------------------------------------------
    // 4. Filesystem Subtitle Discovery Tests
    // -------------------------------------------------------------

    @Test
    fun testFindMatchingSubtitleFilesExactMatch() {
        val dir = tempFolder.newFolder("exact_match")
        val video = File(dir, "BigBuckBunny.mp4").apply { createNewFile() }
        val srtMatch = File(dir, "BigBuckBunny.srt").apply { createNewFile() }
        File(dir, "OtherMovie.srt").apply { createNewFile() }

        val matches = ExternalSubtitleHelper.findMatchingSubtitleFiles(video)
        assertEquals(1, matches.size)
        assertEquals(srtMatch.absolutePath, matches[0].absolutePath)
    }

    @Test
    fun testFindMatchingSubtitleFilesCaseInsensitive() {
        val dir = tempFolder.newFolder("case_insensitive")
        val video = File(dir, "Inception.2010.mkv").apply { createNewFile() }
        val srtLower = File(dir, "inception.2010.srt").apply { createNewFile() }

        val matches = ExternalSubtitleHelper.findMatchingSubtitleFiles(video)
        assertEquals(1, matches.size)
        assertEquals(srtLower.name.lowercase(), matches[0].name.lowercase())
    }

    @Test
    fun testFindMatchingSubtitleFilesMultipleLanguagesAndSorting() {
        val dir = tempFolder.newFolder("multi_lang")
        val video = File(dir, "Avatar.mkv").apply { createNewFile() }
        val srtDefault = File(dir, "Avatar.srt").apply { createNewFile() }
        val srtEn = File(dir, "Avatar.en.srt").apply { createNewFile() }
        val srtEs = File(dir, "Avatar.es.srt").apply { createNewFile() }
        val srtEnForced = File(dir, "Avatar.en.forced.srt").apply { createNewFile() }
        File(dir, "Avatar.Trailer.srt").apply { createNewFile() } // Should be ignored

        val matches = ExternalSubtitleHelper.findMatchingSubtitleFiles(video)
        assertEquals(4, matches.size)

        // Exact match first
        assertEquals(srtDefault.name, matches[0].name)
        // Then non-forced alphabetically: English ("Avatar.en.srt"), Spanish ("Avatar.es.srt")
        assertEquals(srtEn.name, matches[1].name)
        assertEquals(srtEs.name, matches[2].name)
        // Then forced: ("Avatar.en.forced.srt")
        assertEquals(srtEnForced.name, matches[3].name)
    }

    @Test
    fun testFindMatchingSubtitleFilesIgnoresNonSrtAndUnrelated() {
        val dir = tempFolder.newFolder("unrelated_filter")
        val video = File(dir, "Gladiator.mkv").apply { createNewFile() }

        File(dir, "Gladiator.nfo").apply { createNewFile() }
        File(dir, "Gladiator.jpg").apply { createNewFile() }
        File(dir, "Gladiator.txt").apply { createNewFile() }
        File(dir, "Gladiator.vtt").apply { createNewFile() }
        File(dir, "Gladiator.Extended.Edition.srt").apply { createNewFile() }
        File(dir, "Braveheart.srt").apply { createNewFile() }

        val matches = ExternalSubtitleHelper.findMatchingSubtitleFiles(video)
        assertTrue(matches.isEmpty())
    }

    @Test
    fun testFindMatchingSubtitleFilesNoMatchingSubtitles() {
        val dir = tempFolder.newFolder("empty_dir")
        val video = File(dir, "SoloMovie.mkv").apply { createNewFile() }

        val matches = ExternalSubtitleHelper.findMatchingSubtitleFiles(video)
        assertTrue(matches.isEmpty())
    }

    @Test
    fun testFindMatchingSubtitleFilesInvalidInputs() {
        assertTrue(ExternalSubtitleHelper.findMatchingSubtitleFiles("Movie", null).isEmpty())
        assertTrue(ExternalSubtitleHelper.findMatchingSubtitleFiles("", tempFolder.root).isEmpty())
        assertTrue(ExternalSubtitleHelper.findMatchingSubtitleFiles("   ", tempFolder.root).isEmpty())
        assertTrue(ExternalSubtitleHelper.findMatchingSubtitleFiles("Movie", File(tempFolder.root, "non_existent_dir")).isEmpty())
    }

    // -------------------------------------------------------------
    // 5. Media3 SubtitleConfiguration Construction Tests
    // -------------------------------------------------------------

    @Test
    fun testBuildSubtitleConfigurationExactMatchDefault() {
        val testUri = FakeUri("file:///storage/emulated/0/Movies/Film.srt")
        val config = ExternalSubtitleHelper.buildSubtitleConfiguration(
            uri = testUri,
            languageCode = null,
            isForced = false,
            isSdh = false
        )

        assertEquals(MimeTypes.APPLICATION_SUBRIP, config.mimeType)
        assertEquals("external:$testUri", config.id)
        assertNull(config.language)
        assertEquals("External SRT", config.label?.toString())
        assertEquals(0, config.selectionFlags)
        assertEquals(C.ROLE_FLAG_SUBTITLE, config.roleFlags)

        // Verify TrackHelper compatibility
        val mockFormat = Format.Builder()
            .setId(config.id)
            .setSampleMimeType(config.mimeType)
            .setLabel(config.label?.toString())
            .build()
        assertTrue(TrackHelper.isExternalSubtitleFormat(mockFormat))
    }

    @Test
    fun testBuildSubtitleConfigurationWithLanguage() {
        val testUri = FakeUri("file:///storage/emulated/0/Movies/Film.en.srt")
        val config = ExternalSubtitleHelper.buildSubtitleConfiguration(
            uri = testUri,
            languageCode = "en",
            isForced = false,
            isSdh = false
        )

        assertEquals(MimeTypes.APPLICATION_SUBRIP, config.mimeType)
        assertEquals("en", config.language)
        assertEquals("English (External SRT)", config.label?.toString())
        assertEquals(0, config.selectionFlags)
        assertEquals(C.ROLE_FLAG_SUBTITLE, config.roleFlags)

        // Verify TrackHelper compatibility
        val mockFormat = Format.Builder()
            .setId(config.id)
            .setSampleMimeType(config.mimeType)
            .setLanguage(config.language)
            .setLabel(config.label?.toString())
            .build()
        assertTrue(TrackHelper.isExternalSubtitleFormat(mockFormat))
    }

    @Test
    fun testBuildSubtitleConfigurationForcedAndSdhModifiers() {
        val testUriForced = FakeUri("file:///storage/emulated/0/Movies/Film.en.forced.srt")
        val configForced = ExternalSubtitleHelper.buildSubtitleConfiguration(
            uri = testUriForced,
            languageCode = "en",
            isForced = true,
            isSdh = false
        )
        assertEquals("English • Forced (External SRT)", configForced.label?.toString())
        assertEquals(C.SELECTION_FLAG_FORCED, configForced.selectionFlags)

        val testUriSdh = FakeUri("file:///storage/emulated/0/Movies/Film.spa.sdh.srt")
        val configSdh = ExternalSubtitleHelper.buildSubtitleConfiguration(
            uri = testUriSdh,
            languageCode = "spa",
            isForced = false,
            isSdh = true
        )
        assertEquals("Spanish • SDH (External SRT)", configSdh.label?.toString())
        assertEquals(C.ROLE_FLAG_DESCRIBES_MUSIC_AND_SOUND, configSdh.roleFlags)
    }

    // -------------------------------------------------------------
    // 6. VideoItem Discovery Integration Tests
    // -------------------------------------------------------------

    @Test
    fun testDiscoverSubtitlesWithVideoItemFilePath() {
        val dir = tempFolder.newFolder("video_item_test")
        val videoFile = File(dir, "Interstellar.2014.mkv").apply { createNewFile() }
        File(dir, "Interstellar.2014.en.srt").apply { createNewFile() }
        File(dir, "Interstellar.2014.es.srt").apply { createNewFile() }

        val videoItem = VideoItem(
            id = 100L,
            contentUri = FakeUri("content://media/external/video/media/100"),
            title = "Interstellar.2014.mkv",
            durationMs = 10_000_000L,
            width = 3840,
            height = 2160,
            sizeBytes = 10_000_000_000L,
            dateAdded = 1600000000L,
            filePath = videoFile.absolutePath
        )

        val discovered = ExternalSubtitleHelper.discoverSubtitles(
            video = videoItem,
            uriResolver = { FakeUri("file://${it.absolutePath}") }
        )

        assertEquals(2, discovered.size)
        assertEquals("en", discovered[0].languageCode)
        assertEquals("English", discovered[0].languageName)
        assertEquals("English (External SRT)", discovered[0].label)
        assertEquals(MimeTypes.APPLICATION_SUBRIP, discovered[0].configuration.mimeType)

        assertEquals("es", discovered[1].languageCode)
        assertEquals("Spanish", discovered[1].languageName)
        assertEquals("Spanish (External SRT)", discovered[1].label)

        // Also test pure Media3 configurations list
        val configs = ExternalSubtitleHelper.discoverSubtitleConfigurations(
            video = videoItem,
            uriResolver = { FakeUri("file://${it.absolutePath}") }
        )
        assertEquals(2, configs.size)
        assertEquals("en", configs[0].language)
        assertEquals("es", configs[1].language)
    }

    @Test
    fun testDiscoverSubtitlesWithFileUriScheme() {
        val dir = tempFolder.newFolder("file_uri_test")
        val videoFile = File(dir, "TestMovie.mp4").apply { createNewFile() }
        File(dir, "TestMovie.srt").apply { createNewFile() }

        val videoItem = VideoItem(
            id = 101L,
            contentUri = FakeUri("file://${videoFile.absolutePath}"),
            title = "TestMovie.mp4",
            durationMs = 5000L,
            width = 1920,
            height = 1080,
            sizeBytes = 1000L,
            dateAdded = 1600000000L,
            filePath = null // filePath is null, but contentUri is file://
        )

        val discovered = ExternalSubtitleHelper.discoverSubtitles(
            video = videoItem,
            uriResolver = { FakeUri("file://${it.absolutePath}") }
        )

        assertEquals(1, discovered.size)
        assertNull(discovered[0].languageCode)
        assertEquals("External SRT", discovered[0].label)
    }

    @Test
    fun testDiscoverSubtitlesWithOpaqueContentUriReturnsEmpty() {
        // Scoped storage rule: Opaque content:// URI without a resolvable file path
        // cannot safely enumerate directory siblings. It must return empty list without crashing.
        val videoItem = VideoItem(
            id = 102L,
            contentUri = FakeUri("content://media/external/video/media/102"),
            title = "OpaqueVideo.mp4",
            durationMs = 5000L,
            width = 1920,
            height = 1080,
            sizeBytes = 1000L,
            dateAdded = 1600000000L,
            filePath = null
        )

        val discovered = ExternalSubtitleHelper.discoverSubtitles(videoItem)
        assertTrue(discovered.isEmpty())

        val configs = ExternalSubtitleHelper.discoverSubtitleConfigurations(videoItem)
        assertTrue(configs.isEmpty())
    }

    @Test
    fun testDiscoverSubtitlesWithNonExistentFilePathReturnsEmpty() {
        val videoItem = VideoItem(
            id = 103L,
            contentUri = FakeUri("content://media/external/video/media/103"),
            title = "GhostMovie.mp4",
            durationMs = 5000L,
            width = 1920,
            height = 1080,
            sizeBytes = 1000L,
            dateAdded = 1600000000L,
            filePath = "/storage/emulated/0/NonExistent/GhostMovie.mp4"
        )

        val discovered = ExternalSubtitleHelper.discoverSubtitles(videoItem)
        assertTrue(discovered.isEmpty())
    }
}
