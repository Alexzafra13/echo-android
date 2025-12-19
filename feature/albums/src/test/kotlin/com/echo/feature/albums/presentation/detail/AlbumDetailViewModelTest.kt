package com.echo.feature.albums.presentation.detail

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.echo.core.media.player.EchoPlayer
import com.echo.core.media.usecase.PlayTracksUseCase
import com.echo.feature.albums.data.repository.AlbumsRepository
import com.echo.feature.albums.domain.model.Album
import com.echo.feature.albums.domain.model.AlbumWithTracks
import com.echo.feature.albums.domain.model.Track
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
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
class AlbumDetailViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var albumsRepository: AlbumsRepository
    private lateinit var playTracksUseCase: PlayTracksUseCase
    private lateinit var player: EchoPlayer
    private lateinit var savedStateHandle: SavedStateHandle

    private val testAlbumId = "album123"

    private val testAlbum = Album(
        id = testAlbumId,
        title = "Test Album",
        artist = "Test Artist",
        artistId = "artist1",
        year = 2024,
        totalTracks = 2,
        duration = 600,
        coverUrl = "https://example.com/cover.jpg"
    )

    private val testTracks = listOf(
        Track(
            id = "track1",
            title = "Song One",
            artistId = "artist1",
            artistName = "Test Artist",
            albumId = testAlbumId,
            albumName = "Test Album",
            trackNumber = 1,
            duration = 180,
            coverUrl = "https://example.com/cover.jpg"
        ),
        Track(
            id = "track2",
            title = "Song Two",
            artistId = "artist1",
            artistName = "Test Artist",
            albumId = testAlbumId,
            albumName = "Test Album",
            trackNumber = 2,
            duration = 420,
            coverUrl = "https://example.com/cover.jpg"
        )
    )

    private val testAlbumWithTracks = AlbumWithTracks(
        album = testAlbum,
        tracks = testTracks
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        albumsRepository = mockk(relaxed = true)
        playTracksUseCase = mockk(relaxed = true)
        player = mockk(relaxed = true)
        savedStateHandle = SavedStateHandle(mapOf("albumId" to testAlbumId))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): AlbumDetailViewModel {
        return AlbumDetailViewModel(
            albumsRepository = albumsRepository,
            playTracksUseCase = playTracksUseCase,
            player = player,
            savedStateHandle = savedStateHandle
        )
    }

    @Test
    fun `initial state is loading`() = runTest {
        // Given
        coEvery { albumsRepository.getAlbumWithTracks(testAlbumId) } returns Result.success(testAlbumWithTracks)

        // When
        val viewModel = createViewModel()

        // Then
        viewModel.state.test {
            val initialState = awaitItem()
            assertTrue(initialState.isLoading)
        }
    }

    @Test
    fun `loadAlbumDetails updates state with album and tracks on success`() = runTest {
        // Given
        coEvery { albumsRepository.getAlbumWithTracks(testAlbumId) } returns Result.success(testAlbumWithTracks)

        // When
        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        viewModel.state.test {
            val state = awaitItem()
            assertFalse(state.isLoading)
            assertEquals(testAlbum, state.album)
            assertEquals(testTracks, state.tracks)
            assertNull(state.error)
        }
    }

    @Test
    fun `loadAlbumDetails sets error state on failure`() = runTest {
        // Given
        val errorMessage = "Album not found"
        coEvery { albumsRepository.getAlbumWithTracks(testAlbumId) } returns Result.failure(Exception(errorMessage))

        // When
        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        viewModel.state.test {
            val state = awaitItem()
            assertFalse(state.isLoading)
            assertNull(state.album)
            assertTrue(state.tracks.isEmpty())
            assertEquals(errorMessage, state.error)
        }
    }

    @Test
    fun `playAlbum calls playTracksUseCase with all tracks`() = runTest {
        // Given
        coEvery { albumsRepository.getAlbumWithTracks(testAlbumId) } returns Result.success(testAlbumWithTracks)
        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.playAlbum()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        coVerify { playTracksUseCase.playTracks(any(), 0) }
    }

    @Test
    fun `playTrack calls playTracksUseCase with correct start index`() = runTest {
        // Given
        coEvery { albumsRepository.getAlbumWithTracks(testAlbumId) } returns Result.success(testAlbumWithTracks)
        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // When - Play second track
        viewModel.playTrack(testTracks[1])
        testDispatcher.scheduler.advanceUntilIdle()

        // Then - Should start at index 1
        coVerify { playTracksUseCase.playTracks(any(), 1) }
    }

    @Test
    fun `shuffleAlbum enables shuffle mode and plays tracks`() = runTest {
        // Given
        coEvery { albumsRepository.getAlbumWithTracks(testAlbumId) } returns Result.success(testAlbumWithTracks)
        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.shuffleAlbum()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        coVerify { player.setShuffleEnabled(true) }
        coVerify { playTracksUseCase.playTracks(any(), 0) }
    }

    @Test
    fun `playAlbum does nothing when album is null`() = runTest {
        // Given
        coEvery { albumsRepository.getAlbumWithTracks(testAlbumId) } returns Result.failure(Exception("Error"))
        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.playAlbum()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then - playTracksUseCase should not be called
        coVerify(exactly = 0) { playTracksUseCase.playTracks(any(), any()) }
    }

    @Test
    fun `playAlbum does nothing when tracks are empty`() = runTest {
        // Given
        val emptyAlbum = AlbumWithTracks(album = testAlbum, tracks = emptyList())
        coEvery { albumsRepository.getAlbumWithTracks(testAlbumId) } returns Result.success(emptyAlbum)
        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.playAlbum()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then - playTracksUseCase should not be called
        coVerify(exactly = 0) { playTracksUseCase.playTracks(any(), any()) }
    }
}
