package com.lumaplay.tv.player.track

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.C
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import com.lumaplay.tv.R
import com.lumaplay.tv.core.theme.LumaTheme
import com.lumaplay.tv.core.ui.components.LumaBadge
import com.lumaplay.tv.core.ui.components.LumaBadgeVariant
import kotlinx.coroutines.delay

/**
 * Distinguishes track selection modality.
 */
enum class TrackType {
    AUDIO,
    SUBTITLE
}

/**
 * Pure, testable UI state representation for track selection dialogs.
 *
 * Encapsulates track availability, active selection, and counting logic without Android UI dependencies.
 */
data class TrackSelectionUiState(
    val isVisible: Boolean = false,
    val trackType: TrackType = TrackType.AUDIO,
    val audioTracks: List<AudioTrackItem> = emptyList(),
    val subtitleTracks: List<SubtitleTrackItem> = emptyList()
) {
    /**
     * Currently active audio track, or null if none selected.
     */
    val selectedAudioTrack: AudioTrackItem?
        get() = audioTracks.firstOrNull { it.isSelected }

    /**
     * Currently active subtitle track, or null if subtitles are off or unselected.
     */
    val selectedSubtitleTrack: SubtitleTrackItem?
        get() = subtitleTracks.firstOrNull { it.isSelected && !it.isOff }

    /**
     * True if "Subtitles Off" is currently active.
     */
    val isSubtitlesOff: Boolean
        get() = subtitleTracks.firstOrNull { it.isOff }?.isSelected ?: true

    /**
     * Number of real selectable subtitle tracks (excluding the "Off" option).
     */
    val realSubtitleCount: Int
        get() = subtitleTracks.count { !it.isOff }

    /**
     * Total number of available tracks for the active [trackType].
     */
    val activeTrackCount: Int
        get() = when (trackType) {
            TrackType.AUDIO -> audioTracks.size
            TrackType.SUBTITLE -> realSubtitleCount
        }

    /**
     * Whether the active track type has any tracks available to choose from.
     */
    val hasTracks: Boolean
        get() = when (trackType) {
            TrackType.AUDIO -> audioTracks.isNotEmpty()
            TrackType.SUBTITLE -> subtitleTracks.isNotEmpty()
        }
}

/**
 * TV-safe Audio Track Selection Dialog.
 *
 * Implements Google Stitch design specifications:
 * - 16dp rounded corners with surface container low background.
 * - Cyan D-pad focus treatment with 1.02f scaling.
 * - Displays language, channel configuration, codec, and active status.
 * - Restores focus to [returnFocusRequester] on dismissal.
 */
@Composable
fun AudioTrackSelectionDialog(
    isVisible: Boolean,
    tracks: List<AudioTrackItem>,
    onTrackSelected: (AudioTrackItem) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    returnFocusRequester: FocusRequester? = null
) {
    TrackSelectionDialog(
        isVisible = isVisible,
        trackType = TrackType.AUDIO,
        audioTracks = tracks,
        onAudioTrackSelected = onTrackSelected,
        onDismiss = onDismiss,
        modifier = modifier,
        returnFocusRequester = returnFocusRequester
    )
}

/**
 * TV-safe Subtitle Track Selection Dialog.
 *
 * Implements Google Stitch design specifications:
 * - Includes dedicated "Subtitles Off" option at the top.
 * - Displays language, format (SRT, PGS, VTT), and modifier badges (External, Forced, SDH).
 * - Restores focus to [returnFocusRequester] on dismissal.
 */
@Composable
fun SubtitleTrackSelectionDialog(
    isVisible: Boolean,
    tracks: List<SubtitleTrackItem>,
    onTrackSelected: (SubtitleTrackItem) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    returnFocusRequester: FocusRequester? = null
) {
    TrackSelectionDialog(
        isVisible = isVisible,
        trackType = TrackType.SUBTITLE,
        subtitleTracks = tracks,
        onSubtitleTrackSelected = onTrackSelected,
        onDismiss = onDismiss,
        modifier = modifier,
        returnFocusRequester = returnFocusRequester
    )
}

/**
 * Core TV modal dialog for selecting audio or subtitle tracks.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TrackSelectionDialog(
    isVisible: Boolean,
    trackType: TrackType,
    audioTracks: List<AudioTrackItem> = emptyList(),
    subtitleTracks: List<SubtitleTrackItem> = emptyList(),
    onAudioTrackSelected: (AudioTrackItem) -> Unit = {},
    onSubtitleTrackSelected: (SubtitleTrackItem) -> Unit = {},
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    onTrackTypeChanged: ((TrackType) -> Unit)? = null,
    returnFocusRequester: FocusRequester? = null
) {
    if (!isVisible) return

    val dismissWithReturnFocus: () -> Unit = {
        onDismiss()
        runCatching { returnFocusRequester?.requestFocus() }
    }

    // Intercept Back key on TV remote
    BackHandler(enabled = isVisible) {
        dismissWithReturnFocus()
    }

    val selectedAudioIndex = remember(audioTracks) {
        audioTracks.indexOfFirst { it.isSelected }.takeIf { it >= 0 } ?: 0
    }
    val selectedSubtitleIndex = remember(subtitleTracks) {
        subtitleTracks.indexOfFirst { it.isSelected }.takeIf { it >= 0 } ?: 0
    }

    val initialFocusRequester = remember { FocusRequester() }

    LaunchedEffect(isVisible, trackType) {
        delay(60L) // Allow layout to settle before TV remote focus snap
        runCatching { initialFocusRequester.requestFocus() }
    }

    // Modal Scrim Backdrop
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.75f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { dismissWithReturnFocus() }
            ),
        contentAlignment = Alignment.Center
    ) {
        // Modal Card Container (approx 16dp rounded corners per Stitch Level 2)
        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn(tween(180)) + scaleIn(tween(180), initialScale = 0.96f),
            exit = fadeOut(tween(120)) + scaleOut(tween(120), targetScale = 0.96f)
        ) {
            Box(
                modifier = modifier
                    .width(520.dp)
                    .heightIn(max = 480.dp)
                    .shadow(elevation = 24.dp, shape = RoundedCornerShape(16.dp))
                    .clip(RoundedCornerShape(16.dp))
                    .background(LumaTheme.colors.surfaceContainerLow.copy(alpha = 0.96f))
                    .border(
                        width = 1.dp,
                        color = LumaTheme.colors.outlineVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { /* prevent backdrop click through */ }
                    )
                    .padding(20.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // --- Dialog Header ---
                    TrackDialogHeader(
                        trackType = trackType,
                        audioTracks = audioTracks,
                        subtitleTracks = subtitleTracks,
                        onDismiss = dismissWithReturnFocus,
                        onTrackTypeChanged = onTrackTypeChanged
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // --- Dialog Content Body ---
                    when (trackType) {
                        TrackType.AUDIO -> {
                            if (audioTracks.isEmpty()) {
                                TrackEmptyState(
                                    message = "No audio tracks available",
                                    subMessage = "The active video stream contains no selectable audio channels.",
                                    onDismiss = dismissWithReturnFocus,
                                    focusRequester = initialFocusRequester
                                )
                            } else {
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f, fill = false),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(audioTracks) { track ->
                                        val isInitial = audioTracks.indexOf(track) == selectedAudioIndex
                                        AudioTrackRow(
                                            track = track,
                                            onClick = {
                                                if (track.isSupported) {
                                                    onAudioTrackSelected(track)
                                                    dismissWithReturnFocus()
                                                }
                                            },
                                            modifier = if (isInitial) {
                                                Modifier.focusRequester(initialFocusRequester)
                                            } else {
                                                Modifier
                                            }
                                        )
                                    }
                                }
                            }
                        }
                        TrackType.SUBTITLE -> {
                            if (subtitleTracks.isEmpty()) {
                                TrackEmptyState(
                                    message = "No subtitle tracks available",
                                    subMessage = "No embedded or external subtitle tracks were found for this video.",
                                    onDismiss = dismissWithReturnFocus,
                                    focusRequester = initialFocusRequester
                                )
                            } else {
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f, fill = false),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(subtitleTracks) { track ->
                                        val isInitial = subtitleTracks.indexOf(track) == selectedSubtitleIndex
                                        SubtitleTrackRow(
                                            track = track,
                                            onClick = {
                                                onSubtitleTrackSelected(track)
                                                dismissWithReturnFocus()
                                            },
                                            modifier = if (isInitial) {
                                                Modifier.focusRequester(initialFocusRequester)
                                            } else {
                                                Modifier
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // --- Bottom TV Remote D-pad Navigation Hint ---
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "▲ / ▼ Navigate",
                            style = LumaTheme.typography.labelSm,
                            color = LumaTheme.colors.textMuted
                        )
                        Text(
                            text = "OK Select • Back Close",
                            style = LumaTheme.typography.labelSm,
                            color = LumaTheme.colors.cyan
                        )
                    }
                }
            }
        }
    }
}

/**
 * Top header of the track selection dialog with title, track count badge, and optional tab switcher.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun TrackDialogHeader(
    trackType: TrackType,
    audioTracks: List<AudioTrackItem>,
    subtitleTracks: List<SubtitleTrackItem>,
    onDismiss: () -> Unit,
    onTrackTypeChanged: ((TrackType) -> Unit)?
) {
    val title = when (trackType) {
        TrackType.AUDIO -> "Audio Tracks"
        TrackType.SUBTITLE -> "Subtitles"
    }

    val iconRes = when (trackType) {
        TrackType.AUDIO -> R.drawable.ic_action_volume_up
        TrackType.SUBTITLE -> R.drawable.ic_action_subtitles
    }

    val countBadgeText = when (trackType) {
        TrackType.AUDIO -> {
            if (audioTracks.isNotEmpty()) "${audioTracks.size} Available" else "None"
        }
        TrackType.SUBTITLE -> {
            val realCount = subtitleTracks.count { !it.isOff }
            if (realCount > 0) "$realCount Available" else "Off"
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(
                        LumaTheme.colors.cyan.copy(alpha = 0.12f),
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
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                tint = LumaTheme.colors.cyan,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = title,
                style = LumaTheme.typography.headlineSm,
                color = LumaTheme.colors.textPrimary,
                fontWeight = FontWeight.Bold
            )
            LumaBadge(
                text = countBadgeText,
                variant = if (countBadgeText == "None") LumaBadgeVariant.Standard else LumaBadgeVariant.Highlight
            )
        }

        // Close button
        DialogCloseButton(onClick = onDismiss)
    }

    // Optional Audio/Subtitle Tabs if switcher is enabled
    if (onTrackTypeChanged != null) {
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            TrackTabButton(
                text = "Audio (${audioTracks.size})",
                isSelected = trackType == TrackType.AUDIO,
                onClick = { onTrackTypeChanged(TrackType.AUDIO) }
            )
            val subCount = subtitleTracks.count { !it.isOff }
            TrackTabButton(
                text = "Subtitles ($subCount)",
                isSelected = trackType == TrackType.SUBTITLE,
                onClick = { onTrackTypeChanged(TrackType.SUBTITLE) }
            )
        }
    }
}

/**
 * Interactive row representing an Audio Track with TV focus and selection indicators.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun AudioTrackRow(
    track: AudioTrackItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.02f else 1.0f,
        animationSpec = tween(150),
        label = "audio_row_scale"
    )

    val shape = RoundedCornerShape(10.dp)

    val bgColor = when {
        isFocused -> LumaTheme.colors.surfaceContainerHighest
        track.isSelected -> LumaTheme.colors.cyan.copy(alpha = 0.12f)
        else -> LumaTheme.colors.surfaceCard
    }

    val borderColor = when {
        isFocused -> LumaTheme.colors.cyan
        track.isSelected -> LumaTheme.colors.cyan.copy(alpha = 0.6f)
        else -> LumaTheme.colors.outlineVariant.copy(alpha = 0.35f)
    }

    val contentAlpha = if (track.isSupported) 1.0f else 0.45f

    Box(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .clip(shape)
            .background(bgColor)
            .border(if (isFocused) 2.dp else 1.dp, borderColor, shape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = track.isSupported,
                onClick = onClick
            )
            .focusable(interactionSource = interactionSource)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Language Name + Details (Codec, Channels)
            Column(
                modifier = Modifier.weight(1f, fill = false),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = track.languageName,
                        style = LumaTheme.typography.bodyLg,
                        color = if (isFocused) LumaTheme.colors.cyan else LumaTheme.colors.textPrimary.copy(alpha = contentAlpha),
                        fontWeight = if (track.isSelected || isFocused) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (!track.customLabel.isNullOrBlank() && !track.customLabel.equals(track.languageName, ignoreCase = true)) {
                        Text(
                            text = "• ${track.customLabel}",
                            style = LumaTheme.typography.bodySm,
                            color = LumaTheme.colors.textSecondary.copy(alpha = contentAlpha),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Audio Technical Attributes (Codec & Channel Layout)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (track.shortCodecLabel.isNotBlank()) {
                        LumaBadge(
                            text = track.shortCodecLabel,
                            variant = if (track.isSelected) LumaBadgeVariant.Highlight else LumaBadgeVariant.Standard
                        )
                    }

                    if (track.channelLayout.isNotBlank()) {
                        Text(
                            text = track.channelLayout,
                            style = LumaTheme.typography.labelSm,
                            color = LumaTheme.colors.textMuted.copy(alpha = contentAlpha)
                        )
                    }

                    if (!track.isSupported) {
                        Text(
                            text = "Unsupported Decoder",
                            style = LumaTheme.typography.labelSm,
                            color = LumaTheme.colors.error
                        )
                    }
                }
            }

            // Right: Selection Checkmark / Active Tag
            if (track.isSelected) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Active",
                        style = LumaTheme.typography.labelSm,
                        color = LumaTheme.colors.cyan,
                        fontWeight = FontWeight.Bold
                    )
                    Icon(
                        painter = painterResource(id = R.drawable.ic_action_check),
                        contentDescription = "Selected",
                        tint = LumaTheme.colors.cyan,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

/**
 * Interactive row representing a Subtitle Track (including "Subtitles Off").
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun SubtitleTrackRow(
    track: SubtitleTrackItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.02f else 1.0f,
        animationSpec = tween(150),
        label = "sub_row_scale"
    )

    val shape = RoundedCornerShape(10.dp)

    val bgColor = when {
        isFocused -> LumaTheme.colors.surfaceContainerHighest
        track.isSelected -> LumaTheme.colors.cyan.copy(alpha = 0.12f)
        else -> LumaTheme.colors.surfaceCard
    }

    val borderColor = when {
        isFocused -> LumaTheme.colors.cyan
        track.isSelected -> LumaTheme.colors.cyan.copy(alpha = 0.6f)
        else -> LumaTheme.colors.outlineVariant.copy(alpha = 0.35f)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .clip(shape)
            .background(bgColor)
            .border(if (isFocused) 2.dp else 1.dp, borderColor, shape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .focusable(interactionSource = interactionSource)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Title + Format / Modifier Badges
            Column(
                modifier = Modifier.weight(1f, fill = false),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                if (track.isOff) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_action_subtitles_off),
                            contentDescription = null,
                            tint = if (isFocused) LumaTheme.colors.cyan else LumaTheme.colors.textMuted,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Subtitles Off",
                            style = LumaTheme.typography.bodyLg,
                            color = if (isFocused) LumaTheme.colors.cyan else LumaTheme.colors.textPrimary,
                            fontWeight = if (track.isSelected || isFocused) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                    Text(
                        text = "Disable all subtitle overlays",
                        style = LumaTheme.typography.labelSm,
                        color = LumaTheme.colors.textMuted
                    )
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = track.languageName,
                            style = LumaTheme.typography.bodyLg,
                            color = if (isFocused) LumaTheme.colors.cyan else LumaTheme.colors.textPrimary,
                            fontWeight = if (track.isSelected || isFocused) FontWeight.Bold else FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        if (!track.customLabel.isNullOrBlank() && !track.customLabel.equals(track.languageName, ignoreCase = true)) {
                            Text(
                                text = "• ${track.customLabel}",
                                style = LumaTheme.typography.bodySm,
                                color = LumaTheme.colors.textSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Format and Modifier Badges
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (track.formatTag.isNotBlank() && track.formatTag != "Subtitles") {
                            LumaBadge(
                                text = track.formatTag,
                                variant = if (track.isSelected) LumaBadgeVariant.Highlight else LumaBadgeVariant.Standard
                            )
                        }

                        if (track.isExternal) {
                            LumaBadge(
                                text = "External",
                                variant = LumaBadgeVariant.Standard
                            )
                        }

                        val isForced = track.format?.selectionFlags?.and(C.SELECTION_FLAG_FORCED) != 0
                        if (isForced) {
                            LumaBadge(
                                text = "Forced",
                                variant = LumaBadgeVariant.Highlight
                            )
                        }

                        val isSdh = track.format?.roleFlags?.and(C.ROLE_FLAG_DESCRIBES_MUSIC_AND_SOUND) != 0
                        if (isSdh) {
                            LumaBadge(
                                text = "SDH",
                                variant = LumaBadgeVariant.Standard
                            )
                        }
                    }
                }
            }

            // Right: Selection Checkmark
            if (track.isSelected) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Active",
                        style = LumaTheme.typography.labelSm,
                        color = LumaTheme.colors.cyan,
                        fontWeight = FontWeight.Bold
                    )
                    Icon(
                        painter = painterResource(id = R.drawable.ic_action_check),
                        contentDescription = "Selected",
                        tint = LumaTheme.colors.cyan,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

/**
 * Clean empty state presentation when no tracks exist.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun TrackEmptyState(
    message: String,
    subMessage: String,
    onDismiss: () -> Unit,
    focusRequester: FocusRequester
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_action_info),
            contentDescription = null,
            tint = LumaTheme.colors.textMuted,
            modifier = Modifier.size(32.dp)
        )
        Text(
            text = message,
            style = LumaTheme.typography.headlineSm,
            color = LumaTheme.colors.textPrimary,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = subMessage,
            style = LumaTheme.typography.bodySm,
            color = LumaTheme.colors.textSecondary
        )
        Spacer(modifier = Modifier.height(6.dp))

        // Focusable Close Button
        val interactionSource = remember { MutableInteractionSource() }
        val isFocused by interactionSource.collectIsFocusedAsState()

        Box(
            modifier = Modifier
                .height(38.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(if (isFocused) LumaTheme.colors.cyan else LumaTheme.colors.surfaceCard)
                .border(
                    width = if (isFocused) 2.dp else 1.dp,
                    color = if (isFocused) Color.White else LumaTheme.colors.outlineVariant,
                    shape = RoundedCornerShape(8.dp)
                )
                .focusRequester(focusRequester)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onDismiss
                )
                .focusable(interactionSource = interactionSource)
                .padding(horizontal = 24.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Close",
                style = LumaTheme.typography.labelMd,
                color = if (isFocused) Color.Black else LumaTheme.colors.textPrimary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * Top-right close button for the modal window.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun DialogCloseButton(onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(if (isFocused) LumaTheme.colors.cyan else LumaTheme.colors.surfaceCard)
            .border(
                width = if (isFocused) 2.dp else 1.dp,
                color = if (isFocused) Color.White else LumaTheme.colors.outlineVariant.copy(alpha = 0.5f),
                shape = CircleShape
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .focusable(interactionSource = interactionSource),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_action_close),
            contentDescription = "Close Dialog",
            tint = if (isFocused) Color.Black else LumaTheme.colors.textSecondary,
            modifier = Modifier.size(16.dp)
        )
    }
}

/**
 * Tab button for toggling between Audio and Subtitles inside the dialog.
 */
@Composable
private fun TrackTabButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val shape = RoundedCornerShape(8.dp)
    val bgColor = when {
        isFocused -> LumaTheme.colors.surfaceContainerHighest
        isSelected -> LumaTheme.colors.cyan.copy(alpha = 0.18f)
        else -> LumaTheme.colors.surfaceCard
    }
    val borderColor = when {
        isFocused -> LumaTheme.colors.cyan
        isSelected -> LumaTheme.colors.cyan.copy(alpha = 0.6f)
        else -> LumaTheme.colors.outlineVariant.copy(alpha = 0.4f)
    }
    val textColor = when {
        isFocused || isSelected -> LumaTheme.colors.cyan
        else -> LumaTheme.colors.textSecondary
    }

    Box(
        modifier = Modifier
            .height(34.dp)
            .clip(shape)
            .background(bgColor)
            .border(if (isFocused) 2.dp else 1.dp, borderColor, shape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .focusable(interactionSource = interactionSource)
            .padding(horizontal = 14.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = LumaTheme.typography.labelMd.copy(fontWeight = if (isSelected || isFocused) FontWeight.Bold else FontWeight.Medium),
            color = textColor
        )
    }
}
