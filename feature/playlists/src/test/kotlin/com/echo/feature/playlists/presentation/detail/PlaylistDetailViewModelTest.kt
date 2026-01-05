package com.echo.feature.playlists.presentation.detail

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.echo.core.media.player.EchoPlayer
import com.echo.core.media.usecase.PlayTracksUseCase
import com.echo.feature.playlists.data.repository.PlaylistsRepository
import com.echo.feature.playlists.domain.model.Playlist
import com.echo.feature.playlists.domain.model.PlaylistTrack
import com.echo.feature.playlists.domain.model.PlaylistWithTracks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PlaylistDetailViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var playlistsRepository: PlaylistsRepository
    private lateinit var playTracksUseCase: PlayTracksUseCase
    private lateinit var player: EchoPlayer
    private lateinit var savedStateHandle: SavedStateHandle

    private val testPlaylist = Playlist(
        id = "playlist-1",
        name = "Test Playlist",
        description = "A test playlist",
        trackCount = 3,
        duration = 600,
        isPublic = true
    )

    private val testTracks = listOf(
        PlaylistTrack(
            id = "pt-1",
            trackId = "track-1",
            title = "Song 1",
            artistName = "Artist 1",
            albumTitle = "Album 1",
            albumId = "album-1",
            duration = 180,
            trackNumber = 1,
            coverUrl = "http://cover1.jpg",
            order = 0
        ),
        PlaylistTrack(
            id = "pt-2",
            trackId = "track-2",
            title = "Song 2",
            artistName = "Artist 2",
            albumTitle = "Album 2",
            albumId = "album-2",
            duration = 200,
            trackNumber = 2,
            coverUrl = "http://cover2.jpg",
            order = 1
        ),
        PlaylistTrack(
            id = "pt-3",
            trackId = "track-3",
            title = "Song 3",
            artistName = "Artist 3",
            albumTitle = "Album 3",
            albumId = "album-3",
            duration = 220,
            trackNumber = 3,
            coverUrl = "http://cover3.jpg",
            order = 2
        )
    )

    private val testPlaylistWithTracks = PlaylistWithTracks(
        playlist = testPlaylist,
        tracks = testTracks
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        playlistsRepository = mockk(relaxed = true)
        playTracksUseCase = mockk(relaxed = true)
        player = mockk(relaxed = true)
        savedStateHandle = SavedStateHandle(mapOf("playlistId" to "playlist-1"))

        coEvery { playlistsRepository.getPlaylistWithTracks(any()) } returns Result.success(testPlaylistWithTracks)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): PlaylistDetailViewModel {
        return PlaylistDetailViewModel(
            playlistsRepository = playlistsRepository,
            playTracksUseCase = playTracksUseCase,
            player = player,
            savedStateHandle = savedStateHandle
        )
    }

    @Test
    fun `initial state is loading`() = runTest {
        // Given
        val viewModel = createViewModel()

        // Then
        viewModel.state.test {
            val initialState = awaitItem()
            assertTrue(initialState.isLoading)
        }
    }

    @Test
    fun `loadPlaylistDetails loads playlist and tracks successfully`() = runTest {
        // Given
        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        viewModel.state.test {
            val state = awaitItem()
            assertFalse(state.isLoading)
            assertEquals(testPlaylist, state.playlist)
            assertEquals(testTracks, state.tracks)
            assertNull(state.error)
        }
    }

    @Test
    fun `loadPlaylistDetails handles error`() = runTest {
        // Given
        coEvery { playlistsRepository.getPlaylistWithTracks(any()) } returns Result.failure(Exception("Network error"))
        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        viewModel.state.test {
            val state = awaitItem()
            assertFalse(state.isLoading)
            assertNotNull(state.error)
            assertTrue(state.error!!.contains("Network error"))
        }
    }

    @Test
    fun `playPlaylist calls playTracksUseCase with all tracks`() = runTest {
        // Given
        coEvery { playTracksUseCase.playTracks(any(), any()) } just runs
        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.playPlaylist()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        coVerify { playTracksUseCase.playTracks(any(), 0) }
    }

    @Test
    fun `playPlaylist does nothing when playlist is null`() = runTest {
        // Given
        coEvery { playlistsRepository.getPlaylistWithTracks(any()) } returns Result.failure(Exception("Error"))
        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.playPlaylist()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        coVerify(exactly = 0) { playTracksUseCase.playTracks(any(), any()) }
    }

    @Test
    fun `playPlaylist does nothing when tracks are empty`() = runTest {
        // Given
        val emptyPlaylist = PlaylistWithTracks(testPlaylist, emptyList())
        coEvery { playlistsRepository.getPlaylistWithTracks(any()) } returns Result.success(emptyPlaylist)
        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.playPlaylist()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        coVerify(exactly = 0) { playTracksUseCase.playTracks(any(), any()) }
    }

    @Test
    fun `playTrack plays from specific track index`() = runTest {
        // Given
        coEvery { playTracksUseCase.playTracks(any(), any()) } just runs
        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.playTrack(testTracks[1])
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        coVerify { playTracksUseCase.playTracks(any(), 1) }
    }

    @Test
    fun `shufflePlaylist enables shuffle and plays tracks`() = runTest {
        // Given
        coEvery { playTracksUseCase.playTracks(any(), any()) } just runs
        every { player.setShuffleEnabled(any()) } just runs
        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.shufflePlaylist()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        verify { player.setShuffleEnabled(true) }
        coVerify { playTracksUseCase.playTracks(any(), 0) }
    }

    @Test
    fun `removeTrack removes track from playlist`() = runTest {
        // Given
        coEvery { playlistsRepository.removeTrackFromPlaylist(any(), any()) } returns Result.success(Unit)
        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val trackToRemove = testTracks[0]

        // When
        viewModel.removeTrack(trackToRemove)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        coVerify { playlistsRepository.removeTrackFromPlaylist("playlist-1", "track-1") }

        viewModel.state.test {
            val state = awaitItem()
            assertFalse(state.tracks.any { it.id == trackToRemove.id })
            assertEquals(2, state.playlist?.trackCount)
        }
    }

    @Test
    fun `removeTrack handles error`() = runTest {
        // Given
        coEvery { playlistsRepository.removeTrackFromPlaylist(any(), any()) } returns Result.failure(Exception("Delete failed"))
        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.removeTrack(testTracks[0])
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        viewModel.state.test {
            val state = awaitItem()
            assertNotNull(state.error)
            // Tracks should not be modified
            assertEquals(3, state.tracks.size)
        }
    }

    @Test
    fun `refresh reloads playlist details`() = runTest {
        // Given
        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.refresh()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        coVerify(exactly = 2) { playlistsRepository.getPlaylistWithTracks("playlist-1") }
    }
}
