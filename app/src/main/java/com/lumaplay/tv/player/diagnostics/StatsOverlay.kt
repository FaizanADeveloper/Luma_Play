package com.lumaplay.tv.player.diagnostics

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import com.lumaplay.tv.core.theme.LumaTheme

/**
 * Floating 10-foot television diagnostic overlay ("Stats for Nerds").
 *
 * Faithfully matches Google Stitch reference (full_screen_video_player_with_osd):
 * - Glassmorphic surface container lowest with blur simulation.
 * - Accurate real-time telemetry from Media3 ExoPlayer without fabricated metrics.
 * - Clear TV readability with monospace values for technical alignment.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun StatsOverlay(
    isVisible: Boolean,
    telemetry: VideoTelemetryState,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn() + slideInHorizontally { it / 2 },
        exit = fadeOut() + slideOutHorizontally { it / 2 },
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .width(420.dp)
                .shadow(elevation = 24.dp, shape = RoundedCornerShape(16.dp))
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF080E1D).copy(alpha = 0.92f))
                .border(
                    width = 1.dp,
                    color = LumaTheme.colors.outlineVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(16.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Header Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    LumaTheme.colors.cyan.copy(alpha = 0.15f),
                                    Color.Transparent
                                )
                            )
                        )
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(LumaTheme.colors.cyan)
                        )
                        Text(
                            text = "ExoPlayer Media3 Diagnostics",
                            style = LumaTheme.typography.labelMd.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.8.sp
                            ),
                            color = LumaTheme.colors.cyan
                        )
                    }

                    Text(
                        text = "v1.3.1",
                        style = LumaTheme.typography.labelSm.copy(fontSize = 10.sp),
                        color = LumaTheme.colors.textSecondary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(LumaTheme.colors.surfaceContainerHigh)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Telemetry rows
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TelemetryRow(
                        label = "Video Decoder",
                        value = telemetry.formattedVideoDecoder,
                        valueColor = LumaTheme.colors.cyan
                    )
                    TelemetryRow(
                        label = "Resolution / Refresh",
                        value = telemetry.formattedResolutionAndFps,
                        valueColor = LumaTheme.colors.textPrimary
                    )
                    TelemetryRow(
                        label = "Frame Drop Rate",
                        value = telemetry.formattedDroppedFrames,
                        valueColor = if (telemetry.droppedFrames > 0) Color(0xFFFBBF24) else Color(0xFF34D399),
                        leadingDotColor = if (telemetry.droppedFrames > 0) Color(0xFFFBBF24) else Color(0xFF34D399)
                    )
                    TelemetryRow(
                        label = "Color / HDR",
                        value = telemetry.formattedColorInfo,
                        valueColor = if (telemetry.formattedColorInfo.contains("HDR") || telemetry.formattedColorInfo.contains("HLG")) {
                            Color(0xFFFBBF24)
                        } else {
                            LumaTheme.colors.textSecondary
                        }
                    )
                    TelemetryRow(
                        label = "Audio Stream",
                        value = telemetry.formattedAudioFormat,
                        valueColor = LumaTheme.colors.cyan
                    )
                    TelemetryRow(
                        label = "Audio Decoder",
                        value = telemetry.formattedAudioDecoder,
                        valueColor = LumaTheme.colors.textSecondary
                    )
                    TelemetryRow(
                        label = "Buffered Ahead",
                        value = telemetry.formattedBufferAhead,
                        valueColor = LumaTheme.colors.textPrimary
                    )

                }

                Spacer(modifier = Modifier.height(12.dp))

                // Remote D-pad Dismiss Hint
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(LumaTheme.colors.surfaceContainerLowest.copy(alpha = 0.6f))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Press BACK to dismiss diagnostics",
                        style = LumaTheme.typography.labelSm.copy(fontSize = 11.sp),
                        color = LumaTheme.colors.outline
                    )
                }
            }
        }
    }
}

@Composable
private fun TelemetryRow(
    label: String,
    value: String,
    valueColor: Color,
    leadingDotColor: Color? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFF151B2B).copy(alpha = 0.75f))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = LumaTheme.typography.bodySm.copy(fontSize = 12.sp),
            color = LumaTheme.colors.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false)
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (leadingDotColor != null) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(leadingDotColor)
                )
            }
            Text(
                text = value,
                style = LumaTheme.typography.labelSm.copy(
                    fontSize = 11.5.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold
                ),
                color = valueColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
