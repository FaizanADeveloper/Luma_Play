package com.lumaplay.tv.ui.home.components

import android.view.KeyEvent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.foundation.border
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
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
import com.lumaplay.tv.data.model.VideoItem

/**
 * Immersive Cinematic Hero Billboard for the Android TV Home Dashboard.
 *
 * Implements Google Stitch home_dashboard hero specifications:
 * - 460dp cinematic height with dual vignette scrims (responsive left scrim & subtle bottom dissolve).
 * - Real asynchronous video thumbnail loading via Coil with 250ms crossfade.
 * - Stitch displayLg typography for the clean title with subtle drop shadow.
 * - Real metadata badges: resolution, format container, and compact duration.
 * - Remote action buttons: primary cyan Play/Resume, Start Over (when history exists),
 *   dedicated non-destructive Details & Specs dialog, and Rescan.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun HeroBillboard(
    featuredVideo: VideoItem?,
    onPlay: (VideoItem) -> Unit,
    onDetails: (VideoItem) -> Unit,
    modifier: Modifier = Modifier,
    onScan: (() -> Unit)? = null,
    playButtonFocusRequester: FocusRequester? = null,
    sidebarHomeFocusRequester: FocusRequester? = null,
    onFocusGained: (() -> Unit)? = null
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(400.dp)
            .clip(LumaTheme.shapes.large)
            .background(LumaTheme.colors.surfaceContainerLowest)
    ) {
        if (featuredVideo != null) {
            val context = LocalContext.current
            val imageRequest = remember(featuredVideo.contentUri) {
                ImageRequest.Builder(context)
                    .data(featuredVideo.contentUri)
                    .crossfade(250)
                    .build()
            }

            // Asynchronous Thumbnail Background with preserved aspect ratio
            AsyncImage(
                model = imageRequest,
                contentDescription = featuredVideo.title,
                contentScale = ContentScale.Crop,
                alignment = Alignment.CenterEnd,
                modifier = Modifier.fillMaxSize()
            )

            // Responsive Left Scrim (protects typography contrast across 0% to 65% width)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            colorStops = arrayOf(
                                0.0f to LumaTheme.colors.background,
                                0.30f to LumaTheme.colors.background.copy(alpha = 0.90f),
                                0.65f to Color.Transparent,
                                1.0f to Color.Transparent
                            )
                        )
                    )
            )

            // Responsive Bottom Dissolve Scrim (melts billboard into canvas)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colorStops = arrayOf(
                                0.0f to Color.Transparent,
                                0.50f to Color.Transparent,
                                0.82f to LumaTheme.colors.background.copy(alpha = 0.45f),
                                1.0f to LumaTheme.colors.background
                            )
                        )
                    )
            )

            // Content Overlay
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Bottom
            ) {
                // Breadcrumb Anchor with vector folder icon and actual folder name
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_nav_folders),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = LumaTheme.colors.cyan
                    )
                    Text(
                        text = featuredVideo.folderName.uppercase(),
                        style = LumaTheme.typography.labelSm,
                        color = LumaTheme.colors.outline,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Cinema Title in Stitch displayLg with drop shadow
                Text(
                    text = featuredVideo.cleanTitle,
                    style = LumaTheme.typography.displayLg.copy(
                        shadow = Shadow(
                            color = Color.Black.copy(alpha = 0.8f),
                            offset = Offset(0f, 2f),
                            blurRadius = 8f
                        )
                    ),
                    color = LumaTheme.colors.textPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Badges Row (genuine resolution, container, local media, and duration)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LumaBadge(
                        text = featuredVideo.resolutionTag,
                        variant = if (featuredVideo.resolutionTag.startsWith("4K")) LumaBadgeVariant.Highlight else LumaBadgeVariant.Standard
                    )

                    if (featuredVideo.containerTag != null) {
                        LumaBadge(
                            text = featuredVideo.containerTag ?: "",
                            variant = LumaBadgeVariant.Standard
                        )
                    }

                    LumaBadge(
                        text = "LOCAL MEDIA",
                        variant = LumaBadgeVariant.Accent
                    )

                    Text(
                        text = "•",
                        style = LumaTheme.typography.labelSm,
                        color = LumaTheme.colors.outline
                    )

                    Text(
                        text = featuredVideo.compactDuration,
                        style = LumaTheme.typography.labelMd,
                        color = LumaTheme.colors.textPrimary
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Remote Action Buttons Row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier
                        .focusGroup()
                        .focusProperties {
                            up = FocusRequester.Cancel
                        }
                        .onFocusChanged { state ->
                            if (state.hasFocus) {
                                onFocusGained?.invoke()
                            }
                        }
                        .onPreviewKeyEvent { keyEvent ->
                            if (keyEvent.type == KeyEventType.KeyDown &&
                                keyEvent.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_DPAD_UP
                            ) {
                                onFocusGained?.invoke()
                                true
                            } else {
                                false
                            }
                        }
                ) {
                    // Primary Action: Resume (if playback position exists) or Play Video
                    if (featuredVideo.playbackPositionMs > 0L) {
                        LumaButton(
                            onClick = { onPlay(featuredVideo) },
                            variant = LumaButtonVariant.Primary,
                            modifier = Modifier
                                .focusProperties {
                                    if (sidebarHomeFocusRequester != null) {
                                        left = sidebarHomeFocusRequester
                                    }
                                }
                                .then(
                                    if (playButtonFocusRequester != null) {
                                        Modifier.focusRequester(playButtonFocusRequester)
                                    } else Modifier
                                )
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_action_play),
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = "Resume at ${featuredVideo.formattedResumePosition}")
                            }
                        }

                        // Start Over button when history exists
                        LumaButton(
                            onClick = { onPlay(featuredVideo.copy(playbackPositionMs = 0L)) },
                            variant = LumaButtonVariant.Secondary
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_action_replay),
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = "Start Over")
                            }
                        }
                    } else {
                        LumaButton(
                            onClick = { onPlay(featuredVideo) },
                            variant = LumaButtonVariant.Primary,
                            modifier = Modifier
                                .focusProperties {
                                    if (sidebarHomeFocusRequester != null) {
                                        left = sidebarHomeFocusRequester
                                    }
                                }
                                .then(
                                    if (playButtonFocusRequester != null) {
                                        Modifier.focusRequester(playButtonFocusRequester)
                                    } else Modifier
                                )
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_action_play),
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = "Play Video")
                            }
                        }
                    }

                    // Details & Specs (triggers non-destructive specs modal)
                    LumaButton(
                        onClick = { onDetails(featuredVideo) },
                        variant = LumaButtonVariant.Secondary
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_action_info),
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "Details & Specs")
                        }
                    }
                }
            }
        } else {
            // Empty / Welcome state (460dp aligned)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "LumaPlay Cinema",
                    style = LumaTheme.typography.displayLg,
                    color = LumaTheme.colors.textPrimary
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Your offline-first local media sanctuary on Android TV",
                    style = LumaTheme.typography.bodyMd,
                    color = LumaTheme.colors.textSecondary
                )
                if (onScan != null) {
                    Spacer(modifier = Modifier.height(28.dp))
                    LumaButton(
                        onClick = onScan,
                        variant = LumaButtonVariant.Primary,
                        modifier = Modifier
                            .focusProperties {
                                up = FocusRequester.Cancel
                                if (sidebarHomeFocusRequester != null) {
                                    left = sidebarHomeFocusRequester
                                }
                            }
                            .then(
                                if (playButtonFocusRequester != null) {
                                    Modifier.focusRequester(playButtonFocusRequester)
                                } else Modifier
                            )
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_action_refresh),
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "Scan Device Storage")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroSpecRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = LumaTheme.typography.bodySm,
            color = LumaTheme.colors.outline
        )
        Text(
            text = value,
            style = LumaTheme.typography.labelMd,
            color = LumaTheme.colors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Non-destructive, non-scrolling TV modal dialog displaying full technical media specifications.
 * Rendered at the dashboard root level so it never perturbs scroll position.
 */
@Composable
fun MediaDetailsDialog(
    video: VideoItem,
    onDismiss: () -> Unit
) {
    BackHandler(onBack = onDismiss)

    val closeRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        closeRequester.requestFocus()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f)),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .width(540.dp)
                .clip(LumaTheme.shapes.large)
                .background(LumaTheme.colors.surfaceContainerHigh)
                .border(1.dp, LumaTheme.colors.cyan.copy(alpha = 0.40f), LumaTheme.shapes.large)
                .padding(28.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Media Specifications",
                        style = LumaTheme.typography.headlineSm,
                        color = LumaTheme.colors.cyan
                    )
                    LumaBadge(
                        text = video.resolutionTag,
                        variant = LumaBadgeVariant.Highlight
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                HeroSpecRow(label = "Title", value = video.title)
                HeroSpecRow(label = "Location", value = video.folderName)
                HeroSpecRow(label = "Resolution", value = "${video.width} × ${video.height}")
                HeroSpecRow(label = "Duration", value = video.formattedDuration)
                HeroSpecRow(label = "File Size", value = video.formattedFileSize)
                if (video.containerTag != null) {
                    HeroSpecRow(label = "Container", value = video.containerTag ?: "")
                }
                if (!video.mimeType.isNullOrBlank()) {
                    HeroSpecRow(label = "MIME Type", value = video.mimeType ?: "")
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    LumaButton(
                        onClick = onDismiss,
                        variant = LumaButtonVariant.Primary,
                        modifier = Modifier.focusRequester(closeRequester)
                    ) {
                        Text(text = "Close")
                    }
                }
            }
        }
    }
}