package com.lumaplay.tv.ui.music

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.lumaplay.tv.R
import com.lumaplay.tv.core.theme.LumaTheme
import com.lumaplay.tv.core.ui.components.LumaBadge
import com.lumaplay.tv.core.ui.components.LumaBadgeVariant
import com.lumaplay.tv.core.ui.components.LumaButton
import com.lumaplay.tv.core.ui.components.LumaButtonVariant
import com.lumaplay.tv.core.ui.components.LumaCard
import com.lumaplay.tv.data.model.AudioItem
import com.lumaplay.tv.player.audio.AudioPlayerState
import com.lumaplay.tv.player.audio.AudioRepeatMode
import java.util.Locale

/**
 * Foundational 10-foot Android TV Music Library & Player Screen.
 *
 * Implements the approved Google Stitch dual-panel reference:
 * - Left panel: Now Playing card with artwork, track metadata, timeline scrubber, and 5 circular TV transport controls.
 * - Right panel: Accessible track list with genuine metadata and playback indicator.
 * - Fully operable via D-pad remote with deterministic focus routing.
 *
 * @param viewModel Activity-scoped [MusicPlayerViewModel] instance.
 * @param sidebarMusicFocusRequester Shared requester used to route D-pad LEFT back to the sidebar Music item.
 * @param contentFocusRequester Optional requester provided by the container for initial focus.
 */
@OptIn(ExperimentalTvMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun MusicScreen(
    viewModel: MusicPlayerViewModel,
    sidebarMusicFocusRequester: FocusRequester,
    contentFocusRequester: FocusRequester? = null
) {
    val libraryState by viewModel.libraryState.collectAsState()
    val playbackState by viewModel.playbackState.collectAsState()

    val playButtonFocusRequester = remember { FocusRequester() }
    val firstTrackFocusRequester = remember { FocusRequester() }
    val contentEntryRequester = contentFocusRequester ?: remember { FocusRequester() }

    // Direct initial focus handling when library is loaded
    LaunchedEffect(libraryState) {
        if (libraryState is MusicLibraryUiState.Success) {
            // When library first becomes available while idle, request focus to the first track
            if (playbackState.currentTrack == null) {
                runCatching { firstTrackFocusRequester.requestFocus() }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LumaTheme.colors.background)
            .focusRequester(contentEntryRequester)
            .focusRestorer {
                if (playbackState.currentTrack != null) {
                    playButtonFocusRequester
                } else {
                    firstTrackFocusRequester
                }
            }
    ) {
        when (val state = libraryState) {
            is MusicLibraryUiState.Loading -> {
                MusicLoadingContent()
            }

            is MusicLibraryUiState.Empty -> {
                MusicEmptyContent(
                    onScan = { viewModel.scanMusic() },
                    sidebarMusicFocusRequester = sidebarMusicFocusRequester,
                    actionFocusRequester = firstTrackFocusRequester
                )
            }

            is MusicLibraryUiState.Error -> {
                MusicErrorContent(
                    message = state.message,
                    onRetry = { viewModel.scanMusic() },
                    sidebarMusicFocusRequester = sidebarMusicFocusRequester,
                    actionFocusRequester = firstTrackFocusRequester
                )
            }

            is MusicLibraryUiState.Success -> {
                MusicDualPanelContent(
                    tracks = state.tracks,
                    playbackState = playbackState,
                    viewModel = viewModel,
                    sidebarMusicFocusRequester = sidebarMusicFocusRequester,
                    playButtonFocusRequester = playButtonFocusRequester,
                    firstTrackFocusRequester = firstTrackFocusRequester
                )
            }
        }
    }
}

// -----------------------------------------------------------------------------
// Dual-Panel Layout (Now Playing Hero + Track List)
// -----------------------------------------------------------------------------

@OptIn(ExperimentalTvMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
private fun MusicDualPanelContent(
    tracks: List<AudioItem>,
    playbackState: AudioPlayerState,
    viewModel: MusicPlayerViewModel,
    sidebarMusicFocusRequester: FocusRequester,
    playButtonFocusRequester: FocusRequester,
    firstTrackFocusRequester: FocusRequester
) {
    val spacing = LumaTheme.spacing

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                start = spacing.tvSafeMargin,
                end = spacing.tvSafeMargin,
                top = spacing.spaceMd,
                bottom = spacing.spaceMd
            ),
        horizontalArrangement = Arrangement.spacedBy(spacing.gutter)
    ) {
        // --- Left Panel: Now Playing Focus Card (42% width) ---
        NowPlayingPanel(
            playbackState = playbackState,
            viewModel = viewModel,
            sidebarMusicFocusRequester = sidebarMusicFocusRequester,
            playButtonFocusRequester = playButtonFocusRequester,
            firstTrackFocusRequester = firstTrackFocusRequester,
            modifier = Modifier
                .weight(0.42f)
                .fillMaxHeight()
        )

        // --- Right Panel: Track List (58% width) ---
        TrackListPanel(
            tracks = tracks,
            playbackState = playbackState,
            onTrackClick = { track -> viewModel.playTrack(track, tracks) },
            playButtonFocusRequester = playButtonFocusRequester,
            firstTrackFocusRequester = firstTrackFocusRequester,
            modifier = Modifier
                .weight(0.58f)
                .fillMaxHeight()
        )
    }
}

// -----------------------------------------------------------------------------
// Left Panel: Now Playing Hero with TV Transport Controls
// -----------------------------------------------------------------------------

@OptIn(ExperimentalTvMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
private fun NowPlayingPanel(
    playbackState: AudioPlayerState,
    viewModel: MusicPlayerViewModel,
    sidebarMusicFocusRequester: FocusRequester,
    playButtonFocusRequester: FocusRequester,
    firstTrackFocusRequester: FocusRequester,
    modifier: Modifier = Modifier
) {
    val currentTrack = playbackState.currentTrack
    val colors = LumaTheme.colors
    val spacing = LumaTheme.spacing

    val shuffleButtonFocusRequester = remember { FocusRequester() }
    val prevButtonFocusRequester = remember { FocusRequester() }
    val nextButtonFocusRequester = remember { FocusRequester() }
    val repeatButtonFocusRequester = remember { FocusRequester() }

    Column(
        modifier = modifier
            .clip(LumaTheme.shapes.large)
            .background(colors.surfaceContainerLow)
            .padding(spacing.spaceMd),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top section: Artwork + Metadata + Timeline Scrubber
        Column(modifier = Modifier.fillMaxWidth()) {
            // Artwork Box (1:1 aspect ratio square, max 200dp, centered horizontally)
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .align(Alignment.CenterHorizontally)
                    .clip(LumaTheme.shapes.medium)
                    .background(colors.surfaceContainerHighest),
                contentAlignment = Alignment.Center
            ) {
                if (currentTrack?.artworkUri != null) {
                    AsyncImage(
                        model = currentTrack.artworkUri,
                        contentDescription = currentTrack.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        colors.surfaceContainerHighest,
                                        colors.surfaceContainerLowest
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_nav_music),
                            contentDescription = "Music",
                            modifier = Modifier.size(56.dp),
                            tint = colors.cyan.copy(alpha = 0.6f)
                        )
                    }
                }

                // Format badge floating on top-left of artwork
                if (currentTrack != null) {
                    val formatLabel = currentTrack.formatTag
                    LumaBadge(
                        text = formatLabel,
                        variant = LumaBadgeVariant.Highlight,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Track metadata
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_nav_music),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = colors.cyan
                )
                Text(
                    text = currentTrack?.cleanTitle ?: "No Track Selected",
                    style = LumaTheme.typography.headlineMd,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            val subtitle = if (currentTrack != null) {
                if (!currentTrack.album.isNullOrBlank()) {
                    "${currentTrack.displayArtist} • ${currentTrack.displayAlbum}"
                } else {
                    currentTrack.displayArtist
                }
            } else {
                "Select a track from the library to start playback"
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = subtitle,
                    style = LumaTheme.typography.bodyMd,
                    color = colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (currentTrack != null && playbackState.queue.isNotEmpty()) {
                    val queuePositionText = "Track ${playbackState.queueIndex + 1} of ${playbackState.queue.size}"
                    Text(
                        text = queuePositionText,
                        style = LumaTheme.typography.labelSm,
                        color = colors.textMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Timeline Scrubber Bar with subtle progress thumb
            Column(modifier = Modifier.fillMaxWidth()) {
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(14.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    // Track background line
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(LumaTheme.shapes.pill)
                            .background(colors.surfaceContainerHighest)
                    )
                    // Filled cyan progress line
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(playbackState.progress.coerceIn(0f, 1f))
                            .height(4.dp)
                            .clip(LumaTheme.shapes.pill)
                            .background(colors.cyan)
                    )
                    // Subtle circular thumb with cyan border
                    val thumbOffset = maxWidth * playbackState.progress.coerceIn(0f, 1f) - 6.dp
                    Box(
                        modifier = Modifier
                            .offset(x = thumbOffset.coerceAtLeast(0.dp))
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(colors.textPrimary)
                            .border(1.5.dp, colors.cyan, CircleShape)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = playbackState.formattedCurrentPosition,
                        style = LumaTheme.typography.labelSm,
                        color = colors.cyan
                    )
                    Text(
                        text = playbackState.formattedDuration,
                        style = LumaTheme.typography.labelSm,
                        color = colors.textMuted
                    )
                }
            }
        }

        // Bottom section: TV Transport Controls Row (5 circular icon buttons matching Stitch)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. Shuffle Button
            LumaButton(
                onClick = { viewModel.toggleShuffle() },
                variant = if (playbackState.isShuffled) LumaButtonVariant.Secondary else LumaButtonVariant.Ghost,
                shape = CircleShape,
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier
                    .size(44.dp)
                    .focusRequester(shuffleButtonFocusRequester)
                    .focusProperties {
                        left = sidebarMusicFocusRequester
                        right = prevButtonFocusRequester
                        up = firstTrackFocusRequester
                        down = FocusRequester.Cancel
                    }
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_action_shuffle),
                    contentDescription = "Shuffle",
                    modifier = Modifier.size(20.dp),
                    tint = if (playbackState.isShuffled) colors.cyan else colors.textSecondary
                )
            }

            // 2. Previous Button
            LumaButton(
                onClick = { viewModel.previous() },
                variant = LumaButtonVariant.Ghost,
                shape = CircleShape,
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier
                    .size(44.dp)
                    .focusRequester(prevButtonFocusRequester)
                    .focusProperties {
                        left = shuffleButtonFocusRequester
                        right = playButtonFocusRequester
                        up = firstTrackFocusRequester
                        down = FocusRequester.Cancel
                    }
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_action_skip_previous),
                    contentDescription = "Previous",
                    modifier = Modifier.size(20.dp),
                    tint = colors.textPrimary
                )
            }

            // 3. Play / Pause Button (Primary Action Anchor)
            LumaButton(
                onClick = { viewModel.togglePlayPause() },
                variant = LumaButtonVariant.Primary,
                shape = CircleShape,
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier
                    .size(52.dp)
                    .focusRequester(playButtonFocusRequester)
                    .focusProperties {
                        left = prevButtonFocusRequester
                        right = nextButtonFocusRequester
                        up = firstTrackFocusRequester
                        down = FocusRequester.Cancel
                    }
            ) {
                Icon(
                    painter = painterResource(
                        id = if (playbackState.isPlaying) R.drawable.ic_action_pause else R.drawable.ic_action_play
                    ),
                    contentDescription = if (playbackState.isPlaying) "Pause" else "Play",
                    modifier = Modifier.size(24.dp),
                    tint = colors.surfaceContainerLowest
                )
            }

            // 4. Next Button
            LumaButton(
                onClick = { viewModel.next() },
                variant = LumaButtonVariant.Ghost,
                shape = CircleShape,
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier
                    .size(44.dp)
                    .focusRequester(nextButtonFocusRequester)
                    .focusProperties {
                        left = playButtonFocusRequester
                        right = repeatButtonFocusRequester
                        up = firstTrackFocusRequester
                        down = FocusRequester.Cancel
                    }
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_action_skip_next),
                    contentDescription = "Next",
                    modifier = Modifier.size(20.dp),
                    tint = colors.textPrimary
                )
            }

            // 5. Repeat Button (rightmost: D-pad RIGHT targets the track list)
            val isRepeatActive = playbackState.repeatMode != AudioRepeatMode.OFF
            val repeatIconId = if (playbackState.repeatMode == AudioRepeatMode.ONE) {
                R.drawable.ic_action_repeat_one
            } else {
                R.drawable.ic_action_repeat
            }
            LumaButton(
                onClick = { viewModel.cycleRepeatMode() },
                variant = if (isRepeatActive) LumaButtonVariant.Secondary else LumaButtonVariant.Ghost,
                shape = CircleShape,
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier
                    .size(44.dp)
                    .focusRequester(repeatButtonFocusRequester)
                    .focusProperties {
                        left = nextButtonFocusRequester
                        right = firstTrackFocusRequester
                        up = firstTrackFocusRequester
                        down = FocusRequester.Cancel
                    }
            ) {
                Icon(
                    painter = painterResource(id = repeatIconId),
                    contentDescription = "Repeat",
                    modifier = Modifier.size(20.dp),
                    tint = if (isRepeatActive) colors.cyan else colors.textSecondary
                )
            }
        }
    }
}

// -----------------------------------------------------------------------------
// Right Panel: Accessible Track List
// -----------------------------------------------------------------------------

@OptIn(ExperimentalTvMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
private fun TrackListPanel(
    tracks: List<AudioItem>,
    playbackState: AudioPlayerState,
    onTrackClick: (AudioItem) -> Unit,
    playButtonFocusRequester: FocusRequester,
    firstTrackFocusRequester: FocusRequester,
    modifier: Modifier = Modifier
) {
    val colors = LumaTheme.colors
    val spacing = LumaTheme.spacing
    val listState = rememberLazyListState()

    Column(
        modifier = modifier
            .clip(LumaTheme.shapes.large)
            .background(colors.surfaceContainerLow)
            .padding(spacing.spaceMd)
    ) {
        // Track list header with track count badge
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_nav_music),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = colors.cyan
                )
                Text(
                    text = "All Songs",
                    style = LumaTheme.typography.headlineMd,
                    color = colors.textPrimary
                )
                LumaBadge(
                    text = "${tracks.size} Tracks",
                    variant = LumaBadgeVariant.Highlight
                )
            }
        }

        // Lazy vertical track list
        LazyColumn(
            state = listState,
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            itemsIndexed(
                items = tracks,
                key = { _, track -> track.id }
            ) { index, track ->
                val isActiveTrack = playbackState.currentTrack?.id == track.id
                val rowFocusModifier = Modifier
                    .fillMaxWidth()
                    .focusProperties {
                        left = playButtonFocusRequester
                        if (index == 0) {
                            up = FocusRequester.Cancel
                        }
                    }
                    .then(
                        if (index == 0) Modifier.focusRequester(firstTrackFocusRequester)
                        else Modifier
                    )

                LumaCard(
                    onClick = { onTrackClick(track) },
                    modifier = rowFocusModifier
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            // Active playing indicator or Track number
                            if (isActiveTrack) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(colors.cyan.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        painter = painterResource(
                                            id = if (playbackState.isPlaying) R.drawable.ic_action_volume_up else R.drawable.ic_action_pause
                                        ),
                                        contentDescription = if (playbackState.isPlaying) "Playing" else "Paused",
                                        modifier = Modifier.size(16.dp),
                                        tint = colors.cyan
                                    )
                                }
                            } else {
                                Text(
                                    text = String.format(Locale.US, "%02d", index + 1),
                                    style = LumaTheme.typography.labelMd,
                                    color = colors.textMuted,
                                    modifier = Modifier.width(28.dp),
                                    textAlign = TextAlign.Center
                                )
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = track.cleanTitle,
                                    style = LumaTheme.typography.labelLg,
                                    color = if (isActiveTrack) colors.cyan else colors.textPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = track.displayArtist,
                                    style = LumaTheme.typography.bodySm,
                                    color = colors.textSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            val formatTag = track.formatTag
                            LumaBadge(
                                text = formatTag,
                                variant = if (isActiveTrack) LumaBadgeVariant.Highlight else LumaBadgeVariant.Standard
                            )
                            Text(
                                text = track.formattedDuration,
                                style = LumaTheme.typography.labelSm,
                                color = colors.textSecondary
                            )
                        }
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// State Views: Loading, Empty, Error
// -----------------------------------------------------------------------------

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun MusicLoadingContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_nav_music),
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = LumaTheme.colors.cyan
            )
            Text(
                text = "Scanning music library...",
                style = LumaTheme.typography.bodyMd,
                color = LumaTheme.colors.textSecondary
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
private fun MusicEmptyContent(
    onScan: () -> Unit,
    sidebarMusicFocusRequester: FocusRequester,
    actionFocusRequester: FocusRequester? = null
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_nav_music),
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = LumaTheme.colors.textMuted
            )
            Text(
                text = "No Music Found",
                style = LumaTheme.typography.headlineMd,
                color = LumaTheme.colors.textPrimary
            )
            Text(
                text = "No audio tracks discovered in local storage. Add music to /Music or rescan.",
                style = LumaTheme.typography.bodyMd,
                color = LumaTheme.colors.textSecondary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            val buttonModifier = Modifier
                .then(
                    if (actionFocusRequester != null) Modifier.focusRequester(actionFocusRequester)
                    else Modifier
                )
                .focusProperties {
                    left = sidebarMusicFocusRequester
                }
            LumaButton(
                onClick = onScan,
                variant = LumaButtonVariant.Primary,
                modifier = buttonModifier
            ) {
                Text(
                    text = "Scan Music",
                    style = LumaTheme.typography.labelLg
                )
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
private fun MusicErrorContent(
    message: String,
    onRetry: () -> Unit,
    sidebarMusicFocusRequester: FocusRequester,
    actionFocusRequester: FocusRequester? = null
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Text(
                text = "Music Library Error",
                style = LumaTheme.typography.headlineMd,
                color = LumaTheme.colors.error
            )
            Text(
                text = message,
                style = LumaTheme.typography.bodyMd,
                color = LumaTheme.colors.textSecondary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            val buttonModifier = Modifier
                .then(
                    if (actionFocusRequester != null) Modifier.focusRequester(actionFocusRequester)
                    else Modifier
                )
                .focusProperties {
                    left = sidebarMusicFocusRequester
                }
            LumaButton(
                onClick = onRetry,
                variant = LumaButtonVariant.Primary,
                modifier = buttonModifier
            ) {
                Text(
                    text = "Retry",
                    style = LumaTheme.typography.labelLg
                )
            }
        }
    }
}
