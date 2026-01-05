package com.echo.feature.artists.presentation.detail

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.echo.feature.artists.data.repository.ArtistsRepository
import com.echo.feature.artists.domain.model.Artist
import com.echo.feature.artists.domain.model.ArtistAlbum
import com.echo.feature.artists.domain.model.ArtistTopTrack
import com.echo.feature.artists.domain.model.ArtistWithAlbums
import com.echo.feature.artists.domain.model.RelatedArtist
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ArtistDetailViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var artistsRepository: ArtistsRepository
    private lateinit var savedStateHandle: SavedStateHandle

    private val testArtist = Artist(
        id = "artist-1",
        name = "Test Artist",
        imageUrl = "https://example.com/artist.jpg",
        backgroundUrl = "https://example.com/bg.jpg",
        biography = "A great artist",
        albumCount = 5,
        trackCount = 50,
        playCount = 10000,
        listenerCount = 5000
    )

    private val testAlbums = listOf(
        ArtistAlbum(
            id = "album-1",
            title = "First Album",
            artistId = "artist-1",
            artistName = "Test Artist",
            year = 2020,
            trackCount = 10,
            duration = 3600,
            coverUrl = "https://example.com/album1.jpg"
        ),
        ArtistAlbum(
            id = "album-2",
            title = "Second Album",
            artistId = "artist-1",
            artistName = "Test Artist",
            year = 2023,
            trackCount = 12,
            duration = 4000,
            coverUrl = "https://example.com/album2.jpg"
        )
    )

    private val testTopTracks = listOf(
        ArtistTopTrack(
            trackId = "track-1",
            title = "Hit Song",
            albumId = "album-1",
            albumName = "First Album",
            duration = 200,
            playCount = 5000,
            uniqueListeners = 2000
        )
    )

    private val testRelatedArtists = listOf(
        RelatedArtist(
            id = "related-1",
            name = "Similar Artist",
            albumCount = 3,
            songCount = 30,
            matchScore = 85
        )
    )

    private val testArtistWithAlbums = ArtistWithAlbums(
        artist = testArtist,
        albums = testAlbums,
        topTracks = testTopTracks,
        relatedArtists = testRelatedArtists
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        artistsRepository = mockk(relaxed = true)
        savedStateHandle = SavedStateHandle(mapOf("artistId" to "artist-1"))

        coEvery { artistsRepository.getArtistWithAlbums(any()) } returns Result.success(testArtistWithAlbums)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): ArtistDetailViewModel {
        return ArtistDetailViewModel(
            artistsRepository = artistsRepository,
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
    fun `loadArtistDetails loads artist successfully`() = runTest {
        // Given
        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        viewModel.state.test {
            val state = awaitItem()
            assertFalse(state.isLoading)
            assertEquals(testArtist, state.artist)
            assertNull(state.error)
        }
    }

    @Test
    fun `loadArtistDetails loads albums sorted by year descending`() = runTest {
        // Given
        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        viewModel.state.test {
            val state = awaitItem()
            assertEquals(2, state.albums.size)
            // 2023 should come before 2020
            assertEquals(2023, state.albums[0].year)
            assertEquals(2020, state.albums[1].year)
        }
    }

    @Test
    fun `loadArtistDetails loads top tracks`() = runTest {
        // Given
        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        viewModel.state.test {
            val state = awaitItem()
            assertEquals(1, state.topTracks.size)
            assertEquals("Hit Song", state.topTracks[0].title)
        }
    }

    @Test
    fun `loadArtistDetails loads related artists`() = runTest {
        // Given
        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        viewModel.state.test {
            val state = awaitItem()
            assertEquals(1, state.relatedArtists.size)
            assertEquals("Similar Artist", state.relatedArtists[0].name)
        }
    }

    @Test
    fun `loadArtistDetails handles error`() = runTest {
        // Given
        coEvery { artistsRepository.getArtistWithAlbums(any()) } returns Result.failure(Exception("Network error"))
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
    fun `refresh reloads artist details`() = runTest {
        // Given
        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.refresh()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        coVerify(exactly = 2) { artistsRepository.getArtistWithAlbums("artist-1") }
    }

    @Test
    fun `state preserves data after refresh`() = runTest {
        // Given
        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.refresh()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        viewModel.state.test {
            val state = awaitItem()
            assertFalse(state.isLoading)
            assertEquals(testArtist, state.artist)
            assertEquals(2, state.albums.size)
        }
    }

    @Test
    fun `loadArtistDetails handles empty albums`() = runTest {
        // Given
        val emptyAlbumsData = testArtistWithAlbums.copy(albums = emptyList())
        coEvery { artistsRepository.getArtistWithAlbums(any()) } returns Result.success(emptyAlbumsData)
        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        viewModel.state.test {
            val state = awaitItem()
            assertFalse(state.isLoading)
            assertEquals(testArtist, state.artist)
            assertTrue(state.albums.isEmpty())
        }
    }
}
