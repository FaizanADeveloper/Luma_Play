package com.lumaplay.tv.player.diagnostics

import androidx.media3.ui.AspectRatioFrameLayout

/**
 * Presentation aspect ratio modes supported by Media3's [AspectRatioFrameLayout].
 */
enum class AspectRatioMode(val resizeMode: Int, val label: String) {
    FIT(AspectRatioFrameLayout.RESIZE_MODE_FIT, "Fit"),
    FILL(AspectRatioFrameLayout.RESIZE_MODE_FILL, "Fill"),
    ZOOM(AspectRatioFrameLayout.RESIZE_MODE_ZOOM, "Zoom");

    fun next(): AspectRatioMode = when (this) {
        FIT -> FILL
        FILL -> ZOOM
        ZOOM -> FIT
    }
}
