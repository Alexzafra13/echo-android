package com.echo.feature.home.presentation

import app.cash.turbine.test
import com.echo.core.media.model.PlayableRadioStation
import com.echo.core.media.model.RadioMetadata
import com.echo.core.media.model.RadioSignalStatus
import com.echo.core.media.radio.RadioPlaybackManager
import com.echo.core.media.radio.RadioPlaybackState
import com.echo.feature.home.data.model.RadioBrowserCountry
import com.echo.feature.home.data.model.RadioBrowserStation
import com.echo.feature.home.data.model.RadioBrowserTag
import com.echo.feature.home.data.model.RadioStation
import com.echo.feature.home.data.repository.RadioRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
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
class RadioViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var radioRepository: RadioRepository
    private lateinit var radioPlaybackManager: RadioPlaybackManager
    private lateinit var playbackStateFlow: MutableStateFlow<RadioPlaybackState>

    private val testRadioStation = RadioStation(
        id = "1",
        stationUuid = "uuid-123",
        name = "Test FM",
        url = "http://stream.test.com",
        urlResolved = null,
        favicon = "http://test.com/icon.png",
        country = "Spain",
        countryCode = "ES",
        tags = "pop,rock",
        codec = "MP3",
        bitrate = 128,
        lastCheckOk = true
    )

    private val testBrowserStation = RadioBrowserStation(
        stationuuid = "browser-uuid-456",
        name = "Browser Radio",
        url = "http://stream.browser.com",
        urlResolved = "http://stream.browser.com/resolved",
        favicon = "http://browser.com/icon.png",
        country = "USA",
        countrycode = "US",
        tags = "jazz,blues",
        codec = "AAC",
        bitrate = 192,
        votes = 1000,
        clickcount = 5000,
        clicktrend = 100,
        lastcheckok = 1
    )

    private val testPlayableStation = PlayableRadioStation(
        id = "1",
        stationUuid = "uuid-123",
        name = "Test FM",
        url = "http://stream.test.com",
        urlResolved = null,
        favicon = "http://test.com/icon.png",
        country = "Spain",
        countryCode = "ES",
        tags = listOf("pop", "rock"),
        codec = "MP3",
        bitrate = 128,
        isOnline = true
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        radioRepository = mockk(relaxed = true)
        playbackStateFlow = MutableStateFlow(RadioPlaybackState())
        radioPlaybackManager = mockk(relaxed = true) {
            every { state } returns playbackStateFlow
        }

        // Default repository responses with explicit types to avoid ClassCastException
        coEvery { radioRepository.getFavorites() } returns Result.success(emptyList<RadioStation>())
        coEvery { radioRepository.getTopVoted(any()) } returns Result.success(emptyList<RadioBrowserStation>())
        coEvery { radioRepository.getPopular(any()) } returns Result.success(emptyList<RadioBrowserStation>())
        coEvery { radioRepository.getTags(any()) } returns Result.success(emptyList<RadioBrowserTag>())
        coEvery { radioRepository.getCountries() } returns Result.success(emptyList<RadioBrowserCountry>())
        coEvery { radioRepository.searchStations(any(), any()) } returns Result.success(emptyList<RadioBrowserStation>())
        coEvery { radioRepository.getByTag(any(), any()) } returns Result.success(emptyList<RadioBrowserStation>())
        coEvery { radioRepository.getByCountry(any(), any()) } returns Result.success(emptyList<RadioBrowserStation>())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): RadioViewModel {
        return RadioViewModel(radioRepository, radioPlaybackManager)
    }

    @Test
    fun `initial state is loading`() = runTest {
        // When
        val viewModel = createViewModel()

        // Then
        viewModel.uiState.test {
            val initialState = awaitItem()
            assertTrue(initialState.isLoading)
        }
    }

    @Test
    fun `loadInitialData loads favorites`() = runTest {
        // Given
        val favorites = listOf(testRadioStation)
        coEvery { radioRepository.getFavorites() } returns Result.success(favorites)

        // When
        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(favorites, state.favorites)
            assertTrue(state.favoriteIds.contains("uuid-123"))
        }
    }

    @Test
    fun `loadInitialData loads top voted and popular stations`() = runTest {
        // Given
        val topVoted = listOf(testBrowserStation)
        val popular = listOf(testBrowserStation.copy(stationuuid = "popular-1"))
        coEvery { radioRepository.getTopVoted(20) } returns Result.success(topVoted)
        coEvery { radioRepository.getPopular(20) } returns Result.success(popular)

        // When
        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(topVoted, state.topVoted)
            assertEquals(popular, state.popular)
        }
    }

    @Test
    fun `selectTab updates selected tab`() = runTest {
        // Given
        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.selectTab(RadioTab.DISCOVER)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(RadioTab.DISCOVER, state.selectedTab)
        }
    }

    @Test
    fun `onSearchQueryChange triggers search when query is 2+ chars`() = runTest {
        // Given
        val searchResults = listOf(testBrowserStation)
        coEvery { radioRepository.searchStations(name = "te", limit = 50) } returns Result.success(searchResults)

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.onSearchQueryChange("te")
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        coVerify { radioRepository.searchStations(name = "te", limit = 50) }
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("te", state.searchQuery)
            assertEquals(searchResults, state.searchResults)
        }
    }

    @Test
    fun `onSearchQueryChange clears results when query is less than 2 chars`() = runTest {
        // Given
        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // First set some results
        viewModel.onSearchQueryChange("test")
        testDispatcher.scheduler.advanceUntilIdle()

        // When - clear with short query
        viewModel.onSearchQueryChange("t")
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state.searchResults.isEmpty())
        }
    }

    @Test
    fun `selectGenre loads genre stations`() = runTest {
        // Given
        val genreStations = listOf(testBrowserStation)
        coEvery { radioRepository.getByTag("rock", 50) } returns Result.success(genreStations)

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.selectGenre("rock")
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("rock", state.selectedGenre)
            assertEquals(genreStations, state.genreStations)
        }
    }

    @Test
    fun `selectCountry loads country stations`() = runTest {
        // Given
        val countryStations = listOf(testBrowserStation)
        coEvery { radioRepository.getByCountry("ES", 50) } returns Result.success(countryStations)

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.selectCountry("ES")
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("ES", state.selectedCountry)
            assertEquals(countryStations, state.countryStations)
        }
    }

    @Test
    fun `clearBrowseSelection clears genre and country selection`() = runTest {
        // Given
        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.selectGenre("rock")
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.clearBrowseSelection()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(null, state.selectedGenre)
            assertEquals(null, state.selectedCountry)
            assertTrue(state.genreStations.isEmpty())
            assertTrue(state.countryStations.isEmpty())
        }
    }

    @Test
    fun `playStation calls playbackManager with RadioBrowserStation`() = runTest {
        // Given
        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.playStation(testBrowserStation)

        // Then
        verify { radioPlaybackManager.playStation(any()) }
    }

    @Test
    fun `playStation calls playbackManager with RadioStation`() = runTest {
        // Given
        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.playStation(testRadioStation)

        // Then
        verify { radioPlaybackManager.playStation(any()) }
    }

    @Test
    fun `togglePlayPause calls playbackManager`() = runTest {
        // Given
        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.togglePlayPause()

        // Then
        verify { radioPlaybackManager.togglePlayPause() }
    }

    @Test
    fun `stopRadio calls playbackManager stop`() = runTest {
        // Given
        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.stopRadio()

        // Then
        verify { radioPlaybackManager.stop() }
    }

    @Test
    fun `playback state changes update UI state`() = runTest {
        // Given
        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // When - simulate playback state change
        playbackStateFlow.value = RadioPlaybackState(
            isRadioMode = true,
            currentStation = testPlayableStation,
            isPlaying = true,
            signalStatus = RadioSignalStatus.GOOD
        )
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state.isRadioPlaying)
            assertEquals(testPlayableStation, state.currentPlayingStation)
            assertEquals(RadioSignalStatus.GOOD, state.signalStatus)
        }
    }

    @Test
    fun `metadata updates are reflected in UI state`() = runTest {
        // Given
        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val metadata = RadioMetadata(
            stationUuid = "uuid-123",
            title = "Now Playing",
            artist = "Test Artist",
            song = null
        )

        // When
        playbackStateFlow.value = RadioPlaybackState(
            isRadioMode = true,
            currentStation = testPlayableStation,
            metadata = metadata,
            isPlaying = true
        )
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(metadata, state.currentMetadata)
        }
    }

    @Test
    fun `isStationPlaying returns true for currently playing station`() = runTest {
        // Given
        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        playbackStateFlow.value = RadioPlaybackState(
            isRadioMode = true,
            currentStation = testPlayableStation,
            isPlaying = true
        )
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertTrue(viewModel.isStationPlaying("uuid-123"))
        assertFalse(viewModel.isStationPlaying("other-uuid"))
    }

    @Test
    fun `isStationPlaying returns false when not playing`() = runTest {
        // Given
        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        playbackStateFlow.value = RadioPlaybackState(
            isRadioMode = true,
            currentStation = testPlayableStation,
            isPlaying = false // paused
        )
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertFalse(viewModel.isStationPlaying("uuid-123"))
    }

    @Test
    fun `toggleFavorite adds station to favorites`() = runTest {
        // Given
        val savedStation = testRadioStation.copy(id = "new-id")
        coEvery { radioRepository.saveFavorite(testBrowserStation) } returns Result.success(savedStation)

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.toggleFavorite(testBrowserStation)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        coVerify { radioRepository.saveFavorite(testBrowserStation) }
        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state.favoriteIds.contains(testBrowserStation.stationuuid))
        }
    }

    @Test
    fun `toggleFavorite removes station from favorites`() = runTest {
        // Given - station is already a favorite
        val favorites = listOf(testRadioStation)
        coEvery { radioRepository.getFavorites() } returns Result.success(favorites)
        coEvery { radioRepository.deleteFavorite("1") } returns Result.success(Unit)

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // Create a browser station with matching UUID
        val browserStation = testBrowserStation.copy(stationuuid = "uuid-123")

        // When
        viewModel.toggleFavorite(browserStation)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        coVerify { radioRepository.deleteFavorite("1") }
    }

    @Test
    fun `deleteFavorite removes station`() = runTest {
        // Given
        val favorites = listOf(testRadioStation)
        coEvery { radioRepository.getFavorites() } returns Result.success(favorites)
        coEvery { radioRepository.deleteFavorite("1") } returns Result.success(Unit)

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.deleteFavorite(testRadioStation)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        coVerify { radioRepository.deleteFavorite("1") }
        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.favorites.any { it.id == "1" })
        }
    }

    @Test
    fun `refresh reloads all data`() = runTest {
        // Given
        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.refresh()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then - repositories should be called twice (init + refresh)
        coVerify(exactly = 2) { radioRepository.getFavorites() }
        coVerify(exactly = 2) { radioRepository.getTopVoted(any()) }
        coVerify(exactly = 2) { radioRepository.getPopular(any()) }
    }

    @Test
    fun `clearError clears error state`() = runTest {
        // Given
        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // Simulate an error first (by setting it indirectly through a failed operation)
        coEvery { radioRepository.deleteFavorite(any()) } returns Result.failure(Exception("Error"))
        viewModel.deleteFavorite(testRadioStation)
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.clearError()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(null, state.error)
        }
    }
}
