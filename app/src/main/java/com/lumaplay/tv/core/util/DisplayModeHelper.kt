package com.lumaplay.tv.core.util

import android.content.Context
import android.hardware.display.DisplayManager
import android.os.Build
import android.view.Display
import androidx.core.view.DisplayCompat

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
 * Utility for detecting and categorizing Android TV hardware-native panel resolution.
 *
 * Truthfulness note:
 * - This classifies the TV physical panel's native optical matrix / hardware capability.
 * - On many 4K Android TV devices (including Xiaomi, Sony, TCL), Android TV's SurfaceFlinger
 *   operates at a 1080p logical framebuffer and hardware-upscales to the 4K panel.
 * - This utility does NOT falsely claim a panel is 1080p merely because Android's logical
 *   output mode is 1080p.
 * - When native panel resolution cannot be reliably established via hardware telemetry or
 *   confirmed model profiles, it gracefully falls back to [DisplayModeCategory.UNKNOWN] ("Cinema Mode").
 */
object DisplayModeHelper {

    /**
     * Standard vendor system properties where TV OEMs expose physical panel dimensions
     * independently of SurfaceFlinger's logical framebuffer resolution.
     */
    val HARDWARE_PANEL_PROPERTIES = listOf(
        "vendor.display-size",
        "sys.display-size",
        "ro.vendor.display-size",
        "persist.vendor.display.size",
        "ro.display-size"
    )

    /**
     * Documented, verified TV models with physical 4K UHD (3840x2160) panels.
     * Used as a conservative secondary fallback when system properties are sandboxed.
     */
    val KNOWN_4K_PANEL_MODELS = setOf(
        "MiTV-MOOQ3", // Redmi Smart TV X50 (50" 4K UHD, 3840x2160)
        "MiTV-MOOQ1", // Redmi Smart TV X55 (55" 4K UHD, 3840x2160)
        "MiTV-MOOQ2", // Redmi Smart TV X65 (65" 4K UHD, 3840x2160)
        "MiTV-MSSP3", // Mi TV 4X 50" (4K UHD, 3840x2160)
        "MiTV-MSSP1", // Mi TV 4X 55" (4K UHD, 3840x2160)
        "MiTV-MSSP2"  // Mi TV 4X 65" (4K UHD, 3840x2160)
    )

    @Volatile
    private var cachedNativeResolution: Pair<Int, Int>? = null

    @Volatile
    private var hasProbedNativeResolution: Boolean = false

    /**
     * Parses a resolution string like "3840x2160", "3840X2160", or "3840*2160" into (width, height).
     */
    fun parseResolution(raw: String?): Pair<Int, Int>? {
        if (raw.isNullOrBlank()) return null
        val match = Regex("""(\d{3,5})\s*[xX*]\s*(\d{3,5})""").find(raw.trim()) ?: return null
        val (w, h) = match.destructured
        val width = w.toIntOrNull() ?: return null
        val height = h.toIntOrNull() ?: return null
        return if (width > 0 && height > 0) Pair(width, height) else null
    }

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
     * Pure, JVM-testable resolver that correlates hardware vendor telemetry, supported display modes,
     * and confirmed device profiles to reliably establish the physical panel's native capability.
     */
    fun resolveNativeResolution(
        propertyReader: (String) -> String?,
        supportedModes: List<Pair<Int, Int>>,
        deviceModel: String,
        activeMode: Pair<Int, Int>?
    ): Pair<Int, Int>? {
        // 1. Hardware Vendor Telemetry (System Properties)
        for (prop in HARDWARE_PANEL_PROPERTIES) {
            val raw = propertyReader(prop)
            val parsed = parseResolution(raw)
            if (parsed != null) {
                return parsed
            }
        }

        val widthProp = propertyReader("ro.display.width")?.toIntOrNull()
        val heightProp = propertyReader("ro.display.height")?.toIntOrNull()
        if (widthProp != null && heightProp != null && widthProp > 0 && heightProp > 0) {
            return Pair(widthProp, heightProp)
        }

        // 2. Hardware Display Modes Reported by Display Controller
        // If ANY supported hardware mode is 4K or 8K, the display hardware natively supports it.
        val maxMode = supportedModes.maxByOrNull { maxOf(it.first, it.second) }
        if (maxMode != null) {
            val maxDim = maxOf(maxMode.first, maxMode.second)
            val minDim = minOf(maxMode.first, maxMode.second)
            if (maxDim >= 3840 && minDim >= 2160) {
                return maxMode
            }
        }

        // 3. Documented Device Models with Verified Native 4K Panels (Conservative fallback)
        if (KNOWN_4K_PANEL_MODELS.contains(deviceModel.trim())) {
            return Pair(3840, 2160)
        }

        // 4. Confirmed Lower-Resolution Displays
        // If the maximum supported mode is explicitly <= 1280x720, we know the panel is at most 720p.
        if (maxMode != null) {
            val maxDim = maxOf(maxMode.first, maxMode.second)
            val minDim = minOf(maxMode.first, maxMode.second)
            if (maxDim <= 1280 && minDim <= 720) {
                return maxMode
            }
        }

        // 5. Unconfirmed Resolution
        // If activeMode is 1920x1080 (the universal Android TV UI default) but neither vendor
        // telemetry nor supportedModes confirms whether the physical panel is 4K (upscaled)
        // or native 1080p, DO NOT guess or claim 1080p. Return null -> DisplayModeCategory.UNKNOWN ("Cinema Mode").
        return null
    }

    /**
     * Reads a system property using reflection on android.os.SystemProperties with fallback
     * to executing /system/bin/getprop.
     */
    fun readSystemProperty(key: String): String? {
        // 1. Try reflection on android.os.SystemProperties
        val reflection = runCatching {
            val clazz = Class.forName("android.os.SystemProperties")
            val method = clazz.getMethod("get", String::class.java)
            val value = method.invoke(null, key) as? String
            value?.takeIf { it.isNotBlank() }
        }.getOrNull()
        if (reflection != null) return reflection

        // 2. Try Runtime exec /system/bin/getprop
        return runCatching {
            val process = Runtime.getRuntime().exec(arrayOf("/system/bin/getprop", key))
            val output = process.inputStream.bufferedReader().use { it.readText() }.trim()
            process.waitFor()
            output.takeIf { it.isNotBlank() }
        }.getOrNull()
    }

    /**
     * Queries the physical dimensions of the active Android logical display mode.
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
     * Resolves the hardware-native panel resolution for the given [context].
     * Caches the result to avoid expensive queries during Jetpack Compose recomposition.
     */
    fun getNativePanelResolution(context: Context, forceRefresh: Boolean = false): Pair<Int, Int>? {
        if (hasProbedNativeResolution && !forceRefresh) {
            return cachedNativeResolution
        }
        synchronized(this) {
            if (hasProbedNativeResolution && !forceRefresh) {
                return cachedNativeResolution
            }

            val display = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                runCatching { context.display }.getOrNull()
            } else {
                null
            } ?: (context.getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager)
                ?.getDisplay(Display.DEFAULT_DISPLAY)

            val supportedModes = mutableListOf<Pair<Int, Int>>()
            if (display != null) {
                // Query AndroidX DisplayCompat
                runCatching {
                    DisplayCompat.getSupportedModes(context, display).forEach { mode ->
                        if (mode.physicalWidth > 0 && mode.physicalHeight > 0) {
                            supportedModes.add(Pair(mode.physicalWidth, mode.physicalHeight))
                        }
                    }
                }
                // Query Display.supportedModes (API 23+)
                runCatching {
                    display.supportedModes?.forEach { mode ->
                        if (mode.physicalWidth > 0 && mode.physicalHeight > 0) {
                            supportedModes.add(Pair(mode.physicalWidth, mode.physicalHeight))
                        }
                    }
                }
            }

            val activeMode = getPhysicalResolution(context)

            val resolution = resolveNativeResolution(
                propertyReader = ::readSystemProperty,
                supportedModes = supportedModes,
                deviceModel = Build.MODEL ?: "",
                activeMode = activeMode
            )

            cachedNativeResolution = resolution
            hasProbedNativeResolution = true
            return resolution
        }
    }

    /**
     * Resolves the active [DisplayModeCategory] for the physical panel of the given [context].
     */
    fun getDisplayModeCategory(context: Context, forceRefresh: Boolean = false): DisplayModeCategory {
        val resolution = getNativePanelResolution(context, forceRefresh) ?: return DisplayModeCategory.UNKNOWN
        return categorize(resolution.first, resolution.second)
    }

    /**
     * Resolves the user-facing Cinema Mode label for the given [context].
     */
    fun getDisplayModeLabel(context: Context, forceRefresh: Boolean = false): String {
        return getDisplayModeCategory(context, forceRefresh).label
    }
}
