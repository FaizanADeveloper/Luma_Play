package com.lumaplay.tv.ui.home.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import com.lumaplay.tv.R
import com.lumaplay.tv.core.theme.LumaTheme
import com.lumaplay.tv.core.ui.components.LumaButton
import com.lumaplay.tv.core.ui.components.LumaButtonVariant
import com.lumaplay.tv.core.util.StorageTelemetry
import java.util.Locale

/**
 * Storage & Indexing Diagnostics Widget (TV Safe Footer Bar).
 *
 * Implements Google Stitch home_dashboard footer specifications:
 * - Rounded card container ([LumaTheme.colors.surfaceContainerLow]) with soft elevation.
 * - Circular pulse sensor gauge with animated radar pulse and 270° active arc.
 * - Real indexed video count and actual total size formatted from MediaStore.
 * - Truthful Android [StatFs] storage capacity measurements (no fabricated devices or health diagnoses).
 * - Last successful scan relative timestamp.
 * - Remote-accessible D-pad Rescan button with active spinning feedback during background scan.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun StorageIndexFooter(
    videosCount: Int,
    totalSizeBytes: Long,
    lastScannedTimestampMs: Long?,
    isRescanning: Boolean,
    onRescanClick: () -> Unit,
    modifier: Modifier = Modifier,
    rescanErrorMessage: String? = null,
    rescanFocusRequester: FocusRequester? = null,
    sidebarHomeFocusRequester: FocusRequester? = null,
    upFocusRequester: FocusRequester? = null
) {
    val storageTelemetry = remember { StorageTelemetry.query() }

    // Pulse animation for sensor dot
    val infiniteTransition = rememberInfiniteTransition(label = "StorageFooterPulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "SensorPulseAlpha"
    )

    // Continuous rotation animation for sync icon when rescanning
    val syncRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "SyncRotation"
    )

    val relativeScanTime = remember(lastScannedTimestampMs) {
        lastScannedTimestampMs?.let { formatRelativeTime(it) }
    }

    val formattedTotalSize = remember(totalSizeBytes) {
        formatTotalBytes(totalSizeBytes)
    }

    val cyanColor = LumaTheme.colors.cyan

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(LumaTheme.colors.surfaceContainerLow)
            .padding(horizontal = 24.dp, vertical = 20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Group: Pulse Sensor + Telemetry Metrics
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.weight(1f, fill = false)
            ) {
                // 1. Circular Pulse Sensor Gauge matching Stitch SVG
                Canvas(modifier = Modifier.size(40.dp)) {
                    val strokeWidthPx = 3.dp.toPx()
                    val arcPadding = strokeWidthPx / 2f
                    val arcSize = Size(size.width - arcPadding * 2, size.height - arcPadding * 2)
                    val topLeft = Offset(arcPadding, arcPadding)

                    // Outer faint circle (0.2 alpha)
                    drawCircle(
                        color = cyanColor.copy(alpha = 0.20f),
                        radius = size.minDimension / 2f - strokeWidthPx / 2f,
                        style = Stroke(width = strokeWidthPx)
                    )

                    // Active Arc (270 degrees)
                    drawArc(
                        color = cyanColor,
                        startAngle = -90f,
                        sweepAngle = 270f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
                    )

                    // Center glowing/pulsing sensor dot
                    drawCircle(
                        color = cyanColor.copy(alpha = pulseAlpha),
                        radius = 4.dp.toPx(),
                        center = center
                    )
                }

                // 2. Metric Details Column
                Column {
                    // Header Row: Storage Index Status + State + Timestamp
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Storage Index Status:",
                            style = LumaTheme.typography.headlineSm,
                            color = LumaTheme.colors.textPrimary
                        )

                        Text(
                            text = when {
                                isRescanning -> "Scanning..."
                                rescanErrorMessage != null -> "Scan failed"
                                lastScannedTimestampMs != null -> "Up to date"
                                else -> "Ready"
                            },
                            style = LumaTheme.typography.labelLg,
                            color = if (rescanErrorMessage != null) LumaTheme.colors.error else LumaTheme.colors.cyan
                        )

                        Text(
                            text = when {
                                isRescanning -> "(Updating index...)"
                                relativeScanTime != null -> "(Scanned $relativeScanTime)"
                                else -> "(Not yet scanned)"
                            },
                            style = LumaTheme.typography.bodySm,
                            color = LumaTheme.colors.outline
                        )

                        if (rescanErrorMessage != null) {
                            Text(
                                text = "— $rescanErrorMessage",
                                style = LumaTheme.typography.bodySm,
                                color = LumaTheme.colors.error
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    val videoLabel = when (videosCount) {
                        1 -> "Video"
                        else -> "Videos"
                    }

                    // Detail Metrics Row: Real Video Count & Size • Internal Storage Capacity
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Video count & total size
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_nav_videos),
                                contentDescription = null,
                                tint = LumaTheme.colors.cyan,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "$videosCount",
                                style = LumaTheme.typography.bodySm.copy(fontWeight = FontWeight.Bold),
                                color = LumaTheme.colors.textPrimary
                            )
                            Text(
                                text = "$videoLabel ($formattedTotalSize)",
                                style = LumaTheme.typography.bodySm,
                                color = LumaTheme.colors.textSecondary
                            )
                        }

                        Text(
                            text = "•",
                            style = LumaTheme.typography.bodySm,
                            color = LumaTheme.colors.outline
                        )

                        // Real Internal Storage measurement
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_nav_folders),
                                contentDescription = null,
                                tint = LumaTheme.colors.cyan,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = if (storageTelemetry.isAvailable) {
                                    "Internal Storage: ${storageTelemetry.freeGb} GB Free / ${storageTelemetry.totalGb} GB Total"
                                } else {
                                    "Internal Storage: Available"
                                },
                                style = LumaTheme.typography.bodySm,
                                color = LumaTheme.colors.textSecondary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Right Group: Rescan Storage Button
            LumaButton(
                onClick = onRescanClick,
                variant = LumaButtonVariant.Secondary,
                modifier = Modifier
                    .then(
                        if (rescanFocusRequester != null) {
                            Modifier.focusRequester(rescanFocusRequester)
                        } else {
                            Modifier
                        }
                    )
                    .focusProperties {
                        if (upFocusRequester != null) {
                            up = upFocusRequester
                        }
                        if (sidebarHomeFocusRequester != null) {
                            left = sidebarHomeFocusRequester
                        }
                        down = FocusRequester.Cancel
                    }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_action_refresh),
                        contentDescription = null,
                        modifier = Modifier
                            .size(18.dp)
                            .then(
                                if (isRescanning) {
                                    Modifier.rotate(syncRotation)
                                } else {
                                    Modifier
                                }
                            ),
                        tint = if (isRescanning) LumaTheme.colors.cyan else LumaTheme.colors.textPrimary
                    )
                    Text(
                        text = if (isRescanning) "Scanning..." else "Rescan Storage",
                        style = LumaTheme.typography.labelLg,
                        color = LumaTheme.colors.textPrimary
                    )
                }
            }
        }
    }
}

/**
 * Formats a timestamp into a human-readable relative string.
 */
private fun formatRelativeTime(timestampMs: Long): String {
    val diffMs = (System.currentTimeMillis() - timestampMs).coerceAtLeast(0L)
    val diffSec = diffMs / 1000
    val diffMin = diffSec / 60
    return when {
        diffMin < 1 -> "Just now"
        diffMin == 1L -> "1 min ago"
        diffMin < 60 -> "$diffMin mins ago"
        else -> {
            val hours = diffMin / 60
            if (hours == 1L) "1 hour ago" else "$hours hours ago"
        }
    }
}

/**
 * Formats total bytes into MB or GB.
 */
private fun formatTotalBytes(bytes: Long): String {
    if (bytes <= 0) return "0 MB"
    val mb = bytes / (1024.0 * 1024.0)
    return if (mb >= 1024.0) {
        String.format(Locale.US, "%.1f GB", mb / 1024.0)
    } else {
        String.format(Locale.US, "%.0f MB", mb)
    }
}
