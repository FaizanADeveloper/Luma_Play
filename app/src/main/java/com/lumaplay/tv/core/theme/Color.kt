package com.lumaplay.tv.core.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.tv.material3.ColorScheme
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.darkColorScheme

// --- Core Palette Tokens (Google Stitch LumaPlay Design System) ---

// Canvas & Surfaces (Tonal Stepping)
val LumaBackground = Color(0xFF0B1120)            // Level 0: Deep navy charcoal grounding canvas
val LumaSurface = Color(0xFF111C30)               // Level 1: Dark slate blue (dock, trays, modals)
val LumaSurfaceCard = Color(0xFF192840)           // Level 2: Base resting state for media cards & inputs
val LumaSurfaceContainerHigh = Color(0xFF1E314F)  // Level 3: Active selected rows, elevated dialogs
val LumaSurfaceContainerLowest = Color(0xFF080E1D)// Deep inset background (telemetry wells)
val LumaSurfaceContainerHighest = Color(0xFF2F3445) // Inactive track trough

// Brand & Accent Colors
val LumaPrimary = Color(0xFF4285F4)               // Royal Blue action color (timelines, primary CTA)
val LumaCyan = Color(0xFF38BDF8)                  // Electric Cyan focus border & highlight
val LumaViolet = Color(0xFF8B5CF6)                // Deep Violet for featured tags & highlights
val LumaCyanGlow = Color(0x7338BDF8)              // 45% alpha Cyan for luminous TV focus halo

// Typography & Content Colors
val LumaTextPrimary = Color(0xFFF8FAFC)           // Crisp slate-white for headings & active labels
val LumaTextSecondary = Color(0xFF94A3B8)         // Cool slate blue-gray for metadata & descriptions
val LumaTextMuted = Color(0xFF64748B)             // Low-emphasis copy & inactive categories
val LumaOutline = Color(0xFF8C909F)               // Inactive borders & dividing rules
val LumaOutlineVariant = Color(0xFF424753)        // Subtle card borders in resting state

// Status & Badge Accents
val LumaSuccess = Color(0xFF10B981)               // Emerald for completed progress
val LumaWarning = Color(0xFFF59E0B)               // Amber Gold for warning/highlight badges
val LumaError = Color(0xFFEF4444)                 // Crimson for playback/storage errors
val LumaErrorContainer = Color(0xFF93000A)        // Dark error container fill

/**
 * Custom semantic color holder for LumaPlay-specific TV tokens
 * accessible via LocalLumaColors.current.
 */
@Immutable
data class LumaColors(
    val background: Color = LumaBackground,
    val surface: Color = LumaSurface,
    val surfaceCard: Color = LumaSurfaceCard,
    val surfaceContainerHigh: Color = LumaSurfaceContainerHigh,
    val surfaceContainerLowest: Color = LumaSurfaceContainerLowest,
    val surfaceContainerHighest: Color = LumaSurfaceContainerHighest,
    val primary: Color = LumaPrimary,
    val cyan: Color = LumaCyan,
    val violet: Color = LumaViolet,
    val cyanGlow: Color = LumaCyanGlow,
    val textPrimary: Color = LumaTextPrimary,
    val textSecondary: Color = LumaTextSecondary,
    val textMuted: Color = LumaTextMuted,
    val outline: Color = LumaOutline,
    val outlineVariant: Color = LumaOutlineVariant,
    val success: Color = LumaSuccess,
    val warning: Color = LumaWarning,
    val error: Color = LumaError,
    val errorContainer: Color = LumaErrorContainer
)

val LocalLumaColors = staticCompositionLocalOf { LumaColors() }

/**
 * Builds the TV Material 3 dark color scheme mapped directly to Stitch tokens.
 * Maps semantic roles from the provided [colors] instance.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
fun lumaDarkColorScheme(colors: LumaColors = LumaColors()): ColorScheme = darkColorScheme(
    primary = colors.primary,
    onPrimary = colors.textPrimary,
    primaryContainer = Color(0xFF4D8EFE),
    onPrimaryContainer = Color(0xFF00285C),
    secondary = colors.cyan,
    onSecondary = Color(0xFF00354A),
    secondaryContainer = Color(0xFF00A6E0),
    onSecondaryContainer = Color(0xFF00374D),
    tertiary = colors.violet,
    onTertiary = Color(0xFF3C0091),
    tertiaryContainer = Color(0xFFA078FF),
    onTertiaryContainer = Color(0xFF340080),
    background = colors.background,
    onBackground = colors.textPrimary,
    surface = colors.surface,
    onSurface = colors.textPrimary,
    surfaceVariant = colors.surfaceCard,
    onSurfaceVariant = colors.textSecondary,
    error = colors.error,
    onError = Color(0xFF690005),
    errorContainer = colors.errorContainer,
    onErrorContainer = Color(0xFFFFDAD6),
    border = colors.outline,
    borderVariant = colors.outlineVariant
)
