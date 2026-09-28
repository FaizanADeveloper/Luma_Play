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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
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
import com.lumaplay.tv.core.ui.components.LumaCard
import com.lumaplay.tv.data.model.VideoItem

/**
 * Horizontal TV Media Swimlane (Carousel) for Recently Added Videos.
 *
 * Implements Stitch home_dashboard swimlane specifications:
 * - 16:9 media cards (280dp x 158dp) with 16dp rounded corners ([LumaTheme.shapes.large]).
 * - Focused 1.04x scale with 3dp Electric Cyan border and 20dp glow halo.
 * - Real asynchronous thumbnail loading with Coil without obscuring watermark icons.
 * - Top-left "NEW" badge, top-right resolution badge, bottom-right duration badge.
 * - Metadata row with file size pill, bullet, and container format tag.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun MediaCarouselRow(
    title: String,
    videos: List<VideoItem>,
    onVideoClick: (VideoItem) -> Unit,
    modifier: Modifier = Modifier,
    badgeText: String? = null,
    iconResId: Int? = R.drawable.ic_action_new_releases,
    showProgress: Boolean = false,
    sidebarHomeFocusRequester: FocusRequester? = null,
    heroFocusRequester: FocusRequester? = null,
    downFocusRequester: FocusRequester? = null,
    contentPadding: PaddingValues = PaddingValues(start = 40.dp, end = 56.dp, top = 24.dp, bottom = 24.dp),
    headerPadding: PaddingValues = PaddingValues(horizontal = 40.dp)
) {
    if (videos.isEmpty()) return

    Column(modifier = modifier.fillMaxWidth()) {
        // Section Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(headerPadding),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (iconResId != null) {
                    Icon(
                        painter = painterResource(id = iconResId),
                        contentDescription = null,
                        tint = LumaTheme.colors.cyan,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Text(
                    text = title,
                    style = LumaTheme.typography.headlineSm,
                    color = LumaTheme.colors.textPrimary
                )
            }

            badgeText?.let { badge ->
                LumaBadge(
                    text = badge,
                    variant = LumaBadgeVariant.Standard
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Horizontal Carousel
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            contentPadding = contentPadding
        ) {
            itemsIndexed(videos, key = { _, video -> video.id }) { index, video ->
                val cardModifier = Modifier
                    .width(280.dp)
                    .focusProperties {
                        if (index == 0) {
                            if (sidebarHomeFocusRequester != null) {
                                left = sidebarHomeFocusRequester
                            }
                            if (downFocusRequester != null) {
                                down = downFocusRequester
                            }
                        }
                        if (heroFocusRequester != null) {
                            up = heroFocusRequester
                        }
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
                            .background(LumaTheme.colors.surfaceContainerLowest)
                    ) {
                        // Async Artwork / Thumbnail
                        AsyncImage(
                            model = video.contentUri,
                            contentDescription = video.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )

                        // Subtle bottom gradient overlay for legibility
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            Color(0x99080E1D)
                                        )
                                    )
                                )
                        )

                        // Top-Start "NEW" Badge
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(8.dp)
                                .background(
                                    color = LumaTheme.colors.surfaceContainerHigh.copy(alpha = 0.9f),
                                    shape = RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "NEW",
                                style = LumaTheme.typography.labelSm,
                                color = LumaTheme.colors.textPrimary
                            )
                        }

                        // Top-End Resolution Badge
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                                .background(
                                    color = LumaTheme.colors.surfaceContainerLowest.copy(alpha = 0.85f),
                                    shape = RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = video.resolutionTag,
                                style = LumaTheme.typography.labelSm,
                                color = LumaTheme.colors.cyan
                            )
                        }

                        // Bottom-End Duration Badge
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(8.dp)
                                .background(
                                    color = LumaTheme.colors.surfaceContainerLowest.copy(alpha = 0.85f),
                                    shape = RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = video.formattedDuration,
                                style = LumaTheme.typography.labelSm,
                                color = LumaTheme.colors.textPrimary
                            )
                        }

                        // Optional Progress Bar (for future Continue Watching milestone)
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
                                        .background(LumaTheme.colors.cyan)
                                )
                            }
                        }
                    }

                    // Card Label Area
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                    ) {
                        Text(
                            text = video.cleanTitle,
                            style = LumaTheme.typography.labelLg,
                            color = LumaTheme.colors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // File size pill
                            Box(
                                modifier = Modifier
                                    .background(
                                        color = LumaTheme.colors.surfaceContainerHigh,
                                        shape = RoundedCornerShape(4.dp)
                                    )
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = video.formattedFileSize,
                                    style = LumaTheme.typography.labelSm,
                                    color = LumaTheme.colors.cyan
                                )
                            }

                            Text(
                                text = "•",
                                style = LumaTheme.typography.labelSm,
                                color = LumaTheme.colors.outline
                            )

                            // Format / Container info
                            val formatInfo = buildString {
                                video.containerTag?.let { append("$it ") }
                                append(video.resolutionTag)
                            }
                            Text(
                                text = formatInfo.uppercase(),
                                style = LumaTheme.typography.labelSm,
                                color = LumaTheme.colors.textSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}
