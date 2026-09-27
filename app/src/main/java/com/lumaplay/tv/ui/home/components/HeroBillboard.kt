package com.lumaplay.tv.ui.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.lumaplay.tv.core.theme.LumaBackground
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
 * - Full 16:9 cinematic backdrop with dual vignette scrims (left-to-right fade & bottom dissolve).
 * - Real asynchronous video thumbnail loading via Coil.
 * - Dynamic metadata badges (4K UHD, 1080p, duration).
 * - Action buttons with 1.05x focus scaling and Electric Cyan luminous glow.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun HeroBillboard(
    featuredVideo: VideoItem?,
    onPlay: (VideoItem) -> Unit,
    onDetails: (VideoItem) -> Unit,
    onRescan: () -> Unit,
    modifier: Modifier = Modifier,
    playButtonFocusRequester: FocusRequester? = null,
    sidebarHomeFocusRequester: FocusRequester? = null
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(380.dp)
            .clip(LumaTheme.shapes.extraLarge)
            .background(LumaTheme.colors.surfaceContainerLowest)
    ) {
        if (featuredVideo != null) {
            // Asynchronous Thumbnail Background
            AsyncImage(
                model = featuredVideo.contentUri,
                contentDescription = featuredVideo.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Left Scrim (protects typography contrast)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                LumaBackground,
                                LumaBackground.copy(alpha = 0.9f),
                                LumaBackground.copy(alpha = 0.5f),
                                Color.Transparent
                            ),
                            startX = 0f,
                            endX = 1200f
                        )
                    )
            )

            // Bottom Dissolve Scrim (melts into canvas)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                LumaBackground.copy(alpha = 0.6f),
                                LumaBackground
                            )
                        )
                    )
            )

            // Content Overlay
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                verticalArrangement = Arrangement.Bottom
            ) {
                // Breadcrumb Anchor
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "📁 Internal Storage / Videos",
                        style = LumaTheme.typography.labelSm,
                        color = LumaTheme.colors.cyan
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Cinema Title
                Text(
                    text = featuredVideo.title,
                    style = LumaTheme.typography.displayMd,
                    color = LumaTheme.colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Badges Row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LumaBadge(
                        text = featuredVideo.resolutionTag,
                        variant = if (featuredVideo.resolutionTag == "4K") LumaBadgeVariant.Highlight else LumaBadgeVariant.Standard
                    )
                    LumaBadge(
                        text = featuredVideo.formattedDuration,
                        variant = LumaBadgeVariant.Standard
                    )
                    LumaBadge(
                        text = "LOCAL MEDIA",
                        variant = LumaBadgeVariant.Accent
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Action Buttons Row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.focusProperties {
                        up = FocusRequester.Cancel
                    }
                ) {
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
                        Text(text = "▶ Play Video")
                    }

                    LumaButton(
                        onClick = { onDetails(featuredVideo) },
                        variant = LumaButtonVariant.Secondary
                    ) {
                        Text(text = "Details & Specs")
                    }

                    LumaButton(
                        onClick = onRescan,
                        variant = LumaButtonVariant.Ghost
                    ) {
                        Text(text = "Rescan")
                    }
                }
            }
        } else {
            // Empty / Welcome state
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "LumaPlay Cinema",
                    style = LumaTheme.typography.displayMd,
                    color = LumaTheme.colors.textPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Your offline-first local media sanctuary on Android TV",
                    style = LumaTheme.typography.bodyMd,
                    color = LumaTheme.colors.textSecondary
                )
                Spacer(modifier = Modifier.height(24.dp))
                LumaButton(
                    onClick = onRescan,
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
                    Text(text = "Scan Device Storage")
                }
            }
        }
    }
}
