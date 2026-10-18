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
import com.lumaplay.tv.data.repository.MediaStoreAudioRepository
import com.lumaplay.tv.data.repository.MediaStoreVideoRepository
import com.lumaplay.tv.navigation.Screen
import com.lumaplay.tv.player.PlayerScreen
import com.lumaplay.tv.player.audio.AudioPlayerController
import com.lumaplay.tv.ui.home.HomeScreen
import com.lumaplay.tv.ui.home.HomeUiState
import com.lumaplay.tv.ui.home.HomeViewModel
import com.lumaplay.tv.ui.details.VideoDetailsScreen
import com.lumaplay.tv.ui.details.VideoDetailsViewModel
import com.lumaplay.tv.ui.music.MusicPlayerViewModel
import com.lumaplay.tv.ui.music.MusicScreen
import com.lumaplay.tv.ui.splash.LumaLaunchScreen
import com.lumaplay.tv.ui.videos.VideoLibraryScreen
import com.lumaplay.tv.ui.videos.VideosViewModel
import com.lumaplay.tv.data.repository.LocalMediaSearchRepository
import com.lumaplay.tv.ui.search.SearchScreen
import com.lumaplay.tv.ui.search.SearchViewModel

class MainActivity : ComponentActivity() {

    private val homeViewModel by viewModels<HomeViewModel> {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return HomeViewModel(
                    videoRepository = MediaStoreVideoRepository(applicationContext)
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
                    videoRepository = MediaStoreVideoRepository(applicationContext)
                ) as T
            }
        }
    }

    private val videoDetailsViewModel by viewModels<VideoDetailsViewModel>()

    private val musicPlayerViewModel by viewModels<MusicPlayerViewModel> {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return MusicPlayerViewModel(
                    audioRepository = MediaStoreAudioRepository(applicationContext),
                    audioPlayerController = AudioPlayerController(applicationContext)
                ) as T
            }
        }
    }

    private val searchViewModel by viewModels<SearchViewModel> {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return SearchViewModel(
                    searchRepository = LocalMediaSearchRepository(
                        videoRepository = MediaStoreVideoRepository(applicationContext),
                        audioRepository = MediaStoreAudioRepository(applicationContext)
                    )
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
                var detailsOriginScreen by remember { mutableStateOf<Screen>(Screen.Home) }
                var previousScreenBeforePlayer by remember { mutableStateOf<Screen?>(null) }
                var selectedDestination by remember { mutableStateOf(TvNavDestination.Home) }
                var isLaunchCompleted by remember { mutableStateOf(false) }
                val heroPlayButtonRequester = remember { FocusRequester() }
                val dashboardFocusRequester = remember { FocusRequester() }
                val sidebarHomeFocusRequester = remember { FocusRequester() }
                val sidebarVideosFocusRequester = remember { FocusRequester() }
                val videoLibraryFocusRequester = remember { FocusRequester() }
                val videoDetailsPlayFocusRequester = remember { FocusRequester() }
                val sidebarMusicFocusRequester = remember { FocusRequester() }
                val musicContentFocusRequester = remember { FocusRequester() }
                val sidebarSearchFocusRequester = remember { FocusRequester() }
                val searchContentFocusRequester = remember { FocusRequester() }

                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { isGranted ->
                    homeViewModel.onPermissionResult(isGranted)
                    if (isGranted) videosViewModel.onPermissionResult(isGranted)
                    musicPlayerViewModel.onPermissionChecked(isGranted)
                }

                // Check permission on initial launch
                LaunchedEffect(Unit) {
                    val hasPermission = StoragePermissionHelper.hasStoragePermission(this@MainActivity)
                    homeViewModel.onPermissionChecked(hasPermission)
                    videosViewModel.onPermissionChecked(hasPermission)
                    musicPlayerViewModel.onPermissionChecked(hasPermission)
                }

                // Handle Back press on Videos screen to return to Home dashboard
                BackHandler(enabled = currentScreen is Screen.Videos) {
                    currentScreen = Screen.Home
                }

                // Handle Back press on Music screen to return to Home dashboard
                BackHandler(enabled = currentScreen is Screen.Music) {
                    currentScreen = Screen.Home
                }

                // Handle Back press on VideoDetail screen to return to originating screen
                BackHandler(enabled = currentScreen is Screen.VideoDetail) {
                    currentScreen = detailsOriginScreen
                    selectedDestination = when (detailsOriginScreen) {
                        is Screen.Videos -> TvNavDestination.Videos
                        is Screen.Search -> TvNavDestination.Search
                        else -> TvNavDestination.Home
                    }
                }

                // Handle Back press on Search screen to return to Home dashboard
                BackHandler(enabled = currentScreen is Screen.Search) {
                    currentScreen = Screen.Home
                }

                // Listen for onResume to detect when user returns from Android Settings
                DisposableEffect(lifecycle) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) {
                            val hasPermission = StoragePermissionHelper.hasStoragePermission(this@MainActivity)
                            homeViewModel.onPermissionChecked(hasPermission)
                            videosViewModel.onPermissionChecked(hasPermission)
                            musicPlayerViewModel.onPermissionChecked(hasPermission)
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
                        is Screen.Music -> selectedDestination = TvNavDestination.Music
                        is Screen.Home -> selectedDestination = TvNavDestination.Home
                        is Screen.Search -> selectedDestination = TvNavDestination.Search
                        is Screen.VideoDetail -> {
                            selectedDestination = when (detailsOriginScreen) {
                                is Screen.Videos -> TvNavDestination.Videos
                                is Screen.Search -> TvNavDestination.Search
                                else -> TvNavDestination.Home
                            }
                        }
                        is Screen.Player -> {
                            // Defensive coordination: ensure music playback is paused during video playback
                            musicPlayerViewModel.pause()
                        }
                    }
                }

                when (val screen = currentScreen) {
                    is Screen.Player -> {
                        // --- Fullscreen Video Player (Isolated from TV Shell and Overlays) ---
                        PlayerScreen(
                            video = screen.video,
                            onBack = {
                                // Return to whichever screen launched the player
                                currentScreen = previousScreenBeforePlayer ?: when (selectedDestination) {
                                    TvNavDestination.Videos -> Screen.Videos
                                    else -> Screen.Home
                                }
                            }
                        )
                    }

                    else -> {
                        Box(modifier = Modifier.fillMaxSize()) {
                            // --- TV Shell (Sidebar + TopBar + Active Screen) ---
                            Row(modifier = Modifier.fillMaxSize()) {
                                val contentContainerFocusRequester = remember { FocusRequester() }

                                TvSidebar(
                                    selectedDestination = selectedDestination,
                                    onDestinationSelected = { destination ->
                                        selectedDestination = destination
                                        when (destination) {
                                            TvNavDestination.Home -> currentScreen = Screen.Home
                                            TvNavDestination.Videos -> currentScreen = Screen.Videos
                                            TvNavDestination.Music -> currentScreen = Screen.Music
                                            TvNavDestination.Search -> currentScreen = Screen.Search
                                            else -> { /* B2-M4…M5 destinations — coming in later milestones */ }
                                        }
                                    },
                                    dashboardFocusRequester = contentContainerFocusRequester,
                                    homeFocusRequester = sidebarHomeFocusRequester,
                                    videosFocusRequester = sidebarVideosFocusRequester,
                                    musicFocusRequester = sidebarMusicFocusRequester,
                                    searchFocusRequester = sidebarSearchFocusRequester
                                )

                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .focusRequester(contentContainerFocusRequester)
                                        .focusRestorer {
                                            if (currentScreen is Screen.Home && uiState is HomeUiState.Success) {
                                                heroPlayButtonRequester
                                            } else if (currentScreen is Screen.VideoDetail) {
                                                videoDetailsPlayFocusRequester
                                            } else if (currentScreen is Screen.Music) {
                                                musicContentFocusRequester
                                            } else if (currentScreen is Screen.Search) {
                                                searchContentFocusRequester
                                            } else {
                                                FocusRequester.Default
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
                                                    musicPlayerViewModel.pause()
                                                    previousScreenBeforePlayer = Screen.Home
                                                    currentScreen = Screen.Player(video)
                                                },
                                                onDetailsClick = { video ->
                                                    detailsOriginScreen = Screen.Home
                                                    previousScreenBeforePlayer = Screen.VideoDetail(video)
                                                    currentScreen = Screen.VideoDetail(video)
                                                },
                                                playButtonFocusRequester = heroPlayButtonRequester,
                                                sidebarHomeFocusRequester = sidebarHomeFocusRequester
                                            )
                                        }

                                        is Screen.Videos -> {
                                            VideoLibraryScreen(
                                                viewModel = videosViewModel,
                                                onVideoClick = { video ->
                                                    detailsOriginScreen = Screen.Videos
                                                    previousScreenBeforePlayer = Screen.VideoDetail(video)
                                                    currentScreen = Screen.VideoDetail(video)
                                                },
                                                onRequestPermission = {
                                                    permissionLauncher.launch(StoragePermissionHelper.requiredPermission)
                                                },
                                                sidebarVideosFocusRequester = sidebarVideosFocusRequester,
                                                contentFocusRequester = videoLibraryFocusRequester
                                            )
                                        }

                                        is Screen.Music -> {
                                            MusicScreen(
                                                viewModel = musicPlayerViewModel,
                                                sidebarMusicFocusRequester = sidebarMusicFocusRequester,
                                                contentFocusRequester = musicContentFocusRequester
                                            )
                                        }

                                        is Screen.VideoDetail -> {
                                            val detail = currentScreen as Screen.VideoDetail
                                            VideoDetailsScreen(
                                                video = detail.video,
                                                detailsViewModel = videoDetailsViewModel,
                                                onPlay = { videoToPlay ->
                                                    musicPlayerViewModel.pause()
                                                    previousScreenBeforePlayer = currentScreen
                                                    currentScreen = Screen.Player(videoToPlay)
                                                },
                                                onPlayFromBeginning = { videoToPlay ->
                                                    musicPlayerViewModel.pause()
                                                    previousScreenBeforePlayer = currentScreen
                                                    currentScreen = Screen.Player(videoToPlay.copy(playbackPositionMs = 0L))
                                                },
                                                sidebarVideosFocusRequester = if (detailsOriginScreen is Screen.Videos) {
                                                    sidebarVideosFocusRequester
                                                } else {
                                                    sidebarHomeFocusRequester
                                                },
                                                playButtonFocusRequester = videoDetailsPlayFocusRequester
                                            )
                                        }

                                        is Screen.Search -> {
                                            SearchScreen(
                                                viewModel = searchViewModel,
                                                sidebarSearchFocusRequester = sidebarSearchFocusRequester,
                                                contentFocusRequester = searchContentFocusRequester,
                                                onVideoClick = { video ->
                                                    musicPlayerViewModel.pause()
                                                    detailsOriginScreen = Screen.Search
                                                    previousScreenBeforePlayer = Screen.VideoDetail(video)
                                                    currentScreen = Screen.VideoDetail(video)
                                                },
                                                onTrackClick = { track ->
                                                    musicPlayerViewModel.playTrack(track, listOf(track))
                                                    currentScreen = Screen.Music
                                                    selectedDestination = TvNavDestination.Music
                                                },
                                                onArtistClick = { artist ->
                                                    musicPlayerViewModel.playArtist(artist)
                                                    currentScreen = Screen.Music
                                                    selectedDestination = TvNavDestination.Music
                                                },
                                                onAlbumClick = { album ->
                                                    musicPlayerViewModel.playAlbum(album)
                                                    currentScreen = Screen.Music
                                                    selectedDestination = TvNavDestination.Music
                                                },
                                                onFolderClick = { _ ->
                                                    // Folders browsing milestone is B2-M4
                                                }
                                            )
                                        }

                                        else -> {
                                            // Other destinations (Folders, Settings) — B2-M4 to M5
                                            HomeScreen(
                                                uiState = uiState,
                                                onScanClick = { homeViewModel.scanVideos() },
                                                onRequestPermission = {
                                                    permissionLauncher.launch(StoragePermissionHelper.requiredPermission)
                                                },
                                                onVideoClick = { video ->
                                                    musicPlayerViewModel.pause()
                                                    currentScreen = Screen.Player(video)
                                                },
                                                playButtonFocusRequester = heroPlayButtonRequester,
                                                sidebarHomeFocusRequester = sidebarHomeFocusRequester
                                            )
                                        }
                                    }
                                }
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
    }
}