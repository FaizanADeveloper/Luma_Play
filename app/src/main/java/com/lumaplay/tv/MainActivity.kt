package com.lumaplay.tv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import coil.Coil
import coil.ImageLoader
import coil.decode.VideoFrameDecoder
import com.lumaplay.tv.core.theme.LumaPlayTheme
import com.lumaplay.tv.core.ui.shell.TvNavDestination
import com.lumaplay.tv.core.ui.shell.TvSidebar
import com.lumaplay.tv.core.ui.shell.TvTopBar
import com.lumaplay.tv.core.util.StoragePermissionHelper
import com.lumaplay.tv.data.repository.MediaStoreVideoRepository
import com.lumaplay.tv.navigation.Screen
import com.lumaplay.tv.player.PlayerScreen
import com.lumaplay.tv.ui.home.HomeScreen
import com.lumaplay.tv.ui.home.HomeUiState
import com.lumaplay.tv.ui.home.HomeViewModel
import com.lumaplay.tv.ui.splash.LumaLaunchScreen
import com.lumaplay.tv.ui.videos.VideoLibraryScreen
import com.lumaplay.tv.ui.videos.VideosViewModel

class MainActivity : ComponentActivity() {

    private val homeViewModel by viewModels<HomeViewModel> {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return HomeViewModel(
                    videoRepository = MediaStoreVideoRepository(applicationContext.contentResolver)
                ) as T
            }
        }
    }

    /**
     * Separate ViewModel for the Video Library screen.
     * Shares the same [MediaStoreVideoRepository] implementation but
     * maintains its own UI state, sort order and filter independently
     * from the Home dashboard.
     */
    private val videosViewModel by viewModels<VideosViewModel> {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return VideosViewModel(
                    videoRepository = MediaStoreVideoRepository(applicationContext.contentResolver)
                ) as T
            }
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Register Coil's VideoFrameDecoder for asynchronous video thumbnail extraction
        val imageLoader = ImageLoader.Builder(this)
            .components {
                add(VideoFrameDecoder.Factory())
            }
            .crossfade(true)
            .build()
        Coil.setImageLoader(imageLoader)

        setContent {
            LumaPlayTheme {
                val uiState by homeViewModel.uiState.collectAsState()
                var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }
                var selectedDestination by remember { mutableStateOf(TvNavDestination.Home) }
                var isLaunchCompleted by remember { mutableStateOf(false) }
                val heroPlayButtonRequester = remember { FocusRequester() }
                val dashboardFocusRequester = remember { FocusRequester() }
                val sidebarHomeFocusRequester = remember { FocusRequester() }
                val sidebarVideosFocusRequester = remember { FocusRequester() }
                val videoLibraryFocusRequester = remember { FocusRequester() }

                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { isGranted ->
                    homeViewModel.onPermissionResult(isGranted)
                    if (isGranted) videosViewModel.onPermissionResult(isGranted)
                }

                // Check permission on initial launch
                LaunchedEffect(Unit) {
                    val hasPermission = StoragePermissionHelper.hasStoragePermission(this@MainActivity)
                    homeViewModel.onPermissionChecked(hasPermission)
                    videosViewModel.onPermissionChecked(hasPermission)
                }

                // Handle Back press on Videos screen to return to Home dashboard
                BackHandler(enabled = currentScreen is Screen.Videos) {
                    currentScreen = Screen.Home
                }

                // Listen for onResume to detect when user returns from Android Settings
                DisposableEffect(lifecycle) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) {
                            val hasPermission = StoragePermissionHelper.hasStoragePermission(this@MainActivity)
                            homeViewModel.onPermissionChecked(hasPermission)
                            videosViewModel.onPermissionChecked(hasPermission)
                        }
                    }
                    lifecycle.addObserver(observer)
                    onDispose {
                        lifecycle.removeObserver(observer)
                    }
                }

                // Sync sidebar selection with the current logical screen so the
                // active destination pill stays correct during deep navigation.
                LaunchedEffect(currentScreen) {
                    when (currentScreen) {
                        is Screen.Videos -> selectedDestination = TvNavDestination.Videos
                        is Screen.Home -> selectedDestination = TvNavDestination.Home
                        else -> { /* Player is fullscreen — sidebar hidden */ }
                    }
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    // --- TV Shell (Sidebar + TopBar + Active Screen) ---
                    Row(modifier = Modifier.fillMaxSize()) {
                        TvSidebar(
                            selectedDestination = selectedDestination,
                            onDestinationSelected = { destination ->
                                selectedDestination = destination
                                // Map sidebar tap → screen navigation
                                when (destination) {
                                    TvNavDestination.Home -> currentScreen = Screen.Home
                                    TvNavDestination.Videos -> currentScreen = Screen.Videos
                                    else -> { /* B2-M3…M5 destinations — coming in later milestones */ }
                                }
                            },
                            dashboardFocusRequester = dashboardFocusRequester,
                            homeFocusRequester = sidebarHomeFocusRequester,
                            videosFocusRequester = sidebarVideosFocusRequester
                        )

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .focusRequester(dashboardFocusRequester)
                                .focusRestorer {
                                    if (currentScreen is Screen.Home && uiState is HomeUiState.Success) {
                                        heroPlayButtonRequester
                                    } else if (currentScreen is Screen.Videos) {
                                        videoLibraryFocusRequester
                                    } else {
                                        FocusRequester.Default
                                    }
                                }
                                .focusProperties {
                                    left = if (currentScreen is Screen.Videos) {
                                        sidebarVideosFocusRequester
                                    } else {
                                        sidebarHomeFocusRequester
                                    }
                                }
                        ) {
                            TvTopBar()

                            // --- Active content area ---
                            when (currentScreen) {
                                is Screen.Home -> {
                                    HomeScreen(
                                        uiState = uiState,
                                        onScanClick = { homeViewModel.scanVideos() },
                                        onRequestPermission = {
                                            permissionLauncher.launch(StoragePermissionHelper.requiredPermission)
                                        },
                                        onVideoClick = { video ->
                                            currentScreen = Screen.Player(video)
                                        },
                                        playButtonFocusRequester = heroPlayButtonRequester,
                                        sidebarHomeFocusRequester = sidebarHomeFocusRequester
                                    )
                                }

                                is Screen.Videos -> {
                                    VideoLibraryScreen(
                                        viewModel = videosViewModel,
                                        onVideoClick = { video ->
                                            // B2-M1: go directly to player.
                                            // B2-M2: this will route to Screen.VideoDetail(video) instead.
                                            currentScreen = Screen.Player(video)
                                        },
                                        onRequestPermission = {
                                            permissionLauncher.launch(StoragePermissionHelper.requiredPermission)
                                        },
                                        sidebarVideosFocusRequester = sidebarVideosFocusRequester,
                                        contentFocusRequester = videoLibraryFocusRequester
                                    )
                                }

                                is Screen.VideoDetail -> {
                                    // VideoDetailsScreen — implemented in B2-M2.
                                    // For now, navigate directly to the player so the
                                    // card's onClick is never left without a handler.
                                    val detail = currentScreen as Screen.VideoDetail
                                    currentScreen = Screen.Player(detail.video)
                                }

                                else -> {
                                    // Other destinations (Music, Folders, Search) — B2-M3 to M5
                                    HomeScreen(
                                        uiState = uiState,
                                        onScanClick = { homeViewModel.scanVideos() },
                                        onRequestPermission = {
                                            permissionLauncher.launch(StoragePermissionHelper.requiredPermission)
                                        },
                                        onVideoClick = { video ->
                                            currentScreen = Screen.Player(video)
                                        },
                                        playButtonFocusRequester = heroPlayButtonRequester,
                                        sidebarHomeFocusRequester = sidebarHomeFocusRequester
                                    )
                                }
                            }
                        }
                    }

                    // --- Fullscreen Video Player Overlay ---
                    when (val screen = currentScreen) {
                        is Screen.Player -> {
                            PlayerScreen(
                                video = screen.video,
                                onBack = {
                                    // Return to whichever library screen launched the player
                                    currentScreen = when (selectedDestination) {
                                        TvNavDestination.Videos -> Screen.Videos
                                        else -> Screen.Home
                                    }
                                }
                            )
                        }
                        else -> { /* Player screen is not active */ }
                    }

                    // --- Full Branded Launch Experience Overlay ---
                    AnimatedVisibility(
                        visible = !isLaunchCompleted,
                        exit = fadeOut(animationSpec = tween(400))
                    ) {
                        LumaLaunchScreen(
                            onLaunchComplete = { isLaunchCompleted = true }
                        )
                    }
                }
            }
        }
    }
}