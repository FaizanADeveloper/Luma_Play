package com.lumaplay.tv.core.theme

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * TV Spacing tokens calibrated for 10-foot TV viewing and overscan protection.
 */
@Immutable
data class LumaSpacing(
    /** TV Safe Margin (56dp / 3.5rem) preventing edge clipping from TV overscan. */
    val tvSafeMargin: Dp = 56.dp,

    /** Standard horizontal spacing between cards to allow 1.04x focus scale expansion without clipping. */
    val gutter: Dp = 24.dp,

    /** 8dp (0.5rem) - micro gaps, badge padding. */
    val spaceXs: Dp = 8.dp,

    /** 12dp (0.75rem) - card internal padding, chip padding. */
    val spaceSm: Dp = 12.dp,

    /** 20dp (1.25rem) - section headers, button horizontal padding. */
    val spaceMd: Dp = 20.dp,

    /** 32dp (2.0rem) - row gaps, dialog margins. */
    val spaceLg: Dp = 32.dp,

    /** 48dp (3.0rem) - major vertical section spacing. */
    val spaceXl: Dp = 48.dp
)

val LocalLumaSpacing = staticCompositionLocalOf { LumaSpacing() }

/**
 * Screen-level TV safe padding modifier.
 * Applied at screen roots to enforce standard TV overscan boundaries
 * while allowing inner focusable elements to expand without clipping.
 * Reads LocalLumaSpacing.current from the Composable context.
 */
@Composable
fun Modifier.tvSafePadding(spacing: LumaSpacing = LocalLumaSpacing.current): Modifier = this.padding(
    start = spacing.tvSafeMargin,
    top = spacing.tvSafeMargin,
    end = spacing.tvSafeMargin,
    bottom = spacing.tvSafeMargin
)

/**
 * Screen-level padding values for lazy lists and carousels.
 */
fun LumaSpacing.screenPaddingValues(): PaddingValues = PaddingValues(
    start = tvSafeMargin,
    top = tvSafeMargin,
    end = tvSafeMargin,
    bottom = tvSafeMargin
)
