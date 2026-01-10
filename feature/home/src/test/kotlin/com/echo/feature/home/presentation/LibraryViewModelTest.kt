package com.echo.feature.home.presentation

import app.cash.turbine.test
import com.echo.core.media.usecase.PlayTracksUseCase
import com.echo.feature.albums.data.repository.AlbumsRepository
import com.echo.feature.albums.domain.model.Album
import com.echo.feature.albums.domain.model.AlbumWithTracks
import com.echo.feature.albums.domain.model.Track
import com.echo.feature.artists.data.repository.ArtistsRepository
import com.echo.feature.artists.domain.model.Artist
import com.echo.feature.playlists.data.repository.PlaylistsRepository
import com.echo.feature.playlists.domain.model.Playlist
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LibraryViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var albumsRepository: AlbumsRepository
    private lateinit var artistsRepository: ArtistsRepository
    private lateinit var playlistsRepository: PlaylistsRepository
    private lateinit var playTracksUseCase: PlayTracksUseCase
    private lateinit var viewModel: LibraryViewModel

    private val testAlbums = listOf(
        Album(
            id = "album1",
            title = "Test Album",
            artist = "Test Artist",
            artistId = "artist1",
            year = 2024,
            totalTracks = 10,
            duration = 3600,
            coverUrl = "https://example.com/cover.jpg"
        ),
        Album(
            id = "album2",
            title = "Another Album",
            artist = "Another Artist",
            artistId = "artist2",
            year = 2023,
            totalTracks = 8,
            duration = 2800,
            coverUrl = null
        )
    )

    private val testArtists = listOf(
        Artist(
            id = "artist1",
            name = "Zebra Band",
            albumCount = 5,
            trackCount = 50,
            imageUrl = "https://example.com/artist1.jpg"
        ),
        Artist(
            id = "artist2",
            name = "Alpha Artist",
            albumCount = 3,
            trackCount = 30,
            imageUrl = null
        )
    )

    private val testPlaylists = listOf(
        Playlist(
            id = "playlist1",
            name = "My Favorites",
            description = "Best songs",
            trackCount = 25,
            duration = 5400,
            isPublic = false,
            coverUrl = "https://example.com/playlist.jpg",
            createdAt = "2024-01-01",
            updatedAt = "2024-01-15"
        ),
        Playlist(
            id = "playlist2",
            name = "Workout Mix",
            description = null,
            trackCount = 15,
            duration = 3600,
            isPublic = true,
            coverUrl = null,
            createdAt = "2024-01-10",
            updatedAt = "2024-01-10"
        )
    )

    private val testTracks = listOf(
        Track(
            id = "track1",
            title = "Song One",
            artistName = "Test Artist",
            duration = 240,
            trackNumber = 1
        ),
        Track(
            id = "track2",
            title = "Song Two",
            artistName = "Test Artist",
            duration = 300,
            trackNumber = 2
        )
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        albumsRepository = mockk(relaxed = true)
        artistsRepository = mockk(relaxed = true)
        playlistsRepository = mockk(relaxed = true)
        playTracksUseCase = mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state shows loading`() = runTest {
        // Given
        coEvery { albumsRepository.getAlbums(any(), any()) } returns Result.success(testAlbums)
        coEvery { artistsRepository.getArtists(any(), any()) } returns Result.success(testArtists)
        coEvery { playlistsRepository.getPlaylists() } returns Result.success(testPlaylists)

        // When
        viewModel = LibraryViewModel(albumsRepository, artistsRepository, playlistsRepository, playTracksUseCase)

        // Then
        viewModel.state.test {
            val initialState = awaitItem()
            assertTrue(initialState.isLoading)
        }
    }

    @Test
    fun `loadAlbums updates state with albums on success`() = runTest {
        // Given
        coEvery { albumsRepository.getAlbums(any(), any()) } returns Result.success(testAlbums)
        coEvery { artistsRepository.getArtists(any(), any()) } returns Result.success(testArtists)
        coEvery { playlistsRepository.getPlaylists() } returns Result.success(testPlaylists)

        // When
        viewModel = LibraryViewModel(albumsRepository, artistsRepository, playlistsRepository, playTracksUseCase)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        viewModel.state.test {
            val state = awaitItem()
            assertFalse(state.isLoading)
            assertEquals(testAlbums, state.albums)
            assertNull(state.error)
        }
    }

    @Test
    fun `loadAlbums sets error state on failure`() = runTest {
        // Given
        val errorMessage = "Failed to load albums"
        coEvery { albumsRepository.getAlbums(any(), any()) } returns Result.failure(Exception(errorMessage))
        coEvery { artistsRepository.getArtists(any(), any()) } returns Result.success(testArtists)
        coEvery { playlistsRepository.getPlaylists() } returns Result.success(testPlaylists)

        // When
        viewModel = LibraryViewModel(albumsRepository, artistsRepository, playlistsRepository, playTracksUseCase)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        viewModel.state.test {
            val state = awaitItem()
            assertFalse(state.isLoading)
            assertEquals(errorMessage, state.error)
        }
    }

    @Test
    fun `loadArtists sorts artists alphabetically`() = runTest {
        // Given
        coEvery { albumsRepository.getAlbums(any(), any()) } returns Result.success(testAlbums)
        coEvery { artistsRepository.getArtists(any(), any()) } returns Result.success(testArtists)
        coEvery { playlistsRepository.getPlaylists() } returns Result.success(testPlaylists)

        // When
        viewModel = LibraryViewModel(albumsRepository, artistsRepository, playlistsRepository, playTracksUseCase)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        viewModel.state.test {
            val state = awaitItem()
            assertFalse(state.isLoadingArtists)
            // Artists should be sorted: Alpha Artist comes before Zebra Band
            assertEquals("Alpha Artist", state.artists[0].name)
            assertEquals("Zebra Band", state.artists[1].name)
        }
    }

    @Test
    fun `loadPlaylists updates state with playlists`() = runTest {
        // Given
        coEvery { albumsRepository.getAlbums(any(), any()) } returns Result.success(testAlbums)
        coEvery { artistsRepository.getArtists(any(), any()) } returns Result.success(testArtists)
        coEvery { playlistsRepository.getPlaylists() } returns Result.success(testPlaylists)

        // When
        viewModel = LibraryViewModel(albumsRepository, artistsRepository, playlistsRepository, playTracksUseCase)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        viewModel.state.test {
            val state = awaitItem()
            assertFalse(state.isLoadingPlaylists)
            assertEquals(testPlaylists, state.playlists)
        }
    }

    @Test
    fun `playAlbum fetches tracks and plays them`() = runTest {
        // Given
        val albumWithTracks = AlbumWithTracks(
            album = testAlbums[0],
            tracks = testTracks
        )
        coEvery { albumsRepository.getAlbums(any(), any()) } returns Result.success(testAlbums)
        coEvery { artistsRepository.getArtists(any(), any()) } returns Result.success(testArtists)
        coEvery { playlistsRepository.getPlaylists() } returns Result.success(testPlaylists)
        coEvery { albumsRepository.getAlbumWithTracks(testAlbums[0].id) } returns Result.success(albumWithTracks)
        coEvery { playTracksUseCase.playTracks(any(), any()) } just Runs

        viewModel = LibraryViewModel(albumsRepository, artistsRepository, playlistsRepository, playTracksUseCase)
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.playAlbum(testAlbums[0])
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        coVerify { albumsRepository.getAlbumWithTracks(testAlbums[0].id) }
        coVerify { playTracksUseCase.playTracks(any(), 0) }
    }

    @Test
    fun `playAlbum does nothing if tracks are empty`() = runTest {
        // Given
        val albumWithNoTracks = AlbumWithTracks(
            album = testAlbums[0],
            tracks = emptyList()
        )
        coEvery { albumsRepository.getAlbums(any(), any()) } returns Result.success(testAlbums)
        coEvery { artistsRepository.getArtists(any(), any()) } returns Result.success(testArtists)
        coEvery { playlistsRepository.getPlaylists() } returns Result.success(testPlaylists)
        coEvery { albumsRepository.getAlbumWithTracks(testAlbums[0].id) } returns Result.success(albumWithNoTracks)

        viewModel = LibraryViewModel(albumsRepository, artistsRepository, playlistsRepository, playTracksUseCase)
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.playAlbum(testAlbums[0])
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        coVerify(exactly = 0) { playTracksUseCase.playTracks(any(), any()) }
    }

    @Test
    fun `refresh reloads all data`() = runTest {
        // Given
        coEvery { albumsRepository.getAlbums(any(), any()) } returns Result.success(testAlbums)
        coEvery { artistsRepository.getArtists(any(), any()) } returns Result.success(testArtists)
        coEvery { playlistsRepository.getPlaylists() } returns Result.success(testPlaylists)

        viewModel = LibraryViewModel(albumsRepository, artistsRepository, playlistsRepository, playTracksUseCase)
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.refresh()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then - each repository should be called twice (init + refresh)
        coVerify(exactly = 2) { albumsRepository.getAlbums(skip = 0, take = 100) }
        coVerify(exactly = 2) { artistsRepository.getArtists(skip = 0, take = 100) }
        coVerify(exactly = 2) { playlistsRepository.getPlaylists() }
    }

    @Test
    fun `showCreatePlaylistDialog sets dialog visible`() = runTest {
        // Given
        coEvery { albumsRepository.getAlbums(any(), any()) } returns Result.success(testAlbums)
        coEvery { artistsRepository.getArtists(any(), any()) } returns Result.success(testArtists)
        coEvery { playlistsRepository.getPlaylists() } returns Result.success(testPlaylists)

        viewModel = LibraryViewModel(albumsRepository, artistsRepository, playlistsRepository, playTracksUseCase)
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.showCreatePlaylistDialog()

        // Then
        viewModel.state.test {
            val state = awaitItem()
            assertTrue(state.showCreatePlaylistDialog)
            assertNull(state.createPlaylistError)
        }
    }

    @Test
    fun `hideCreatePlaylistDialog hides dialog and clears state`() = runTest {
        // Given
        coEvery { albumsRepository.getAlbums(any(), any()) } returns Result.success(testAlbums)
        coEvery { artistsRepository.getArtists(any(), any()) } returns Result.success(testArtists)
        coEvery { playlistsRepository.getPlaylists() } returns Result.success(testPlaylists)

        viewModel = LibraryViewModel(albumsRepository, artistsRepository, playlistsRepository, playTracksUseCase)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.showCreatePlaylistDialog()

        // When
        viewModel.hideCreatePlaylistDialog()

        // Then
        viewModel.state.test {
            val state = awaitItem()
            assertFalse(state.showCreatePlaylistDialog)
            assertFalse(state.isCreatingPlaylist)
            assertNull(state.createPlaylistError)
        }
    }

    @Test
    fun `createPlaylist adds new playlist to list on success`() = runTest {
        // Given
        val newPlaylist = Playlist(
            id = "playlist3",
            name = "New Playlist",
            description = "My new playlist",
            trackCount = 0,
            duration = 0,
            coverUrl = null,
            isPublic = false,
            createdAt = "2024-01-20",
            updatedAt = "2024-01-20"
        )
        coEvery { albumsRepository.getAlbums(any(), any()) } returns Result.success(testAlbums)
        coEvery { artistsRepository.getArtists(any(), any()) } returns Result.success(testArtists)
        coEvery { playlistsRepository.getPlaylists() } returns Result.success(testPlaylists)
        coEvery { playlistsRepository.createPlaylist("New Playlist", "My new playlist") } returns Result.success(newPlaylist)

        viewModel = LibraryViewModel(albumsRepository, artistsRepository, playlistsRepository, playTracksUseCase)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.showCreatePlaylistDialog()

        // When
        viewModel.createPlaylist("New Playlist", "My new playlist")
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        viewModel.state.test {
            val state = awaitItem()
            assertFalse(state.showCreatePlaylistDialog)
            assertFalse(state.isCreatingPlaylist)
            assertNull(state.createPlaylistError)
            // New playlist should be at the beginning
            assertEquals("playlist3", state.playlists[0].id)
            assertEquals(3, state.playlists.size)
        }
    }

    @Test
    fun `createPlaylist sets error on failure`() = runTest {
        // Given
        coEvery { albumsRepository.getAlbums(any(), any()) } returns Result.success(testAlbums)
        coEvery { artistsRepository.getArtists(any(), any()) } returns Result.success(testArtists)
        coEvery { playlistsRepository.getPlaylists() } returns Result.success(testPlaylists)
        coEvery { playlistsRepository.createPlaylist(any(), any()) } returns
            Result.failure(Exception("Name already exists"))

        viewModel = LibraryViewModel(albumsRepository, artistsRepository, playlistsRepository, playTracksUseCase)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.showCreatePlaylistDialog()

        // When
        viewModel.createPlaylist("New Playlist", null)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        viewModel.state.test {
            val state = awaitItem()
            assertTrue(state.showCreatePlaylistDialog) // Dialog stays open on error
            assertFalse(state.isCreatingPlaylist)
            assertEquals("Name already exists", state.createPlaylistError)
        }
    }

    @Test
    fun `createPlaylist shows loading state while creating`() = runTest {
        // Given
        coEvery { albumsRepository.getAlbums(any(), any()) } returns Result.success(testAlbums)
        coEvery { artistsRepository.getArtists(any(), any()) } returns Result.success(testArtists)
        coEvery { playlistsRepository.getPlaylists() } returns Result.success(testPlaylists)
        coEvery { playlistsRepository.createPlaylist(any(), any()) } returns
            Result.success(testPlaylists[0])

        viewModel = LibraryViewModel(albumsRepository, artistsRepository, playlistsRepository, playTracksUseCase)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.showCreatePlaylistDialog()

        // When
        viewModel.createPlaylist("Test", null)

        // Then - before advancing, should be loading
        viewModel.state.test {
            val state = awaitItem()
            assertTrue(state.isCreatingPlaylist)
        }
    }
}
