package com.echo.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.echo.core.datastore.preferences.ServerPreferences
import com.echo.core.datastore.preferences.SessionPreferences
import com.echo.feature.albums.presentation.detail.AlbumDetailScreen
import com.echo.feature.auth.presentation.login.LoginScreen
import com.echo.feature.home.presentation.HomeScreen
import com.echo.feature.player.presentation.PlayerScreen
import com.echo.feature.server.presentation.addserver.AddServerScreen
import com.echo.feature.server.presentation.welcome.WelcomeScreen

object EchoDestinations {
    const val WELCOME = "welcome"
    const val ADD_SERVER = "add_server"
    const val LOGIN = "login"
    const val FIRST_LOGIN = "first_login"
    const val HOME = "home"
    const val SEARCH = "search"
    const val LIBRARY = "library"
    const val ALBUMS = "albums"
    const val ALBUM_DETAIL = "album"
    const val ARTISTS = "artists"
    const val ARTIST_DETAIL = "artist"
    const val PLAYLISTS = "playlists"
    const val PLAYLIST_DETAIL = "playlist"
    const val PLAYER = "player"
    const val QUEUE = "queue"
    const val PROFILE = "profile"
    const val SETTINGS = "settings"
}

@Composable
fun EchoNavGraph(
    navController: NavHostController,
    serverPreferences: ServerPreferences,
    sessionPreferences: SessionPreferences
) {
    val activeServer by serverPreferences.activeServer.collectAsState(initial = null)
    val session by sessionPreferences.session.collectAsState()

    // Determine start destination
    val startDestination = when {
        activeServer == null -> EchoDestinations.WELCOME
        session == null -> "${EchoDestinations.LOGIN}/{serverId}"
        session?.mustChangePassword == true -> EchoDestinations.FIRST_LOGIN
        else -> EchoDestinations.HOME
    }

    NavHost(
        navController = navController,
        startDestination = if (activeServer == null) {
            EchoDestinations.WELCOME
        } else if (session == null) {
            EchoDestinations.WELCOME // Will navigate to login
        } else {
            EchoDestinations.HOME
        }
    ) {
        // Welcome - Server selection
        composable(EchoDestinations.WELCOME) {
            WelcomeScreen(
                onAddServer = {
                    navController.navigate(EchoDestinations.ADD_SERVER)
                },
                onSelectServer = { serverId ->
                    navController.navigate("${EchoDestinations.LOGIN}/$serverId")
                }
            )
        }

        // Add Server
        composable(EchoDestinations.ADD_SERVER) {
            AddServerScreen(
                onServerAdded = { serverId ->
                    navController.navigate("${EchoDestinations.LOGIN}/$serverId") {
                        popUpTo(EchoDestinations.WELCOME)
                    }
                },
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        // Login
        composable(
            route = "${EchoDestinations.LOGIN}/{serverId}",
            arguments = listOf(
                navArgument("serverId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val serverId = backStackEntry.arguments?.getString("serverId") ?: ""
            LoginScreen(
                serverId = serverId,
                onLoginSuccess = { mustChangePassword ->
                    if (mustChangePassword) {
                        navController.navigate(EchoDestinations.FIRST_LOGIN) {
                            popUpTo(EchoDestinations.WELCOME) { inclusive = true }
                        }
                    } else {
                        navController.navigate(EchoDestinations.HOME) {
                            popUpTo(EchoDestinations.WELCOME) { inclusive = true }
                        }
                    }
                },
                onChangeServer = {
                    navController.navigate(EchoDestinations.WELCOME) {
                        popUpTo(EchoDestinations.WELCOME) { inclusive = true }
                    }
                }
            )
        }

        // First Login (change password)
        composable(EchoDestinations.FIRST_LOGIN) {
            // TODO: Implement FirstLoginScreen
            HomeScreen(
                onNavigateToAlbum = {},
                onNavigateToSearch = {},
                onNavigateToProfile = {}
            )
        }

        // Home
        composable(EchoDestinations.HOME) {
            HomeScreen(
                onNavigateToAlbum = { albumId ->
                    navController.navigate("${EchoDestinations.ALBUM_DETAIL}/$albumId")
                },
                onNavigateToSearch = {
                    navController.navigate(EchoDestinations.SEARCH)
                },
                onNavigateToProfile = {
                    navController.navigate(EchoDestinations.PROFILE)
                }
            )
        }

        // Album Detail
        composable(
            route = "${EchoDestinations.ALBUM_DETAIL}/{albumId}",
            arguments = listOf(
                navArgument("albumId") { type = NavType.StringType }
            )
        ) {
            AlbumDetailScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToArtist = { artistId ->
                    navController.navigate("${EchoDestinations.ARTIST_DETAIL}/$artistId")
                }
            )
        }

        // Player
        composable(EchoDestinations.PLAYER) {
            PlayerScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToQueue = {
                    navController.navigate(EchoDestinations.QUEUE)
                }
            )
        }

        // TODO: Add remaining destinations (artists, queue, search, etc.)
    }
}
