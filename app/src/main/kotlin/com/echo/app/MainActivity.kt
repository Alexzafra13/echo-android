package com.echo.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import com.echo.core.ui.components.MiniPlayer
import com.echo.core.ui.components.MiniPlayerState
import com.echo.core.ui.theme.EchoTheme
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
        enableEdgeToEdge()

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

                    // Hide MiniPlayer on login/welcome screens and full player screen
                    val currentRoute = navBackStackEntry?.destination?.route
                    val hideMiniPlayerRoutes = listOf(
                        EchoDestinations.WELCOME,
                        EchoDestinations.ADD_SERVER,
                        EchoDestinations.LOGIN,
                        EchoDestinations.PLAYER
                    )
                    val showMiniPlayer by remember(currentRoute, playerState.currentTrack) {
                        derivedStateOf {
                            playerState.currentTrack != null &&
                                currentRoute != null &&
                                !hideMiniPlayerRoutes.any { currentRoute.startsWith(it) }
                        }
                    }

                    Box(modifier = Modifier.fillMaxSize()) {
                        EchoNavGraph(
                            navController = navController,
                            serverPreferences = serverPreferences,
                            sessionPreferences = sessionPreferences
                        )

                        // MiniPlayer at the bottom
                        MiniPlayer(
                            state = MiniPlayerState(
                                isVisible = showMiniPlayer,
                                isPlaying = playerState.isPlaying,
                                trackTitle = playerState.currentTrack?.title ?: "",
                                artistName = playerState.currentTrack?.artist ?: "",
                                coverUrl = playerState.currentTrack?.coverUrl,
                                progress = playerState.progress
                            ),
                            onPlayerClick = {
                                navController.navigate(EchoDestinations.PLAYER)
                            },
                            onPlayPauseClick = {
                                echoPlayer.togglePlayPause()
                            },
                            onNextClick = {
                                echoPlayer.seekToNext()
                            },
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .navigationBarsPadding()
                        )
                    }
                }
            }
        }
    }
}
