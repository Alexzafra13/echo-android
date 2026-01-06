package com.echo.feature.home.presentation

import android.content.Context
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
import kotlinx.coroutines.flow.flowOf
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

    private lateinit var context: Context
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

    private val testCountry = RadioBrowserCountry(
        name = "Spain",
        isoCode = "ES",
        stationcount = 500
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        context = mockk(relaxed = true)
        radioRepository = mockk(relaxed = true)
        playbackStateFlow = MutableStateFlow(RadioPlaybackState())
        radioPlaybackManager = mockk(relaxed = true) {
            every { state } returns playbackStateFlow
        }

        // Default repository responses
        coEvery { radioRepository.getTags(any()) } returns Result.success(emptyList<RadioBrowserTag>())
        coEvery { radioRepository.getCountries() } returns Result.success(emptyList<RadioBrowserCountry>())
        coEvery { radioRepository.searchStations(any(), any(), any(), any(), any()) } returns Result.success(emptyList<RadioBrowserStation>())
        coEvery { radioRepository.getByCountry(any(), any()) } returns Result.success(emptyList<RadioBrowserStation>())
        coEvery { radioRepository.getByTag(any(), any()) } returns Result.success(emptyList<RadioBrowserStation>())
        every { radioRepository.observeFavorites() } returns flowOf(emptyList())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): RadioViewModel {
        return RadioViewModel(context, radioRepository, radioPlaybackManager)
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
    fun `loadInitialData loads local stations for user country`() = runTest {
        // Given
        val stations = listOf(testBrowserStation)
        coEvery { radioRepository.getByCountry(any(), any()) } returns Result.success(stations)

        // When
        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(stations, state.localStations)
        }
    }

    @Test
    fun `onSearchQueryChange triggers search when query is 2+ chars`() = runTest {
        // Given
        val searchResults = listOf(testBrowserStation)
        coEvery { radioRepository.searchStations(name = "te", limit = 30) } returns Result.success(searchResults)

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.onSearchQueryChange("te")
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        coVerify { radioRepository.searchStations(name = "te", limit = 30) }
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
    fun `loadStationsForGenre loads stations for genre`() = runTest {
        // Given
        val genreStations = listOf(testBrowserStation)
        coEvery { radioRepository.getByTag("rock", 20) } returns Result.success(genreStations)

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.loadStationsForGenre("rock")
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(genreStations, state.genreStationsMap["rock"])
        }
    }

    @Test
    fun `selectInternationalCountry loads stations for country`() = runTest {
        // Given
        val countryStations = listOf(testBrowserStation)
        coEvery { radioRepository.getByCountry("ES", 20) } returns Result.success(countryStations)

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.selectInternationalCountry(testCountry)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("ES", state.internationalCountryCode)
            assertEquals("Spain", state.internationalCountryName)
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
    fun `current playing station is tracked in state`() = runTest {
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
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("uuid-123", state.currentPlayingStation?.stationUuid)
            assertTrue(state.isRadioPlaying)
        }
    }

    @Test
    fun `paused state is tracked correctly`() = runTest {
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
        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.isRadioPlaying)
        }
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
    }

    @Test
    fun `toggleFavorite removes station from favorites`() = runTest {
        // Given - station is already a favorite
        val favorites = listOf(testRadioStation)
        every { radioRepository.observeFavorites() } returns flowOf(favorites)
        coEvery { radioRepository.deleteFavoriteByUuid("uuid-123") } returns Result.success(Unit)

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // Create a browser station with matching UUID
        val browserStation = testBrowserStation.copy(stationuuid = "uuid-123")

        // When
        viewModel.toggleFavorite(browserStation)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        coVerify { radioRepository.deleteFavoriteByUuid("uuid-123") }
    }

    @Test
    fun `deleteFavorite removes station`() = runTest {
        // Given
        val favorites = listOf(testRadioStation)
        every { radioRepository.observeFavorites() } returns flowOf(favorites)
        coEvery { radioRepository.deleteFavoriteByUuid("uuid-123") } returns Result.success(Unit)

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.deleteFavorite(testRadioStation)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        coVerify { radioRepository.deleteFavoriteByUuid("uuid-123") }
    }

    @Test
    fun `refresh reloads local and international stations`() = runTest {
        // Given
        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.refresh()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then - repository should be called multiple times (init + refresh for both sections)
        coVerify(atLeast = 4) { radioRepository.getByCountry(any(), any()) }
    }

    @Test
    fun `clearError clears error state`() = runTest {
        // Given
        val viewModel = createViewModel()
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

    @Test
    fun `clearSearch clears search state`() = runTest {
        // Given
        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onSearchQueryChange("test")
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.clearSearch()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("", state.searchQuery)
            assertTrue(state.searchResults.isEmpty())
            assertFalse(state.isSearchActive)
        }
    }

    @Test
    fun `showInternationalCountryPicker updates state`() = runTest {
        // Given
        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.showInternationalCountryPicker()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state.showInternationalCountryPicker)
        }
    }

    @Test
    fun `hideInternationalCountryPicker updates state`() = runTest {
        // Given
        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.showInternationalCountryPicker()
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.hideInternationalCountryPicker()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.showInternationalCountryPicker)
        }
    }

    @Test
    fun `loadMoreStationsForSection loads more local stations`() = runTest {
        // Given
        val moreStations = listOf(testBrowserStation)
        coEvery { radioRepository.getByCountry(any(), 100) } returns Result.success(moreStations)

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.loadMoreStationsForSection(RadioSectionType.LOCAL)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        coVerify { radioRepository.getByCountry(any(), 100) }
    }

    @Test
    fun `loadMoreStationsForSection loads more genre stations`() = runTest {
        // Given
        val moreStations = listOf(testBrowserStation)
        coEvery { radioRepository.getByTag("rock", 100) } returns Result.success(moreStations)

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.loadMoreStationsForSection(RadioSectionType.GENRE, "rock")
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        coVerify { radioRepository.getByTag("rock", 100) }
    }

    @Test
    fun `getStationsForSection returns correct stations`() = runTest {
        // Given
        val stations = listOf(testBrowserStation)
        coEvery { radioRepository.getByCountry(any(), any()) } returns Result.success(stations)

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        val localStations = viewModel.getStationsForSection(RadioSectionType.LOCAL)

        // Then
        assertEquals(stations, localStations)
    }
}
