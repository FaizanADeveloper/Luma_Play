package com.lumaplay.tv.player

import android.app.Activity
import android.content.Context
import android.graphics.PixelFormat
import android.graphics.Region
import android.graphics.drawable.ColorDrawable
import android.util.Log
import android.view.KeyEvent
import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.ColorInfo
import androidx.media3.common.Format
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.DecoderReuseEvaluation
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import com.lumaplay.tv.R
import com.lumaplay.tv.core.theme.LumaTheme
import com.lumaplay.tv.core.theme.tvSafePadding
import com.lumaplay.tv.core.ui.components.LumaBadge
import com.lumaplay.tv.core.ui.components.LumaBadgeVariant
import com.lumaplay.tv.core.ui.components.LumaButton
import com.lumaplay.tv.core.ui.components.LumaButtonVariant
import com.lumaplay.tv.data.model.VideoItem
import com.lumaplay.tv.player.diagnostics.AspectRatioMode
import com.lumaplay.tv.player.diagnostics.PlaybackSpeed
import com.lumaplay.tv.player.diagnostics.StatsOverlay
import com.lumaplay.tv.player.diagnostics.VideoTelemetryState
import androidx.media3.common.Tracks
import androidx.media3.common.VideoSize
import com.lumaplay.tv.data.util.MediaSpecsExtractor
import androidx.annotation.DrawableRes
import com.lumaplay.tv.player.track.AudioTrackItem
import com.lumaplay.tv.player.track.AudioTrackSelectionDialog
import com.lumaplay.tv.player.track.ExternalSubtitleHelper
import com.lumaplay.tv.player.track.SubtitleTrackItem
import com.lumaplay.tv.player.track.SubtitleTrackSelectionDialog
import com.lumaplay.tv.player.track.TrackHelper
import com.lumaplay.tv.player.seek.DeferredSeekState
import com.lumaplay.tv.player.seek.SeekClampingHelper
import com.lumaplay.tv.player.seek.SeekInterval
import com.lumaplay.tv.player.seek.SeekPreferences
import com.lumaplay.tv.player.seek.ThumbnailPreviewCard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Custom [PlayerView] that explicitly gathers transparent region for its underlying [SurfaceView].
 *
 * Prevents SurfaceFlinger / Hardware Composer from treating the video surface as occluded
 * by parent windows or Compose layouts, enabling native DEVICE overlay composition.
 */
class LumaPlayerView(context: Context) : PlayerView(context) {

    override fun gatherTransparentRegion(region: Region?): Boolean {
        Log.i("LumaTransparent", "gatherTransparentRegion START: region=$region, sv=$videoSurfaceView")
        var opaque = super.gatherTransparentRegion(region)
        Log.i("LumaTransparent", "gatherTransparentRegion after super: region=$region, opaque=$opaque")
        val sv = videoSurfaceView
        if (sv != null && region != null && sv.width > 0 && sv.height > 0) {
            val location = IntArray(2)
            sv.getLocationInWindow(location)
            val l = location[0]
            val t = location[1]
            val r = l + sv.width
            val b = t + sv.height
            region.op(l, t, r, b, Region.Op.UNION)
            Log.i("LumaTransparent", "gatherTransparentRegion UNIONed [$l, $t, $r, $b], region now=$region")
            opaque = false
        }
        Log.i("LumaTransparent", "gatherTransparentRegion END: region=$region, return opaque=$opaque")
        return opaque
    }
}

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
    val lifecycleOwner = LocalLifecycleOwner.current
    val focusRequester = remember { FocusRequester() }
    val playPauseFocusRequester = remember { FocusRequester() }

    // Playback state
    var isPlaying by remember { mutableStateOf(false) }
    var currentPositionMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(video.durationMs) }
    var bufferedPositionMs by remember { mutableLongStateOf(0L) }
    var isBuffering by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showControls by remember { mutableStateOf(true) }
    var lastInteractionTime by remember { mutableLongStateOf(System.currentTimeMillis()) }

    // Phase 5.1: Controls and diagnostics state
    var playbackSpeed by remember { mutableFloatStateOf(PlaybackSpeed.DEFAULT_SPEED) }
    var aspectRatioMode by remember { mutableStateOf(AspectRatioMode.FIT) }
    var showStats by remember { mutableStateOf(false) }

    // Phase 5.2F/G: Configurable seek interval and deferred scrubbing state
    val seekPreferences = remember { SeekPreferences(context) }
    var seekInterval by remember { mutableStateOf(seekPreferences.getSeekInterval()) }
    var deferredSeekState by remember { mutableStateOf(DeferredSeekState.IDLE) }
    val scrubberFocusRequester = remember { FocusRequester() }

    // Real-time telemetry state collected via AnalyticsListener & source metadata fallback
    var videoDecoderName by remember { mutableStateOf<String?>(null) }
    var audioDecoderName by remember { mutableStateOf<String?>(null) }
    var videoWidth by remember { mutableIntStateOf(if (video.width > 0) video.width else 0) }
    var videoHeight by remember { mutableIntStateOf(if (video.height > 0) video.height else 0) }
    var media3FrameRate by remember { mutableFloatStateOf(-1f) }
    var sourceFrameRate by remember { mutableFloatStateOf(-1f) }
    var cumulativeDroppedFrames by remember { mutableIntStateOf(0) }
    var colorInfo by remember { mutableStateOf<ColorInfo?>(null) }
    var audioMimeType by remember { mutableStateOf<String?>(null) }
    var audioChannelCount by remember { mutableIntStateOf(0) }
    var audioSampleRate by remember { mutableIntStateOf(0) }

    val effectiveFrameRate = when {
        media3FrameRate > 0f -> media3FrameRate
        sourceFrameRate > 0f -> sourceFrameRate
        else -> -1f
    }

    // Helper to reveal OSD
    fun triggerControls() {
        showControls = true
        lastInteractionTime = System.currentTimeMillis()
    }

    // Phase 5.2D: Dialog visibility and TV remote focus restoration
    var showAudioDialog by remember { mutableStateOf(false) }
    var showSubtitleDialog by remember { mutableStateOf(false) }
    val audioPillFocusRequester = remember { FocusRequester() }
    val subtitlePillFocusRequester = remember { FocusRequester() }

    // Auto-hide controls after 3 seconds while playing, suspended while stats overlay, track dialogs, or scrubbing is active
    LaunchedEffect(showControls, isPlaying, lastInteractionTime, showStats, showAudioDialog, showSubtitleDialog, deferredSeekState.isScrubbing) {
        if (showControls && isPlaying && !showStats && !showAudioDialog && !showSubtitleDialog && !deferredSeekState.isScrubbing) {
            delay(3000L)
            showControls = false
        }
    }

    // Request remote focus on Play/Pause when controls are revealed (unless scrubbing is active)
    LaunchedEffect(showControls) {
        if (showControls && !deferredSeekState.isScrubbing) {
            delay(50L)
            runCatching { playPauseFocusRequester.requestFocus() }
        }
    }

    // Phase 5.2: Track state observed from Media3 player
    var audioTracks by remember { mutableStateOf<List<AudioTrackItem>>(emptyList()) }
    var subtitleTracks by remember { mutableStateOf<List<SubtitleTrackItem>>(emptyList()) }

    val activeAudioTrack = remember(audioTracks) {
        audioTracks.firstOrNull { it.isSelected }
    }
    val audioPillText = remember(activeAudioTrack) {
        activeAudioTrack?.osdSummary ?: "Audio"
    }

    val activeSubtitleTrack = remember(subtitleTracks) {
        subtitleTracks.firstOrNull { it.isSelected }
    }
    val subtitlePillText = remember(activeSubtitleTrack) {
        activeSubtitleTrack?.osdSummary ?: "Subtitles Off"
    }

    var currentTimeText by remember {
        mutableStateOf(SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date()))
    }
    LaunchedEffect(Unit) {
        while (true) {
            currentTimeText = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
            delay(1000L)
        }
    }

    val isUsbStorage = remember(video.filePath) {
        val path = video.filePath ?: ""
        path.contains("usb", ignoreCase = true) ||
            (path.startsWith("/storage/") && !path.startsWith("/storage/emulated/"))
    }
    val storageLabel = if (isUsbStorage) "USB Storage" else "Internal Media"

    val isHdr = remember(colorInfo) {
        val transfer = colorInfo?.colorTransfer ?: -1
        transfer == C.COLOR_TRANSFER_ST2084 || transfer == C.COLOR_TRANSFER_HLG
    }

    val audioBadgeText = remember(activeAudioTrack, audioMimeType) {
        activeAudioTrack?.shortCodecLabel?.uppercase()
            ?: when {
                audioMimeType?.contains("truehd", ignoreCase = true) == true -> "TRUEHD"
                audioMimeType?.contains("eac3", ignoreCase = true) == true || audioMimeType?.contains("e-ac-3", ignoreCase = true) == true -> "E-AC3"
                audioMimeType?.contains("opus", ignoreCase = true) == true -> "OPUS"
                audioMimeType?.contains("flac", ignoreCase = true) == true -> "FLAC"
                audioMimeType?.contains("aac", ignoreCase = true) == true || audioMimeType?.contains("mp4a", ignoreCase = true) == true -> "AAC"
                else -> "AUDIO"
            }
    }

    LaunchedEffect(audioTracks, subtitleTracks) {
        val activeAudio = audioTracks.firstOrNull { it.isSelected }
        val activeSubtitle = subtitleTracks.firstOrNull { it.isSelected && !it.isOff }
        Log.d(
            "LumaPlayTracks",
            "Observed tracks: ${audioTracks.size} audio (active=${activeAudio?.displayName}), " +
                "${subtitleTracks.size} subtitles (active=${activeSubtitle?.displayName ?: "Off"})"
        )
    }

    // Single ExoPlayer instance managed across the screen lifecycle
    val player = remember(video.contentUri) {
        val renderersFactory = DefaultRenderersFactory(context).apply {
            setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_OFF)
            setEnableDecoderFallback(true)
            forceEnableMediaCodecAsynchronousQueueing()
        }
        val audioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
            .setUsage(C.USAGE_MEDIA)
            .build()

        val subtitleConfigs = ExternalSubtitleHelper.discoverSubtitleConfigurations(video)
        val mediaItem = MediaItem.Builder()
            .setUri(video.contentUri)
            .setSubtitleConfigurations(subtitleConfigs)
            .build()

        ExoPlayer.Builder(context, renderersFactory)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .build().apply {
                setMediaItem(mediaItem)
                prepare()
                playWhenReady = true
            }
    }

    // AnalyticsListener to capture real-time playback telemetry
    val analyticsListener = remember {
        object : AnalyticsListener {
            override fun onVideoDecoderInitialized(
                eventTime: AnalyticsListener.EventTime,
                decoderName: String,
                initializedTimestampMs: Long,
                initializationDurationMs: Long
            ) {
                videoDecoderName = decoderName
                Log.i("LumaPlayPlayback", "Video decoder initialized: $decoderName in ${initializationDurationMs}ms")
            }

            override fun onAudioDecoderInitialized(
                eventTime: AnalyticsListener.EventTime,
                decoderName: String,
                initializedTimestampMs: Long,
                initializationDurationMs: Long
            ) {
                audioDecoderName = decoderName
                Log.i("LumaPlayPlayback", "Audio decoder initialized: $decoderName in ${initializationDurationMs}ms")
            }

            override fun onVideoInputFormatChanged(
                eventTime: AnalyticsListener.EventTime,
                format: Format,
                decoderReuseEvaluation: DecoderReuseEvaluation?
            ) {
                if (format.width > 0 && format.height > 0) {
                    videoWidth = format.width
                    videoHeight = format.height
                }
                if (format.frameRate > 0f) {
                    media3FrameRate = format.frameRate
                }
                if (format.colorInfo != null) {
                    colorInfo = format.colorInfo
                }
                Log.i(
                    "LumaPlayPlayback",
                    "Video input format: ${format.sampleMimeType}, ${format.width}x${format.height}@${format.frameRate}fps, colorInfo=${format.colorInfo}"
                )
            }

            override fun onAudioInputFormatChanged(
                eventTime: AnalyticsListener.EventTime,
                format: Format,
                decoderReuseEvaluation: DecoderReuseEvaluation?
            ) {
                audioMimeType = format.sampleMimeType
                audioChannelCount = format.channelCount
                audioSampleRate = format.sampleRate
                Log.i(
                    "LumaPlayPlayback",
                    "Audio input format: ${format.sampleMimeType}, channels=${format.channelCount}, rate=${format.sampleRate}"
                )
            }

            override fun onDroppedVideoFrames(
                eventTime: AnalyticsListener.EventTime,
                droppedFrames: Int,
                elapsedMs: Long
            ) {
                cumulativeDroppedFrames += droppedFrames
                Log.w("LumaPlayPlayback", "Dropped $droppedFrames video frames (cumulative: $cumulativeDroppedFrames) in ${elapsedMs}ms")
            }

            override fun onRenderedFirstFrame(
                eventTime: AnalyticsListener.EventTime,
                output: Any,
                renderTimeMs: Long
            ) {
                Log.i("LumaPlayPlayback", "Rendered first frame in ${renderTimeMs}ms")
                (context as? Activity)?.window?.decorView?.let { dv ->
                    dv.post {
                        dv.parent?.requestTransparentRegion(dv)
                    }
                }
            }
        }
    }

    // Attach lifecycle listeners and keep screen on
    DisposableEffect(player, lifecycleOwner) {
        val activity = context as? Activity
        val window = activity?.window
        val decorView = window?.decorView
        val originalBackground = decorView?.background
        val originalFormat = window?.attributes?.format ?: PixelFormat.OPAQUE

        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window?.setFormat(PixelFormat.TRANSLUCENT)
        decorView?.background = null
        window?.setBackgroundDrawable(ColorDrawable(android.graphics.Color.TRANSPARENT))
        decorView?.parent?.requestTransparentRegion(decorView)

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
                    decorView?.post {
                        decorView.parent?.requestTransparentRegion(decorView)
                        logViewTree(decorView)
                    }
                }
            }

            override fun onVideoSizeChanged(videoSize: VideoSize) {
                if (videoSize.width > 0 && videoSize.height > 0) {
                    videoWidth = videoSize.width
                    videoHeight = videoSize.height
                }
            }

            override fun onTracksChanged(tracks: Tracks) {
                // Update reactive audio & subtitle track collections from authoritative Media3 state
                audioTracks = TrackHelper.parseAudioTracks(tracks)
                subtitleTracks = TrackHelper.parseSubtitleTracks(tracks, includeOffOption = true)

                if (media3FrameRate <= 0f) {
                    val playerFps = player.videoFormat?.frameRate
                    if (playerFps != null && playerFps > 0f) {
                        media3FrameRate = playerFps
                    } else {
                        for (group in tracks.groups) {
                            if (group.type == C.TRACK_TYPE_VIDEO) {
                                val mediaTrackGroup = group.mediaTrackGroup
                                for (i in 0 until mediaTrackGroup.length) {
                                    val trackFormat = mediaTrackGroup.getFormat(i)
                                    if (trackFormat.frameRate > 0f) {
                                        media3FrameRate = trackFormat.frameRate
                                        break
                                    }
                                }
                            }
                        }
                    }
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                errorMessage = error.localizedMessage ?: "Unable to play video."
                isBuffering = false
                showControls = true
            }
        }

        player.addListener(listener)
        player.addAnalyticsListener(analyticsListener)

        val lifecycleObserver = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE,
                Lifecycle.Event.ON_STOP -> {
                    player.pause()
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(lifecycleObserver)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(lifecycleObserver)
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            decorView?.background = originalBackground
            window?.setBackgroundDrawable(originalBackground)
            window?.setFormat(originalFormat)
            decorView?.parent?.requestTransparentRegion(decorView)
            player.removeListener(listener)
            player.removeAnalyticsListener(analyticsListener)
            player.stop()
            player.clearMediaItems()
            player.release()
        }
    }

    // Periodic position and buffer polling while playing
    LaunchedEffect(player, isPlaying) {
        while (true) {
            if (player.isPlaying) {
                currentPositionMs = player.currentPosition
                durationMs = player.duration.coerceAtLeast(0L)
                bufferedPositionMs = player.bufferedPosition
            }
            delay(500L)
        }
    }

    // Request remote focus on composition
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    // Asynchronously fetch authentic container source metadata as fallback for containers (like WebM/MKV)
    // where Media3 decoders/extractors do not populate Format.frameRate.
    LaunchedEffect(video.contentUri) {
        val specs = withContext(Dispatchers.IO) {
            MediaSpecsExtractor.extractSpecs(context, video)
        }
        val fps = specs.frameRate
        if (fps != null && fps > 0f) {
            sourceFrameRate = fps
        }
    }

    // Handle system Back button: dismiss scrubbing, track dialogs or stats overlay first if open
    BackHandler(enabled = true) {
        when {
            deferredSeekState.isScrubbing -> {
                deferredSeekState = DeferredSeekState.IDLE
                triggerControls()
                runCatching { playPauseFocusRequester.requestFocus() }
            }
            showAudioDialog -> {
                showAudioDialog = false
                triggerControls()
                runCatching { audioPillFocusRequester.requestFocus() }
            }
            showSubtitleDialog -> {
                showSubtitleDialog = false
                triggerControls()
                runCatching { subtitlePillFocusRequester.requestFocus() }
            }
            showStats -> {
                showStats = false
                triggerControls()
            }
            else -> {
                onBack()
            }
        }
    }

    val telemetry = VideoTelemetryState(
        videoDecoderName = videoDecoderName,
        audioDecoderName = audioDecoderName,
        videoWidth = videoWidth,
        videoHeight = videoHeight,
        frameRate = effectiveFrameRate,
        droppedFrames = cumulativeDroppedFrames,
        colorInfo = colorInfo,
        audioMimeType = audioMimeType,
        audioChannelCount = audioChannelCount,
        audioSampleRate = audioSampleRate,
        bufferedDurationMs = (bufferedPositionMs - currentPositionMs).coerceAtLeast(0L),
        totalDurationMs = durationMs
    )

    val playerState = VideoPlayerState(
        isPlaying = isPlaying,
        currentPositionMs = currentPositionMs,
        durationMs = durationMs,
        bufferedPositionMs = bufferedPositionMs,
        isBuffering = isBuffering,
        errorMessage = errorMessage,
        showControls = showControls,
        playbackSpeed = playbackSpeed,
        aspectRatioMode = aspectRatioMode,
        showStats = showStats,
        telemetry = telemetry,
        seekInterval = seekInterval,
        deferredSeekState = deferredSeekState
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .focusRequester(focusRequester)
            .focusable()
            .onPreviewKeyEvent { keyEvent ->
                if (keyEvent.type != KeyEventType.KeyDown) {
                    return@onPreviewKeyEvent false
                }

                val keyCode = keyEvent.nativeKeyEvent.keyCode

                if (!showControls) {
                    when (keyCode) {
                        KeyEvent.KEYCODE_BACK -> {
                            when {
                                deferredSeekState.isScrubbing -> {
                                    deferredSeekState = DeferredSeekState.IDLE
                                    triggerControls()
                                    runCatching { playPauseFocusRequester.requestFocus() }
                                    true
                                }
                                showAudioDialog -> {
                                    showAudioDialog = false
                                    triggerControls()
                                    runCatching { audioPillFocusRequester.requestFocus() }
                                    true
                                }
                                showSubtitleDialog -> {
                                    showSubtitleDialog = false
                                    triggerControls()
                                    runCatching { subtitlePillFocusRequester.requestFocus() }
                                    true
                                }
                                showStats -> {
                                    showStats = false
                                    triggerControls()
                                    true
                                }
                                else -> {
                                    onBack()
                                    true
                                }
                            }
                        }
                        KeyEvent.KEYCODE_INFO -> {
                            showStats = true
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
                        KeyEvent.KEYCODE_DPAD_CENTER,
                        KeyEvent.KEYCODE_ENTER,
                        KeyEvent.KEYCODE_NUMPAD_ENTER -> {
                            if (player.isPlaying) player.pause() else player.play()
                            triggerControls()
                            true
                        }
                        KeyEvent.KEYCODE_DPAD_LEFT -> {
                            val dur = player.duration.coerceAtLeast(0L)
                            deferredSeekState = DeferredSeekState.startScrubbing(player.currentPosition, dur)
                                .stepBackward(seekInterval.millis)
                            triggerControls()
                            runCatching { scrubberFocusRequester.requestFocus() }
                            true
                        }
                        KeyEvent.KEYCODE_DPAD_RIGHT -> {
                            val dur = player.duration.coerceAtLeast(0L)
                            deferredSeekState = DeferredSeekState.startScrubbing(player.currentPosition, dur)
                                .stepForward(seekInterval.millis)
                            triggerControls()
                            runCatching { scrubberFocusRequester.requestFocus() }
                            true
                        }
                        KeyEvent.KEYCODE_MEDIA_REWIND -> {
                            val dur = player.duration.coerceAtLeast(0L)
                            val newPosition = SeekClampingHelper.calculateBackward(player.currentPosition, seekInterval.millis, dur)
                            player.seekTo(newPosition)
                            currentPositionMs = newPosition
                            triggerControls()
                            true
                        }
                        KeyEvent.KEYCODE_MEDIA_FAST_FORWARD -> {
                            val dur = player.duration.coerceAtLeast(0L)
                            val newPosition = SeekClampingHelper.calculateForward(player.currentPosition, seekInterval.millis, dur)
                            player.seekTo(newPosition)
                            currentPositionMs = newPosition
                            triggerControls()
                            true
                        }
                        else -> {
                            triggerControls()
                            false
                        }
                    }
                } else {
                    when (keyCode) {
                        KeyEvent.KEYCODE_BACK -> {
                            when {
                                deferredSeekState.isScrubbing -> {
                                    deferredSeekState = DeferredSeekState.IDLE
                                    triggerControls()
                                    runCatching { playPauseFocusRequester.requestFocus() }
                                    true
                                }
                                showAudioDialog -> {
                                    showAudioDialog = false
                                    triggerControls()
                                    runCatching { audioPillFocusRequester.requestFocus() }
                                    true
                                }
                                showSubtitleDialog -> {
                                    showSubtitleDialog = false
                                    triggerControls()
                                    runCatching { subtitlePillFocusRequester.requestFocus() }
                                    true
                                }
                                showStats -> {
                                    showStats = false
                                    triggerControls()
                                    true
                                }
                                else -> {
                                    onBack()
                                    true
                                }
                            }
                        }
                        KeyEvent.KEYCODE_INFO -> {
                            showStats = !showStats
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
                        KeyEvent.KEYCODE_MEDIA_REWIND -> {
                            val dur = player.duration.coerceAtLeast(0L)
                            val newPosition = SeekClampingHelper.calculateBackward(player.currentPosition, seekInterval.millis, dur)
                            player.seekTo(newPosition)
                            currentPositionMs = newPosition
                            triggerControls()
                            true
                        }
                        KeyEvent.KEYCODE_MEDIA_FAST_FORWARD -> {
                            val dur = player.duration.coerceAtLeast(0L)
                            val newPosition = SeekClampingHelper.calculateForward(player.currentPosition, seekInterval.millis, dur)
                            player.seekTo(newPosition)
                            currentPositionMs = newPosition
                            triggerControls()
                            true
                        }
                        else -> {
                            triggerControls()
                            false
                        }
                    }
                }
            }
    ) {
        // --- 1. Video Surface ---
        AndroidView(
            factory = { ctx ->
                LumaPlayerView(ctx).apply {
                    this.player = player
                    useController = false
                    resizeMode = aspectRatioMode.resizeMode
                    layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
                    setShutterBackgroundColor(android.graphics.Color.TRANSPARENT)
                    findViewById<View>(androidx.media3.ui.R.id.exo_shutter)?.let { shutter ->
                        shutter.visibility = View.GONE
                        (shutter.parent as? ViewGroup)?.removeView(shutter)
                    }
                    videoSurfaceView?.addOnLayoutChangeListener { v, left, top, right, bottom, _, _, _, _ ->
                        if (right > left && bottom > top) {
                            v.post {
                                v.parent?.requestTransparentRegion(v)
                                (ctx as? Activity)?.window?.decorView?.let { dv ->
                                    dv.parent?.requestTransparentRegion(dv)
                                }
                            }
                        }
                    }
                    post {
                        (ctx as? Activity)?.window?.decorView?.let { dv ->
                            dv.parent?.requestTransparentRegion(dv)
                        }
                    }
                }
            },
            update = { playerView ->
                playerView.player = player
                playerView.resizeMode = aspectRatioMode.resizeMode
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
                        // Left: Circular Back button + Title
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.weight(1f, fill = false)
                        ) {
                            PlayerTransportButton(
                                onClick = {
                                    if (showStats) {
                                        showStats = false
                                        triggerControls()
                                    } else {
                                        onBack()
                                    }
                                }
                            ) { isFocused ->
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_action_arrow_back),
                                    contentDescription = "Back",
                                    tint = if (isFocused) LumaTheme.colors.cyan else LumaTheme.colors.textPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Column(
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = video.title,
                                    style = LumaTheme.typography.headlineMd.copy(fontWeight = FontWeight.Bold),
                                    color = LumaTheme.colors.textPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        // Right: Technical Format Badges + Source Indicator + Live Clock
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Technical Format Badges Cluster
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(LumaTheme.colors.surfaceContainerLowest.copy(alpha = 0.8f))
                                    .padding(horizontal = 8.dp, vertical = 5.dp)
                            ) {
                                if (isHdr) {
                                    Text(
                                        text = "HDR10",
                                        style = LumaTheme.typography.labelSm.copy(
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 0.8.sp
                                        ),
                                        color = Color(0xFFFCD34D),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(Color(0xFFFCD34D).copy(alpha = 0.2f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Text(
                                    text = video.resolutionTag,
                                    style = LumaTheme.typography.labelSm.copy(
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.5.sp
                                    ),
                                    color = LumaTheme.colors.textSecondary,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(LumaTheme.colors.surfaceContainerHigh)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                                Text(
                                    text = audioBadgeText,
                                    style = LumaTheme.typography.labelSm.copy(
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.8.sp
                                    ),
                                    color = LumaTheme.colors.cyan,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(LumaTheme.colors.cyan.copy(alpha = 0.2f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }

                            // Storage Source Indicator
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(LumaTheme.colors.surfaceContainerHigh.copy(alpha = 0.8f))
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_action_usb),
                                    contentDescription = null,
                                    tint = LumaTheme.colors.cyan,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = storageLabel,
                                    style = LumaTheme.typography.labelSm.copy(fontWeight = FontWeight.Medium),
                                    color = LumaTheme.colors.textPrimary
                                )
                            }

                            // Live Clock Pill
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(LumaTheme.colors.surfaceContainerHigh.copy(alpha = 0.8f))
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = currentTimeText,
                                    style = LumaTheme.typography.labelMd.copy(fontWeight = FontWeight.SemiBold),
                                    color = LumaTheme.colors.textPrimary
                                )
                            }
                        }
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
                                    Color.Black.copy(alpha = 0.95f)
                                )
                            )
                        )
                        .tvSafePadding()
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Timeline Scrubber Section (Stage B)
                        val scrubberInteractionSource = remember { MutableInteractionSource() }
                        val isScrubberFocused by scrubberInteractionSource.collectIsFocusedAsState()

                        LaunchedEffect(isScrubberFocused) {
                            if (isScrubberFocused && !deferredSeekState.isScrubbing) {
                                deferredSeekState = DeferredSeekState.startScrubbing(
                                    currentPositionMs,
                                    durationMs
                                )
                            }
                        }

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // Timestamps directly above scrubber
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = playerState.formattedCurrentPosition,
                                    style = LumaTheme.typography.labelMd.copy(
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.5.sp
                                    ),
                                    color = LumaTheme.colors.cyan
                                )

                                Text(
                                    text = playerState.formattedDuration,
                                    style = LumaTheme.typography.labelMd.copy(
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                        letterSpacing = 0.5.sp
                                    ),
                                    color = LumaTheme.colors.textSecondary
                                )
                            }

                            // Scrubber Track with Illuminated Thumb and TV Remote Focus
                            BoxWithConstraints(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(24.dp)
                                    .focusRequester(scrubberFocusRequester)
                                    .focusable(interactionSource = scrubberInteractionSource)
                                    .onPreviewKeyEvent { keyEvent ->
                                        if (keyEvent.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                                        when (keyEvent.nativeKeyEvent.keyCode) {
                                            KeyEvent.KEYCODE_DPAD_LEFT -> {
                                                deferredSeekState = deferredSeekState.stepBackward(seekInterval.millis)
                                                triggerControls()
                                                true
                                            }
                                            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                                                deferredSeekState = deferredSeekState.stepForward(seekInterval.millis)
                                                triggerControls()
                                                true
                                            }
                                            KeyEvent.KEYCODE_DPAD_CENTER,
                                            KeyEvent.KEYCODE_ENTER,
                                            KeyEvent.KEYCODE_NUMPAD_ENTER -> {
                                                if (deferredSeekState.isScrubbing) {
                                                    val target = deferredSeekState.previewPositionMs
                                                    player.seekTo(target)
                                                    currentPositionMs = target
                                                    deferredSeekState = DeferredSeekState.IDLE
                                                    triggerControls()
                                                    runCatching { playPauseFocusRequester.requestFocus() }
                                                    true
                                                } else {
                                                    false
                                                }
                                            }
                                            KeyEvent.KEYCODE_BACK -> {
                                                if (deferredSeekState.isScrubbing) {
                                                    deferredSeekState = DeferredSeekState.IDLE
                                                    triggerControls()
                                                    runCatching { playPauseFocusRequester.requestFocus() }
                                                    true
                                                } else {
                                                    false
                                                }
                                            }
                                            KeyEvent.KEYCODE_DPAD_DOWN -> {
                                                deferredSeekState = DeferredSeekState.IDLE
                                                triggerControls()
                                                runCatching { playPauseFocusRequester.requestFocus() }
                                                true
                                            }
                                            else -> false
                                        }
                                    },
                                contentAlignment = Alignment.CenterStart
                            ) {
                                val progress = playerState.progress.coerceIn(0f, 1f)
                                val thumbOffset = ((maxWidth - 16.dp) * progress).coerceAtLeast(0.dp)

                                // Floating Preview Thumbnail Card above the thumb (Stage B)
                                if (deferredSeekState.isScrubbing) {
                                    val cardWidth = 200.dp
                                    val rawOffset = ((maxWidth - 16.dp) * progress) - (cardWidth / 2) + 8.dp
                                    val clampedCardOffset = rawOffset.coerceIn(0.dp, (maxWidth - cardWidth).coerceAtLeast(0.dp))

                                    ThumbnailPreviewCard(
                                        videoUri = video.contentUri,
                                        previewPositionMs = deferredSeekState.previewPositionMs,
                                        formattedPosition = deferredSeekState.formattedPreviewPosition,
                                        modifier = Modifier
                                            .offset(x = clampedCardOffset, y = (-140).dp)
                                    )
                                }

                                // Background trough
                                val trackHeight = if (isScrubberFocused || deferredSeekState.isScrubbing) 8.dp else 6.dp
                                val trackShape = RoundedCornerShape(trackHeight / 2)
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(trackHeight)
                                        .clip(trackShape)
                                        .background(LumaTheme.colors.surfaceContainerHighest.copy(alpha = 0.8f))
                                        .then(
                                            if (isScrubberFocused || deferredSeekState.isScrubbing) {
                                                Modifier.border(1.dp, LumaTheme.colors.cyan.copy(alpha = 0.6f), trackShape)
                                            } else {
                                                Modifier
                                            }
                                        )
                                ) {
                                    // Buffered progress
                                    if (playerState.bufferedProgress > 0f) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth(playerState.bufferedProgress.coerceIn(0f, 1f))
                                                .fillMaxHeight()
                                                .background(LumaTheme.colors.surfaceCard.copy(alpha = 0.8f))
                                        )
                                    }
                                    // Elapsed progress
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(progress)
                                            .fillMaxHeight()
                                            .background(LumaTheme.colors.cyan)
                                    )
                                }

                                // Illuminated thumb head (16dp normal, 20dp when focused/scrubbing)
                                val isThumbActive = isScrubberFocused || deferredSeekState.isScrubbing
                                val thumbSize = if (isThumbActive) 20.dp else 16.dp
                                val thumbDotSize = if (isThumbActive) 8.dp else 6.dp
                                Box(
                                    modifier = Modifier
                                        .offset(x = thumbOffset)
                                        .size(thumbSize)
                                        .cyanFocusGlow(
                                            isFocused = isThumbActive,
                                            shape = CircleShape,
                                            glowColor = LumaTheme.colors.cyan,
                                            glowRadius = 10.dp
                                        )
                                        .background(LumaTheme.colors.cyan, CircleShape)
                                        .border(
                                            if (isThumbActive) 2.dp else 1.dp,
                                            Color.White,
                                            CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(thumbDotSize)
                                            .background(Color(0xFF080E1D), CircleShape)
                                    )
                                }
                            }
                        }

                        // Playback Remote Control Row (matching Stitch full_screen_video_player_with_osd)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Left: Track Selection Pills (Audio & Subtitles)
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Audio Track Pill
                                PlayerPillButton(
                                    onClick = {
                                        showAudioDialog = true
                                        showSubtitleDialog = false
                                        triggerControls()
                                    },
                                    text = audioPillText,
                                    iconRes = R.drawable.ic_action_volume_up,
                                    isActive = showAudioDialog,
                                    focusRequester = audioPillFocusRequester,
                                    modifier = Modifier.widthIn(max = 140.dp)
                                )

                                // Subtitle Track Pill
                                val isSubtitleActive = activeSubtitleTrack != null && !activeSubtitleTrack.isOff
                                PlayerPillButton(
                                    onClick = {
                                        showSubtitleDialog = true
                                        showAudioDialog = false
                                        triggerControls()
                                    },
                                    text = subtitlePillText,
                                    iconRes = if (isSubtitleActive) R.drawable.ic_action_subtitles else R.drawable.ic_action_subtitles_off,
                                    isActive = showSubtitleDialog || isSubtitleActive,
                                    focusRequester = subtitlePillFocusRequester,
                                    modifier = Modifier.widthIn(max = 130.dp)
                                )
                            }

                            // Center: Primary Transport Controls (5 buttons, Remote Snap Zone)
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Previous
                                PlayerTransportButton(
                                    onClick = {
                                        val newPos = 0L
                                        player.seekTo(newPos)
                                        currentPositionMs = newPos
                                        triggerControls()
                                    }
                                ) { isFocused ->
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_action_skip_previous),
                                        contentDescription = "Previous",
                                        tint = if (isFocused) LumaTheme.colors.cyan else LumaTheme.colors.textPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                // Rewind
                                PlayerTransportButton(
                                    onClick = {
                                        val newPos = SeekClampingHelper.calculateBackward(
                                            player.currentPosition,
                                            seekInterval.millis,
                                            durationMs
                                        )
                                        player.seekTo(newPos)
                                        currentPositionMs = newPos
                                        triggerControls()
                                    }
                                ) { isFocused ->
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_action_replay_10),
                                        contentDescription = "Rewind ${seekInterval.label}",
                                        tint = if (isFocused) LumaTheme.colors.cyan else LumaTheme.colors.textPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                // Main Play / Pause Button (Focused 10-foot TV State)
                                PlayerTransportButton(
                                    onClick = {
                                        if (player.isPlaying) player.pause() else player.play()
                                        triggerControls()
                                    },
                                    isPrimary = true,
                                    focusRequester = playPauseFocusRequester,
                                    modifier = Modifier.focusProperties { up = scrubberFocusRequester }
                                ) { isFocused ->
                                    Icon(
                                        painter = painterResource(
                                            id = if (playerState.isPlaying) R.drawable.ic_action_pause else R.drawable.ic_action_play
                                        ),
                                        contentDescription = if (playerState.isPlaying) "Pause" else "Play",
                                        tint = Color(0xFF001E2C),
                                        modifier = Modifier.size(28.dp)
                                    )
                                }

                                // Fast Forward
                                PlayerTransportButton(
                                    onClick = {
                                        val newPos = SeekClampingHelper.calculateForward(
                                            player.currentPosition,
                                            seekInterval.millis,
                                            durationMs
                                        )
                                        player.seekTo(newPos)
                                        currentPositionMs = newPos
                                        triggerControls()
                                    }
                                ) { isFocused ->
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_action_forward_10),
                                        contentDescription = "Forward ${seekInterval.label}",
                                        tint = if (isFocused) LumaTheme.colors.cyan else LumaTheme.colors.textPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                // Next
                                PlayerTransportButton(
                                    onClick = {
                                        val maxDur = player.duration.coerceAtLeast(0L)
                                        val newPos = (player.currentPosition + 30_000L).coerceAtMost(maxDur)
                                        player.seekTo(newPos)
                                        currentPositionMs = newPos
                                        triggerControls()
                                    }
                                ) { isFocused ->
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_action_skip_next),
                                        contentDescription = "Next",
                                        tint = if (isFocused) LumaTheme.colors.cyan else LumaTheme.colors.textPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            // Right: Player Adjustments & Active Stats Toggle
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Skip interval cycle (Stage A)
                                PlayerPillButton(
                                    onClick = {
                                        val next = seekInterval.next()
                                        seekInterval = next
                                        seekPreferences.setSeekInterval(next)
                                        triggerControls()
                                    },
                                    text = "Skip: ${seekInterval.label}"
                                )

                                // Speed cycle
                                PlayerPillButton(
                                    onClick = {
                                        val next = PlaybackSpeed.nextSpeed(playbackSpeed)
                                        playbackSpeed = next
                                        player.setPlaybackSpeed(next)
                                        triggerControls()
                                    },
                                    text = PlaybackSpeed.formatSpeed(playbackSpeed)
                                )

                                // Aspect ratio cycle
                                PlayerPillButton(
                                    onClick = {
                                        aspectRatioMode = aspectRatioMode.next()
                                        triggerControls()
                                    },
                                    text = aspectRatioMode.label,
                                    iconRes = R.drawable.ic_action_aspect_ratio
                                )

                                // Stats for Nerds toggle
                                PlayerPillButton(
                                    onClick = {
                                        showStats = !showStats
                                        triggerControls()
                                    },
                                    text = "Stats",
                                    iconRes = R.drawable.ic_action_analytics,
                                    isActive = showStats
                                )
                            }
                        }

                        // TV Remote D-Pad Navigation Hint Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Left status dot + Direct playback label
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF34D399))
                                )
                                val statusText = if (audioDecoderName?.contains("passthrough", ignoreCase = true) == true) {
                                    "DIRECT BITSTREAM AUDIO ACTIVE"
                                } else {
                                    "DIRECT PLAY • HARDWARE ACCELERATED"
                                }
                                Text(
                                    text = statusText,
                                    style = LumaTheme.typography.labelSm.copy(
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.8.sp
                                    ),
                                    color = LumaTheme.colors.textPrimary
                                )
                            }

                            // Right key hints pill
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(LumaTheme.colors.surfaceContainerLowest.copy(alpha = 0.6f))
                                    .padding(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "◄ / ►",
                                        style = LumaTheme.typography.labelSm.copy(fontWeight = FontWeight.Bold),
                                        color = LumaTheme.colors.textPrimary
                                    )
                                    Text(
                                        text = "Seek ${seekInterval.label}",
                                        style = LumaTheme.typography.labelSm,
                                        color = LumaTheme.colors.textSecondary
                                    )
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "OK",
                                        style = LumaTheme.typography.labelSm.copy(fontWeight = FontWeight.Bold),
                                        color = LumaTheme.colors.textPrimary
                                    )
                                    Text(
                                        text = "Pause/Resume",
                                        style = LumaTheme.typography.labelSm,
                                        color = LumaTheme.colors.textSecondary
                                    )
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "▲ / ▼",
                                        style = LumaTheme.typography.labelSm.copy(fontWeight = FontWeight.Bold),
                                        color = LumaTheme.colors.textPrimary
                                    )
                                    Text(
                                        text = "Track Selection",
                                        style = LumaTheme.typography.labelSm,
                                        color = LumaTheme.colors.textSecondary
                                    )
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "INFO",
                                        style = LumaTheme.typography.labelSm.copy(fontWeight = FontWeight.Bold),
                                        color = LumaTheme.colors.cyan
                                    )
                                    Text(
                                        text = "Toggle Specs",
                                        style = LumaTheme.typography.labelSm,
                                        color = LumaTheme.colors.textSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- 5. Floating Technical Stats Overlay ---
        StatsOverlay(
            isVisible = showStats && errorMessage == null,
            telemetry = telemetry,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .tvSafePadding()
                .padding(top = 72.dp, end = 24.dp)
        )

        // --- 6. Audio Track Selection Dialog ---
        AudioTrackSelectionDialog(
            isVisible = showAudioDialog,
            tracks = audioTracks,
            onTrackSelected = { track ->
                player.trackSelectionParameters = TrackHelper.selectAudioTrack(player.trackSelectionParameters, track)
            },
            onDismiss = {
                showAudioDialog = false
                triggerControls()
            },
            returnFocusRequester = audioPillFocusRequester
        )

        // --- 7. Subtitle Track Selection Dialog ---
        SubtitleTrackSelectionDialog(
            isVisible = showSubtitleDialog,
            tracks = subtitleTracks,
            onTrackSelected = { track ->
                player.trackSelectionParameters = TrackHelper.selectSubtitleTrack(player.trackSelectionParameters, track)
            },
            onDismiss = {
                showSubtitleDialog = false
                triggerControls()
            },
            returnFocusRequester = subtitlePillFocusRequester
        )
    }
}

private fun Modifier.cyanFocusGlow(
    isFocused: Boolean,
    shape: Shape,
    glowColor: Color = Color(0xFF7BD0FF),
    glowRadius: Dp = 10.dp
): Modifier = if (isFocused) {
    this.drawBehind {
        val stroke1 = 2.dp.toPx()
        val stroke2 = 5.dp.toPx()
        val stroke3 = glowRadius.toPx()
        when (shape) {
            CircleShape -> {
                val r = size.minDimension / 2f
                drawCircle(color = glowColor.copy(alpha = 0.12f), radius = r + stroke3)
                drawCircle(color = glowColor.copy(alpha = 0.24f), radius = r + stroke2)
                drawCircle(color = glowColor.copy(alpha = 0.40f), radius = r + stroke1)
            }
            is RoundedCornerShape -> {
                val baseRadius = 8.dp.toPx()
                drawRoundRect(
                    color = glowColor.copy(alpha = 0.10f),
                    topLeft = Offset(-stroke3, -stroke3),
                    size = Size(size.width + stroke3 * 2, size.height + stroke3 * 2),
                    cornerRadius = CornerRadius(baseRadius + stroke3, baseRadius + stroke3)
                )
                drawRoundRect(
                    color = glowColor.copy(alpha = 0.20f),
                    topLeft = Offset(-stroke2, -stroke2),
                    size = Size(size.width + stroke2 * 2, size.height + stroke2 * 2),
                    cornerRadius = CornerRadius(baseRadius + stroke2, baseRadius + stroke2)
                )
                drawRoundRect(
                    color = glowColor.copy(alpha = 0.35f),
                    topLeft = Offset(-stroke1, -stroke1),
                    size = Size(size.width + stroke1 * 2, size.height + stroke1 * 2),
                    cornerRadius = CornerRadius(baseRadius + stroke1, baseRadius + stroke1)
                )
            }
            else -> {
                drawRect(
                    color = glowColor.copy(alpha = 0.20f),
                    topLeft = Offset(-stroke2, -stroke2),
                    size = Size(size.width + stroke2 * 2, size.height + stroke2 * 2)
                )
            }
        }
    }
} else {
    this
}

@Composable
private fun PlayerTransportButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isPrimary: Boolean = false,
    focusRequester: FocusRequester? = null,
    content: @Composable (isFocused: Boolean) -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isFocused) (if (isPrimary) 1.10f else 1.06f) else 1.0f,
        animationSpec = tween(150),
        label = "button_scale"
    )

    val shape = CircleShape
    val size = if (isPrimary) 56.dp else 40.dp
    val bgColor = when {
        isPrimary -> if (isFocused) LumaTheme.colors.cyan else LumaTheme.colors.cyan.copy(alpha = 0.9f)
        isFocused -> LumaTheme.colors.surfaceContainerHighest
        else -> LumaTheme.colors.surfaceContainerHigh.copy(alpha = 0.8f)
    }
    val borderColor = when {
        isFocused && isPrimary -> Color.White
        isFocused -> LumaTheme.colors.cyan
        isPrimary -> LumaTheme.colors.cyan
        else -> LumaTheme.colors.outlineVariant.copy(alpha = 0.4f)
    }

    Box(
        modifier = modifier
            .size(size)
            .scale(scale)
            .cyanFocusGlow(
                isFocused = isFocused,
                shape = shape,
                glowColor = LumaTheme.colors.cyan,
                glowRadius = if (isPrimary) 14.dp else 9.dp
            )
            .background(bgColor, shape)
            .border(if (isFocused) 2.dp else 1.dp, borderColor, shape)
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .focusable(interactionSource = interactionSource),
        contentAlignment = Alignment.Center
    ) {
        content(isFocused)
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun PlayerPillButton(
    onClick: () -> Unit,
    text: String,
    modifier: Modifier = Modifier,
    isActive: Boolean = false,
    @DrawableRes iconRes: Int? = null,
    focusRequester: FocusRequester? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.05f else 1.0f,
        animationSpec = tween(150),
        label = "pill_scale"
    )

    val shape = RoundedCornerShape(8.dp)
    val bgColor = when {
        isFocused -> LumaTheme.colors.surfaceContainerHighest
        isActive -> LumaTheme.colors.cyan.copy(alpha = 0.2f)
        else -> LumaTheme.colors.surfaceContainerHigh.copy(alpha = 0.75f)
    }
    val borderColor = when {
        isFocused -> LumaTheme.colors.cyan
        isActive -> LumaTheme.colors.cyan.copy(alpha = 0.6f)
        else -> LumaTheme.colors.outlineVariant.copy(alpha = 0.35f)
    }
    val contentColor = when {
        isFocused -> LumaTheme.colors.cyan
        isActive -> LumaTheme.colors.cyan
        else -> LumaTheme.colors.textPrimary
    }

    Box(
        modifier = modifier
            .height(36.dp)
            .scale(scale)
            .cyanFocusGlow(
                isFocused = isFocused,
                shape = shape,
                glowColor = LumaTheme.colors.cyan,
                glowRadius = 8.dp
            )
            .background(bgColor, shape)
            .border(if (isFocused) 2.dp else 1.dp, borderColor, shape)
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .focusable(interactionSource = interactionSource)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            if (iconRes != null) {
                Icon(
                    painter = painterResource(id = iconRes),
                    contentDescription = null,
                    tint = LumaTheme.colors.cyan,
                    modifier = Modifier.size(16.dp)
                )
            }
            Text(
                text = text,
                style = LumaTheme.typography.labelMd.copy(fontWeight = FontWeight.SemiBold),
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
        }
    }
}

private fun logViewTree(view: View, depth: Int = 0) {
    val indent = "  ".repeat(depth)
    Log.i("LumaViewTree", "$indent${view.javaClass.simpleName} id=${view.id} opaque=${view.isOpaque} bg=${view.background} vis=${view.visibility}")
    if (view is ViewGroup) {
        for (i in 0 until view.childCount) {
            logViewTree(view.getChildAt(i), depth + 1)
        }
    }
}
