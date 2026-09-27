package com.lumaplay.tv.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import com.lumaplay.tv.core.theme.LumaTheme

/**
 * Visual status and categorization variants for LumaPlay TV badges.
 */
enum class LumaBadgeVariant {
    /** Neutral metadata badge (e.g. video resolution "4K", audio format "FLAC"). */
    Standard,

    /** High-visibility Electric Cyan badge (e.g. "NEW", active filter). */
    Highlight,

    /** Violet accent badge (e.g. "MASTER", featured content). */
    Accent,

    /** Emerald green badge indicating success or verified status. */
    Success,

    /** Crimson badge indicating playback or storage warning/error. */
    Error
}

/**
 * LumaPlay TV Presentation-Only Metadata Badge.
 *
 * Displays compact tags for formats, resolutions, codecs, and status indicators:
 * - Presentation-only: strictly renders supplied [text] and [variant] styling.
 * - Does not infer or claim hardware playback capabilities.
 * - Non-interactive: contains no focus or click handlers.
 * - Compact TV typography ([LumaTheme.typography.badge]) for crisp 10-foot legibility.
 * - Tokenized styling adhering to the Stitch Level 2 design system.
 *
 * @param text The label text displayed within the badge.
 * @param modifier Composable modifier for placement or layout adjustments.
 * @param variant Visual hierarchy variant ([LumaBadgeVariant.Standard], [LumaBadgeVariant.Highlight], etc.).
 * @param shape Corner geometry, defaults to [LumaTheme.shapes.small] (4dp).
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun LumaBadge(
    text: String,
    modifier: Modifier = Modifier,
    variant: LumaBadgeVariant = LumaBadgeVariant.Standard,
    shape: Shape = LumaTheme.shapes.small
) {
    val (containerColor: Color, contentColor: Color, borderColor: Color) = when (variant) {
        LumaBadgeVariant.Standard -> Triple(
            LumaTheme.colors.surface.copy(alpha = 0.8f),
            LumaTheme.colors.textPrimary,
            LumaTheme.colors.outlineVariant
        )
        LumaBadgeVariant.Highlight -> Triple(
            LumaTheme.colors.cyan.copy(alpha = 0.15f),
            LumaTheme.colors.cyan,
            LumaTheme.colors.cyan.copy(alpha = 0.5f)
        )
        LumaBadgeVariant.Accent -> Triple(
            LumaTheme.colors.violet.copy(alpha = 0.2f),
            LumaTheme.colors.violet,
            LumaTheme.colors.violet.copy(alpha = 0.5f)
        )
        LumaBadgeVariant.Success -> Triple(
            LumaTheme.colors.success.copy(alpha = 0.15f),
            LumaTheme.colors.success,
            LumaTheme.colors.success.copy(alpha = 0.5f)
        )
        LumaBadgeVariant.Error -> Triple(
            LumaTheme.colors.error.copy(alpha = 0.15f),
            LumaTheme.colors.error,
            LumaTheme.colors.error.copy(alpha = 0.5f)
        )
    }

    Box(
        modifier = modifier
            .clip(shape)
            .background(containerColor)
            .border(
                width = 1.dp,
                color = borderColor,
                shape = shape
            )
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            style = LumaTheme.typography.labelSm,
            color = contentColor
        )
    }
}
