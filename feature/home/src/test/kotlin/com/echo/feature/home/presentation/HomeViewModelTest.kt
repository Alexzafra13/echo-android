package com.echo.feature.home.presentation

import app.cash.turbine.test
import com.echo.core.media.usecase.PlayTracksUseCase
import com.echo.feature.albums.data.repository.AlbumsRepository
import com.echo.feature.albums.domain.model.Album
import io.mockk.coEvery
import io.mockk.coVerify
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
class HomeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var albumsRepository: AlbumsRepository
    private lateinit var playTracksUseCase: PlayTracksUseCase
    private lateinit var viewModel: HomeViewModel

    private val testAlbum = Album(
        id = "1",
        title = "Test Album",
        artist = "Test Artist",
        artistId = "artist1",
        year = 2024,
        totalTracks = 10,
        duration = 3600,
        coverUrl = "https://example.com/cover.jpg"
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        albumsRepository = mockk(relaxed = true)
        playTracksUseCase = mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is loading`() = runTest {
        // Given
        coEvery { albumsRepository.getFeaturedAlbum() } returns Result.success(testAlbum)
        coEvery { albumsRepository.getRecentAlbums(any()) } returns Result.success(listOf(testAlbum))
        coEvery { albumsRepository.getTopPlayedAlbums(any()) } returns Result.success(emptyList())
        coEvery { albumsRepository.getRecentlyPlayedAlbums(any()) } returns Result.success(emptyList())

        // When
        viewModel = HomeViewModel(albumsRepository, playTracksUseCase)

        // Then
        viewModel.state.test {
            val initialState = awaitItem()
            assertTrue(initialState.isLoading)
        }
    }

    @Test
    fun `loadHomeData updates state with albums on success`() = runTest {
        // Given
        val albums = listOf(testAlbum)
        coEvery { albumsRepository.getFeaturedAlbum() } returns Result.success(testAlbum)
        coEvery { albumsRepository.getRecentAlbums(any()) } returns Result.success(albums)
        coEvery { albumsRepository.getTopPlayedAlbums(any()) } returns Result.success(albums)
        coEvery { albumsRepository.getRecentlyPlayedAlbums(any()) } returns Result.success(albums)

        // When
        viewModel = HomeViewModel(albumsRepository, playTracksUseCase)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        viewModel.state.test {
            val state = awaitItem()
            assertFalse(state.isLoading)
            assertEquals(testAlbum, state.featuredAlbum)
            assertEquals(albums, state.recentAlbums)
            assertEquals(albums, state.topPlayedAlbums)
            assertEquals(albums, state.recentlyPlayedAlbums)
            assertNull(state.error)
        }
    }

    @Test
    fun `loadHomeData sets error state on failure`() = runTest {
        // Given
        val errorMessage = "Network error"
        coEvery { albumsRepository.getFeaturedAlbum() } returns Result.failure(Exception(errorMessage))
        coEvery { albumsRepository.getRecentAlbums(any()) } returns Result.failure(Exception(errorMessage))
        coEvery { albumsRepository.getTopPlayedAlbums(any()) } returns Result.failure(Exception(errorMessage))
        coEvery { albumsRepository.getRecentlyPlayedAlbums(any()) } returns Result.failure(Exception(errorMessage))

        // When
        viewModel = HomeViewModel(albumsRepository, playTracksUseCase)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        viewModel.state.test {
            val state = awaitItem()
            assertFalse(state.isLoading)
            assertTrue(state.error != null)
        }
    }

    @Test
    fun `refresh calls loadHomeData`() = runTest {
        // Given
        coEvery { albumsRepository.getFeaturedAlbum() } returns Result.success(testAlbum)
        coEvery { albumsRepository.getRecentAlbums(any()) } returns Result.success(listOf(testAlbum))
        coEvery { albumsRepository.getTopPlayedAlbums(any()) } returns Result.success(emptyList())
        coEvery { albumsRepository.getRecentlyPlayedAlbums(any()) } returns Result.success(emptyList())

        viewModel = HomeViewModel(albumsRepository, playTracksUseCase)
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.refresh()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then - Should call repository methods twice (init + refresh)
        coVerify(exactly = 2) { albumsRepository.getFeaturedAlbum() }
        coVerify(exactly = 2) { albumsRepository.getRecentAlbums(any()) }
    }
}
