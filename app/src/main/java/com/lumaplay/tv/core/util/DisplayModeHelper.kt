package com.lumaplay.tv.core.util

import android.content.Context
import android.hardware.display.DisplayManager
import android.os.Build
import android.view.Display

/**
 * Display resolution classification categories for Android TV.
 */
enum class DisplayModeCategory(val label: String) {
    UHD_8K("8K Cinema Mode"),
    UHD_4K("4K Cinema Mode"),
    FHD_1080P("1080p Cinema Mode"),
    HD_720P("720p Cinema Mode"),
    UNKNOWN("Cinema Mode");

    companion object {
        const val DEFAULT_LABEL = "Cinema Mode"
    }
}

/**
 * Utility for detecting and categorizing Android TV physical display output mode.
 *
 * Truthfulness note:
 * - This classifies the active display mode negotiated by Android with the display output (HDMI/panel).
 * - It does not guarantee or claim knowledge of an external television's native optical matrix when
 *   an upstream streaming box downscales the HDMI signal to 1080p.
 */
object DisplayModeHelper {

    /**
     * Pure, JVM-testable resolution classifier.
     *
     * Handles both landscape (TV standard) and portrait/transposed orientations.
     * Rejects zero, negative, and sub-720p unsupported dimensions into [DisplayModeCategory.UNKNOWN].
     */
    fun categorize(width: Int, height: Int): DisplayModeCategory {
        if (width <= 0 || height <= 0) {
            return DisplayModeCategory.UNKNOWN
        }

        val maxDim = maxOf(width, height)
        val minDim = minOf(width, height)

        return when {
            // 8K UHD (7680 x 4320)
            maxDim >= 7680 && minDim >= 4320 -> DisplayModeCategory.UHD_8K

            // 4K UHD (3840 x 2160) & 4K DCI (4096 x 2160)
            maxDim >= 3840 && minDim >= 2160 -> DisplayModeCategory.UHD_4K

            // 1080p Full HD (1920 x 1080)
            maxDim >= 1920 && minDim >= 1080 -> DisplayModeCategory.FHD_1080P

            // 720p HD (1280 x 720)
            maxDim >= 1280 && minDim >= 720 -> DisplayModeCategory.HD_720P

            // Below 720p, zero, or unconventional aspect ratios
            else -> DisplayModeCategory.UNKNOWN
        }
    }

    /**
     * Queries the physical dimensions of the active Android display mode.
     *
     * Safely queries [context.display] on Android 11+ (API 30+) with fallback to
     * [DisplayManager.getDisplay] for non-Activity contexts.
     *
     * @return [Pair] of (physicalWidth, physicalHeight) in pixels, or null if unavailable.
     */
    fun getPhysicalResolution(context: Context): Pair<Int, Int>? {
        return runCatching {
            val display = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                runCatching { context.display }.getOrNull()
            } else {
                null
            } ?: (context.getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager)
                ?.getDisplay(Display.DEFAULT_DISPLAY)

            val mode = display?.mode
            if (mode != null && mode.physicalWidth > 0 && mode.physicalHeight > 0) {
                Pair(mode.physicalWidth, mode.physicalHeight)
            } else {
                null
            }
        }.getOrNull()
    }

    /**
     * Resolves the active [DisplayModeCategory] for the given [context].
     */
    fun getDisplayModeCategory(context: Context): DisplayModeCategory {
        val resolution = getPhysicalResolution(context) ?: return DisplayModeCategory.UNKNOWN
        return categorize(resolution.first, resolution.second)
    }

    /**
     * Resolves the user-facing Cinema Mode label for the given [context].
     */
    fun getDisplayModeLabel(context: Context): String {
        return getDisplayModeCategory(context).label
    }
}
