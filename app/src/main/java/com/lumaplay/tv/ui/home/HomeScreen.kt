package com.lumaplay.tv.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import com.lumaplay.tv.core.theme.LumaTheme
import com.lumaplay.tv.core.ui.components.LumaButton
import com.lumaplay.tv.core.ui.components.LumaButtonVariant
import com.lumaplay.tv.core.ui.components.LumaCard
import com.lumaplay.tv.data.model.VideoItem
import com.lumaplay.tv.ui.home.components.HeroBillboard
import com.lumaplay.tv.ui.home.components.MediaCarouselRow

/**
 * Full Cinematic TV Home Dashboard.
 *
 * Implements the Google Stitch home_dashboard design:
 * - Immersive Hero Billboard with active video preview, backdrop scrims, and D-pad action buttons.
 * - Horizontal media swimlanes with real asynchronous thumbnails.
 * - Quick Storage Folders row for instant directory browsing.
 * - Non-blocking D-pad vertical scrolling between sections.
 * - Polished state screens for Permissions, Loading, Empty, and Errors.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onScanClick: () -> Unit,
    onRequestPermission: () -> Unit,
    onVideoClick: (VideoItem) -> Unit,
    modifier: Modifier = Modifier,
    playButtonFocusRequester: FocusRequester? = null,
    sidebarHomeFocusRequester: FocusRequester? = null
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(LumaTheme.colors.background)
    ) {
        when (uiState) {
            is HomeUiState.CheckingPermission -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Initializing LumaPlay TV...",
                        style = LumaTheme.typography.bodyLg,
                        color = LumaTheme.colors.textSecondary
                    )
                }
            }

            is HomeUiState.PermissionRequired -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Text(
                            text = "Storage Permission Required",
                            style = LumaTheme.typography.headlineMd,
                            color = LumaTheme.colors.textPrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "LumaPlay requires access to device storage to discover and play local videos.",
                            style = LumaTheme.typography.bodyMd,
                            color = LumaTheme.colors.textSecondary
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        LumaButton(
                            onClick = onRequestPermission,
                            variant = LumaButtonVariant.Primary,
                            modifier = Modifier.focusProperties {
                                up = FocusRequester.Cancel
                                if (sidebarHomeFocusRequester != null) {
                                    left = sidebarHomeFocusRequester
                                }
                            }
                        ) {
                            Text(text = "Grant Storage Access")
                        }
                    }
                }
            }

            is HomeUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Scanning Storage for Videos...",
                            style = LumaTheme.typography.headlineSm,
                            color = LumaTheme.colors.cyan
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Indexing local video files via MediaStore",
                            style = LumaTheme.typography.bodySm,
                            color = LumaTheme.colors.textSecondary
                        )
                    }
                }
            }

            is HomeUiState.Empty -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Text(
                            text = "No Videos Found",
                            style = LumaTheme.typography.headlineMd,
                            color = LumaTheme.colors.textPrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No local video files were detected on internal or connected storage.",
                            style = LumaTheme.typography.bodyMd,
                            color = LumaTheme.colors.textSecondary
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        LumaButton(
                            onClick = onScanClick,
                            variant = LumaButtonVariant.Secondary,
                            modifier = Modifier.focusProperties {
                                up = FocusRequester.Cancel
                                if (sidebarHomeFocusRequester != null) {
                                    left = sidebarHomeFocusRequester
                                }
                            }
                        ) {
                            Text(text = "Rescan Storage")
                        }
                    }
                }
            }

            is HomeUiState.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Text(
                            text = "Scanning Error",
                            style = LumaTheme.typography.headlineMd,
                            color = LumaTheme.colors.error
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = uiState.message,
                            style = LumaTheme.typography.bodyMd,
                            color = LumaTheme.colors.textSecondary
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        LumaButton(
                            onClick = onScanClick,
                            variant = LumaButtonVariant.Primary,
                            modifier = Modifier.focusProperties {
                                up = FocusRequester.Cancel
                                if (sidebarHomeFocusRequester != null) {
                                    left = sidebarHomeFocusRequester
                                }
                            }
                        ) {
                            Text(text = "Retry Scan")
                        }
                    }
                }
            }

            is HomeUiState.Success -> {
                val scrollState = rememberScrollState()

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(horizontal = 40.dp, vertical = 20.dp)
                ) {
                    // --- 1. Hero Showcase Billboard ---
                    val featuredVideo = uiState.videos.firstOrNull()
                    HeroBillboard(
                        featuredVideo = featuredVideo,
                        onPlay = onVideoClick,
                        onDetails = { /* Details hook */ },
                        onRescan = onScanClick,
                        playButtonFocusRequester = playButtonFocusRequester,
                        sidebarHomeFocusRequester = sidebarHomeFocusRequester
                    )

                    Spacer(modifier = Modifier.height(36.dp))

                    // --- 2. Recent Videos Swimlane ---
                    MediaCarouselRow(
                        title = "Recent Videos",
                        badgeText = "${uiState.videos.size} Videos",
                        videos = uiState.videos,
                        onVideoClick = onVideoClick,
                        sidebarHomeFocusRequester = sidebarHomeFocusRequester
                    )

                    Spacer(modifier = Modifier.height(36.dp))

                    // --- 3. Quick Storage Folders Swimlane ---
                    Text(
                        text = "Storage Folders",
                        style = LumaTheme.typography.headlineSm,
                        color = LumaTheme.colors.textPrimary
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    val folders = listOf(
                        Triple("Movies", "Internal Storage / Movies", "📁"),
                        Triple("Download", "Internal Storage / Download", "📥"),
                        Triple("DCIM", "Internal Storage / DCIM", "📷"),
                        Triple("USB Drive", "Removable Media", "💾")
                    )

                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusProperties {
                                down = FocusRequester.Cancel
                            },
                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                        contentPadding = PaddingValues(end = 48.dp)
                    ) {
                        itemsIndexed(folders) { index, (name, subtitle, icon) ->
                            val cardModifier = if (index == 0 && sidebarHomeFocusRequester != null) {
                                Modifier
                                    .width(220.dp)
                                    .focusProperties {
                                        left = sidebarHomeFocusRequester
                                    }
                            } else {
                                Modifier.width(220.dp)
                            }

                            LumaCard(
                                onClick = { /* Will open Folder Browser in Batch 2 */ },
                                modifier = cardModifier
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp)
                                ) {
                                    Text(
                                        text = icon,
                                        style = LumaTheme.typography.headlineLg,
                                        color = LumaTheme.colors.cyan
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = name,
                                        style = LumaTheme.typography.headlineSm,
                                        color = LumaTheme.colors.textPrimary
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = subtitle,
                                        style = LumaTheme.typography.bodySm,
                                        color = LumaTheme.colors.textSecondary
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(48.dp))
                }
            }
        }
    }
}
