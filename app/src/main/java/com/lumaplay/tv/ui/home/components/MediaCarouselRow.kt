package com.lumaplay.tv.ui.home.components

import androidx.compose.foundation.background
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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.lumaplay.tv.core.theme.LumaTheme
import com.lumaplay.tv.core.ui.components.LumaBadge
import com.lumaplay.tv.core.ui.components.LumaBadgeVariant
import com.lumaplay.tv.core.ui.components.LumaCard
import com.lumaplay.tv.data.model.VideoItem

/**
 * Reusable Horizontal TV Media Swimlane (Carousel).
 *
 * Implements Stitch home_dashboard swimlane specifications:
 * - 16:9 media cards with 16dp rounded corners ([LumaTheme.shapes.large]).
 * - Focused 1.04x scale with 3dp Electric Cyan border and 20dp glow halo.
 * - Real asynchronous thumbnail loading with Coil.
 * - Dynamic resolution badge, duration badge, and optional progress track.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun MediaCarouselRow(
    title: String,
    videos: List<VideoItem>,
    onVideoClick: (VideoItem) -> Unit,
    modifier: Modifier = Modifier,
    badgeText: String? = null,
    showProgress: Boolean = false,
    sidebarHomeFocusRequester: FocusRequester? = null
) {
    if (videos.isEmpty()) return

    Column(modifier = modifier.fillMaxWidth()) {
        // Section Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = LumaTheme.typography.headlineSm,
                color = LumaTheme.colors.textPrimary
            )

            badgeText?.let { badge ->
                LumaBadge(
                    text = badge,
                    variant = LumaBadgeVariant.Standard
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Horizontal Carousel
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            contentPadding = PaddingValues(end = 48.dp)
        ) {
            itemsIndexed(videos, key = { _, video -> video.id }) { index, video ->
                val cardModifier = if (index == 0 && sidebarHomeFocusRequester != null) {
                    Modifier
                        .width(280.dp)
                        .focusProperties {
                            left = sidebarHomeFocusRequester
                        }
                } else {
                    Modifier.width(280.dp)
                }

                LumaCard(
                    onClick = { onVideoClick(video) },
                    modifier = cardModifier
                ) {
                    // 16:9 Thumbnail Area
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(158.dp)
                            .background(LumaTheme.colors.surfaceContainerHigh)
                    ) {
                        // Async Artwork / Thumbnail
                        AsyncImage(
                            model = video.contentUri,
                            contentDescription = video.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )

                        // Fallback play watermark icon
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(LumaTheme.shapes.circle)
                                .background(Color.Black.copy(alpha = 0.5f))
                                .align(Alignment.Center),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "▶",
                                style = LumaTheme.typography.labelLg,
                                color = LumaTheme.colors.cyan
                            )
                        }

                        // Top-End Resolution Badge
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                        ) {
                            LumaBadge(
                                text = video.resolutionTag,
                                variant = if (video.resolutionTag == "4K") LumaBadgeVariant.Highlight else LumaBadgeVariant.Standard
                            )
                        }

                        // Bottom-End Duration Badge
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(8.dp)
                        ) {
                            LumaBadge(
                                text = video.formattedDuration,
                                variant = LumaBadgeVariant.Standard
                            )
                        }

                        // Optional Progress Bar
                        if (showProgress) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .align(Alignment.BottomCenter)
                                    .background(LumaTheme.colors.surfaceContainerHighest)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(0.35f)
                                        .fillMaxHeight()
                                        .background(LumaTheme.colors.primary)
                                )
                            }
                        }
                    }

                    // Card Label Area
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        Text(
                            text = video.title,
                            style = LumaTheme.typography.labelLg,
                            color = LumaTheme.colors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${video.resolutionTag} • Local File",
                            style = LumaTheme.typography.bodySm,
                            color = LumaTheme.colors.textSecondary
                        )
                    }
                }
            }
        }
    }
}
