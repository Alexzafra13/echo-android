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
import com.echo.feature.home.presentation.LibraryScreen
import com.echo.feature.home.presentation.RadioScreen
import com.echo.feature.home.presentation.SocialScreen
import com.echo.feature.player.presentation.PlayerScreen
import com.echo.feature.search.presentation.SearchScreen
import com.echo.feature.server.presentation.addserver.AddServerScreen
import com.echo.feature.server.presentation.welcome.WelcomeScreen
import com.echo.feature.settings.presentation.AdminScreen
import com.echo.feature.settings.presentation.SettingsScreen

object EchoDestinations {
    const val WELCOME = "welcome"
    const val ADD_SERVER = "add_server"
    const val LOGIN = "login"
    const val FIRST_LOGIN = "first_login"
    const val HOME = "home"
    const val SEARCH = "search"
    const val LIBRARY = "library"
    const val RADIO = "radio"
    const val SOCIAL = "social"
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
    const val ADMIN = "admin"
    const val NOTIFICATIONS = "notifications"
}

@Composable
fun EchoNavGraph(
    navController: NavHostController,
    serverPreferences: ServerPreferences,
    sessionPreferences: SessionPreferences
) {
    val activeServer by serverPreferences.activeServer.collectAsState(initial = null)
    val session by sessionPreferences.session.collectAsState()

    // Determine start destination based on auth state
    val startDestination = when {
        activeServer == null -> EchoDestinations.WELCOME
        session == null -> EchoDestinations.WELCOME
        else -> EchoDestinations.HOME
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
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

        // Search
        composable(EchoDestinations.SEARCH) {
            SearchScreen(
                onNavigateToAlbum = { albumId ->
                    navController.navigate("${EchoDestinations.ALBUM_DETAIL}/$albumId")
                },
                onNavigateToArtist = { artistId ->
                    navController.navigate("${EchoDestinations.ARTIST_DETAIL}/$artistId")
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // Library
        composable(EchoDestinations.LIBRARY) {
            LibraryScreen(
                onNavigateToAlbum = { albumId ->
                    navController.navigate("${EchoDestinations.ALBUM_DETAIL}/$albumId")
                },
                onNavigateToArtist = { artistId ->
                    navController.navigate("${EchoDestinations.ARTIST_DETAIL}/$artistId")
                },
                onNavigateToPlaylist = { playlistId ->
                    navController.navigate("${EchoDestinations.PLAYLIST_DETAIL}/$playlistId")
                }
            )
        }

        // Radio
        composable(EchoDestinations.RADIO) {
            RadioScreen()
        }

        // Social
        composable(EchoDestinations.SOCIAL) {
            SocialScreen()
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

        // Settings/Profile
        composable(EchoDestinations.PROFILE) {
            SettingsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onLogout = {
                    navController.navigate(EchoDestinations.WELCOME) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onNavigateToAdmin = {
                    navController.navigate(EchoDestinations.ADMIN)
                }
            )
        }

        // Admin Panel
        composable(EchoDestinations.ADMIN) {
            AdminScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // TODO: Add remaining destinations (artists, queue, search, etc.)
    }
}
