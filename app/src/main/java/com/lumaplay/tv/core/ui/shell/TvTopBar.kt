package com.lumaplay.tv.core.ui.shell

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import com.lumaplay.tv.R
import com.lumaplay.tv.core.theme.LumaTheme

/**
 * Top Status Bar for Android TV.
 *
 * Implements Google Stitch home_dashboard header specifications:
 * - 64dp height and 56dp TV-safe margin horizontal content padding.
 * - Real display mode detection based on screen density & width (1080p Cinema Mode / 4K UHD).
 * - Real network transport query via [ConnectivityManager] (Ethernet LAN / Wi-Fi / Offline) with vector icon.
 * - 32dp user profile avatar circle with vector person symbol.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvTopBar(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current

    // Real display mode calculation
    val displayMode = remember(configuration) {
        val widthDp = configuration.screenWidthDp
        if (widthDp >= 2560) "4K Cinema Mode" else "1080p Cinema Mode"
    }

    // Real network connectivity status query
    val networkStatus = remember {
        runCatching {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            val activeNetwork = cm?.activeNetwork
            val caps = cm?.getNetworkCapabilities(activeNetwork)
            when {
                caps == null -> "Offline"
                caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "LAN Connected"
                caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi Connected"
                else -> "Connected"
            }
        }.getOrDefault("Local Ready")
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .background(LumaTheme.colors.surfaceContainerLowest.copy(alpha = 0.85f))
            .padding(horizontal = LumaTheme.spacing.tvSafeMargin)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. Display Mode Badge (Stitch: bg-surface-container-high, text-secondary)
            Box(
                modifier = Modifier
                    .clip(LumaTheme.shapes.small)
                    .background(LumaTheme.colors.surfaceContainerHigh)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = displayMode,
                    style = LumaTheme.typography.labelSm,
                    color = LumaTheme.colors.cyan
                )
            }

            // 2. System Status Indicators & Avatar
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Network indicator with vector icon
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_status_wifi),
                        contentDescription = "Network Status",
                        modifier = Modifier.size(16.dp),
                        tint = LumaTheme.colors.textSecondary
                    )
                    Text(
                        text = networkStatus,
                        style = LumaTheme.typography.labelSm,
                        color = LumaTheme.colors.textSecondary
                    )
                }

                // Profile Avatar (Stitch: w-8 h-8 rounded-full bg-primary)
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(LumaTheme.shapes.circle)
                        .background(LumaTheme.colors.cyan),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_status_person),
                        contentDescription = "User Profile",
                        modifier = Modifier.size(18.dp),
                        tint = LumaTheme.colors.onSecondary
                    )
                }
            }
        }
    }
}
