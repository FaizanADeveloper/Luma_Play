package com.lumaplay.tv.core.ui.shell

import android.view.KeyEvent
import com.lumaplay.tv.core.util.StorageTelemetry
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import com.lumaplay.tv.R
import com.lumaplay.tv.core.theme.LumaTheme
import com.lumaplay.tv.core.ui.components.LumaBrandHeader
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Top-level TV navigation destinations defined in Google Stitch designs.
 */
enum class TvNavDestination(val label: String, val iconRes: Int) {
    Home("Home", R.drawable.ic_nav_home),
    Videos("Videos", R.drawable.ic_nav_videos),
    Music("Music", R.drawable.ic_nav_music),
    Folders("Folders", R.drawable.ic_nav_folders),
    Search("Search", R.drawable.ic_nav_search),
    Settings("Settings", R.drawable.ic_nav_settings)
}

/**
 * Collapsible Android TV Sidebar Navigation.
 *
 * Implements Google Stitch specifications:
 * - Collapsed width of 88dp (icons and brand emblem).
 * - Expands to 280dp when navigated into via remote D-pad.
 * - Official LumaPlay Ribbon Play Emblem and two-tone brand wordmark.
 * - Consistent vector Material navigation symbols.
 * - Active pill indicator styling with Stitch elevated container ([LumaTheme.colors.surfaceContainerHigh]).
 * - Real internal storage query via [StatFs] with pulsing indicator dot.
 * - Live system time ticker formatted with TV typography.
 * - Non-blocking D-pad traversal: pressing D-pad Right smoothly exits into main content.
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
    videosFocusRequester: FocusRequester? = null,
    musicFocusRequester: FocusRequester? = null,
    searchFocusRequester: FocusRequester? = null
) {
    var isSidebarFocused by remember { mutableStateOf(false) }

    val sidebarWidth by animateDpAsState(
        targetValue = if (isSidebarFocused) 280.dp else 88.dp,
        label = "SidebarWidthAnimation"
    )

    // Dynamic real storage query via shared StorageTelemetry
    val storageTelemetry = remember { StorageTelemetry.query() }

    // Live clock ticker
    var currentTime by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        val formatter = SimpleDateFormat("hh:mm a", Locale.getDefault())
        while (true) {
            currentTime = formatter.format(Date())
            delay(10_000L)
        }
    }

    // Pulse animation for storage telemetry sensor
    val infiniteTransition = rememberInfiniteTransition(label = "StoragePulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "StoragePulseAlpha"
    )

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
                LumaBrandHeader(
                    showWordmark = isSidebarFocused,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                    emblemSize = 36.dp
                )

                Spacer(modifier = Modifier.height(20.dp))

                // --- 2. Navigation Items ---
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusGroup(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    TvNavDestination.entries.forEach { destination ->
                        val isSelected = destination == selectedDestination
                        var isItemFocused by remember { mutableStateOf(false) }

                        val itemFocusModifier = when (destination) {
                            TvNavDestination.Home -> if (homeFocusRequester != null) Modifier.focusRequester(homeFocusRequester) else Modifier
                            TvNavDestination.Videos -> if (videosFocusRequester != null) Modifier.focusRequester(videosFocusRequester) else Modifier
                            TvNavDestination.Music -> if (musicFocusRequester != null) Modifier.focusRequester(musicFocusRequester) else Modifier
                            TvNavDestination.Search -> if (searchFocusRequester != null) Modifier.focusRequester(searchFocusRequester) else Modifier
                            else -> Modifier
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .then(itemFocusModifier)
                                .clip(LumaTheme.shapes.medium)
                                .background(
                                    when {
                                        isItemFocused -> LumaTheme.colors.surfaceContainerHigh
                                        isSelected -> LumaTheme.colors.surfaceContainerHigh
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
                                    left = FocusRequester.Cancel
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
                                    .fillMaxHeight()
                                    .padding(horizontal = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Consistent Material Vector Icon
                                Icon(
                                    painter = painterResource(id = destination.iconRes),
                                    contentDescription = destination.label,
                                    modifier = Modifier.size(22.dp),
                                    tint = if (isItemFocused || isSelected) {
                                        LumaTheme.colors.cyan
                                    } else {
                                        LumaTheme.colors.textSecondary
                                    }
                                )

                                AnimatedVisibility(
                                    visible = isSidebarFocused,
                                    enter = fadeIn(),
                                    exit = fadeOut()
                                ) {
                                    Row {
                                        Spacer(modifier = Modifier.width(14.dp))
                                        Text(
                                            text = destination.label,
                                            style = LumaTheme.typography.labelLg,
                                            color = if (isItemFocused || isSelected) {
                                                LumaTheme.colors.textPrimary
                                            } else {
                                                LumaTheme.colors.textSecondary
                                            }
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
                    .padding(horizontal = 2.dp)
            ) {
                // Real Internal Storage Card (Stitch: bg-surface-container-low, rounded-lg)
                if (isSidebarFocused) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(LumaTheme.shapes.medium)
                            .background(LumaTheme.colors.surfaceContainerLow)
                            .padding(12.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    // Pulsing indicator dot
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(LumaTheme.colors.cyan.copy(alpha = pulseAlpha))
                                    )
                                    Text(
                                        text = "Internal Storage",
                                        style = LumaTheme.typography.labelSm,
                                        color = LumaTheme.colors.textSecondary
                                    )
                                }
                                Text(
                                    text = if (storageTelemetry.isAvailable) "Available" else "Unavailable",
                                    style = LumaTheme.typography.labelSm,
                                    color = if (storageTelemetry.isAvailable) LumaTheme.colors.cyan else LumaTheme.colors.outline
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Storage Capacity Bar
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(LumaTheme.shapes.pill)
                                    .background(LumaTheme.colors.surfaceContainerHighest)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(storageTelemetry.usedRatio)
                                        .fillMaxHeight()
                                        .background(LumaTheme.colors.cyan)
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Metrics Labels
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = if (storageTelemetry.isAvailable) "${storageTelemetry.freeGb} GB Free" else "--",
                                    style = LumaTheme.typography.labelSm,
                                    color = LumaTheme.colors.outline
                                )
                                Text(
                                    text = if (storageTelemetry.isAvailable) "${storageTelemetry.totalGb} GB Total" else "--",
                                    style = LumaTheme.typography.labelSm,
                                    color = LumaTheme.colors.outline
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                } else {
                    // Compact mini indicator when collapsed
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(LumaTheme.colors.cyan.copy(alpha = pulseAlpha))
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }

                // Clock & System Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = if (isSidebarFocused) Arrangement.SpaceBetween else Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isSidebarFocused) {
                        Text(
                            text = "TV System",
                            style = LumaTheme.typography.labelMd,
                            color = LumaTheme.colors.outline
                        )
                        Text(
                            text = currentTime,
                            style = LumaTheme.typography.headlineSm,
                            color = LumaTheme.colors.textPrimary
                        )
                    } else {
                        Text(
                            text = currentTime,
                            style = LumaTheme.typography.labelSm,
                            color = LumaTheme.colors.textSecondary
                        )
                    }
                }
            }
        }
    }
}
