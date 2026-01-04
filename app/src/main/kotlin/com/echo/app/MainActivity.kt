package com.echo.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.echo.app.navigation.EchoDestinations
import com.echo.app.navigation.EchoNavGraph
import com.echo.core.datastore.preferences.ServerPreferences
import com.echo.core.datastore.preferences.SessionPreferences
import com.echo.core.datastore.preferences.ThemeMode
import com.echo.core.datastore.preferences.ThemePreferences
import com.echo.core.media.player.EchoPlayer
import com.echo.core.ui.components.EchoBottomNavBar
import com.echo.core.ui.components.EchoTopBar
import com.echo.core.ui.components.MiniPlayer
import com.echo.core.ui.components.MiniPlayerState
import com.echo.core.ui.theme.EchoTheme
import com.echo.core.ui.util.ColorExtractor
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var serverPreferences: ServerPreferences

    @Inject
    lateinit var sessionPreferences: SessionPreferences

    @Inject
    lateinit var echoPlayer: EchoPlayer

    @Inject
    lateinit var themePreferences: ThemePreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Make status bar fully transparent (no scrim)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(
                scrim = android.graphics.Color.TRANSPARENT
            )
        )

        setContent {
            val themeMode by themePreferences.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
            val systemDarkTheme = isSystemInDarkTheme()

            val darkTheme = when (themeMode) {
                ThemeMode.SYSTEM -> systemDarkTheme
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }

            EchoTheme(darkTheme = darkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    val session by sessionPreferences.session.collectAsState()
                    val playerState by echoPlayer.state.collectAsState()
                    val navBackStackEntry by navController.currentBackStackEntryAsState()

                    val currentRoute = navBackStackEntry?.destination?.route

                    // Routes where we hide bottom nav and mini player
                    val authRoutes = listOf(
                        EchoDestinations.WELCOME,
                        EchoDestinations.ADD_SERVER,
                        EchoDestinations.LOGIN
                    )
                    val isAuthScreen = currentRoute != null &&
                        authRoutes.any { currentRoute.startsWith(it) }

                    // Main screens where we show top bar
                    val mainRoutes = listOf(
                        EchoDestinations.HOME,
                        EchoDestinations.LIBRARY,
                        EchoDestinations.SEARCH,
                        EchoDestinations.RADIO,
                        EchoDestinations.SOCIAL
                    )
                    val isMainScreen by remember(currentRoute) {
                        derivedStateOf {
                            currentRoute != null && mainRoutes.any { currentRoute == it }
                        }
                    }

                    // Show bottom nav on all screens except auth and player
                    val showBottomNav by remember(currentRoute) {
                        derivedStateOf {
                            currentRoute != null &&
                                !isAuthScreen &&
                                currentRoute != EchoDestinations.PLAYER
                        }
                    }

                    // Show mini player when playing and not on auth/player screens
                    val showMiniPlayer by remember(currentRoute, playerState.currentTrack) {
                        derivedStateOf {
                            playerState.currentTrack != null &&
                                currentRoute != null &&
                                !isAuthScreen &&
                                currentRoute != EchoDestinations.PLAYER
                        }
                    }

                    // Extract dominant color from album art
                    val context = LocalContext.current
                    val dominantColor = remember { mutableStateOf<Color?>(null) }

                    LaunchedEffect(playerState.currentTrack?.coverUrl) {
                        val coverUrl = playerState.currentTrack?.coverUrl
                        if (coverUrl != null) {
                            dominantColor.value = ColorExtractor.extractDominantColor(
                                context = context,
                                imageUrl = coverUrl
                            )
                        } else {
                            dominantColor.value = null
                        }
                    }

                    // Track scroll for header glass effect
                    var totalScrollOffset by remember { mutableFloatStateOf(0f) }
                    val hasScrolled by remember {
                        derivedStateOf { totalScrollOffset > 50f }
                    }

                    // Reset scroll offset when navigating to a new screen
                    LaunchedEffect(currentRoute) {
                        totalScrollOffset = 0f
                    }

                    val nestedScrollConnection = remember {
                        object : NestedScrollConnection {
                            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                                // When scrolling down (content up), available.y is negative
                                // We want totalScrollOffset to increase when scrolling down
                                totalScrollOffset = (totalScrollOffset - available.y).coerceAtLeast(0f)
                                return Offset.Zero
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .nestedScroll(nestedScrollConnection)
                    ) {
                        // Main content - full screen, content scrolls behind overlays
                        EchoNavGraph(
                            navController = navController,
                            serverPreferences = serverPreferences,
                            sessionPreferences = sessionPreferences
                        )

                        // Floating overlay at top (TopBar)
                        if (isMainScreen) {
                            EchoTopBar(
                                onShuffleClick = {
                                    echoPlayer.shuffleAll()
                                },
                                onNotificationsClick = {
                                    // TODO: Navigate to notifications
                                },
                                onProfileClick = {
                                    navController.navigate(EchoDestinations.PROFILE)
                                },
                                hasScrolled = hasScrolled,
                                modifier = Modifier.align(Alignment.TopCenter)
                            )
                        }

                        // Floating overlay at bottom (MiniPlayer + BottomNav)
                        Column(
                            modifier = Modifier.align(Alignment.BottomCenter)
                        ) {
                            // Get next track info from queue
                            val nextTrack = if (playerState.queue.isNotEmpty() &&
                                playerState.currentIndex < playerState.queue.size - 1) {
                                playerState.queue.getOrNull(playerState.currentIndex + 1)
                            } else null

                            // MiniPlayer floats above bottom nav
                            MiniPlayer(
                                state = MiniPlayerState(
                                    isVisible = showMiniPlayer,
                                    isPlaying = playerState.isPlaying,
                                    trackTitle = playerState.currentTrack?.title ?: "",
                                    artistName = playerState.currentTrack?.artist ?: "",
                                    coverUrl = playerState.currentTrack?.coverUrl,
                                    progress = playerState.progress,
                                    dominantColor = dominantColor.value,
                                    nextTrackTitle = nextTrack?.title,
                                    nextArtistName = nextTrack?.artist
                                ),
                                onPlayerClick = {
                                    navController.navigate(EchoDestinations.PLAYER)
                                },
                                onPlayPauseClick = {
                                    echoPlayer.togglePlayPause()
                                },
                                onNextClick = {
                                    echoPlayer.seekToNext()
                                }
                            )

                            // Bottom Navigation
                            if (showBottomNav) {
                                EchoBottomNavBar(
                                    currentRoute = currentRoute,
                                    onNavigate = { item ->
                                        navController.navigate(item.route) {
                                            // Pop up to home, clearing the back stack
                                            popUpTo(EchoDestinations.HOME) {
                                                inclusive = false
                                                saveState = isMainScreen
                                            }
                                            launchSingleTop = true
                                            restoreState = isMainScreen
                                        }
                                    },
                                    modifier = Modifier.navigationBarsPadding()
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
