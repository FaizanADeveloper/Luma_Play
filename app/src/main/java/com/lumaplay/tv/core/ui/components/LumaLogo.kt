package com.lumaplay.tv.core.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import com.lumaplay.tv.core.theme.LumaTheme

/**
 * Renders the official Stitch LumaPlay Ribbon Play Emblem with linear gradients.
 */
@Composable
fun LumaLogoEmblem(
    modifier: Modifier = Modifier,
    size: Dp = 36.dp
) {
    Canvas(
        modifier = modifier
            .size(size)
            .semantics { contentDescription = "LumaPlay TV Emblem" }
    ) {
        val s = this.size.minDimension / 48f
        scale(s, pivot = Offset.Zero) {
            val outerGrad = Brush.linearGradient(
                colors = listOf(Color(0xFF38BDF8), Color(0xFF4285F4)),
                start = Offset(6.8f, 4.8f),
                end = Offset(41.2f, 43.2f)
            )
            val innerGrad = Brush.linearGradient(
                colors = listOf(Color(0xFF8B5CF6), Color(0xFF4285F4)),
                start = Offset(20.8f, 15f),
                end = Offset(41.2f, 35f)
            )

            // Outer Play Ribbon
            val outerPath = Path().apply {
                moveTo(6.8f, 8f)
                cubicTo(6.8f, 5.5f, 9.3f, 4f, 11.6f, 5.2f)
                lineTo(39f, 21.2f)
                cubicTo(41.2f, 22.5f, 41.2f, 25.5f, 39f, 26.8f)
                lineTo(11.6f, 42.8f)
                cubicTo(9.3f, 44f, 6.8f, 42.5f, 6.8f, 40f)
                close()
            }

            // Inner Ribbon Fold
            val innerPath = Path().apply {
                moveTo(20.8f, 15f)
                lineTo(39f, 25.6f)
                cubicTo(40.3f, 26.4f, 40.3f, 27.6f, 39f, 28.4f)
                lineTo(28.8f, 34.5f)
                cubicTo(25.8f, 36.3f, 21.8f, 34f, 21.8f, 30f)
                close()
            }

            drawPath(path = outerPath, brush = outerGrad)
            drawPath(path = innerPath, brush = innerGrad, alpha = 0.85f)
            drawCircle(
                color = Color.White,
                radius = 3.5f,
                center = Offset(15.8f, 24f)
            )
        }
    }
}

/**
 * Renders the brand emblem with the signature "LumaPlay" two-tone wordmark.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun LumaBrandHeader(
    showWordmark: Boolean,
    modifier: Modifier = Modifier,
    emblemSize: Dp = 36.dp
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        LumaLogoEmblem(size = emblemSize)

        AnimatedVisibility(
            visible = showWordmark,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Spacer(modifier = Modifier.width(12.dp))
                val brandText = buildAnnotatedString {
                    withStyle(SpanStyle(color = LumaTheme.colors.textPrimary)) {
                        append("Luma")
                    }
                    withStyle(SpanStyle(color = LumaTheme.colors.cyan)) {
                        append("Play")
                    }
                }
                Text(
                    text = brandText,
                    style = LumaTheme.typography.headlineMd,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}
