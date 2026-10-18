package com.lumaplay.tv.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.zIndex
import kotlinx.coroutines.Job
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.delay
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import android.view.KeyEvent
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.lumaplay.tv.R
import com.lumaplay.tv.core.theme.LumaTheme
import com.lumaplay.tv.core.ui.components.LumaBadge
import com.lumaplay.tv.core.ui.components.LumaBadgeVariant
import com.lumaplay.tv.core.ui.components.LumaButton
import com.lumaplay.tv.core.ui.components.LumaButtonVariant
import com.lumaplay.tv.core.ui.components.LumaCard
import com.lumaplay.tv.data.model.AlbumItem
import com.lumaplay.tv.data.model.ArtistItem
import com.lumaplay.tv.data.model.AudioItem
import com.lumaplay.tv.data.model.SearchResults
import com.lumaplay.tv.data.model.VideoItem

/**
 * Universal Offline Search Screen for Android TV.
 *
 * Implements the approved Google Stitch design specifications for 10-foot TV interaction:
 * - 12-column layout (5-column query input, filters & 6-column keyboard; 7-column categorized results).
 * - Full remote control / D-pad support with visible Electric Cyan focus rings.
 * - Presentation-only text input bar driven by a TV-friendly 6-column on-screen keyboard.
 * - Single-line category filters (All, Videos, Music, Folders) with compact badges.
 * - Categorized result sections: 2-column widescreen video card grid, music tracks, artists, albums, folders.
 * - Loading, Empty, Error, and Idle states.
 * - Informational notice for voice search planned in Phase 7 without microphone permissions.
 *
 * @param viewModel State manager for query debouncing, filtering, and repository discovery.
 * @param sidebarSearchFocusRequester Shared requester used to navigate D-pad LEFT back to sidebar Search item.
 * @param contentFocusRequester Optional requester for initial focus on screen entry.
 * @param onVideoClick Callback when a video search result item is clicked.
 * @param onTrackClick Callback when an audio track search result item is clicked.
 * @param onArtistClick Callback when an artist search result item is clicked.
 * @param onAlbumClick Callback when an album search result item is clicked.
 * @param onFolderClick Callback when a folder search result item is clicked.
 */
@OptIn(ExperimentalTvMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    sidebarSearchFocusRequester: FocusRequester? = null,
    contentFocusRequester: FocusRequester? = null,
    onVideoClick: ((VideoItem) -> Unit)? = null,
    onTrackClick: ((AudioItem) -> Unit)? = null,
    onArtistClick: ((ArtistItem) -> Unit)? = null,
    onAlbumClick: ((AlbumItem) -> Unit)? = null,
    onFolderClick: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val spacing = LumaTheme.spacing

    var showVoiceNotice by remember { mutableStateOf(false) }

    // Requesters for deterministic D-pad landing and bidirectional panel bridging
    val firstKeyFocusRequester = remember { FocusRequester() }
    val initialFocusRequester = contentFocusRequester ?: firstKeyFocusRequester
    val keyFFocusRequester = remember { FocusRequester() }
    val allChipFocusRequester = remember { FocusRequester() }
    val foldersChipFocusRequester = remember { FocusRequester() }
    val micFocusRequester = remember { FocusRequester() }
    val firstResultFocusRequester = remember { FocusRequester() }

    val hasResults = uiState.status is SearchStatus.Success && !uiState.isFilteredEmpty
    val activeResultsFocusRequester = if (hasResults) firstResultFocusRequester else null

    LaunchedEffect(Unit) {
        delay(150)
        runCatching { initialFocusRequester.requestFocus() }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(LumaTheme.colors.background)
            .padding(
                start = spacing.spaceMd,
                end = spacing.tvSafeMargin,
                top = spacing.spaceSm,
                bottom = spacing.spaceSm
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .focusGroup(),
            horizontalArrangement = Arrangement.spacedBy(spacing.gutter)
        ) {
            // =================================================================
            // LEFT REGION: 5 COLUMNS (Query Bar, Filter Chips, Keyboard, Voice Notice)
            // =================================================================
            Column(
                modifier = Modifier
                    .weight(5f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(spacing.spaceSm)
            ) {
                // Query Display Bar with Mic and Clear Action
                SearchQueryBar(
                    query = uiState.query,
                    onMicClick = { showVoiceNotice = !showVoiceNotice },
                    onClearClick = { viewModel.onClearQuery() },
                    resultsFocusRequester = activeResultsFocusRequester,
                    sidebarSearchFocusRequester = sidebarSearchFocusRequester,
                    micFocusRequester = micFocusRequester,
                    foldersChipFocusRequester = foldersChipFocusRequester
                )

                // Category Filter Chips Row (All, Videos, Music, Folders)
                SearchFilterChipsRow(
                    selectedCategory = uiState.selectedCategory,
                    rawResults = uiState.rawResults,
                    isSearchSuccess = uiState.status is SearchStatus.Success,
                    onCategorySelected = viewModel::onCategorySelected,
                    resultsFocusRequester = activeResultsFocusRequester,
                    sidebarSearchFocusRequester = sidebarSearchFocusRequester,
                    allChipFocusRequester = allChipFocusRequester,
                    foldersChipFocusRequester = foldersChipFocusRequester,
                    firstKeyFocusRequester = initialFocusRequester,
                    keyFFocusRequester = keyFFocusRequester,
                    micFocusRequester = micFocusRequester
                )

                // On-Screen 6-Column TV Keyboard
                SearchOnScreenKeyboard(
                    currentQuery = uiState.query,
                    onQueryChange = viewModel::onQueryChanged,
                    onClearQuery = viewModel::onClearQuery,
                    firstKeyFocusRequester = initialFocusRequester,
                    keyFFocusRequester = keyFFocusRequester,
                    allChipFocusRequester = allChipFocusRequester,
                    foldersChipFocusRequester = foldersChipFocusRequester,
                    sidebarSearchFocusRequester = sidebarSearchFocusRequester,
                    resultsFocusRequester = activeResultsFocusRequester
                )

                // Voice Search Planned Notice (Phase 7 informative banner placed below keyboard per Stitch spec)
                if (showVoiceNotice) {
                    VoiceSearchPlannedNotice(onDismiss = { showVoiceNotice = false })
                }
            }

            // =================================================================
            // RIGHT REGION: 7 COLUMNS (Categorized Results / States)
            // =================================================================
            Column(
                modifier = Modifier
                    .weight(7f)
                    .fillMaxHeight()
                    .focusRestorer(if (hasResults) firstResultFocusRequester else FocusRequester.Default)
                    .focusGroup()
            ) {
                SearchResultsContent(
                    uiState = uiState,
                    firstResultFocusRequester = firstResultFocusRequester,
                    firstKeyFocusRequester = initialFocusRequester,
                    onVideoClick = onVideoClick,
                    onTrackClick = onTrackClick,
                    onArtistClick = onArtistClick,
                    onAlbumClick = onAlbumClick,
                    onFolderClick = onFolderClick
                )
            }
        }

        // Bottom-Right D-pad Navigation Affordance Pill (matching Google Stitch design)
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 12.dp, end = 8.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(LumaTheme.colors.surfaceContainerHigh.copy(alpha = 0.9f))
                .border(
                    1.dp,
                    LumaTheme.colors.outlineVariant.copy(alpha = 0.5f),
                    RoundedCornerShape(20.dp)
                )
                .padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "DPAD: Navigate",
                    style = LumaTheme.typography.labelSm,
                    color = LumaTheme.colors.textSecondary
                )
                Text(
                    text = "•",
                    color = LumaTheme.colors.outline,
                    style = LumaTheme.typography.labelSm
                )
                Text(
                    text = "OK: Select",
                    style = LumaTheme.typography.labelSm.copy(fontWeight = FontWeight.Bold),
                    color = LumaTheme.colors.cyan
                )
                Text(
                    text = "•",
                    color = LumaTheme.colors.outline,
                    style = LumaTheme.typography.labelSm
                )
                Text(
                    text = "BACK: Exit",
                    style = LumaTheme.typography.labelSm,
                    color = LumaTheme.colors.textSecondary
                )
            }
        }
    }
}

// -----------------------------------------------------------------------------
// Search Query Display Bar
// -----------------------------------------------------------------------------

@OptIn(ExperimentalTvMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
private fun SearchQueryBar(
    query: String,
    onMicClick: () -> Unit,
    onClearClick: () -> Unit,
    resultsFocusRequester: FocusRequester? = null,
    sidebarSearchFocusRequester: FocusRequester? = null,
    micFocusRequester: FocusRequester? = null,
    foldersChipFocusRequester: FocusRequester? = null,
    modifier: Modifier = Modifier
) {
    val spacing = LumaTheme.spacing
    val coroutineScope = rememberCoroutineScope()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(LumaTheme.colors.surfaceContainerHigh)
            .border(1.dp, LumaTheme.colors.outlineVariant, RoundedCornerShape(12.dp))
            .padding(horizontal = spacing.spaceSm, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.spaceSm)
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_nav_search),
            contentDescription = "Search icon",
            tint = LumaTheme.colors.cyan,
            modifier = Modifier.size(24.dp)
        )

        // Query text or hint
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 4.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            if (query.isEmpty()) {
                Text(
                    text = "Type to search...",
                    style = LumaTheme.typography.headlineSm,
                    color = LumaTheme.colors.textMuted
                )
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = query,
                        style = LumaTheme.typography.headlineSm,
                        color = LumaTheme.colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Box(
                        modifier = Modifier
                            .width(2.dp)
                            .height(20.dp)
                            .background(LumaTheme.colors.cyan)
                    )
                }
            }
        }

        // Microphone Button (Voice search planned informational trigger)
        val micModifier = Modifier
            .then(if (micFocusRequester != null) Modifier.focusRequester(micFocusRequester) else Modifier)
            .then(
                if (sidebarSearchFocusRequester != null) {
                    Modifier.focusProperties { left = sidebarSearchFocusRequester }
                } else Modifier
            )
            .then(
                if (foldersChipFocusRequester != null) {
                    Modifier.focusProperties { down = foldersChipFocusRequester }
                } else Modifier
            )
            .then(
                if (query.isEmpty()) {
                    Modifier
                        .focusProperties {
                            if (resultsFocusRequester != null) right = FocusRequester.Cancel
                        }
                        .onPreviewKeyEvent { keyEvent ->
                            if (keyEvent.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                                if (resultsFocusRequester != null) {
                                    if (keyEvent.type == KeyEventType.KeyDown) {
                                        resultsFocusRequester.requestFocus()
                                    }
                                    true
                                } else false
                            } else false
                        }
                } else Modifier
            )

        LumaButton(
            onClick = onMicClick,
            variant = LumaButtonVariant.Secondary,
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
            modifier = micModifier
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_action_graphic_eq),
                contentDescription = "Voice search info",
                tint = LumaTheme.colors.cyan,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "MIC",
                style = LumaTheme.typography.labelSm,
                color = LumaTheme.colors.cyan
            )
        }

        // Clear query button (active when query is not blank)
        if (query.isNotEmpty()) {
            val clearModifier = Modifier
                .then(
                    if (foldersChipFocusRequester != null) {
                        Modifier.focusProperties { down = foldersChipFocusRequester }
                    } else Modifier
                )
                .then(
                    Modifier
                        .focusProperties {
                            if (resultsFocusRequester != null) right = FocusRequester.Cancel
                        }
                        .onPreviewKeyEvent { keyEvent ->
                            if (keyEvent.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                                if (resultsFocusRequester != null) {
                                    if (keyEvent.type == KeyEventType.KeyDown) {
                                        resultsFocusRequester.requestFocus()
                                    }
                                    true
                                } else false
                            } else false
                        }
                )

            LumaButton(
                onClick = onClearClick,
                variant = LumaButtonVariant.Ghost,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                modifier = clearModifier
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_action_close),
                    contentDescription = "Clear query",
                    tint = LumaTheme.colors.textSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

// -----------------------------------------------------------------------------
// Voice Search Planned Informational Notice (Phase 7)
// -----------------------------------------------------------------------------

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun VoiceSearchPlannedNotice(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = LumaTheme.spacing

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(LumaTheme.colors.surfaceContainerLow)
            .border(1.dp, LumaTheme.colors.cyan.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
            .padding(spacing.spaceSm)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.spaceSm)
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_action_info),
                contentDescription = null,
                tint = LumaTheme.colors.cyan,
                modifier = Modifier.size(20.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(spacing.spaceXs)
                ) {
                    Text(
                        text = "Voice Search",
                        style = LumaTheme.typography.labelMd,
                        color = LumaTheme.colors.textPrimary
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(LumaTheme.colors.cyan.copy(alpha = 0.15f))
                            .border(1.dp, LumaTheme.colors.cyan.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "PHASE 7",
                            style = LumaTheme.typography.labelSm,
                            color = LumaTheme.colors.cyan,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Voice search is scheduled for Phase 7. Universal offline text search is currently active.",
                    style = LumaTheme.typography.labelSm,
                    color = LumaTheme.colors.textSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            LumaButton(
                onClick = onDismiss,
                variant = LumaButtonVariant.Ghost,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "DISMISS",
                    style = LumaTheme.typography.labelSm,
                    color = LumaTheme.colors.cyan
                )
            }
        }
    }
}

// -----------------------------------------------------------------------------
// Filter Chips Row (All, Videos, Music, Folders)
// -----------------------------------------------------------------------------

@OptIn(ExperimentalTvMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
private fun SearchFilterChipsRow(
    selectedCategory: SearchCategory,
    rawResults: SearchResults,
    isSearchSuccess: Boolean,
    onCategorySelected: (SearchCategory) -> Unit,
    resultsFocusRequester: FocusRequester? = null,
    sidebarSearchFocusRequester: FocusRequester? = null,
    allChipFocusRequester: FocusRequester? = null,
    foldersChipFocusRequester: FocusRequester? = null,
    firstKeyFocusRequester: FocusRequester? = null,
    keyFFocusRequester: FocusRequester? = null,
    micFocusRequester: FocusRequester? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SearchCategory.entries.forEach { category ->
            val isSelected = category == selectedCategory
            val isFirstCategory = category == SearchCategory.entries.first()
            val isLastCategory = category == SearchCategory.entries.last()

            val count = if (isSearchSuccess) {
                when (category) {
                    SearchCategory.ALL -> rawResults.totalCount
                    SearchCategory.VIDEOS -> rawResults.videos.size
                    SearchCategory.MUSIC -> rawResults.tracks.size + rawResults.artists.size + rawResults.albums.size
                    SearchCategory.FOLDERS -> rawResults.folders.size
                }
            } else null

            val chipModifier = Modifier
                .then(
                    if (isFirstCategory && allChipFocusRequester != null) {
                        Modifier.focusRequester(allChipFocusRequester)
                    } else if (isLastCategory && foldersChipFocusRequester != null) {
                        Modifier.focusRequester(foldersChipFocusRequester)
                    } else Modifier
                )
                .then(
                    if (isFirstCategory) {
                        Modifier.focusProperties {
                            if (sidebarSearchFocusRequester != null) left = sidebarSearchFocusRequester
                            if (firstKeyFocusRequester != null) down = firstKeyFocusRequester
                        }
                    } else if (isLastCategory) {
                        Modifier
                            .focusProperties {
                                if (micFocusRequester != null) up = micFocusRequester
                                if (keyFFocusRequester != null) down = keyFFocusRequester
                            }
                            .onPreviewKeyEvent { keyEvent ->
                                if (keyEvent.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                                    if (resultsFocusRequester != null) {
                                        if (keyEvent.type == KeyEventType.KeyDown) {
                                            resultsFocusRequester.requestFocus()
                                        }
                                        true
                                    } else false
                                } else false
                            }
                    } else Modifier
                )

            LumaButton(
                onClick = { onCategorySelected(category) },
                variant = if (isSelected) LumaButtonVariant.Primary else LumaButtonVariant.Secondary,
                contentPadding = PaddingValues(horizontal = 9.dp, vertical = 6.dp),
                shape = RoundedCornerShape(8.dp),
                modifier = chipModifier
            ) {
                Text(
                    text = category.label,
                    style = LumaTheme.typography.labelMd,
                    color = if (isSelected) LumaTheme.colors.onSecondary else LumaTheme.colors.textPrimary,
                    maxLines = 1,
                    softWrap = false
                )
                if (count != null && count > 0) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                if (isSelected) LumaTheme.colors.surfaceContainerLowest
                                else LumaTheme.colors.surfaceContainerHigh
                            )
                            .padding(horizontal = 5.dp, vertical = 1.5.dp)
                    ) {
                        Text(
                            text = count.toString(),
                            style = LumaTheme.typography.labelSm,
                            color = if (isSelected) LumaTheme.colors.cyan else LumaTheme.colors.textSecondary,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// Animated On-Screen Keyboard Key Button Helper
// -----------------------------------------------------------------------------

@Composable
private fun SearchKeyboardKeyButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (isPressed: Boolean) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val bounceScale = remember { Animatable(1f) }
    val pressOverlayAlpha = remember { Animatable(0f) }
    var animJob by remember { mutableStateOf<Job?>(null) }
    val cyanColor = LumaTheme.colors.cyan

    val handleActivation: () -> Unit = {
        onClick()
        animJob?.cancel()
        animJob = coroutineScope.launch {
            launch {
                bounceScale.snapTo(1.07f)
                bounceScale.animateTo(
                    targetValue = 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                )
            }
            launch {
                pressOverlayAlpha.snapTo(1f)
                pressOverlayAlpha.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(durationMillis = 180)
                )
            }
        }
    }

    val animatedModifier = modifier
        .zIndex(if (bounceScale.value > 1.001f) 1f else 0f)
        .graphicsLayer {
            scaleX = bounceScale.value
            scaleY = bounceScale.value
        }

    LumaButton(
        onClick = handleActivation,
        variant = LumaButtonVariant.Secondary,
        shape = RoundedCornerShape(8.dp),
        contentPadding = PaddingValues(0.dp),
        modifier = animatedModifier
    ) {
        val isPressed = pressOverlayAlpha.value > 0.05f
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    val alpha = pressOverlayAlpha.value
                    if (alpha > 0f) {
                        drawRoundRect(
                            color = cyanColor.copy(alpha = 0.35f * alpha),
                            cornerRadius = CornerRadius(8.dp.toPx())
                        )
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            content(isPressed)
        }
    }
}

// -----------------------------------------------------------------------------
// On-Screen 6-Column TV Keyboard
// -----------------------------------------------------------------------------

@OptIn(ExperimentalTvMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
private fun SearchOnScreenKeyboard(
    currentQuery: String,
    onQueryChange: (String) -> Unit,
    onClearQuery: () -> Unit,
    firstKeyFocusRequester: FocusRequester,
    keyFFocusRequester: FocusRequester? = null,
    allChipFocusRequester: FocusRequester? = null,
    foldersChipFocusRequester: FocusRequester? = null,
    sidebarSearchFocusRequester: FocusRequester?,
    resultsFocusRequester: FocusRequester? = null,
    modifier: Modifier = Modifier
) {
    val keyboardRows = listOf(
        listOf("A", "B", "C", "D", "E", "F"),
        listOf("G", "H", "I", "J", "K", "L"),
        listOf("M", "N", "O", "P", "Q", "R"),
        listOf("S", "T", "U", "V", "W", "X"),
        listOf("Y", "Z", "1", "2", "3", "4"),
        listOf("5", "6", "7", "8", "9", "0")
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(LumaTheme.colors.surfaceContainerLow)
            .border(1.dp, LumaTheme.colors.outlineVariant, RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Alphanumeric rows
        keyboardRows.forEachIndexed { rowIndex, rowKeys ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                rowKeys.forEachIndexed { colIndex, charKey ->
                    val isFirstKey = rowIndex == 0 && colIndex == 0
                    val isKeyF = rowIndex == 0 && colIndex == rowKeys.size - 1
                    val isLeftEdgeKey = colIndex == 0
                    val isRightEdgeKey = colIndex == rowKeys.size - 1

                    val keyModifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .then(
                            if (isFirstKey) {
                                Modifier.focusRequester(firstKeyFocusRequester)
                            } else if (isKeyF && keyFFocusRequester != null) Modifier.focusRequester(keyFFocusRequester)
                            else Modifier
                        )
                        .then(
                            Modifier.focusProperties {
                                if (isFirstKey && allChipFocusRequester != null) {
                                    up = allChipFocusRequester
                                } else if (isKeyF && foldersChipFocusRequester != null) {
                                    up = foldersChipFocusRequester
                                }
                                if (isLeftEdgeKey && sidebarSearchFocusRequester != null) {
                                    left = sidebarSearchFocusRequester
                                }
                            }
                        )
                        .then(
                            if (isRightEdgeKey) {
                                Modifier.onPreviewKeyEvent { keyEvent ->
                                    if (keyEvent.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                                        if (resultsFocusRequester != null) {
                                            if (keyEvent.type == KeyEventType.KeyDown) {
                                                resultsFocusRequester.requestFocus()
                                            }
                                            true
                                        } else false
                                    } else false
                                }
                            } else Modifier
                        )

                    SearchKeyboardKeyButton(
                        onClick = { onQueryChange(currentQuery + charKey) },
                        modifier = keyModifier
                    ) { isPressed ->
                        Text(
                            text = charKey,
                            style = LumaTheme.typography.headlineSm,
                            color = if (isPressed) LumaTheme.colors.cyan else LumaTheme.colors.textPrimary
                        )
                    }
                }
            }
        }

        // Action row matching Stitch layout: SPACE (3 cols), DELETE (2 cols), CLR (1 col)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // SPACE (3 columns)
            SearchKeyboardKeyButton(
                onClick = { onQueryChange("$currentQuery ") },
                modifier = Modifier
                    .weight(3f)
                    .height(38.dp)
                    .then(
                        if (sidebarSearchFocusRequester != null) {
                            Modifier.focusProperties { left = sidebarSearchFocusRequester }
                        } else Modifier
                    )
            ) { isPressed ->
                Text(
                    text = "SPACE",
                    style = LumaTheme.typography.labelMd,
                    color = if (isPressed) LumaTheme.colors.cyan else LumaTheme.colors.textPrimary
                )
            }

            // DELETE (2 columns)
            SearchKeyboardKeyButton(
                onClick = {
                    if (currentQuery.isNotEmpty()) {
                        onQueryChange(currentQuery.dropLast(1))
                    }
                },
                modifier = Modifier
                    .weight(2f)
                    .height(38.dp)
            ) { isPressed ->
                Text(
                    text = "DELETE",
                    style = LumaTheme.typography.labelMd,
                    color = if (isPressed) LumaTheme.colors.cyan else LumaTheme.colors.textPrimary
                )
            }

            // CLR (1 column)
            val clrModifier = Modifier
                .weight(1f)
                .height(38.dp)
                .onPreviewKeyEvent { keyEvent ->
                    if (keyEvent.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                        if (resultsFocusRequester != null) {
                            if (keyEvent.type == KeyEventType.KeyDown) {
                                resultsFocusRequester.requestFocus()
                            }
                            true
                        } else false
                    } else false
                }

            SearchKeyboardKeyButton(
                onClick = onClearQuery,
                modifier = clrModifier
            ) { isPressed ->
                Text(
                    text = "CLR",
                    style = LumaTheme.typography.labelMd,
                    color = if (isPressed) LumaTheme.colors.textPrimary else LumaTheme.colors.cyan
                )
            }
        }
    }
}

// -----------------------------------------------------------------------------
// Categorized Search Results Content (Right 7 Columns)
// -----------------------------------------------------------------------------

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun SearchResultsContent(
    uiState: SearchUiState,
    firstResultFocusRequester: FocusRequester,
    firstKeyFocusRequester: FocusRequester,
    onVideoClick: ((VideoItem) -> Unit)?,
    onTrackClick: ((AudioItem) -> Unit)?,
    onArtistClick: ((ArtistItem) -> Unit)?,
    onAlbumClick: ((AlbumItem) -> Unit)?,
    onFolderClick: ((String) -> Unit)?,
    modifier: Modifier = Modifier
) {
    when (val status = uiState.status) {
        is SearchStatus.Idle -> {
            SearchIdlePrompt(modifier = modifier)
        }

        is SearchStatus.Loading -> {
            SearchLoadingState(query = uiState.query, modifier = modifier)
        }

        is SearchStatus.Error -> {
            SearchErrorState(message = status.message, modifier = modifier)
        }

        is SearchStatus.Empty -> {
            SearchEmptyState(query = uiState.query, category = uiState.selectedCategory, modifier = modifier)
        }

        is SearchStatus.Success -> {
            if (uiState.isFilteredEmpty) {
                SearchEmptyState(query = uiState.query, category = uiState.selectedCategory, modifier = modifier)
            } else {
                SearchResultsList(
                    filteredResults = uiState.filteredResults,
                    selectedCategory = uiState.selectedCategory,
                    query = uiState.query,
                    firstResultFocusRequester = firstResultFocusRequester,
                    firstKeyFocusRequester = firstKeyFocusRequester,
                    onVideoClick = onVideoClick,
                    onTrackClick = onTrackClick,
                    onArtistClick = onArtistClick,
                    onAlbumClick = onAlbumClick,
                    onFolderClick = onFolderClick,
                    modifier = modifier
                )
            }
        }
    }
}

// -----------------------------------------------------------------------------
// Presentation States (Idle, Loading, Error, Empty)
// -----------------------------------------------------------------------------

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun SearchIdlePrompt(modifier: Modifier = Modifier) {
    val spacing = LumaTheme.spacing

    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(16.dp))
            .background(LumaTheme.colors.surfaceCard)
            .border(1.dp, LumaTheme.colors.outlineVariant, RoundedCornerShape(16.dp))
            .padding(spacing.spaceLg),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(spacing.spaceSm)
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_nav_search),
                contentDescription = null,
                tint = LumaTheme.colors.cyan,
                modifier = Modifier.size(56.dp)
            )
            Text(
                text = "Universal Offline Search",
                style = LumaTheme.typography.displayMd,
                color = LumaTheme.colors.textPrimary
            )
            Text(
                text = "Use the on-screen keyboard to search across local videos, music tracks, artists, albums, and storage folders.",
                style = LumaTheme.typography.bodyLg,
                color = LumaTheme.colors.textSecondary
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun SearchLoadingState(query: String, modifier: Modifier = Modifier) {
    val spacing = LumaTheme.spacing

    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(16.dp))
            .background(LumaTheme.colors.surfaceCard)
            .border(1.dp, LumaTheme.colors.outlineVariant, RoundedCornerShape(16.dp))
            .padding(spacing.spaceLg),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(spacing.spaceSm)
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_nav_search),
                contentDescription = null,
                tint = LumaTheme.colors.cyan,
                modifier = Modifier.size(48.dp)
            )
            Text(
                text = "Searching Local Media...",
                style = LumaTheme.typography.headlineMd,
                color = LumaTheme.colors.textPrimary
            )
            Text(
                text = "Querying \"$query\" across storage and indexed libraries.",
                style = LumaTheme.typography.bodyMd,
                color = LumaTheme.colors.textSecondary
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun SearchErrorState(message: String, modifier: Modifier = Modifier) {
    val spacing = LumaTheme.spacing

    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(16.dp))
            .background(LumaTheme.colors.surfaceCard)
            .border(1.dp, LumaTheme.colors.error, RoundedCornerShape(16.dp))
            .padding(spacing.spaceLg),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(spacing.spaceSm)
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_action_info),
                contentDescription = null,
                tint = LumaTheme.colors.error,
                modifier = Modifier.size(48.dp)
            )
            Text(
                text = "Search Error",
                style = LumaTheme.typography.headlineMd,
                color = LumaTheme.colors.textPrimary
            )
            Text(
                text = message,
                style = LumaTheme.typography.bodyMd,
                color = LumaTheme.colors.textSecondary
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun SearchEmptyState(query: String, category: SearchCategory, modifier: Modifier = Modifier) {
    val spacing = LumaTheme.spacing

    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(16.dp))
            .background(LumaTheme.colors.surfaceCard)
            .border(1.dp, LumaTheme.colors.outlineVariant, RoundedCornerShape(16.dp))
            .padding(spacing.spaceLg),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(spacing.spaceSm)
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_action_info),
                contentDescription = null,
                tint = LumaTheme.colors.textMuted,
                modifier = Modifier.size(48.dp)
            )
            Text(
                text = "No results found for \"$query\"",
                style = LumaTheme.typography.headlineMd,
                color = LumaTheme.colors.textPrimary
            )
            val subText = if (category != SearchCategory.ALL) {
                "No ${category.label.lowercase()} found matching this term. Try switching to \"All\" or another category."
            } else {
                "Try searching for a different title, artist, album, or folder name."
            }
            Text(
                text = subText,
                style = LumaTheme.typography.bodyMd,
                color = LumaTheme.colors.textSecondary
            )
        }
    }
}

// -----------------------------------------------------------------------------
// Categorized Results List
// -----------------------------------------------------------------------------

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun SearchResultsList(
    filteredResults: SearchResults,
    selectedCategory: SearchCategory,
    query: String,
    firstResultFocusRequester: FocusRequester,
    firstKeyFocusRequester: FocusRequester,
    onVideoClick: ((VideoItem) -> Unit)?,
    onTrackClick: ((AudioItem) -> Unit)?,
    onArtistClick: ((ArtistItem) -> Unit)?,
    onAlbumClick: ((AlbumItem) -> Unit)?,
    onFolderClick: ((String) -> Unit)?,
    modifier: Modifier = Modifier
) {
    val spacing = LumaTheme.spacing

    val showVideos = selectedCategory in listOf(SearchCategory.ALL, SearchCategory.VIDEOS) && filteredResults.videos.isNotEmpty()
    val showTracks = selectedCategory in listOf(SearchCategory.ALL, SearchCategory.MUSIC) && filteredResults.tracks.isNotEmpty()
    val showArtists = selectedCategory in listOf(SearchCategory.ALL, SearchCategory.MUSIC) && filteredResults.artists.isNotEmpty()
    val showAlbums = selectedCategory in listOf(SearchCategory.ALL, SearchCategory.MUSIC) && filteredResults.albums.isNotEmpty()
    val showFolders = selectedCategory in listOf(SearchCategory.ALL, SearchCategory.FOLDERS) && filteredResults.folders.isNotEmpty()

    val isFirstSectionVideos = showVideos
    val isFirstSectionTracks = !showVideos && showTracks
    val isFirstSectionArtists = !showVideos && !showTracks && showArtists
    val isFirstSectionAlbums = !showVideos && !showTracks && !showArtists && showAlbums
    val isFirstSectionFolders = !showVideos && !showTracks && !showArtists && !showAlbums && showFolders

    LazyColumn(
        modifier = modifier
            .fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(spacing.spaceSm),
        contentPadding = PaddingValues(bottom = spacing.spaceLg)
    ) {
        // 1. VIDEOS SECTION (2-Column Grid matching Stitch)
        if (showVideos) {
            item(key = "header_videos") {
                SectionHeader(
                    title = "Videos Matching “$query”",
                    count = filteredResults.videos.size,
                    categoryTag = "Local Videos"
                )
            }
            val videoPairs = filteredResults.videos.chunked(2)
            itemsIndexed(videoPairs, key = { index, pair -> "vidrow_" + pair.joinToString("_") { it.id.toString() } }) { rowIndex, pair ->
                val isFirstVideoRow = rowIndex == 0
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing.spaceSm)
                ) {
                    val leftCardModifier = Modifier
                        .weight(1f)
                        .then(
                            if (isFirstSectionVideos && isFirstVideoRow) {
                                Modifier.focusRequester(firstResultFocusRequester)
                            } else Modifier
                        )
                        .focusProperties {
                            left = firstKeyFocusRequester
                        }
                        .onFocusChanged {
                            if (isFirstVideoRow) {
                                android.util.Log.d("LumaSearchFocus", "First video card focus changed: isFocused=${it.isFocused}, hasFocus=${it.hasFocus}")
                                if (it.isFocused || it.hasFocus) {
                                    android.util.Log.d("LumaSearchFocus", "First video card GAINED focus! Caller trace:", Exception())
                                }
                            }
                        }

                    VideoResultCard(
                        video = pair[0],
                        onClick = { onVideoClick?.invoke(pair[0]) },
                        modifier = leftCardModifier
                    )
                    if (pair.size > 1) {
                        VideoResultCard(
                            video = pair[1],
                            onClick = { onVideoClick?.invoke(pair[1]) },
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        // 2. TRACKS SECTION
        if (showTracks) {
            item(key = "header_tracks") {
                SectionHeader(
                    title = "Music & Soundtracks",
                    count = filteredResults.tracks.size,
                    categoryTag = "Lossless Master Audio"
                )
            }
            itemsIndexed(filteredResults.tracks, key = { _, track -> "track_${track.id}" }) { index, track ->
                val isFirstTrack = isFirstSectionTracks && index == 0
                val cardModifier = Modifier
                    .fillMaxWidth()
                    .then(if (isFirstTrack) Modifier.focusRequester(firstResultFocusRequester) else Modifier)
                    .focusProperties {
                        left = firstKeyFocusRequester
                    }
                TrackResultCard(
                    track = track,
                    onClick = { onTrackClick?.invoke(track) },
                    modifier = cardModifier
                )
            }
        }

        // 3. ARTISTS SECTION
        if (showArtists) {
            item(key = "header_artists") {
                SectionHeader(
                    title = "Artists",
                    count = filteredResults.artists.size,
                    categoryTag = "Music Artists"
                )
            }
            itemsIndexed(filteredResults.artists, key = { _, artist -> "artist_${artist.name}" }) { index, artist ->
                val isFirstArtist = isFirstSectionArtists && index == 0
                val cardModifier = Modifier
                    .fillMaxWidth()
                    .then(if (isFirstArtist) Modifier.focusRequester(firstResultFocusRequester) else Modifier)
                    .focusProperties {
                        left = firstKeyFocusRequester
                    }
                ArtistResultCard(
                    artist = artist,
                    onClick = { onArtistClick?.invoke(artist) },
                    modifier = cardModifier
                )
            }
        }

        // 4. ALBUMS SECTION
        if (showAlbums) {
            item(key = "header_albums") {
                SectionHeader(
                    title = "Albums",
                    count = filteredResults.albums.size,
                    categoryTag = "Music Albums"
                )
            }
            itemsIndexed(filteredResults.albums, key = { _, album -> "album_${album.id}" }) { index, album ->
                val isFirstAlbum = isFirstSectionAlbums && index == 0
                val cardModifier = Modifier
                    .fillMaxWidth()
                    .then(if (isFirstAlbum) Modifier.focusRequester(firstResultFocusRequester) else Modifier)
                    .focusProperties {
                        left = firstKeyFocusRequester
                    }
                AlbumResultCard(
                    album = album,
                    onClick = { onAlbumClick?.invoke(album) },
                    modifier = cardModifier
                )
            }
        }

        // 5. FOLDERS SECTION
        if (showFolders) {
            item(key = "header_folders") {
                SectionHeader(
                    title = "Folders Matching Query",
                    count = filteredResults.folders.size,
                    categoryTag = "Local Storage"
                )
            }
            itemsIndexed(filteredResults.folders, key = { _, folder -> "folder_$folder" }) { index, folder ->
                val isFirstFolder = isFirstSectionFolders && index == 0
                val cardModifier = Modifier
                    .fillMaxWidth()
                    .then(if (isFirstFolder) Modifier.focusRequester(firstResultFocusRequester) else Modifier)
                    .focusProperties {
                        left = firstKeyFocusRequester
                    }
                FolderResultCard(
                    folder = folder,
                    onClick = { onFolderClick?.invoke(folder) },
                    modifier = cardModifier
                )
            }
        }
    }
}

// -----------------------------------------------------------------------------
// Section Header
// -----------------------------------------------------------------------------

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun SectionHeader(
    title: String,
    count: Int,
    categoryTag: String? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 12.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = LumaTheme.typography.headlineSm,
                color = LumaTheme.colors.textPrimary
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(LumaTheme.colors.surfaceContainerHigh)
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "$count result${if (count != 1) "s" else ""}",
                    style = LumaTheme.typography.labelSm,
                    color = LumaTheme.colors.cyan
                )
            }
        }
        if (categoryTag != null) {
            Text(
                text = categoryTag,
                style = LumaTheme.typography.labelSm,
                color = LumaTheme.colors.textSecondary
            )
        }
    }
}

// -----------------------------------------------------------------------------
// Result Item Cards
// -----------------------------------------------------------------------------

/**
 * 2-column widescreen video card matching Stitch design specifications.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun VideoResultCard(
    video: VideoItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    LumaCard(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // 16:9 Thumbnail Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                    .background(LumaTheme.colors.surfaceContainerLowest)
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(video.contentUri)
                        .crossfade(true)
                        .build(),
                    contentDescription = video.cleanTitle,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Top-left resolution badge
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(LumaTheme.colors.surface.copy(alpha = 0.85f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = video.resolutionTag,
                        style = LumaTheme.typography.labelSm,
                        color = LumaTheme.colors.cyan,
                        maxLines = 1,
                        softWrap = false
                    )
                }

                // Top-right duration badge
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(LumaTheme.colors.surface.copy(alpha = 0.85f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = video.formattedDuration,
                        style = LumaTheme.typography.labelSm,
                        color = LumaTheme.colors.textPrimary,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            // Metadata below thumbnail
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Text(
                    text = video.cleanTitle,
                    style = LumaTheme.typography.headlineSm,
                    color = LumaTheme.colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${video.formattedFileSize} • ${video.folderName}",
                    style = LumaTheme.typography.bodySm,
                    color = LumaTheme.colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Horizontal audio track card matching Stitch design specifications.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun TrackResultCard(
    track: AudioItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    LumaCard(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 48x48dp Artwork box
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(LumaTheme.colors.surfaceContainerLowest),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_nav_music),
                    contentDescription = null,
                    tint = LumaTheme.colors.cyan,
                    modifier = Modifier.size(24.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = track.cleanTitle,
                        style = LumaTheme.typography.labelLg,
                        color = LumaTheme.colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(LumaTheme.colors.surfaceContainerLowest)
                            .padding(horizontal = 6.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = track.formatTag,
                            style = LumaTheme.typography.labelSm,
                            color = LumaTheme.colors.cyan,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${track.displayArtist} • ${track.displayAlbum}",
                    style = LumaTheme.typography.bodySm,
                    color = LumaTheme.colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Duration Pill
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(LumaTheme.colors.surfaceContainerLowest)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = track.formattedDuration,
                    style = LumaTheme.typography.labelMd,
                    color = LumaTheme.colors.textPrimary
                )
            }
        }
    }
}

/**
 * Horizontal artist card matching Stitch design specifications.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun ArtistResultCard(
    artist: ArtistItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    LumaCard(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(LumaTheme.colors.surfaceContainerLowest),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_status_person),
                    contentDescription = null,
                    tint = LumaTheme.colors.violet,
                    modifier = Modifier.size(24.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = artist.displayName,
                    style = LumaTheme.typography.labelLg,
                    color = LumaTheme.colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${artist.formattedTrackCount} • ${artist.formattedAlbumCount}",
                    style = LumaTheme.typography.bodySm,
                    color = LumaTheme.colors.textSecondary
                )
            }
        }
    }
}

/**
 * Horizontal album card matching Stitch design specifications.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun AlbumResultCard(
    album: AlbumItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    LumaCard(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(LumaTheme.colors.surfaceContainerLowest),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_nav_music),
                    contentDescription = null,
                    tint = LumaTheme.colors.cyan,
                    modifier = Modifier.size(24.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = album.displayTitle,
                    style = LumaTheme.typography.labelLg,
                    color = LumaTheme.colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                val subtitle = buildString {
                    append(album.displayArtist)
                    album.formattedYear?.let { append(" • $it") }
                    append(" • ${album.formattedTrackCount}")
                }
                Text(
                    text = subtitle,
                    style = LumaTheme.typography.bodySm,
                    color = LumaTheme.colors.textSecondary
                )
            }
        }
    }
}

/**
 * Horizontal folder card matching Stitch design specifications.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun FolderResultCard(
    folder: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    LumaCard(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(LumaTheme.colors.surfaceContainerLowest),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_nav_folders),
                    contentDescription = null,
                    tint = LumaTheme.colors.cyan,
                    modifier = Modifier.size(26.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = folder,
                    style = LumaTheme.typography.headlineSm,
                    color = LumaTheme.colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Local Storage Folder",
                    style = LumaTheme.typography.bodySm,
                    color = LumaTheme.colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Icon(
                painter = painterResource(R.drawable.ic_action_arrow_forward),
                contentDescription = null,
                tint = LumaTheme.colors.cyan,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
