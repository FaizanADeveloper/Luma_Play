package com.lumaplay.tv.ui.music

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.focusable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.focusGroup
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.ColumnScope
import androidx.tv.material3.Border
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Glow
import androidx.tv.material3.Icon
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.lumaplay.tv.R
import com.lumaplay.tv.core.theme.LumaTheme
import com.lumaplay.tv.core.ui.components.LumaBadge
import com.lumaplay.tv.core.ui.components.LumaBadgeVariant
import com.lumaplay.tv.core.ui.components.LumaButton
import com.lumaplay.tv.core.ui.components.LumaButtonVariant
import com.lumaplay.tv.data.model.AlbumItem
import com.lumaplay.tv.data.model.ArtistItem
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
    val firstAlbumFocusRequester = remember { FocusRequester() }
    val firstArtistFocusRequester = remember { FocusRequester() }
    val songsChipFocusRequester = contentFocusRequester ?: remember { FocusRequester() }
    val albumsChipFocusRequester = remember { FocusRequester() }
    val artistsChipFocusRequester = remember { FocusRequester() }
    val playlistsChipFocusRequester = remember { FocusRequester() }
    val recentlyPlayedChipFocusRequester = remember { FocusRequester() }

    val activeChipFocusRequester = when ((libraryState as? MusicLibraryUiState.Success)?.selectedCategory) {
        MusicCategory.ALBUMS -> albumsChipFocusRequester
        MusicCategory.ARTISTS -> artistsChipFocusRequester
        MusicCategory.PLAYLISTS -> playlistsChipFocusRequester
        MusicCategory.RECENTLY_PLAYED -> recentlyPlayedChipFocusRequester
        else -> songsChipFocusRequester
    }

    val activeContentFocusRequester = when ((libraryState as? MusicLibraryUiState.Success)?.selectedCategory) {
        MusicCategory.ALBUMS -> firstAlbumFocusRequester
        MusicCategory.ARTISTS -> firstArtistFocusRequester
        else -> firstTrackFocusRequester
    }

    // Direct initial focus handling when entering Music Screen while idle
    LaunchedEffect(Unit) {
        if (playbackState.currentTrack == null) {
            runCatching { songsChipFocusRequester.requestFocus() }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LumaTheme.colors.background)
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
                    librarySuccessState = state,
                    playbackState = playbackState,
                    viewModel = viewModel,
                    sidebarMusicFocusRequester = sidebarMusicFocusRequester,
                    playButtonFocusRequester = playButtonFocusRequester,
                    firstTrackFocusRequester = firstTrackFocusRequester,
                    firstAlbumFocusRequester = firstAlbumFocusRequester,
                    firstArtistFocusRequester = firstArtistFocusRequester,
                    activeChipFocusRequester = activeChipFocusRequester,
                    activeContentFocusRequester = activeContentFocusRequester,
                    songsChipFocusRequester = songsChipFocusRequester,
                    albumsChipFocusRequester = albumsChipFocusRequester,
                    artistsChipFocusRequester = artistsChipFocusRequester,
                    playlistsChipFocusRequester = playlistsChipFocusRequester,
                    recentlyPlayedChipFocusRequester = recentlyPlayedChipFocusRequester
                )
            }
        }
    }
}

// -----------------------------------------------------------------------------
// Layout Restructuring: Three Vertical Zones (Top Navigation, Dual Panels, Reserved Albums)
// -----------------------------------------------------------------------------

@OptIn(ExperimentalTvMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
private fun MusicDualPanelContent(
    librarySuccessState: MusicLibraryUiState.Success,
    playbackState: AudioPlayerState,
    viewModel: MusicPlayerViewModel,
    sidebarMusicFocusRequester: FocusRequester,
    playButtonFocusRequester: FocusRequester,
    firstTrackFocusRequester: FocusRequester,
    firstAlbumFocusRequester: FocusRequester,
    firstArtistFocusRequester: FocusRequester,
    activeChipFocusRequester: FocusRequester,
    activeContentFocusRequester: FocusRequester,
    songsChipFocusRequester: FocusRequester,
    albumsChipFocusRequester: FocusRequester,
    artistsChipFocusRequester: FocusRequester,
    playlistsChipFocusRequester: FocusRequester,
    recentlyPlayedChipFocusRequester: FocusRequester
) {
    val spacing = LumaTheme.spacing

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                start = spacing.tvSafeMargin,
                end = spacing.tvSafeMargin,
                top = spacing.spaceSm,
                bottom = spacing.spaceSm
            ),
        verticalArrangement = Arrangement.spacedBy(spacing.spaceSm)
    ) {
        // --- Zone 1: Full-Width Top Category Navigation Row ---
        MusicTopNavigationRow(
            selectedCategory = librarySuccessState.selectedCategory,
            tracksCount = librarySuccessState.tracks.size,
            albumsCount = librarySuccessState.albums.size,
            artistsCount = librarySuccessState.artists.size,
            onSelectCategory = { viewModel.selectCategory(it) },
            sidebarMusicFocusRequester = sidebarMusicFocusRequester,
            playButtonFocusRequester = playButtonFocusRequester,
            activeContentFocusRequester = activeContentFocusRequester,
            songsChipFocusRequester = songsChipFocusRequester,
            albumsChipFocusRequester = albumsChipFocusRequester,
            artistsChipFocusRequester = artistsChipFocusRequester,
            playlistsChipFocusRequester = playlistsChipFocusRequester,
            recentlyPlayedChipFocusRequester = recentlyPlayedChipFocusRequester
        )

        // --- Zone 2: Main Dual-Panel 10-Foot Experience (~5/12 and ~7/12) ---
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing.gutter)
        ) {
            // Left Panel: Now Playing Focus Card (5/12 width, ~41.7%)
            NowPlayingPanel(
                playbackState = playbackState,
                viewModel = viewModel,
                sidebarMusicFocusRequester = sidebarMusicFocusRequester,
                playButtonFocusRequester = playButtonFocusRequester,
                firstTrackFocusRequester = firstTrackFocusRequester,
                activeChipFocusRequester = activeChipFocusRequester,
                activeContentFocusRequester = activeContentFocusRequester,
                modifier = Modifier
                    .weight(5f)
                    .fillMaxHeight()
            )

            // Right Panel: Music Browsing (Songs, Albums, Artists, Playlists, Recently Played) (7/12 width, ~58.3%)
            MusicBrowsingPanel(
                librarySuccessState = librarySuccessState,
                playbackState = playbackState,
                viewModel = viewModel,
                playButtonFocusRequester = playButtonFocusRequester,
                firstTrackFocusRequester = firstTrackFocusRequester,
                firstAlbumFocusRequester = firstAlbumFocusRequester,
                firstArtistFocusRequester = firstArtistFocusRequester,
                activeChipFocusRequester = activeChipFocusRequester,
                modifier = Modifier
                    .weight(7f)
                    .fillMaxHeight()
            )
        }

        // --- Zone 3: Reserved Region for Stage 4 Recently Played Albums Carousel ---
        ReservedRecentlyPlayedAlbumsRegion()
    }
}

/**
 * Zone 1: Full-width top category navigation row containing the 5 reference categories
 * and the Audio DSP Direct Bitstream indicator matching Google Stitch reference.
 */
@OptIn(ExperimentalTvMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
private fun MusicTopNavigationRow(
    selectedCategory: MusicCategory,
    tracksCount: Int,
    albumsCount: Int,
    artistsCount: Int,
    onSelectCategory: (MusicCategory) -> Unit,
    sidebarMusicFocusRequester: FocusRequester,
    playButtonFocusRequester: FocusRequester,
    activeContentFocusRequester: FocusRequester,
    songsChipFocusRequester: FocusRequester,
    albumsChipFocusRequester: FocusRequester,
    artistsChipFocusRequester: FocusRequester,
    playlistsChipFocusRequester: FocusRequester,
    recentlyPlayedChipFocusRequester: FocusRequester,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(42.dp)
            .focusGroup()
    ) {
        val isCompactWidth = maxWidth < 760.dp

        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left section: 5 Remote Focusable Category Chips matching Google Stitch reference
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CategoryChip(
                    label = "All Songs",
                    count = tracksCount,
                    isSelected = selectedCategory == MusicCategory.SONGS,
                    onClick = { onSelectCategory(MusicCategory.SONGS) },
                    focusRequester = songsChipFocusRequester,
                    modifier = Modifier.focusProperties {
                        left = sidebarMusicFocusRequester
                        right = albumsChipFocusRequester
                        down = playButtonFocusRequester
                    }
                )

                CategoryChip(
                    label = "Albums",
                    count = albumsCount,
                    isSelected = selectedCategory == MusicCategory.ALBUMS,
                    onClick = { onSelectCategory(MusicCategory.ALBUMS) },
                    focusRequester = albumsChipFocusRequester,
                    modifier = Modifier.focusProperties {
                        left = songsChipFocusRequester
                        right = artistsChipFocusRequester
                        down = playButtonFocusRequester
                    }
                )

                CategoryChip(
                    label = "Artists",
                    count = artistsCount,
                    isSelected = selectedCategory == MusicCategory.ARTISTS,
                    onClick = { onSelectCategory(MusicCategory.ARTISTS) },
                    focusRequester = artistsChipFocusRequester,
                    modifier = Modifier.focusProperties {
                        left = albumsChipFocusRequester
                        right = playlistsChipFocusRequester
                        down = activeContentFocusRequester
                    }
                )

                CategoryChip(
                    label = "Playlists",
                    count = null,
                    isSelected = selectedCategory == MusicCategory.PLAYLISTS,
                    onClick = { onSelectCategory(MusicCategory.PLAYLISTS) },
                    focusRequester = playlistsChipFocusRequester,
                    modifier = Modifier.focusProperties {
                        left = artistsChipFocusRequester
                        right = recentlyPlayedChipFocusRequester
                        down = activeContentFocusRequester
                    }
                )

                CategoryChip(
                    label = "Recently Played",
                    count = null,
                    isSelected = selectedCategory == MusicCategory.RECENTLY_PLAYED,
                    onClick = { onSelectCategory(MusicCategory.RECENTLY_PLAYED) },
                    focusRequester = recentlyPlayedChipFocusRequester,
                    modifier = Modifier.focusProperties {
                        left = playlistsChipFocusRequester
                        right = FocusRequester.Cancel
                        down = activeContentFocusRequester
                    }
                )
            }

            // Right section: Audio DSP: Direct Bitstream Status Indicator (compact icon when width < 760dp)
            AudioDspIndicator(compact = isCompactWidth)
        }
    }
}

/**
 * Visual status indicator badge for audio direct bitstream DSP matching Google Stitch reference.
 * Supports compact icon-only presentation when horizontal space is constrained (e.g. sidebar expanded).
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun AudioDspIndicator(
    compact: Boolean = false,
    modifier: Modifier = Modifier
) {
    val colors = LumaTheme.colors
    Row(
        modifier = modifier
            .height(38.dp)
            .clip(LumaTheme.shapes.medium)
            .background(colors.surfaceContainerLow)
            .padding(
                horizontal = if (compact) 10.dp else 12.dp,
                vertical = 8.dp
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_action_graphic_eq),
            contentDescription = if (compact) "Audio DSP: Direct Bitstream" else null,
            modifier = Modifier.size(16.dp),
            tint = colors.cyan
        )
        if (!compact) {
            Text(
                text = "AUDIO DSP: DIRECT BITSTREAM",
                style = LumaTheme.typography.labelSm,
                color = colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Zone 3: Reserved bottom region establishing the structural anchor for the subsequent
 * Stage 4 Recently Played Albums implementation without introducing viewport clipping.
 */
@Composable
private fun ReservedRecentlyPlayedAlbumsRegion(
    modifier: Modifier = Modifier
) {
    // Structural placeholder for Stage 4 Recently Played Albums carousel.
    // Preserved as zero-height layout node during Stage 1 to prevent viewport clipping.
    Spacer(modifier = modifier)
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
    activeChipFocusRequester: FocusRequester,
    activeContentFocusRequester: FocusRequester,
    modifier: Modifier = Modifier
) {
    val currentTrack = playbackState.currentTrack
    val colors = LumaTheme.colors
    val spacing = LumaTheme.spacing

    val scrubberFocusRequester = remember { FocusRequester() }
    var isScrubberFocused by remember { mutableStateOf(false) }
    val canSeek = playbackState.currentTrack != null && playbackState.durationMs > 0L
    val transportUpFocusRequester = if (canSeek) scrubberFocusRequester else activeChipFocusRequester

    val shuffleButtonFocusRequester = remember { FocusRequester() }
    val prevButtonFocusRequester = remember { FocusRequester() }
    val nextButtonFocusRequester = remember { FocusRequester() }
    val repeatButtonFocusRequester = remember { FocusRequester() }

    BoxWithConstraints(
        modifier = modifier
            .focusGroup()
            .clip(LumaTheme.shapes.large)
            .background(colors.surfaceContainerLow)
    ) {
        val isCompactHeight = maxHeight < 380.dp
        val artworkSize = when {
            maxHeight >= 410.dp -> 184.dp
            maxHeight >= 370.dp -> 176.dp
            maxHeight >= 330.dp -> 144.dp
            else -> 120.dp
        }.coerceAtMost(maxWidth - 28.dp)

        val artworkBottomSpacer = if (isCompactHeight) 6.dp else 10.dp
        val metadataBottomSpacer = if (isCompactHeight) 6.dp else 10.dp
        val scrubberPaddingVertical = if (isCompactHeight) 2.dp else 4.dp
        val transportRowTopPadding = if (isCompactHeight) 6.dp else 8.dp
        val secBtnSize = 44.dp
        val priBtnSize = 56.dp
        val secIconSize = 20.dp
        val priIconSize = 26.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    horizontal = spacing.spaceMd,
                    vertical = if (isCompactHeight) spacing.spaceSm else spacing.spaceMd
                ),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top section: Artwork + Metadata + Timeline Scrubber
            // Weighted (weight = 1f, fill = false) so it yields space to unweighted transport controls
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .fillMaxWidth()
            ) {
                // Artwork Box (1:1 aspect ratio square, centered horizontally)
                Box(
                    modifier = Modifier
                        .size(artworkSize)
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
                                modifier = Modifier.size(if (artworkSize < 150.dp) 40.dp else 56.dp),
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
                                .padding(if (isCompactHeight) 6.dp else 8.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(artworkBottomSpacer))

                // Track metadata
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_nav_music),
                        contentDescription = null,
                        modifier = Modifier.size(if (isCompactHeight) 18.dp else 20.dp),
                        tint = colors.cyan
                    )
                    Text(
                        text = currentTrack?.cleanTitle ?: "No Track Selected",
                        style = if (isCompactHeight) LumaTheme.typography.headlineSm else LumaTheme.typography.headlineMd,
                        color = colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

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
                        style = if (isCompactHeight) LumaTheme.typography.bodySm else LumaTheme.typography.bodyMd,
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

                Spacer(modifier = Modifier.height(metadataBottomSpacer))

                // Timeline Scrubber Bar with interactive TV D-pad seeking
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(scrubberFocusRequester)
                        .focusProperties {
                            canFocus = canSeek
                            down = playButtonFocusRequester
                            up = activeChipFocusRequester
                        }
                        .onFocusChanged { isScrubberFocused = it.isFocused }
                        .focusable(enabled = canSeek)
                        .onKeyEvent { keyEvent ->
                            if (!canSeek) return@onKeyEvent false
                            when (keyEvent.key) {
                                Key.DirectionRight -> {
                                    if (keyEvent.type == KeyEventType.KeyDown) {
                                        viewModel.seekBy(10_000L)
                                    }
                                    true
                                }
                                Key.DirectionLeft -> {
                                    if (keyEvent.type == KeyEventType.KeyDown) {
                                        viewModel.seekBy(-10_000L)
                                    }
                                    true
                                }
                                Key.DirectionCenter, Key.Enter -> {
                                    if (keyEvent.type == KeyEventType.KeyDown) {
                                        viewModel.togglePlayPause()
                                    }
                                    true
                                }
                                else -> false
                            }
                        }
                        .clip(LumaTheme.shapes.pill)
                        .border(
                            width = if (isScrubberFocused) 1.5.dp else 0.dp,
                            color = if (isScrubberFocused) colors.cyan else Color.Transparent,
                            shape = LumaTheme.shapes.pill
                        )
                        .background(
                            color = if (isScrubberFocused) colors.cyan.copy(alpha = 0.08f) else Color.Transparent,
                            shape = LumaTheme.shapes.pill
                        )
                        .padding(horizontal = 8.dp, vertical = scrubberPaddingVertical)
                ) {
                    BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(if (isCompactHeight) 12.dp else 16.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        val trackHeight = if (isScrubberFocused) 5.dp else 4.dp
                        val thumbRadius = if (isScrubberFocused) 8.dp else 6.dp
                        val thumbDiameter = thumbRadius * 2
                        val progress = playbackState.progress.coerceIn(0f, 1f)
                        val thumbOffset = (maxWidth * progress - thumbRadius).coerceIn(0.dp, maxWidth - thumbDiameter)

                        // Track background line
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(trackHeight)
                                .clip(LumaTheme.shapes.pill)
                                .background(colors.surfaceContainerHighest)
                        )
                        // Filled cyan progress line
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progress)
                                .height(trackHeight)
                                .clip(LumaTheme.shapes.pill)
                                .background(colors.cyan)
                        )
                        // Circular thumb with luminous cyan accent
                        Box(
                            modifier = Modifier
                                .offset(x = thumbOffset)
                                .size(thumbDiameter)
                                .clip(CircleShape)
                                .background(colors.textPrimary)
                                .border(
                                    width = if (isScrubberFocused) 2.dp else 1.5.dp,
                                    color = colors.cyan,
                                    shape = CircleShape
                                )
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = playbackState.formattedCurrentPosition,
                            style = LumaTheme.typography.labelSm,
                            color = if (isScrubberFocused) colors.cyan else colors.cyan.copy(alpha = 0.85f)
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
            // Unweighted, measured first to guarantee full visibility on screen
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 4.dp, end = 4.dp, top = transportRowTopPadding),
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
                        .size(secBtnSize)
                        .focusRequester(shuffleButtonFocusRequester)
                        .focusProperties {
                            left = sidebarMusicFocusRequester
                            right = prevButtonFocusRequester
                            up = transportUpFocusRequester
                            down = FocusRequester.Cancel
                        }
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_action_shuffle),
                        contentDescription = "Shuffle",
                        modifier = Modifier.size(secIconSize),
                        tint = if (playbackState.isShuffled) colors.cyan else LocalContentColor.current
                    )
                }

                // 2. Previous Button
                LumaButton(
                    onClick = { viewModel.previous() },
                    variant = LumaButtonVariant.Ghost,
                    shape = CircleShape,
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier
                        .size(secBtnSize)
                        .focusRequester(prevButtonFocusRequester)
                        .focusProperties {
                            left = shuffleButtonFocusRequester
                            right = playButtonFocusRequester
                            up = transportUpFocusRequester
                            down = FocusRequester.Cancel
                        }
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_action_skip_previous),
                        contentDescription = "Previous",
                        modifier = Modifier.size(secIconSize),
                        tint = LocalContentColor.current
                    )
                }

                // 3. Play / Pause Button (Primary Action Anchor)
                LumaButton(
                    onClick = { viewModel.togglePlayPause() },
                    variant = LumaButtonVariant.Primary,
                    shape = CircleShape,
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier
                        .size(priBtnSize)
                        .focusRequester(playButtonFocusRequester)
                        .focusProperties {
                            left = prevButtonFocusRequester
                            right = nextButtonFocusRequester
                            up = transportUpFocusRequester
                            down = FocusRequester.Cancel
                        }
                ) {
                    Icon(
                        painter = painterResource(
                            id = if (playbackState.isPlaying) R.drawable.ic_action_pause else R.drawable.ic_action_play
                        ),
                        contentDescription = if (playbackState.isPlaying) "Pause" else "Play",
                        modifier = Modifier.size(priIconSize),
                        tint = LocalContentColor.current
                    )
                }

                // 4. Next Button
                LumaButton(
                    onClick = { viewModel.next() },
                    variant = LumaButtonVariant.Ghost,
                    shape = CircleShape,
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier
                        .size(secBtnSize)
                        .focusRequester(nextButtonFocusRequester)
                        .focusProperties {
                            left = playButtonFocusRequester
                            right = repeatButtonFocusRequester
                            up = transportUpFocusRequester
                            down = FocusRequester.Cancel
                        }
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_action_skip_next),
                        contentDescription = "Next",
                        modifier = Modifier.size(secIconSize),
                        tint = LocalContentColor.current
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
                        .size(secBtnSize)
                        .focusRequester(repeatButtonFocusRequester)
                        .focusProperties {
                            left = nextButtonFocusRequester
                            right = activeContentFocusRequester
                            up = transportUpFocusRequester
                            down = FocusRequester.Cancel
                        }
                ) {
                    Icon(
                        painter = painterResource(id = repeatIconId),
                        contentDescription = "Repeat",
                        modifier = Modifier.size(secIconSize),
                        tint = if (isRepeatActive) colors.cyan else LocalContentColor.current
                    )
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// Right Panel: Music Browsing (Songs, Albums, Artists) with Category Chips
// -----------------------------------------------------------------------------

@OptIn(ExperimentalTvMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
private fun MusicBrowsingPanel(
    librarySuccessState: MusicLibraryUiState.Success,
    playbackState: AudioPlayerState,
    viewModel: MusicPlayerViewModel,
    playButtonFocusRequester: FocusRequester,
    firstTrackFocusRequester: FocusRequester,
    firstAlbumFocusRequester: FocusRequester,
    firstArtistFocusRequester: FocusRequester,
    activeChipFocusRequester: FocusRequester,
    modifier: Modifier = Modifier
) {
    val colors = LumaTheme.colors
    val spacing = LumaTheme.spacing
    val tracks = librarySuccessState.tracks
    val albums = librarySuccessState.albums
    val artists = librarySuccessState.artists
    val selectedCategory = librarySuccessState.selectedCategory
    val coroutineScope = rememberCoroutineScope()

    val songsListState = rememberLazyListState()
    val albumsListState = rememberLazyListState()
    val artistsListState = rememberLazyListState()

    Column(
        modifier = modifier
            .focusGroup()
            .clip(LumaTheme.shapes.large)
            .background(colors.surfaceContainerLow)
            .padding(horizontal = spacing.spaceMd, vertical = spacing.spaceSm)
    ) {
        // Contextual browsing content per active category
        when (selectedCategory) {
            MusicCategory.SONGS -> {
                if (tracks.isEmpty()) {
                    BrowsingEmptyState(
                        title = "No Songs Available",
                        message = "No audio tracks discovered.",
                        playButtonFocusRequester = playButtonFocusRequester,
                        activeChipFocusRequester = activeChipFocusRequester,
                        actionFocusRequester = firstTrackFocusRequester
                    )
                } else {
                    LazyColumn(
                        state = songsListState,
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        contentPadding = PaddingValues(vertical = 4.dp),
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .focusGroup()
                    ) {
                        itemsIndexed(
                            items = tracks,
                            key = { _, track -> track.id }
                        ) { index, track ->
                            val isActiveTrack = playbackState.currentTrack?.id == track.id
                            val rowFocusModifier = Modifier
                                .fillMaxWidth()
                                .let { if (index == 0) it.focusRequester(firstTrackFocusRequester) else it }
                                .focusProperties {
                                    left = playButtonFocusRequester
                                    if (index == 0) {
                                        up = activeChipFocusRequester
                                    }
                                }

                            TrackRowCard(
                                onClick = { viewModel.playTrack(track, tracks) },
                                isActiveTrack = isActiveTrack,
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

            MusicCategory.ALBUMS -> {
                if (albums.isEmpty()) {
                    BrowsingEmptyState(
                        title = "No Albums Found",
                        message = "No album metadata discovered in local storage.",
                        playButtonFocusRequester = playButtonFocusRequester,
                        activeChipFocusRequester = activeChipFocusRequester,
                        actionFocusRequester = firstAlbumFocusRequester
                    )
                } else {
                    LazyColumn(
                        state = albumsListState,
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        contentPadding = PaddingValues(vertical = 4.dp),
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .focusGroup()
                    ) {
                        itemsIndexed(
                            items = albums,
                            key = { _, album -> album.id }
                        ) { index, album ->
                            val isCurrentAlbumPlaying = playbackState.currentTrack != null &&
                                album.tracks.any { it.id == playbackState.currentTrack.id }

                            val rowFocusModifier = Modifier
                                .fillMaxWidth()
                                .let { if (index == 0) it.focusRequester(firstAlbumFocusRequester) else it }
                                .focusProperties {
                                    left = playButtonFocusRequester
                                    if (index == 0) {
                                        up = activeChipFocusRequester
                                    }
                                }

                            AlbumRowCard(
                                album = album,
                                isCurrentAlbumPlaying = isCurrentAlbumPlaying,
                                onClick = { viewModel.playAlbum(album) },
                                modifier = rowFocusModifier
                            )
                        }
                    }
                }
            }

            MusicCategory.ARTISTS -> {
                if (artists.isEmpty()) {
                    BrowsingEmptyState(
                        title = "No Artists Found",
                        message = "No artist metadata discovered in local storage.",
                        playButtonFocusRequester = playButtonFocusRequester,
                        activeChipFocusRequester = activeChipFocusRequester,
                        actionFocusRequester = firstArtistFocusRequester
                    )
                } else {
                    LazyColumn(
                        state = artistsListState,
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        contentPadding = PaddingValues(vertical = 4.dp),
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .focusGroup()
                    ) {
                        itemsIndexed(
                            items = artists,
                            key = { _, artist -> artist.name }
                        ) { index, artist ->
                            val isCurrentArtistPlaying = playbackState.currentTrack != null &&
                                artist.tracks.any { it.id == playbackState.currentTrack.id }

                            val rowFocusModifier = Modifier
                                .fillMaxWidth()
                                .let { if (index == 0) it.focusRequester(firstArtistFocusRequester) else it }
                                .focusProperties {
                                    left = playButtonFocusRequester
                                    if (index == 0) {
                                        up = activeChipFocusRequester
                                    }
                                }

                            ArtistRowCard(
                                artist = artist,
                                isCurrentArtistPlaying = isCurrentArtistPlaying,
                                onClick = { viewModel.playArtist(artist) },
                                modifier = rowFocusModifier
                            )
                        }
                    }
                }
            }

            MusicCategory.PLAYLISTS -> {
                BrowsingEmptyState(
                    title = "No Playlists Found",
                    message = "No local playlists available.",
                    playButtonFocusRequester = playButtonFocusRequester,
                    activeChipFocusRequester = activeChipFocusRequester,
                    actionFocusRequester = playButtonFocusRequester
                )
            }

            MusicCategory.RECENTLY_PLAYED -> {
                BrowsingEmptyState(
                    title = "No Recently Played Tracks",
                    message = "Recently played tracks will appear here once audio playback history is recorded.",
                    playButtonFocusRequester = playButtonFocusRequester,
                    activeChipFocusRequester = activeChipFocusRequester,
                    actionFocusRequester = playButtonFocusRequester
                )
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun CategoryChip(
    label: String,
    count: Int? = null,
    isSelected: Boolean,
    onClick: () -> Unit,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier
) {
    val colors = LumaTheme.colors
    val shape = LumaTheme.shapes.medium // 8.dp

    Card(
        onClick = onClick,
        modifier = modifier
            .height(38.dp)
            .focusRequester(focusRequester)
            .onFocusChanged {
                android.util.Log.d("LumaMusicFocus", "CategoryChip $label isFocused=${it.isFocused}")
            },
        shape = CardDefaults.shape(shape = shape),
        colors = CardDefaults.colors(
            containerColor = if (isSelected) colors.surfaceContainerHigh else colors.surfaceCard,
            contentColor = if (isSelected) colors.cyan else colors.textSecondary,
            focusedContainerColor = if (isSelected) colors.surfaceContainerHighest else colors.surfaceContainerHigh,
            focusedContentColor = if (isSelected) colors.cyan else colors.textPrimary,
            pressedContainerColor = colors.surfaceContainerHighest,
            pressedContentColor = colors.textPrimary
        ),
        scale = CardDefaults.scale(
            scale = 1.0f,
            focusedScale = 1.04f,
            pressedScale = 1.0f
        ),
        border = CardDefaults.border(
            border = if (isSelected) {
                Border(
                    border = BorderStroke(1.dp, colors.cyan.copy(alpha = 0.5f)),
                    shape = shape
                )
            } else {
                Border.None
            },
            focusedBorder = Border(
                border = BorderStroke(2.dp, colors.cyan),
                shape = shape
            )
        ),
        glow = CardDefaults.glow(
            glow = Glow.None,
            focusedGlow = Glow(elevationColor = colors.cyanGlow, elevation = 12.dp),
            pressedGlow = Glow.None
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxHeight()
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (isSelected) {
                val infiniteTransition = rememberInfiniteTransition(label = "ChipDotPulse")
                val dotAlpha by infiniteTransition.animateFloat(
                    initialValue = 0.4f,
                    targetValue = 1.0f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(durationMillis = 800, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "ChipDotAlpha"
                )
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(colors.cyan.copy(alpha = dotAlpha))
                )
            }
            Text(
                text = if (count != null) "$label ($count)" else label,
                style = LumaTheme.typography.labelMd,
                color = if (isSelected) colors.cyan else colors.textSecondary
            )
        }
    }
}

/**
 * 10-foot TV Album Row Card with artwork, title, artist, year, track count, and playing state.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun AlbumRowCard(
    album: AlbumItem,
    isCurrentAlbumPlaying: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LumaTheme.colors
    TrackRowCard(
        onClick = onClick,
        isActiveTrack = isCurrentAlbumPlaying,
        modifier = modifier
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
                // Album artwork or fallback
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(LumaTheme.shapes.small)
                        .background(colors.surfaceContainerHigh),
                    contentAlignment = Alignment.Center
                ) {
                    if (album.artworkUri != null) {
                        AsyncImage(
                            model = album.artworkUri,
                            contentDescription = album.displayTitle,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_nav_music),
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = if (isCurrentAlbumPlaying) colors.cyan else colors.textMuted
                        )
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = album.displayTitle,
                        style = LumaTheme.typography.labelLg,
                        color = if (isCurrentAlbumPlaying) colors.cyan else colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = album.displayArtist,
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
                album.formattedYear?.let { yearText ->
                    LumaBadge(
                        text = yearText,
                        variant = LumaBadgeVariant.Standard
                    )
                }
                LumaBadge(
                    text = album.formattedTrackCount,
                    variant = if (isCurrentAlbumPlaying) LumaBadgeVariant.Highlight else LumaBadgeVariant.Standard
                )
            }
        }
    }
}

/**
 * 10-foot TV Artist Row Card with representative artwork, artist name, album/track counts, and playing state.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun ArtistRowCard(
    artist: ArtistItem,
    isCurrentArtistPlaying: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LumaTheme.colors
    TrackRowCard(
        onClick = onClick,
        isActiveTrack = isCurrentArtistPlaying,
        modifier = modifier
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
                // Artist avatar or fallback
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(colors.surfaceContainerHigh),
                    contentAlignment = Alignment.Center
                ) {
                    if (artist.artworkUri != null) {
                        AsyncImage(
                            model = artist.artworkUri,
                            contentDescription = artist.displayName,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_status_person),
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = if (isCurrentArtistPlaying) colors.cyan else colors.textMuted
                        )
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = artist.displayName,
                        style = LumaTheme.typography.labelLg,
                        color = if (isCurrentArtistPlaying) colors.cyan else colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${artist.formattedAlbumCount} • ${artist.formattedTrackCount}",
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
                LumaBadge(
                    text = artist.formattedAlbumCount,
                    variant = LumaBadgeVariant.Standard
                )
                LumaBadge(
                    text = artist.formattedTrackCount,
                    variant = if (isCurrentArtistPlaying) LumaBadgeVariant.Highlight else LumaBadgeVariant.Standard
                )
            }
        }
    }
}

@Composable
private fun BrowsingEmptyState(
    title: String,
    message: String,
    playButtonFocusRequester: FocusRequester,
    activeChipFocusRequester: FocusRequester,
    actionFocusRequester: FocusRequester
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .focusRequester(actionFocusRequester)
            .focusProperties {
                left = playButtonFocusRequester
                up = activeChipFocusRequester
            }
            .focusable(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(24.dp)
        ) {
            Text(
                text = title,
                style = LumaTheme.typography.headlineMd,
                color = LumaTheme.colors.textPrimary
            )
            Text(
                text = message,
                style = LumaTheme.typography.bodyMd,
                color = LumaTheme.colors.textSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Dedicated 10-foot TV track row card tailored for the Music queue list.
 *
 * Implements Google Stitch specifications for list rows:
 * - 8dp corner radius ([LumaTheme.shapes.medium], Stitch rounded-lg).
 * - Fixed 1.0x scale (no viewport clipping overflow).
 * - No outer glow ([Glow.None]) to prevent horizontal edge scissor artifacts in [LazyColumn].
 * - Subtle 1.5dp Electric Cyan focus border for clear D-pad feedback.
 * - Distinct surface levels between resting, active, and focused states.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun TrackRowCard(
    onClick: () -> Unit,
    isActiveTrack: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = LumaTheme.colors
    val shape = LumaTheme.shapes.medium

    Card(
        onClick = onClick,
        modifier = modifier,
        shape = CardDefaults.shape(shape = shape),
        colors = CardDefaults.colors(
            containerColor = if (isActiveTrack) colors.surfaceContainerHigh else colors.surfaceCard,
            contentColor = colors.textPrimary,
            focusedContainerColor = if (isActiveTrack) colors.surfaceContainerHighest else colors.surfaceContainerHigh,
            focusedContentColor = colors.textPrimary,
            pressedContainerColor = colors.surfaceContainerHighest,
            pressedContentColor = colors.textPrimary
        ),
        scale = CardDefaults.scale(
            scale = 1.0f,
            focusedScale = 1.0f,
            pressedScale = 1.0f
        ),
        border = CardDefaults.border(
            focusedBorder = Border(
                border = BorderStroke(width = 1.5.dp, color = colors.cyan),
                shape = shape
            )
        ),
        glow = CardDefaults.glow(
            glow = Glow.None,
            focusedGlow = Glow.None,
            pressedGlow = Glow.None
        ),
        content = content
    )
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
