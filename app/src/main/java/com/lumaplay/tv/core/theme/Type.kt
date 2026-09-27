package com.lumaplay.tv.core.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Typography

/**
 * LumaPlay TV Typography Scale (Google Stitch Specification).
 *
 * Calibrated for the 10-foot television viewing experience (2.5m - 3.5m)
 * projected onto 1080p and 4K displays. Uses system sans-serif with
 * open counters and geometric proportions matching the Inter specification.
 */
private val LumaFontFamily = FontFamily.SansSerif

// --- 1. Display Scales (Hero titles, billboard banners, modal highlights) ---

val LumaDisplayLg = TextStyle(
    fontFamily = LumaFontFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 48.sp,
    lineHeight = 56.sp,
    letterSpacing = (-0.02).em
)

val LumaDisplayMd = TextStyle(
    fontFamily = LumaFontFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 38.sp,
    lineHeight = 46.sp,
    letterSpacing = (-0.015).em
)

// --- 2. Headline Scales (Browse rows, category headers, screen titles) ---

val LumaHeadlineLg = TextStyle(
    fontFamily = LumaFontFamily,
    fontWeight = FontWeight.SemiBold,
    fontSize = 28.sp,
    lineHeight = 36.sp,
    letterSpacing = (-0.01).em
)

val LumaHeadlineMd = TextStyle(
    fontFamily = LumaFontFamily,
    fontWeight = FontWeight.SemiBold,
    fontSize = 24.sp,
    lineHeight = 32.sp,
    letterSpacing = (-0.005).em
)

val LumaHeadlineSm = TextStyle(
    fontFamily = LumaFontFamily,
    fontWeight = FontWeight.SemiBold,
    fontSize = 20.sp,
    lineHeight = 28.sp,
    letterSpacing = 0.em
)

// --- 3. Body Scales (Plot synopses, track listings, technical descriptions) ---
// Note: Body text never drops below 14sp to eliminate TV subpixel crawl.

val LumaBodyLg = TextStyle(
    fontFamily = LumaFontFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 18.sp,
    lineHeight = 26.sp,
    letterSpacing = 0.em
)

val LumaBodyMd = TextStyle(
    fontFamily = LumaFontFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 16.sp,
    lineHeight = 24.sp,
    letterSpacing = 0.01.em
)

val LumaBodySm = TextStyle(
    fontFamily = LumaFontFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 14.sp,
    lineHeight = 20.sp,
    letterSpacing = 0.01.em
)

// --- 4. Label Scales (Action buttons, filter chips, uppercase technical badges) ---

val LumaLabelLg = TextStyle(
    fontFamily = LumaFontFamily,
    fontWeight = FontWeight.SemiBold,
    fontSize = 16.sp,
    lineHeight = 22.sp,
    letterSpacing = 0.02.em
)

val LumaLabelMd = TextStyle(
    fontFamily = LumaFontFamily,
    fontWeight = FontWeight.SemiBold,
    fontSize = 13.sp,
    lineHeight = 18.sp,
    letterSpacing = 0.04.em
)

val LumaLabelSm = TextStyle(
    fontFamily = LumaFontFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 11.sp,
    lineHeight = 14.sp,
    letterSpacing = 0.08.em
)

/**
 * Direct semantic access to the 11 Stitch typography tokens
 * accessible via LocalLumaTypography.current.
 */
@Immutable
data class LumaTypography(
    val displayLg: TextStyle = LumaDisplayLg,
    val displayMd: TextStyle = LumaDisplayMd,
    val headlineLg: TextStyle = LumaHeadlineLg,
    val headlineMd: TextStyle = LumaHeadlineMd,
    val headlineSm: TextStyle = LumaHeadlineSm,
    val bodyLg: TextStyle = LumaBodyLg,
    val bodyMd: TextStyle = LumaBodyMd,
    val bodySm: TextStyle = LumaBodySm,
    val labelLg: TextStyle = LumaLabelLg,
    val labelMd: TextStyle = LumaLabelMd,
    val labelSm: TextStyle = LumaLabelSm
)

val LocalLumaTypography = staticCompositionLocalOf { LumaTypography() }

/**
 * Builds the TV Material 3 Typography instance mapped from Stitch tokens.
 * Maps the 11 Stitch scales into the 15 standard Material 3 roles from the provided [typography] parameter.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
fun lumaTvTypography(typography: LumaTypography = LumaTypography()): Typography = Typography(
    displayLarge = typography.displayLg,
    displayMedium = typography.displayMd,
    displaySmall = typography.headlineLg,
    headlineLarge = typography.headlineLg,
    headlineMedium = typography.headlineMd,
    headlineSmall = typography.headlineSm,
    titleLarge = typography.headlineMd,
    titleMedium = typography.headlineSm,
    titleSmall = typography.labelLg,
    bodyLarge = typography.bodyLg,
    bodyMedium = typography.bodyMd,
    bodySmall = typography.bodySm,
    labelLarge = typography.labelLg,
    labelMedium = typography.labelMd,
    labelSmall = typography.labelSm
)
