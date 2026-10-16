package com.lumaplay.tv.player.seek

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import coil.decode.VideoFrameDecoder
import coil.request.ImageRequest
import coil.request.videoFrameMillis
import com.lumaplay.tv.R
import com.lumaplay.tv.core.theme.LumaTheme
import kotlinx.coroutines.delay

/**
 * Floating 16:9 thumbnail preview card that hovers above the timeline thumb during deferred seeking.
 *
 * Employs a 120ms debounce so rapid remote navigation does not overload the MediaCodec/ExoPlayer
 * pipeline with intermediate frame extraction requests.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun ThumbnailPreviewCard(
    videoUri: Uri,
    previewPositionMs: Long,
    formattedPosition: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Debounce the frame extraction timestamp by 120ms during rapid D-pad scrubbing
    var debouncedPositionMs by remember { mutableLongStateOf(previewPositionMs) }
    LaunchedEffect(previewPositionMs) {
        delay(120L)
        debouncedPositionMs = previewPositionMs
    }

    val imageRequest = remember(videoUri, debouncedPositionMs) {
        ImageRequest.Builder(context)
            .data(videoUri)
            .decoderFactory(VideoFrameDecoder.Factory())
            .videoFrameMillis(debouncedPositionMs)
            .size(400, 224) // 2x density downsampling for 200x112dp thumbnail to bound memory
            .crossfade(true)
            .build()
    }

    val shape = RoundedCornerShape(8.dp)

    Box(
        modifier = modifier
            .wrapContentSize(Alignment.TopStart, unbounded = true)
            .requiredSize(width = 200.dp, height = 112.dp)
            .clip(shape)
            .background(LumaTheme.colors.surfaceContainerLowest.copy(alpha = 0.95f))
            .border(1.5.dp, LumaTheme.colors.cyan, shape),
        contentAlignment = Alignment.Center
    ) {
        // Fallback / Loading background with media icon
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_nav_videos),
                contentDescription = null,
                tint = LumaTheme.colors.textSecondary.copy(alpha = 0.4f),
                modifier = Modifier.size(36.dp)
            )
        }

        // Asynchronously extracted video frame
        AsyncImage(
            model = imageRequest,
            contentDescription = "Seek preview at $formattedPosition",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Monospace timestamp pill pinned to bottom
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.75f))
                .padding(vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = formattedPosition,
                style = LumaTheme.typography.labelSm.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp
                ),
                color = LumaTheme.colors.cyan
            )
        }
    }
}
