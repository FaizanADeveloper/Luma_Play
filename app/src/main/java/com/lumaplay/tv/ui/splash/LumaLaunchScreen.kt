package com.lumaplay.tv.ui.splash

import android.view.KeyEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import com.lumaplay.tv.core.theme.LumaTheme
import com.lumaplay.tv.core.ui.components.LumaLogoEmblem
import kotlinx.coroutines.delay

/**
 * Full Branded LumaPlay Launch Screen.
 *
 * Appears seamlessly immediately after the Android system splash screen and
 * smoothly transitions into the Home Dashboard once initial startup is ready.
 *
 * Incorporates the complete official Google Stitch brand identity:
 * - Official LumaPlay ribbon play emblem
 * - Two-tone "LumaPlay" title ("Luma" slate-white, "Play" electric cyan)
 * - "MEDIA FOR A BIGGER SCREEN" tagline
 * - Deep #0D1322 midnight navy canvas with subtle cyan atmospheric ambient glow
 * - Sized and calibrated for optimal 10-foot legibility on 50-inch 4K TV displays
 * - Non-blocking startup: does not wait indefinitely for storage permissions
 * - Seamless ~1.2s presentation with instantaneous key-press skip capability
 *
 * @param onLaunchComplete Callback invoked when the launch transition finishes.
 * @param modifier Composable modifier.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun LumaLaunchScreen(
    onLaunchComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focusRequester = remember { FocusRequester() }

    // Natural brief presentation duration (~1.2s), then dismiss cleanly into Home Dashboard
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        delay(1200L)
        onLaunchComplete()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(LumaTheme.colors.background)
            .focusRequester(focusRequester)
            .focusable()
            .onPreviewKeyEvent { keyEvent ->
                // Allow remote D-pad click or any key press to dismiss immediately
                if (keyEvent.type == KeyEventType.KeyDown) {
                    onLaunchComplete()
                    true
                } else {
                    false
                }
            },
        contentAlignment = Alignment.Center
    ) {
        // --- 1. Subtle Cyan / Blue Atmospheric Backdrop Glow ---
        Box(
            modifier = Modifier
                .size(640.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0x3838BDF8), // 22% Cyan glow in center
                            Color(0x144285F4), // 8% Royal blue blend
                            Color.Transparent  // Fades softly into #0D1322
                        )
                    )
                )
        )

        // --- 2. Full Official Stitch Logo Presentation (Vertical Centered Composition) ---
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Official Ribbon Play Emblem (Prominent 112dp for 50-inch 4K TV presentation)
            LumaLogoEmblem(
                size = 112.dp
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Main Two-Tone Brand Title: Luma (White) + Play (Cyan)
            val brandText = buildAnnotatedString {
                withStyle(
                    SpanStyle(
                        color = LumaTheme.colors.textPrimary,
                        fontWeight = FontWeight.W800,
                        letterSpacing = 1.sp
                    )
                ) {
                    append("Luma")
                }
                withStyle(
                    SpanStyle(
                        color = LumaTheme.colors.cyan,
                        fontWeight = FontWeight.W800,
                        letterSpacing = 1.sp
                    )
                ) {
                    append("Play")
                }
            }

            Text(
                text = brandText,
                fontSize = 56.sp,
                lineHeight = 58.sp,
                maxLines = 1,
                softWrap = false
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Official Stitch Tagline: MEDIA FOR A BIGGER SCREEN
            Text(
                text = "MEDIA FOR A BIGGER SCREEN",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = LumaTheme.colors.textSecondary,
                letterSpacing = 3.5.sp,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}
