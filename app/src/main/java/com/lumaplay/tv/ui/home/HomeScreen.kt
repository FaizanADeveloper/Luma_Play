package com.lumaplay.tv.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.focusGroup
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import com.lumaplay.tv.R
import com.lumaplay.tv.core.theme.LumaTheme
import com.lumaplay.tv.core.ui.components.LumaBadge
import com.lumaplay.tv.core.ui.components.LumaBadgeVariant
import com.lumaplay.tv.core.ui.components.LumaButton
import com.lumaplay.tv.core.ui.components.LumaButtonVariant
import com.lumaplay.tv.core.ui.components.LumaCard
import com.lumaplay.tv.data.model.VideoItem
import androidx.compose.ui.text.font.FontWeight
import com.lumaplay.tv.ui.home.components.HeroBillboard
import com.lumaplay.tv.ui.home.components.MediaCarouselRow
import com.lumaplay.tv.ui.home.components.MediaDetailsDialog
import com.lumaplay.tv.ui.home.components.StorageIndexFooter

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
    var detailsVideo by remember { mutableStateOf<VideoItem?>(null) }

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
                val coroutineScope = rememberCoroutineScope()
                val firstFolderFocusRequester = remember { FocusRequester() }
                val rescanFocusRequester = remember { FocusRequester() }

                val density = LocalDensity.current
                val topSafeMargin = with(density) { 16.dp.toPx() }
                val bottomSafeMargin = with(density) { 96.dp.toPx() }
                val heroThresholdPx = with(density) { 450.dp.toPx() }

                @OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
                val tvBringIntoViewSpec = remember(topSafeMargin, bottomSafeMargin, heroThresholdPx, scrollState) {
                    object : androidx.compose.foundation.gestures.BringIntoViewSpec {
                        override fun calculateScrollDistance(
                            offset: Float,
                            size: Float,
                            containerSize: Float
                        ): Float {
                            // If the item requesting bring-into-view is within the Hero Showcase (y < 450dp),
                            // always restore the entire hero billboard by scrolling back to 0!
                            val absoluteY = scrollState.value + offset
                            if (absoluteY < heroThresholdPx) {
                                return if (scrollState.value > 0) -scrollState.value.toFloat() else 0f
                            }

                            val targetTop = topSafeMargin
                            val targetBottom = containerSize - bottomSafeMargin
                            if (offset >= targetTop && offset + size <= targetBottom) {
                                return 0f
                            }
                            return if (offset < targetTop) {
                                offset - targetTop
                            } else {
                                offset + size - targetBottom
                            }
                        }
                    }
                }

                @OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
                androidx.compose.runtime.CompositionLocalProvider(
                    androidx.compose.foundation.gestures.LocalBringIntoViewSpec provides tvBringIntoViewSpec
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState)
                            .padding(vertical = 8.dp)
                    ) {
                        // --- 1. Hero Showcase Billboard ---
                        val featuredVideo = uiState.videos.firstOrNull()
                        HeroBillboard(
                            featuredVideo = featuredVideo,
                            onPlay = onVideoClick,
                            onDetails = { video -> detailsVideo = video },
                            playButtonFocusRequester = playButtonFocusRequester,
                            sidebarHomeFocusRequester = sidebarHomeFocusRequester,
                            onFocusGained = {
                                if (scrollState.value > 0) {
                                    coroutineScope.launch {
                                        scrollState.animateScrollTo(0)
                                    }
                                }
                            },
                            modifier = Modifier.padding(horizontal = 40.dp)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // --- 2. Recently Added Videos Swimlane ---
                        MediaCarouselRow(
                            title = "Recently Added Videos",
                            badgeText = when (uiState.videos.size) {
                                1 -> "1 Video"
                                else -> "${uiState.videos.size} Videos"
                            },
                            videos = uiState.videos,
                            onVideoClick = onVideoClick,
                            sidebarHomeFocusRequester = sidebarHomeFocusRequester,
                            heroFocusRequester = playButtonFocusRequester,
                            downFocusRequester = firstFolderFocusRequester,
                            contentPadding = PaddingValues(start = 40.dp, end = 56.dp, top = 24.dp, bottom = 24.dp),
                            headerPadding = PaddingValues(horizontal = 40.dp)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // --- 3. Storage Folders Swimlane ---
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 40.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_nav_folders),
                                    contentDescription = null,
                                    tint = LumaTheme.colors.cyan,
                                    modifier = Modifier.size(22.dp)
                                )
                                Text(
                                    text = "Storage Folders",
                                    style = LumaTheme.typography.headlineSm,
                                    color = LumaTheme.colors.textPrimary
                                )
                            }

                            LumaBadge(
                                text = "Internal Storage",
                                variant = LumaBadgeVariant.Standard
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        val moviesCount = uiState.videos.count { it.folderName.contains("Movies", ignoreCase = true) }
                        val downloadCount = uiState.videos.count { it.folderName.contains("Download", ignoreCase = true) }
                        val dcimCount = uiState.videos.count { it.folderName.contains("DCIM", ignoreCase = true) }

                        val discoverableFolders = listOf(
                            StorageFolderItem(
                                name = "Movies",
                                path = "Internal Storage / Movies",
                                iconResId = R.drawable.ic_folder_movie,
                                badgeText = if (moviesCount == 1) "1 video" else "$moviesCount videos"
                            ),
                            StorageFolderItem(
                                name = "Download",
                                path = "Internal Storage / Download",
                                iconResId = R.drawable.ic_folder_download,
                                badgeText = when {
                                    downloadCount == 1 -> "1 video"
                                    downloadCount > 1 -> "$downloadCount videos"
                                    else -> "Ready"
                                }
                            ),
                            StorageFolderItem(
                                name = "DCIM",
                                path = "Internal Storage / DCIM",
                                iconResId = R.drawable.ic_folder_camera,
                                badgeText = when {
                                    dcimCount == 1 -> "1 video"
                                    dcimCount > 1 -> "$dcimCount videos"
                                    else -> "Camera"
                                }
                            ),
                            StorageFolderItem(
                                name = "Music",
                                path = "Internal Storage / Music",
                                iconResId = R.drawable.ic_folder_music,
                                badgeText = "Audio"
                            )
                        )

                        LazyRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusProperties {
                                    down = rescanFocusRequester
                                },
                            horizontalArrangement = Arrangement.spacedBy(24.dp),
                            contentPadding = PaddingValues(start = 40.dp, end = 56.dp, top = 24.dp, bottom = 24.dp)
                        ) {
                            itemsIndexed(discoverableFolders) { index, folder ->
                                val cardModifier = if (index == 0) {
                                    Modifier
                                        .width(240.dp)
                                        .focusRequester(firstFolderFocusRequester)
                                        .focusProperties {
                                            down = rescanFocusRequester
                                            if (sidebarHomeFocusRequester != null) {
                                                left = sidebarHomeFocusRequester
                                            }
                                        }
                                } else {
                                    Modifier
                                        .width(240.dp)
                                        .focusProperties {
                                            down = rescanFocusRequester
                                        }
                                }

                                LumaCard(
                                    onClick = { /* Functional shortcut: Folder navigation in upcoming milestone */ },
                                    modifier = cardModifier
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.Top
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(44.dp)
                                                    .background(
                                                        color = LumaTheme.colors.surfaceContainerHighest,
                                                        shape = RoundedCornerShape(10.dp)
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    painter = painterResource(id = folder.iconResId),
                                                    contentDescription = folder.name,
                                                    tint = LumaTheme.colors.cyan,
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            }

                                            Box(
                                                modifier = Modifier
                                                    .background(
                                                        color = LumaTheme.colors.surfaceContainerHighest,
                                                        shape = RoundedCornerShape(4.dp)
                                                    )
                                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                            ) {
                                                Text(
                                                    text = folder.badgeText,
                                                    style = LumaTheme.typography.labelSm,
                                                    color = LumaTheme.colors.textSecondary
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(16.dp))

                                        Text(
                                            text = folder.name,
                                            style = LumaTheme.typography.labelLg,
                                            color = LumaTheme.colors.textPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = folder.path,
                                            style = LumaTheme.typography.bodySm,
                                            color = LumaTheme.colors.textSecondary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(28.dp))

                        // --- 4. Storage & Indexing Diagnostics Footer ---
                        val totalVideosSize = remember(uiState.videos) {
                            uiState.videos.sumOf { it.sizeBytes }
                        }

                        StorageIndexFooter(
                            videosCount = uiState.videos.size,
                            totalSizeBytes = totalVideosSize,
                            lastScannedTimestampMs = uiState.lastScannedTimestampMs,
                            isRescanning = uiState.isRescanning,
                            rescanErrorMessage = uiState.rescanErrorMessage,
                            onRescanClick = onScanClick,
                            rescanFocusRequester = rescanFocusRequester,
                            sidebarHomeFocusRequester = sidebarHomeFocusRequester,
                            upFocusRequester = firstFolderFocusRequester,
                            modifier = Modifier.padding(horizontal = 40.dp)
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // --- 5. Remote Navigation Guidance Pill (Stitch Reference) ---
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 40.dp),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(9999.dp))
                                    .background(LumaTheme.colors.surfaceContainerHighest.copy(alpha = 0.90f))
                                    .padding(horizontal = 16.dp, vertical = 6.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = "DPAD:",
                                            style = LumaTheme.typography.labelSm,
                                            fontWeight = FontWeight.Bold,
                                            color = LumaTheme.colors.cyan
                                        )
                                        Text(
                                            text = "Navigate",
                                            style = LumaTheme.typography.labelSm,
                                            color = LumaTheme.colors.textPrimary
                                        )
                                    }

                                    Text(
                                        text = "•",
                                        style = LumaTheme.typography.labelSm,
                                        color = LumaTheme.colors.outline
                                    )

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = "OK:",
                                            style = LumaTheme.typography.labelSm,
                                            fontWeight = FontWeight.Bold,
                                            color = LumaTheme.colors.cyan
                                        )
                                        Text(
                                            text = "Select",
                                            style = LumaTheme.typography.labelSm,
                                            color = LumaTheme.colors.textPrimary
                                        )
                                    }

                                    Text(
                                        text = "•",
                                        style = LumaTheme.typography.labelSm,
                                        color = LumaTheme.colors.outline
                                    )

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = "BACK:",
                                            style = LumaTheme.typography.labelSm,
                                            fontWeight = FontWeight.Bold,
                                            color = LumaTheme.colors.cyan
                                        )
                                        Text(
                                            text = "Exit",
                                            style = LumaTheme.typography.labelSm,
                                            color = LumaTheme.colors.textPrimary
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

    if (detailsVideo != null) {
        MediaDetailsDialog(
            video = detailsVideo!!,
            onDismiss = { detailsVideo = null }
        )
    }
}
}

/**
 * Storage folder item representing a real discoverable media directory on local storage.
 */
private data class StorageFolderItem(
    val name: String,
    val path: String,
    val iconResId: Int,
    val badgeText: String
)
