package com.lumaplay.tv.player

import android.app.Activity
import android.view.KeyEvent
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import com.lumaplay.tv.core.theme.LumaTheme
import com.lumaplay.tv.core.theme.tvSafePadding
import com.lumaplay.tv.core.ui.components.LumaBadge
import com.lumaplay.tv.core.ui.components.LumaBadgeVariant
import com.lumaplay.tv.core.ui.components.LumaButton
import com.lumaplay.tv.core.ui.components.LumaButtonVariant
import com.lumaplay.tv.data.model.VideoItem
import kotlinx.coroutines.delay

/**
 * Fullscreen Media3 ExoPlayer TV Screen.
 *
 * Implements:
 * - Managed ExoPlayer lifecycle (prepared via remember, released via DisposableEffect).
 * - Full TV remote D-pad navigation (Center/Enter toggles Play/Pause, Left/Right seeks 10s).
 * - System BackHandler triggering [onBack].
 * - Auto-hiding 10-foot television OSD adhering to Google Stitch design tokens.
 * - Loading/buffering indicators and error states.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun PlayerScreen(
    video: VideoItem,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val focusRequester = remember { FocusRequester() }

    // Playback state
    var isPlaying by remember { mutableStateOf(false) }
    var currentPositionMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(video.durationMs) }
    var isBuffering by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showControls by remember { mutableStateOf(true) }
    var lastInteractionTime by remember { mutableLongStateOf(System.currentTimeMillis()) }

    // Helper to reveal OSD
    fun triggerControls() {
        showControls = true
        lastInteractionTime = System.currentTimeMillis()
    }

    // Auto-hide controls after 3 seconds while playing
    LaunchedEffect(showControls, isPlaying, lastInteractionTime) {
        if (showControls && isPlaying) {
            delay(3000L)
            showControls = false
        }
    }

    // Single ExoPlayer instance managed across the screen lifecycle
    val player = remember(video.contentUri) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(video.contentUri))
            prepare()
            playWhenReady = true
        }
    }

    // Attach lifecycle listeners and keep screen on
    DisposableEffect(player) {
        val window = (context as? Activity)?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
                if (!playing) {
                    showControls = true
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                isBuffering = playbackState == Player.STATE_BUFFERING
                if (playbackState == Player.STATE_READY) {
                    durationMs = player.duration.coerceAtLeast(0L)
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                errorMessage = error.localizedMessage ?: "Unable to play video."
                isBuffering = false
                showControls = true
            }
        }

        player.addListener(listener)

        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            player.removeListener(listener)
            player.stop()
            player.clearMediaItems()
            player.release()
        }
    }

    // Periodic position polling while playing
    LaunchedEffect(player, isPlaying) {
        while (true) {
            if (player.isPlaying) {
                currentPositionMs = player.currentPosition
                durationMs = player.duration.coerceAtLeast(0L)
            }
            delay(500L)
        }
    }

    // Request remote focus on composition
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    // Handle system Back button
    BackHandler(enabled = true) {
        onBack()
    }

    val playerState = VideoPlayerState(
        isPlaying = isPlaying,
        currentPositionMs = currentPositionMs,
        durationMs = durationMs,
        isBuffering = isBuffering,
        errorMessage = errorMessage,
        showControls = showControls
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(focusRequester)
            .focusable()
            .onPreviewKeyEvent { keyEvent ->
                if (keyEvent.type != KeyEventType.KeyDown) {
                    return@onPreviewKeyEvent false
                }

                when (keyEvent.nativeKeyEvent.keyCode) {
                    KeyEvent.KEYCODE_DPAD_CENTER,
                    KeyEvent.KEYCODE_ENTER,
                    KeyEvent.KEYCODE_NUMPAD_ENTER -> {
                        if (player.isPlaying) player.pause() else player.play()
                        triggerControls()
                        true
                    }

                    KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                        if (player.isPlaying) player.pause() else player.play()
                        triggerControls()
                        true
                    }

                    KeyEvent.KEYCODE_MEDIA_PLAY -> {
                        player.play()
                        triggerControls()
                        true
                    }

                    KeyEvent.KEYCODE_MEDIA_PAUSE -> {
                        player.pause()
                        triggerControls()
                        true
                    }

                    KeyEvent.KEYCODE_DPAD_LEFT,
                    KeyEvent.KEYCODE_MEDIA_REWIND -> {
                        val newPosition = (player.currentPosition - 10_000L).coerceAtLeast(0L)
                        player.seekTo(newPosition)
                        currentPositionMs = newPosition
                        triggerControls()
                        true
                    }

                    KeyEvent.KEYCODE_DPAD_RIGHT,
                    KeyEvent.KEYCODE_MEDIA_FAST_FORWARD -> {
                        val maxDuration = player.duration.coerceAtLeast(0L)
                        val newPosition = (player.currentPosition + 10_000L).coerceAtMost(maxDuration)
                        player.seekTo(newPosition)
                        currentPositionMs = newPosition
                        triggerControls()
                        true
                    }

                    KeyEvent.KEYCODE_BACK -> {
                        onBack()
                        true
                    }

                    else -> {
                        triggerControls()
                        false
                    }
                }
            }
    ) {
        // --- 1. Video Surface ---
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    this.player = player
                    useController = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // --- 2. Buffering Indicator ---
        if (playerState.isBuffering && playerState.errorMessage == null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .clip(LumaTheme.shapes.medium)
                        .background(LumaTheme.colors.surface.copy(alpha = 0.85f))
                        .padding(horizontal = 24.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = "Buffering...",
                        style = LumaTheme.typography.bodyMd,
                        color = LumaTheme.colors.cyan
                    )
                }
            }
        }

        // --- 3. Error Overlay ---
        if (playerState.errorMessage != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.8f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(32.dp)
                ) {
                    Text(
                        text = "Playback Error",
                        style = LumaTheme.typography.headlineMd,
                        color = LumaTheme.colors.error
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = playerState.errorMessage,
                        style = LumaTheme.typography.bodyMd,
                        color = LumaTheme.colors.textSecondary
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    LumaButton(
                        onClick = onBack,
                        variant = LumaButtonVariant.Primary
                    ) {
                        Text(text = "Return to Library")
                    }
                }
            }
        }

        // --- 4. TV On-Screen Display (OSD) Overlay ---
        AnimatedVisibility(
            visible = playerState.showControls && playerState.errorMessage == null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Header Scrim
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color.Black.copy(alpha = 0.85f),
                                    Color.Transparent
                                )
                            )
                        )
                        .tvSafePadding()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f, fill = false)) {
                            Text(
                                text = video.title,
                                style = LumaTheme.typography.headlineMd,
                                color = LumaTheme.colors.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        LumaBadge(
                            text = video.resolutionTag,
                            variant = if (video.resolutionTag == "4K") {
                                LumaBadgeVariant.Highlight
                            } else {
                                LumaBadgeVariant.Standard
                            }
                        )
                    }
                }

                // Bottom Transport Scrim
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.9f)
                                )
                            )
                        )
                        .tvSafePadding()
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Progress Bar Track
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(LumaTheme.colors.surfaceContainerHighest)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(playerState.progress)
                                    .height(6.dp)
                                    .background(LumaTheme.colors.cyan)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Controls Metadata & Time
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Elapsed / Total Duration
                            Text(
                                text = "${playerState.formattedCurrentPosition} / ${playerState.formattedDuration}",
                                style = LumaTheme.typography.labelMd,
                                color = LumaTheme.colors.textPrimary
                            )

                            // Play / Pause Indicator
                            Text(
                                text = if (playerState.isPlaying) "❚❚ Playing" else "▶ Paused",
                                style = LumaTheme.typography.labelMd,
                                color = LumaTheme.colors.cyan
                            )

                            // TV Remote D-Pad Hints
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                LumaBadge(
                                    text = "Center: Play/Pause",
                                    variant = LumaBadgeVariant.Standard
                                )
                                LumaBadge(
                                    text = "◀ / ▶: ±10s",
                                    variant = LumaBadgeVariant.Standard
                                )
                                LumaBadge(
                                    text = "Back: Exit",
                                    variant = LumaBadgeVariant.Standard
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
