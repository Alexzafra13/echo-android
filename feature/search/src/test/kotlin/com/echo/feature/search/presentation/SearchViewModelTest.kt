package com.echo.feature.search.presentation

import app.cash.turbine.test
import com.echo.core.media.usecase.PlayTracksUseCase
import com.echo.feature.search.data.repository.SearchRepository
import com.echo.feature.search.domain.model.SearchAlbum
import com.echo.feature.search.domain.model.SearchArtist
import com.echo.feature.search.domain.model.SearchTrack
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var searchRepository: SearchRepository
    private lateinit var playTracksUseCase: PlayTracksUseCase
    private lateinit var viewModel: SearchViewModel

    private val testAlbum = SearchAlbum(
        id = "album1",
        title = "Test Album",
        artist = "Test Artist",
        artistId = "artist1",
        year = 2024,
        coverUrl = "https://example.com/cover.jpg"
    )

    private val testArtist = SearchArtist(
        id = "artist1",
        name = "Test Artist",
        albumCount = 5,
        trackCount = 50,
        imageUrl = "https://example.com/artist.jpg"
    )

    private val testTrack = SearchTrack(
        id = "track1",
        title = "Test Track",
        artistId = "artist1",
        artistName = "Test Artist",
        albumId = "album1",
        albumName = "Test Album",
        duration = 180,
        coverUrl = "https://example.com/cover.jpg"
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        searchRepository = mockk(relaxed = true)
        playTracksUseCase = mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is empty`() = runTest {
        viewModel = SearchViewModel(searchRepository, playTracksUseCase)

        viewModel.state.test {
            val state = awaitItem()
            assertEquals("", state.query)
            assertTrue(state.albums.isEmpty())
            assertTrue(state.artists.isEmpty())
            assertTrue(state.tracks.isEmpty())
            assertFalse(state.isLoading)
        }
    }

    @Test
    fun `short query clears results`() = runTest {
        coEvery { searchRepository.searchAll(any(), any()) } returns Triple(
            Result.success(listOf(testAlbum)),
            Result.success(listOf(testArtist)),
            Result.success(listOf(testTrack))
        )

        viewModel = SearchViewModel(searchRepository, playTracksUseCase)

        // First search with valid query
        viewModel.onQueryChange("test")
        testDispatcher.scheduler.advanceTimeBy(400)
        testDispatcher.scheduler.runCurrent()

        // Then short query
        viewModel.onQueryChange("t")
        testDispatcher.scheduler.runCurrent()

        viewModel.state.test {
            val state = awaitItem()
            assertEquals("t", state.query)
            assertTrue(state.albums.isEmpty())
            assertTrue(state.artists.isEmpty())
            assertTrue(state.tracks.isEmpty())
        }
    }

    @Test
    fun `search is debounced`() = runTest {
        coEvery { searchRepository.searchAll(any(), any()) } returns Triple(
            Result.success(listOf(testAlbum)),
            Result.success(listOf(testArtist)),
            Result.success(listOf(testTrack))
        )

        viewModel = SearchViewModel(searchRepository, playTracksUseCase)

        // Type multiple characters quickly
        viewModel.onQueryChange("te")
        testDispatcher.scheduler.advanceTimeBy(100)
        viewModel.onQueryChange("tes")
        testDispatcher.scheduler.advanceTimeBy(100)
        viewModel.onQueryChange("test")
        testDispatcher.scheduler.advanceTimeBy(100)

        // Should not have searched yet (only 300ms passed total)
        coVerify(exactly = 0) { searchRepository.searchAll(any(), any()) }

        // Wait for debounce
        testDispatcher.scheduler.advanceTimeBy(300)
        testDispatcher.scheduler.runCurrent()

        // Now should have searched once
        coVerify(exactly = 1) { searchRepository.searchAll("test", any()) }
    }

    @Test
    fun `search updates state with results`() = runTest {
        coEvery { searchRepository.searchAll("test", any()) } returns Triple(
            Result.success(listOf(testAlbum)),
            Result.success(listOf(testArtist)),
            Result.success(listOf(testTrack))
        )

        viewModel = SearchViewModel(searchRepository, playTracksUseCase)

        viewModel.onQueryChange("test")
        testDispatcher.scheduler.advanceTimeBy(400)
        testDispatcher.scheduler.runCurrent()

        viewModel.state.test {
            val state = awaitItem()
            assertEquals("test", state.query)
            assertEquals(1, state.albums.size)
            assertEquals(testAlbum, state.albums[0])
            assertEquals(1, state.artists.size)
            assertEquals(testArtist, state.artists[0])
            assertEquals(1, state.tracks.size)
            assertEquals(testTrack, state.tracks[0])
            assertFalse(state.isLoading)
        }
    }

    @Test
    fun `search handles partial failures gracefully`() = runTest {
        coEvery { searchRepository.searchAll("test", any()) } returns Triple(
            Result.success(listOf(testAlbum)),
            Result.failure(Exception("Network error")),
            Result.success(listOf(testTrack))
        )

        viewModel = SearchViewModel(searchRepository, playTracksUseCase)

        viewModel.onQueryChange("test")
        testDispatcher.scheduler.advanceTimeBy(400)
        testDispatcher.scheduler.runCurrent()

        viewModel.state.test {
            val state = awaitItem()
            assertEquals(1, state.albums.size)
            assertTrue(state.artists.isEmpty()) // Failed, so empty
            assertEquals(1, state.tracks.size)
        }
    }

    @Test
    fun `clearSearch resets state`() = runTest {
        coEvery { searchRepository.searchAll(any(), any()) } returns Triple(
            Result.success(listOf(testAlbum)),
            Result.success(listOf(testArtist)),
            Result.success(listOf(testTrack))
        )

        viewModel = SearchViewModel(searchRepository, playTracksUseCase)

        // Perform search
        viewModel.onQueryChange("test")
        testDispatcher.scheduler.advanceTimeBy(400)
        testDispatcher.scheduler.runCurrent()

        // Clear
        viewModel.clearSearch()

        viewModel.state.test {
            val state = awaitItem()
            assertEquals("", state.query)
            assertTrue(state.albums.isEmpty())
            assertTrue(state.artists.isEmpty())
            assertTrue(state.tracks.isEmpty())
        }
    }

    @Test
    fun `playTrack calls use case with correct parameters`() = runTest {
        viewModel = SearchViewModel(searchRepository, playTracksUseCase)

        viewModel.playTrack(testTrack)
        testDispatcher.scheduler.runCurrent()

        coVerify {
            playTracksUseCase.playTrack(
                trackId = "track1",
                title = "Test Track",
                artist = "Test Artist",
                albumId = "album1",
                albumTitle = "Test Album",
                duration = 180000L,
                trackNumber = 0,
                coverUrl = "https://example.com/cover.jpg"
            )
        }
    }

    @Test
    fun `playAllTracks plays all tracks from index`() = runTest {
        coEvery { searchRepository.searchAll("test", any()) } returns Triple(
            Result.success(emptyList()),
            Result.success(emptyList()),
            Result.success(listOf(testTrack, testTrack.copy(id = "track2", title = "Track 2")))
        )

        viewModel = SearchViewModel(searchRepository, playTracksUseCase)

        viewModel.onQueryChange("test")
        testDispatcher.scheduler.advanceTimeBy(400)
        testDispatcher.scheduler.runCurrent()

        viewModel.playAllTracks(1)
        testDispatcher.scheduler.runCurrent()

        coVerify {
            playTracksUseCase.playTracks(any(), 1)
        }
    }
}
