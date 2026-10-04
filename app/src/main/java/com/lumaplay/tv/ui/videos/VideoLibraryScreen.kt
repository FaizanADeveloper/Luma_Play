package com.lumaplay.tv.ui.videos

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.lumaplay.tv.core.theme.LumaTheme
import com.lumaplay.tv.core.ui.components.LumaBadge
import com.lumaplay.tv.core.ui.components.LumaBadgeVariant
import com.lumaplay.tv.core.ui.components.LumaButton
import com.lumaplay.tv.core.ui.components.LumaButtonVariant
import com.lumaplay.tv.core.ui.components.LumaCard
import com.lumaplay.tv.data.model.VideoItem


// -----------------------------------------------------------------
// Video Library Screen — B2-M1
// -----------------------------------------------------------------

/**
 * Full-screen video library browse screen.
 *
 * Layout (from design: video_library/screen.png):
 * - Header row: "Videos" title, item count badge, sort-order tabs
 * - Filter chip row: All, 4K UHD, 1080p, 720p, SD
 * - 4-column LazyVerticalGrid of video cards
 * - Footer strip: rescan action
 *
 * Design honesty rules applied:
 * - Only resolution-based filters are shown (width/height from MediaStore).
 * - No HDR, Dolby Vision, Movies, or Unwatched filters (metadata unavailable).
 * - Cards show title, duration, and resolution badge only.
 * - No fabricated codec, bitrate, or hardware-decode badges.
 *
 * @param viewModel The [VideosViewModel] managing state, sorting, and filtering.
 * @param onVideoClick Navigates to the video player for the selected [VideoItem].
 * @param onRequestPermission Launches the system storage permission dialog.
 * @param sidebarHomeFocusRequester Shared requester used to route D-pad LEFT back to sidebar Home.
 */
@OptIn(ExperimentalTvMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun VideoLibraryScreen(
    viewModel: VideosViewModel,
    onVideoClick: (VideoItem) -> Unit,
    onRequestPermission: () -> Unit,
    sidebarVideosFocusRequester: FocusRequester,
    contentFocusRequester: FocusRequester? = null
) {
    val uiState by viewModel.uiState.collectAsState()
    val sortOrder by viewModel.sortOrder.collectAsState()
    val activeFilter by viewModel.activeFilter.collectAsState()
    val spacing = LumaTheme.spacing

    // Stable FocusRequesters to guarantee focus return
    val allFilterFocusRequester = contentFocusRequester ?: remember { FocusRequester() }
    val clearFilterFocusRequester = remember { FocusRequester() }
    var shouldFocusAllFilter by remember { mutableStateOf(false) }

    // Request initial focus on "All Videos" filter when entering Video Library
    LaunchedEffect(Unit) {
        runCatching { allFilterFocusRequester.requestFocus() }
    }

    // When filter is cleared from empty state, smoothly transfer focus to the "All" chip
    LaunchedEffect(shouldFocusAllFilter, uiState) {
        if (shouldFocusAllFilter && uiState is VideosUiState.Success) {
            shouldFocusAllFilter = false
            runCatching { allFilterFocusRequester.requestFocus() }
        }
    }

    // When entering empty state from a filter with no results, focus the Clear Filter action
    LaunchedEffect(uiState) {
        if (uiState is VideosUiState.Empty && activeFilter != VideoFilter.All) {
            runCatching { clearFilterFocusRequester.requestFocus() }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LumaTheme.colors.background)
    ) {
        when (val state = uiState) {
            is VideosUiState.CheckingPermission,
            is VideosUiState.Loading -> {
                VideoLibraryLoadingContent()
            }

            is VideosUiState.PermissionRequired -> {
                VideoLibraryPermissionContent(
                    onRequestPermission = onRequestPermission,
                    sidebarVideosFocusRequester = sidebarVideosFocusRequester
                )
            }

            is VideosUiState.Empty -> {
                VideoLibraryEmptyContent(
                    activeFilter = activeFilter,
                    onClearFilter = {
                        shouldFocusAllFilter = true
                        viewModel.setFilter(VideoFilter.All)
                    },
                    onRescan = { viewModel.scanVideos() },
                    clearFilterFocusRequester = clearFilterFocusRequester,
                    sidebarVideosFocusRequester = sidebarVideosFocusRequester
                )
            }

            is VideosUiState.Error -> {
                VideoLibraryErrorContent(
                    message = state.message,
                    onRetry = { viewModel.scanVideos() },
                    sidebarVideosFocusRequester = sidebarVideosFocusRequester
                )
            }

            is VideosUiState.Success -> {
                VideoLibrarySuccessContent(
                    videos = state.videos,
                    sortOrder = sortOrder,
                    activeFilter = activeFilter,
                    onSortChange = viewModel::setSortOrder,
                    onFilterChange = viewModel::setFilter,
                    onVideoClick = onVideoClick,
                    onRescan = viewModel::scanVideos,
                    allFilterFocusRequester = allFilterFocusRequester,
                    sidebarVideosFocusRequester = sidebarVideosFocusRequester
                )
            }
        }
    }
}

// -----------------------------------------------------------------
// Success content — header + filter row + grid + footer
// -----------------------------------------------------------------

@OptIn(ExperimentalTvMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
private fun androidx.compose.foundation.layout.ColumnScope.VideoLibrarySuccessContent(
    videos: List<VideoItem>,
    sortOrder: VideoSortOrder,
    activeFilter: VideoFilter,
    onSortChange: (VideoSortOrder) -> Unit,
    onFilterChange: (VideoFilter) -> Unit,
    onVideoClick: (VideoItem) -> Unit,
    onRescan: () -> Unit,
    allFilterFocusRequester: FocusRequester,
    sidebarVideosFocusRequester: FocusRequester
) {
    val spacing = LumaTheme.spacing
    val gridState = rememberLazyGridState()

    // Header row
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = spacing.tvSafeMargin,
                end = spacing.tvSafeMargin,
                top = spacing.spaceMd,
                bottom = spacing.spaceSm
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.spaceSm)
    ) {
        Text(
            text = "Videos",
            style = LumaTheme.typography.displayMd,
            color = LumaTheme.colors.textPrimary
        )
        Spacer(modifier = Modifier.width(spacing.spaceXs))
        // Item count badge using actual indexed count before filter
        LumaBadge(
            text = "${videos.size} ITEMS",
            variant = LumaBadgeVariant.Standard
        )
    }

    // Filter chip row
    VideoFilterRow(
        activeFilter = activeFilter,
        onFilterChange = onFilterChange,
        allFilterFocusRequester = allFilterFocusRequester,
        sidebarVideosFocusRequester = sidebarVideosFocusRequester,
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = spacing.tvSafeMargin,
                end = spacing.tvSafeMargin,
                bottom = spacing.spaceXs
            )
    )

    // Sort order row
    VideoSortRow(
        sortOrder = sortOrder,
        onSortChange = onSortChange,
        sidebarVideosFocusRequester = sidebarVideosFocusRequester,
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = spacing.tvSafeMargin,
                end = spacing.tvSafeMargin,
                bottom = 0.dp // Shifted space into grid contentPadding
            )
    )

    // 4-column video grid
    LazyVerticalGrid(
        columns = GridCells.Fixed(4),
        state = gridState,
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f),
        contentPadding = PaddingValues(
            start = spacing.tvSafeMargin,
            top = 24.dp, // Provide space for top/left focus glow without moving layout
            end = spacing.tvSafeMargin,
            bottom = spacing.spaceLg
        ),
        horizontalArrangement = Arrangement.spacedBy(spacing.gutter),
        verticalArrangement = Arrangement.spacedBy(spacing.gutter)
    ) {
        itemsIndexed(
            items = videos,
            key = { _, video -> video.id }
        ) { index, video ->
            val isFirstInRow = index % 4 == 0

            VideoGridCard(
                video = video,
                onClick = { onVideoClick(video) },
                modifier = Modifier
                    .focusProperties {
                        if (isFirstInRow) left = sidebarVideosFocusRequester
                    }
            )
        }
    }

    // Footer strip — rescan action
    VideoLibraryFooter(
        onRescan = onRescan,
        sidebarVideosFocusRequester = sidebarVideosFocusRequester,
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = spacing.tvSafeMargin,
                end = spacing.tvSafeMargin,
                bottom = spacing.spaceMd,
                top = spacing.spaceXs
            )
    )
}

// -----------------------------------------------------------------
// Video Grid Card
// -----------------------------------------------------------------

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun VideoGridCard(
    video: VideoItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isFocused by remember { mutableStateOf(false) }

    LumaCard(
        onClick = onClick,
        modifier = modifier
            .height(160.dp)
            .onFocusChanged { isFocused = it.isFocused; android.util.Log.d("LumaFocus", "Card isFocused: $isFocused") }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Video thumbnail via Coil VideoFrameDecoder
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(video.contentUri)
                    .crossfade(true)
                    .build(),
                contentDescription = video.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Dark gradient scrim for text legibility (always visible at bottom)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0.0f to Color.Transparent,
                            0.5f to Color.Black.copy(alpha = 0.2f),
                            1.0f to Color.Black.copy(alpha = 0.85f)
                        )
                    )
            )

            // Resolution badge — top-left
            if (video.resolutionTag != "VIDEO") {
                LumaBadge(
                    text = video.resolutionTag,
                    variant = when (video.resolutionTag) {
                        "4K" -> LumaBadgeVariant.Highlight
                        else -> LumaBadgeVariant.Standard
                    },
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                )
            }

            // On-focus Play hint — top-right area, fades in via alpha
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .alpha(if (isFocused) 1f else 0f)
            ) {
                LumaBadge(
                    text = "▶  PLAY",
                    variant = LumaBadgeVariant.Highlight
                )
            }

            // Title + duration — bottom
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = video.title,
                    style = LumaTheme.typography.labelMd,
                    color = LumaTheme.colors.textPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = video.formattedDuration,
                    style = LumaTheme.typography.labelSm,
                    color = LumaTheme.colors.textSecondary
                )
            }
        }
    }
}

// -----------------------------------------------------------------
// Filter chip row
// -----------------------------------------------------------------

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun VideoFilterRow(
    activeFilter: VideoFilter,
    onFilterChange: (VideoFilter) -> Unit,
    allFilterFocusRequester: FocusRequester,
    sidebarVideosFocusRequester: FocusRequester,
    modifier: Modifier = Modifier
) {
    val colors = LumaTheme.colors
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        VideoFilter.entries.forEach { filter ->
            val isActive = filter == activeFilter
            val isAll = filter == VideoFilter.All
            val buttonModifier = if (isAll) {
                Modifier
                    .focusRequester(allFilterFocusRequester)
                    .focusProperties {
                        left = sidebarVideosFocusRequester
                    }
            } else {
                Modifier
            }

            LumaButton(
                onClick = { onFilterChange(filter) },
                variant = if (isActive) LumaButtonVariant.Primary else LumaButtonVariant.Ghost,
                modifier = buttonModifier,
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = filter.label,
                    style = LumaTheme.typography.labelMd,
                    color = if (isActive) colors.textPrimary else colors.textSecondary
                )
            }
        }
    }
}

// -----------------------------------------------------------------
// Sort order row
// -----------------------------------------------------------------

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun VideoSortRow(
    sortOrder: VideoSortOrder,
    onSortChange: (VideoSortOrder) -> Unit,
    sidebarVideosFocusRequester: FocusRequester,
    modifier: Modifier = Modifier
) {
    val colors = LumaTheme.colors
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "SORT BY",
            style = LumaTheme.typography.labelSm,
            color = colors.textMuted,
            modifier = Modifier.padding(end = 4.dp)
        )
        VideoSortOrder.entries.forEachIndexed { index, order ->
            val isActive = order == sortOrder
            val buttonModifier = Modifier
                .focusProperties {
                    if (index == 0) {
                        left = sidebarVideosFocusRequester
                    }
                }

            LumaButton(
                onClick = { onSortChange(order) },
                variant = if (isActive) LumaButtonVariant.Secondary else LumaButtonVariant.Ghost,
                modifier = buttonModifier,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                if (isActive) {
                    Text(
                        text = "• ${order.label}",
                        style = LumaTheme.typography.labelSm,
                        color = colors.cyan
                    )
                } else {
                    Text(
                        text = order.label,
                        style = LumaTheme.typography.labelSm,
                        color = colors.textSecondary
                    )
                }
            }
        }
    }
}

// -----------------------------------------------------------------
// Footer strip
// -----------------------------------------------------------------

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun VideoLibraryFooter(
    onRescan: () -> Unit,
    sidebarVideosFocusRequester: FocusRequester,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(LumaTheme.colors.surfaceCard)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "Local media library",
            style = LumaTheme.typography.bodySm,
            color = LumaTheme.colors.textMuted
        )
        LumaButton(
            onClick = onRescan,
            variant = LumaButtonVariant.Secondary,
            modifier = Modifier.focusProperties {
                left = sidebarVideosFocusRequester
            },
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                text = "↻  Rescan Library",
                style = LumaTheme.typography.labelMd,
                color = LumaTheme.colors.textPrimary
            )
        }
    }
}

// -----------------------------------------------------------------
// Loading state
// -----------------------------------------------------------------

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun VideoLibraryLoadingContent() {
    val spacing = LumaTheme.spacing
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(spacing.tvSafeMargin)
    ) {
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(spacing.spaceMd)
        ) {
            Text(
                text = "Scanning video library…",
                style = LumaTheme.typography.headlineSm,
                color = LumaTheme.colors.textSecondary
            )
        }
    }
}

// -----------------------------------------------------------------
// Empty state
// -----------------------------------------------------------------

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun VideoLibraryEmptyContent(
    activeFilter: VideoFilter,
    onClearFilter: () -> Unit,
    onRescan: () -> Unit,
    clearFilterFocusRequester: FocusRequester,
    sidebarVideosFocusRequester: FocusRequester
) {
    val spacing = LumaTheme.spacing
    val isFiltered = activeFilter != VideoFilter.All

    val emptyTitle = when (activeFilter) {
        VideoFilter.UHD4K -> "No 4K videos found"
        VideoFilter.HD1080 -> "No 1080p videos found"
        VideoFilter.HD720 -> "No 720p videos found"
        VideoFilter.SD -> "No SD videos found"
        VideoFilter.All -> "No videos found"
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(spacing.tvSafeMargin),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(spacing.spaceMd)
        ) {
            Text(
                text = emptyTitle,
                style = LumaTheme.typography.headlineMd,
                color = LumaTheme.colors.textPrimary
            )
            Text(
                text = if (isFiltered)
                    "Try a different filter or rescan your library."
                else
                    "Connect a drive or rescan to discover local video files.",
                style = LumaTheme.typography.bodyMd,
                color = LumaTheme.colors.textSecondary
            )
            Spacer(modifier = Modifier.height(spacing.spaceMd))
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.spaceSm)) {
                if (isFiltered) {
                    LumaButton(
                        onClick = onClearFilter,
                        variant = LumaButtonVariant.Secondary,
                        modifier = Modifier
                            .focusRequester(clearFilterFocusRequester)
                            .focusProperties {
                                left = sidebarVideosFocusRequester
                                up = FocusRequester.Cancel
                            }
                    ) {
                        Text("Clear Filter")
                    }
                }
                LumaButton(
                    onClick = onRescan,
                    variant = LumaButtonVariant.Primary,
                    modifier = Modifier
                        .then(
                            if (!isFiltered) Modifier.focusRequester(clearFilterFocusRequester) else Modifier
                        )
                        .focusProperties {
                            if (!isFiltered) left = sidebarVideosFocusRequester
                            up = FocusRequester.Cancel
                        }
                ) {
                    Text("Rescan Library")
                }
            }
        }
    }
}

// -----------------------------------------------------------------
// Error state
// -----------------------------------------------------------------

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun VideoLibraryErrorContent(
    message: String,
    onRetry: () -> Unit,
    sidebarVideosFocusRequester: FocusRequester
) {
    val spacing = LumaTheme.spacing
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(spacing.tvSafeMargin),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(spacing.spaceMd)
        ) {
            LumaBadge(text = "SCAN ERROR", variant = LumaBadgeVariant.Error)
            Text(
                text = "Unable to scan video library",
                style = LumaTheme.typography.headlineMd,
                color = LumaTheme.colors.textPrimary
            )
            Text(
                text = message,
                style = LumaTheme.typography.bodyMd,
                color = LumaTheme.colors.textSecondary
            )
            Spacer(modifier = Modifier.height(spacing.spaceMd))
            LumaButton(
                onClick = onRetry,
                variant = LumaButtonVariant.Primary,
                modifier = Modifier.focusProperties {
                    left = sidebarVideosFocusRequester
                    up = FocusRequester.Cancel
                }
            ) {
                Text("Retry Scan")
            }
        }
    }
}

// -----------------------------------------------------------------
// Permission state
// -----------------------------------------------------------------

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun VideoLibraryPermissionContent(
    onRequestPermission: () -> Unit,
    sidebarVideosFocusRequester: FocusRequester
) {
    val spacing = LumaTheme.spacing
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(spacing.tvSafeMargin),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(spacing.spaceMd)
        ) {
            LumaBadge(text = "STORAGE ACCESS REQUIRED", variant = LumaBadgeVariant.Accent)
            Text(
                text = "Storage permission is needed to browse videos",
                style = LumaTheme.typography.headlineMd,
                color = LumaTheme.colors.textPrimary
            )
            Text(
                text = "Grant storage access so LumaPlay can discover your local video library.",
                style = LumaTheme.typography.bodyMd,
                color = LumaTheme.colors.textSecondary
            )
            Spacer(modifier = Modifier.height(spacing.spaceMd))
            LumaButton(
                onClick = onRequestPermission,
                variant = LumaButtonVariant.Primary,
                modifier = Modifier.focusProperties {
                    left = sidebarVideosFocusRequester
                    up = FocusRequester.Cancel
                }
            ) {
                Text("Grant Access")
            }
        }
    }
}





