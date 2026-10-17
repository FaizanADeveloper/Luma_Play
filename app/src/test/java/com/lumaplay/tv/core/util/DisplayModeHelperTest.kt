package com.lumaplay.tv.core.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class DisplayModeHelperTest {

    @Test
    fun test8kClassification() {
        val standard8k = DisplayModeHelper.categorize(7680, 4320)
        assertEquals(DisplayModeCategory.UHD_8K, standard8k)
        assertEquals("8K Cinema Mode", standard8k.label)

        val transposed8k = DisplayModeHelper.categorize(4320, 7680)
        assertEquals(DisplayModeCategory.UHD_8K, transposed8k)
        assertEquals("8K Cinema Mode", transposed8k.label)

        val wider8k = DisplayModeHelper.categorize(8192, 4320)
        assertEquals(DisplayModeCategory.UHD_8K, wider8k)
        assertEquals("8K Cinema Mode", wider8k.label)
    }

    @Test
    fun test4kClassification() {
        val standard4k = DisplayModeHelper.categorize(3840, 2160)
        assertEquals(DisplayModeCategory.UHD_4K, standard4k)
        assertEquals("4K Cinema Mode", standard4k.label)

        val dci4k = DisplayModeHelper.categorize(4096, 2160)
        assertEquals(DisplayModeCategory.UHD_4K, dci4k)
        assertEquals("4K Cinema Mode", dci4k.label)

        val portrait4k = DisplayModeHelper.categorize(2160, 3840)
        assertEquals(DisplayModeCategory.UHD_4K, portrait4k)
        assertEquals("4K Cinema Mode", portrait4k.label)

        val portraitDci4k = DisplayModeHelper.categorize(2160, 4096)
        assertEquals(DisplayModeCategory.UHD_4K, portraitDci4k)
        assertEquals("4K Cinema Mode", portraitDci4k.label)
    }

    @Test
    fun test1080pClassification() {
        val standard1080p = DisplayModeHelper.categorize(1920, 1080)
        assertEquals(DisplayModeCategory.FHD_1080P, standard1080p)
        assertEquals("1080p Cinema Mode", standard1080p.label)

        val transposed1080p = DisplayModeHelper.categorize(1080, 1920)
        assertEquals(DisplayModeCategory.FHD_1080P, transposed1080p)
        assertEquals("1080p Cinema Mode", transposed1080p.label)

        val slightlyWider1080p = DisplayModeHelper.categorize(2048, 1080)
        assertEquals(DisplayModeCategory.FHD_1080P, slightlyWider1080p)
        assertEquals("1080p Cinema Mode", slightlyWider1080p.label)
    }

    @Test
    fun test720pClassification() {
        val standard720p = DisplayModeHelper.categorize(1280, 720)
        assertEquals(DisplayModeCategory.HD_720P, standard720p)
        assertEquals("720p Cinema Mode", standard720p.label)

        val transposed720p = DisplayModeHelper.categorize(720, 1280)
        assertEquals(DisplayModeCategory.HD_720P, transposed720p)
        assertEquals("720p Cinema Mode", transposed720p.label)

        val commonTv768p = DisplayModeHelper.categorize(1366, 768)
        assertEquals(DisplayModeCategory.HD_720P, commonTv768p)
        assertEquals("720p Cinema Mode", commonTv768p.label)
    }

    @Test
    fun testZeroAndNegativeDimensions() {
        val zero = DisplayModeHelper.categorize(0, 0)
        assertEquals(DisplayModeCategory.UNKNOWN, zero)
        assertEquals("Cinema Mode", zero.label)

        val negative = DisplayModeHelper.categorize(-1, -1)
        assertEquals(DisplayModeCategory.UNKNOWN, negative)
        assertEquals("Cinema Mode", negative.label)

        val zeroWidth = DisplayModeHelper.categorize(0, 1080)
        assertEquals(DisplayModeCategory.UNKNOWN, zeroWidth)
        assertEquals("Cinema Mode", zeroWidth.label)

        val zeroHeight = DisplayModeHelper.categorize(1920, 0)
        assertEquals(DisplayModeCategory.UNKNOWN, zeroHeight)
        assertEquals("Cinema Mode", zeroHeight.label)

        val negativeWidth = DisplayModeHelper.categorize(-1920, 1080)
        assertEquals(DisplayModeCategory.UNKNOWN, negativeWidth)
        assertEquals("Cinema Mode", negativeWidth.label)

        val negativeHeight = DisplayModeHelper.categorize(1920, -1080)
        assertEquals(DisplayModeCategory.UNKNOWN, negativeHeight)
        assertEquals("Cinema Mode", negativeHeight.label)
    }

    @Test
    fun testSmallUnsupportedDimensions() {
        val vga = DisplayModeHelper.categorize(640, 480)
        assertEquals(DisplayModeCategory.UNKNOWN, vga)
        assertEquals("Cinema Mode", vga.label)

        val svga = DisplayModeHelper.categorize(800, 600)
        assertEquals(DisplayModeCategory.UNKNOWN, svga)
        assertEquals("Cinema Mode", svga.label)

        val subHd = DisplayModeHelper.categorize(1024, 600)
        assertEquals(DisplayModeCategory.UNKNOWN, subHd)
        assertEquals("Cinema Mode", subHd.label)

        val narrow720p = DisplayModeHelper.categorize(1000, 720)
        assertEquals(DisplayModeCategory.UNKNOWN, narrow720p)
        assertEquals("Cinema Mode", narrow720p.label)
    }

    @Test
    fun testCategoryLabels() {
        assertEquals("8K Cinema Mode", DisplayModeCategory.UHD_8K.label)
        assertEquals("4K Cinema Mode", DisplayModeCategory.UHD_4K.label)
        assertEquals("1080p Cinema Mode", DisplayModeCategory.FHD_1080P.label)
        assertEquals("720p Cinema Mode", DisplayModeCategory.HD_720P.label)
        assertEquals("Cinema Mode", DisplayModeCategory.UNKNOWN.label)
        assertEquals("Cinema Mode", DisplayModeCategory.DEFAULT_LABEL)
    }

    @Test
    fun testParseResolution() {
        assertEquals(Pair(3840, 2160), DisplayModeHelper.parseResolution("3840x2160"))
        assertEquals(Pair(3840, 2160), DisplayModeHelper.parseResolution("3840X2160"))
        assertEquals(Pair(1920, 1080), DisplayModeHelper.parseResolution("  1920*1080  "))
        assertEquals(Pair(1280, 720), DisplayModeHelper.parseResolution("1280x720"))
        assertNull(DisplayModeHelper.parseResolution(null))
        assertNull(DisplayModeHelper.parseResolution(""))
        assertNull(DisplayModeHelper.parseResolution("invalid"))
        assertNull(DisplayModeHelper.parseResolution("0x0"))
    }

    @Test
    fun testResolveNativeResolutionViaVendorProperties() {
        // Simulating Xiaomi/Redmi vendor.display-size = 3840x2160
        val xiaomiProps: (String) -> String? = { key ->
            when (key) {
                "vendor.display-size" -> "3840x2160"
                else -> null
            }
        }
        val res = DisplayModeHelper.resolveNativeResolution(
            propertyReader = xiaomiProps,
            supportedModes = listOf(Pair(1920, 1080)),
            deviceModel = "GenericModel",
            activeMode = Pair(1920, 1080)
        )
        assertNotNull(res)
        assertEquals(Pair(3840, 2160), res)
        assertEquals(DisplayModeCategory.UHD_4K, DisplayModeHelper.categorize(res!!.first, res.second))
        assertEquals("4K Cinema Mode", DisplayModeHelper.categorize(res.first, res.second).label)

        // Simulating Sony sys.display-size = 3840x2160
        val sonyProps: (String) -> String? = { key ->
            when (key) {
                "sys.display-size" -> "3840x2160"
                else -> null
            }
        }
        val sonyRes = DisplayModeHelper.resolveNativeResolution(
            propertyReader = sonyProps,
            supportedModes = listOf(Pair(1920, 1080)),
            deviceModel = "SonyTV",
            activeMode = Pair(1920, 1080)
        )
        assertNotNull(sonyRes)
        assertEquals(Pair(3840, 2160), sonyRes)
        assertEquals("4K Cinema Mode", DisplayModeHelper.categorize(sonyRes!!.first, sonyRes.second).label)
    }

    @Test
    fun testResolveNativeResolutionViaSupportedModes() {
        val noProps: (String) -> String? = { null }
        // When display modes include a 4K mode
        val modes = listOf(Pair(1920, 1080), Pair(3840, 2160))
        val res = DisplayModeHelper.resolveNativeResolution(
            propertyReader = noProps,
            supportedModes = modes,
            deviceModel = "GenericTV",
            activeMode = Pair(1920, 1080)
        )
        assertNotNull(res)
        assertEquals(Pair(3840, 2160), res)
        assertEquals("4K Cinema Mode", DisplayModeHelper.categorize(res!!.first, res.second).label)
    }

    @Test
    fun testResolveNativeResolutionViaConfirmedModelProfile() {
        val noProps: (String) -> String? = { null }
        // Redmi Smart TV X50 model profile with no accessible vendor properties
        val res = DisplayModeHelper.resolveNativeResolution(
            propertyReader = noProps,
            supportedModes = listOf(Pair(1920, 1080)),
            deviceModel = "MiTV-MOOQ3",
            activeMode = Pair(1920, 1080)
        )
        assertNotNull(res)
        assertEquals(Pair(3840, 2160), res)
        assertEquals("4K Cinema Mode", DisplayModeHelper.categorize(res!!.first, res.second).label)

        // Redmi Smart TV X55
        val x55Res = DisplayModeHelper.resolveNativeResolution(
            propertyReader = noProps,
            supportedModes = listOf(Pair(1920, 1080)),
            deviceModel = "MiTV-MOOQ1",
            activeMode = Pair(1920, 1080)
        )
        assertNotNull(x55Res)
        assertEquals(Pair(3840, 2160), x55Res)
    }

    @Test
    fun testResolveNativeResolutionUnconfirmedFallback() {
        val noProps: (String) -> String? = { null }
        // Generic unknown TV reporting 1080p active display mode with no 4K evidence:
        // Must NOT falsely claim 1080p panel! Must return null -> UNKNOWN ("Cinema Mode").
        val res = DisplayModeHelper.resolveNativeResolution(
            propertyReader = noProps,
            supportedModes = listOf(Pair(1920, 1080)),
            deviceModel = "GenericAndroidTV",
            activeMode = Pair(1920, 1080)
        )
        assertNull(res)
    }

    @Test
    fun testResolveNativeResolutionConfirmedLowerResolution() {
        val noProps: (String) -> String? = { null }
        // Display controller only reports max mode of 720p: confirmed 720p
        val res720p = DisplayModeHelper.resolveNativeResolution(
            propertyReader = noProps,
            supportedModes = listOf(Pair(1280, 720)),
            deviceModel = "OldHdReadyTV",
            activeMode = Pair(1280, 720)
        )
        assertNotNull(res720p)
        assertEquals(Pair(1280, 720), res720p)
        assertEquals("720p Cinema Mode", DisplayModeHelper.categorize(res720p!!.first, res720p.second).label)

        // Vendor explicitly confirms 1080p panel
        val props1080p: (String) -> String? = { key ->
            if (key == "vendor.display-size") "1920x1080" else null
        }
        val res1080p = DisplayModeHelper.resolveNativeResolution(
            propertyReader = props1080p,
            supportedModes = listOf(Pair(1920, 1080)),
            deviceModel = "Native1080pTV",
            activeMode = Pair(1920, 1080)
        )
        assertNotNull(res1080p)
        assertEquals(Pair(1920, 1080), res1080p)
        assertEquals("1080p Cinema Mode", DisplayModeHelper.categorize(res1080p!!.first, res1080p.second).label)
    }
}
