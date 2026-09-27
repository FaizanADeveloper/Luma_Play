package com.lumaplay.tv.core.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme

/**
 * LumaPlay TV Design System Theme.
 *
 * Configures the native Android TV Material 3 environment using Google Stitch design tokens:
 * - Establishes the dark midnight navy and cyan TV color scheme ([lumaDarkColorScheme]).
 * - Calibrates the 10-foot viewing distance typography scale ([lumaTvTypography]).
 * - Binds semantic TV design tokens ([LocalLumaColors], [LocalLumaSpacing], [LocalLumaShapes], [LocalLumaTypography]).
 *
 * Adheres strictly to the dark-only, cinema-grade home theatre aesthetic.
 * Both native TV Material 3 components and custom TV components receive unified tokens and styles.
 *
 * @param colors Semantic color tokens for LumaPlay TV surfaces, accents, and glows.
 * @param spacing TV-safe margin and 1080p/4K spacing tokens.
 * @param shapes Rounded corner, circle, and pill geometry tokens.
 * @param typography 10-foot typography scale tokens.
 * @param content The composable content tree to be styled.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun LumaPlayTheme(
    colors: LumaColors = LumaColors(),
    spacing: LumaSpacing = LumaSpacing(),
    shapes: LumaShapes = LumaShapes(),
    typography: LumaTypography = LumaTypography(),
    content: @Composable () -> Unit
) {
    // Propagate custom colors and typography into both TV Material 3 and custom tokens
    val tvMaterialColorScheme = lumaDarkColorScheme(colors = colors)
    val tvMaterialTypography = lumaTvTypography(typography = typography)

    CompositionLocalProvider(
        LocalLumaColors provides colors,
        LocalLumaSpacing provides spacing,
        LocalLumaShapes provides shapes,
        LocalLumaTypography provides typography
    ) {
        MaterialTheme(
            colorScheme = tvMaterialColorScheme,
            typography = tvMaterialTypography,
            content = content
        )
    }
}

/**
 * Unified static accessor object for LumaPlay TV design tokens.
 *
 * Provides a clean and idiomatic call site syntax:
 * - `LumaTheme.colors`
 * - `LumaTheme.spacing`
 * - `LumaTheme.shapes`
 * - `LumaTheme.typography`
 */
object LumaTheme {
    val colors: LumaColors
        @Composable
        @ReadOnlyComposable
        get() = LocalLumaColors.current

    val spacing: LumaSpacing
        @Composable
        @ReadOnlyComposable
        get() = LocalLumaSpacing.current

    val shapes: LumaShapes
        @Composable
        @ReadOnlyComposable
        get() = LocalLumaShapes.current

    val typography: LumaTypography
        @Composable
        @ReadOnlyComposable
        get() = LocalLumaTypography.current
}
