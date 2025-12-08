package com.echo.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.echo.app.navigation.EchoDestinations
import com.echo.app.navigation.EchoNavGraph
import com.echo.core.datastore.preferences.ServerPreferences
import com.echo.core.datastore.preferences.SessionPreferences
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            EchoTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    val session by sessionPreferences.session.collectAsState()

                    // Show MiniPlayer only when logged in
                    val showMiniPlayer = session != null

                    Box(modifier = Modifier.fillMaxSize()) {
                        EchoNavGraph(
                            navController = navController,
                            serverPreferences = serverPreferences,
                            sessionPreferences = sessionPreferences
                        )

                        // MiniPlayer at the bottom
                        MiniPlayer(
                            state = MiniPlayerState(
                                isVisible = false, // TODO: Connect to actual player state
                                isPlaying = false,
                                trackTitle = "",
                                artistName = "",
                                coverUrl = null,
                                progress = 0f
                            ),
                            onPlayerClick = {
                                navController.navigate(EchoDestinations.PLAYER)
                            },
                            onPlayPauseClick = {
                                // TODO: Toggle playback
                            },
                            onNextClick = {
                                // TODO: Skip to next track
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
