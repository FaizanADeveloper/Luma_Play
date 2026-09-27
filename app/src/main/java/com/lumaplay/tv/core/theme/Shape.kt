package com.lumaplay.tv.core.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * Shape tokens following the Stitch Level 2 (Rounded) design specification.
 */
@Immutable
data class LumaShapes(
    /** 4dp corner radius for micro indicators, code tags. */
    val small: Shape = RoundedCornerShape(4.dp),

    /** 8dp corner radius for buttons, chips, and format badges. */
    val medium: Shape = RoundedCornerShape(8.dp),

    /** 16dp corner radius for 16:9 media cards, hero tiles, and browse items. */
    val large: Shape = RoundedCornerShape(16.dp),

    /** 24dp corner radius for modal dialogs, OSD sheets, and diagnostic panels. */
    val extraLarge: Shape = RoundedCornerShape(24.dp),

    /** Fully rounded circle shape for avatar or circular icon buttons. */
    val circle: Shape = CircleShape,

    /** Fully rounded pill/capsule shape for playback FABs, badges, and progress indicators. */
    val pill: Shape = RoundedCornerShape(percent = 50)
)

val LocalLumaShapes = staticCompositionLocalOf { LumaShapes() }
