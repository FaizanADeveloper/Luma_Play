package com.lumaplay.tv.core.ui.shell

import android.os.Environment
import android.os.StatFs
import android.view.KeyEvent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import com.lumaplay.tv.core.theme.LumaTheme
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Top-level TV navigation destinations defined in Google Stitch designs.
 */
enum class TvNavDestination(val label: String, val iconText: String) {
    Home("Home", "⌂"),
    Videos("Videos", "▶"),
    Music("Music", "♫"),
    Folders("Folders", "📁"),
    Search("Search", "🔍"),
    Settings("Settings", "⚙")
}

/**
 * Collapsible Android TV Sidebar Navigation.
 *
 * Implements Google Stitch specifications:
 * - Collapsed width of 88dp (icons and brand emblem).
 * - Expands to 260dp when navigated into via remote D-pad.
 * - Active cyan pill (4dp width) pinned to the left edge of the selected destination.
 * - Real storage capacity query via [StatFs] (no hardcoded fake USB labels).
 * - Real live system time ticker.
 * - Does not trap focus: pressing D-pad Right smoothly exits into main content.
 *
 * @param selectedDestination Currently active navigation destination.
 * @param onDestinationSelected Callback when user presses D-pad Center on a destination.
 * @param modifier Composable modifier.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvSidebar(
    selectedDestination: TvNavDestination,
    onDestinationSelected: (TvNavDestination) -> Unit,
    modifier: Modifier = Modifier,
    dashboardFocusRequester: FocusRequester? = null,
    homeFocusRequester: FocusRequester? = null,
    videosFocusRequester: FocusRequester? = null
) {
    var isSidebarFocused by remember { mutableStateOf(false) }

    val sidebarWidth by animateDpAsState(
        targetValue = if (isSidebarFocused) 260.dp else 88.dp,
        label = "SidebarWidthAnimation"
    )

    // Dynamic real storage query
    val storageInfo = remember {
        runCatching {
            val stat = StatFs(Environment.getExternalStorageDirectory().path)
            val freeBytes = stat.availableBytes
            val totalBytes = stat.totalBytes
            val freeGb = freeBytes / (1024L * 1024L * 1024L)
            val totalGb = totalBytes / (1024L * 1024L * 1024L)
            val usedRatio = if (totalBytes > 0) 1f - (freeBytes.toFloat() / totalBytes.toFloat()) else 0f
            Triple(freeGb, totalGb, usedRatio.coerceIn(0f, 1f))
        }.getOrDefault(Triple(0L, 0L, 0f))
    }

    // Live clock ticker
    var currentTime by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        val formatter = SimpleDateFormat("hh:mm a", Locale.getDefault())
        while (true) {
            currentTime = formatter.format(Date())
            delay(10_000L)
        }
    }

    Box(
        modifier = modifier
            .width(sidebarWidth)
            .fillMaxHeight()
            .background(LumaTheme.colors.surfaceContainerLowest)
            .padding(16.dp)
            .onFocusChanged { state ->
                isSidebarFocused = state.hasFocus
            }
    ) {
        Column(
            modifier = Modifier.fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // --- 1. Brand Logo & Header ---
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(LumaTheme.shapes.medium)
                            .background(LumaTheme.colors.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "L",
                            style = LumaTheme.typography.headlineSm,
                            color = LumaTheme.colors.textPrimary
                        )
                    }

                    AnimatedVisibility(
                        visible = isSidebarFocused,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Row {
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "LumaPlay",
                                style = LumaTheme.typography.headlineMd,
                                color = LumaTheme.colors.textPrimary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // --- 2. Nav Items ---
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusGroup()
                ) {
                    TvNavDestination.entries.forEach { destination ->
                        val isSelected = destination == selectedDestination
                        var isItemFocused by remember { mutableStateOf(false) }

                        val itemFocusModifier = when (destination) {
                            TvNavDestination.Home -> if (homeFocusRequester != null) Modifier.focusRequester(homeFocusRequester) else Modifier
                            TvNavDestination.Videos -> if (videosFocusRequester != null) Modifier.focusRequester(videosFocusRequester) else Modifier
                            else -> Modifier
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .padding(vertical = 4.dp)
                                .then(itemFocusModifier)
                                .clip(LumaTheme.shapes.medium)
                                .background(
                                    when {
                                        isItemFocused -> LumaTheme.colors.surfaceContainerHigh
                                        isSelected -> LumaTheme.colors.surface.copy(alpha = 0.6f)
                                        else -> Color.Transparent
                                    }
                                )
                                .border(
                                    width = if (isItemFocused) 2.dp else 0.dp,
                                    color = if (isItemFocused) LumaTheme.colors.cyan else Color.Transparent,
                                    shape = LumaTheme.shapes.medium
                                )
                                .onFocusChanged { focusState ->
                                    isItemFocused = focusState.isFocused
                                }
                                .focusProperties {
                                    if (destination == TvNavDestination.Home) {
                                        up = FocusRequester.Cancel
                                    }
                                    if (destination == TvNavDestination.Settings) {
                                        down = FocusRequester.Cancel
                                    }
                                    if (dashboardFocusRequester != null) {
                                        right = dashboardFocusRequester
                                    }
                                }
                                .focusable()
                                .onPreviewKeyEvent { keyEvent ->
                                    if (keyEvent.type == KeyEventType.KeyDown &&
                                        (keyEvent.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_DPAD_CENTER ||
                                         keyEvent.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_ENTER)
                                    ) {
                                        onDestinationSelected(destination)
                                        true
                                    } else {
                                        false
                                    }
                                }
                        ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Active cyan pill indicator on left edge
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .width(4.dp)
                                        .fillMaxHeight()
                                        .background(LumaTheme.colors.cyan)
                                )
                            } else {
                                Spacer(modifier = Modifier.width(4.dp))
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Text(
                                text = destination.iconText,
                                style = LumaTheme.typography.headlineSm,
                                color = if (isItemFocused || isSelected) LumaTheme.colors.cyan else LumaTheme.colors.textSecondary
                            )

                            AnimatedVisibility(
                                visible = isSidebarFocused,
                                enter = fadeIn(),
                                exit = fadeOut()
                            ) {
                                Row {
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Text(
                                        text = destination.label,
                                        style = LumaTheme.typography.labelLg,
                                        color = if (isItemFocused || isSelected) LumaTheme.colors.textPrimary else LumaTheme.colors.textSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

            // --- 3. Bottom Storage & Time Telemetry ---
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp)
            ) {
                // Storage Bar
                if (isSidebarFocused) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(LumaTheme.shapes.medium)
                            .background(LumaTheme.colors.surface)
                            .padding(10.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Device Storage",
                                    style = LumaTheme.typography.labelSm,
                                    color = LumaTheme.colors.textSecondary
                                )
                                Text(
                                    text = "${storageInfo.first} GB Free",
                                    style = LumaTheme.typography.labelSm,
                                    color = LumaTheme.colors.cyan
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(LumaTheme.colors.surfaceContainerHighest)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(storageInfo.third)
                                        .fillMaxHeight()
                                        .background(LumaTheme.colors.primary)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Clock
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (isSidebarFocused) Arrangement.SpaceBetween else Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isSidebarFocused) {
                        Text(
                            text = "TV System",
                            style = LumaTheme.typography.labelSm,
                            color = LumaTheme.colors.textMuted
                        )
                    }
                    Text(
                        text = currentTime,
                        style = LumaTheme.typography.labelMd,
                        color = LumaTheme.colors.textSecondary
                    )
                }
            }
        }
    }
}
