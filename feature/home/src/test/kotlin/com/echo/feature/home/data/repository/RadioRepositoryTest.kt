package com.echo.feature.home.data.repository

import com.echo.core.datastore.preferences.SavedServer
import com.echo.core.datastore.preferences.ServerPreferences
import com.echo.core.network.api.ApiClientFactory
import com.echo.feature.home.data.api.RadioApi
import com.echo.feature.home.data.model.RadioBrowserCountry
import com.echo.feature.home.data.model.RadioBrowserStation
import com.echo.feature.home.data.model.RadioBrowserTag
import com.echo.feature.home.data.model.RadioStation
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit

class RadioRepositoryTest {

    private lateinit var radioApi: RadioApi
    private lateinit var apiClientFactory: ApiClientFactory
    private lateinit var serverPreferences: ServerPreferences
    private lateinit var repository: RadioRepository

    private val testServer = SavedServer(
        id = "server-1",
        name = "Test Server",
        url = "http://localhost:3000",
        addedAt = System.currentTimeMillis()
    )

    private val testBrowserStation = RadioBrowserStation(
        stationuuid = "test-uuid",
        name = "Test Radio",
        url = "http://stream.test.com",
        urlResolved = "http://stream.test.com/resolved",
        favicon = "http://test.com/icon.png",
        country = "Spain",
        countrycode = "ES",
        tags = "pop,rock",
        codec = "MP3",
        bitrate = 128,
        votes = 100,
        clickcount = 500,
        clicktrend = 10,
        lastcheckok = 1
    )

    private val testRadioStation = RadioStation(
        id = "1",
        stationUuid = "test-uuid",
        name = "Test Radio",
        url = "http://stream.test.com",
        urlResolved = "http://stream.test.com/resolved",
        favicon = "http://test.com/icon.png",
        country = "Spain",
        countryCode = "ES",
        tags = "pop,rock",
        codec = "MP3",
        bitrate = 128
    )

    @Before
    fun setup() {
        radioApi = mockk(relaxed = true)

        val retrofit = mockk<Retrofit> {
            every { create(RadioApi::class.java) } returns radioApi
        }

        apiClientFactory = mockk {
            every { getClient(any()) } returns retrofit
        }

        serverPreferences = mockk {
            every { activeServer } returns flowOf(testServer)
        }

        repository = RadioRepository(apiClientFactory, serverPreferences)
    }

    // ============================================
    // Search Stations Tests
    // ============================================

    @Test
    fun `searchStations builds correct query params with name only`() = runTest {
        // Given
        val paramsSlot = slot<Map<String, String>>()
        coEvery { radioApi.searchStations(capture(paramsSlot)) } returns listOf(testBrowserStation)

        // When
        repository.searchStations(name = "jazz")

        // Then
        val params = paramsSlot.captured
        assertEquals("jazz", params["name"])
        assertEquals("50", params["limit"])
        assertEquals("true", params["hidebroken"])
        assertTrue(params.size == 3)
    }

    @Test
    fun `searchStations builds correct query params with all parameters`() = runTest {
        // Given
        val paramsSlot = slot<Map<String, String>>()
        coEvery { radioApi.searchStations(capture(paramsSlot)) } returns listOf(testBrowserStation)

        // When
        repository.searchStations(
            name = "jazz",
            country = "Spain",
            countryCode = "ES",
            tag = "jazz",
            limit = 100
        )

        // Then
        val params = paramsSlot.captured
        assertEquals("jazz", params["name"])
        assertEquals("Spain", params["country"])
        assertEquals("ES", params["countrycode"])
        assertEquals("jazz", params["tag"])
        assertEquals("100", params["limit"])
        assertEquals("true", params["hidebroken"])
    }

    @Test
    fun `searchStations returns success with stations`() = runTest {
        // Given
        val stations = listOf(testBrowserStation)
        coEvery { radioApi.searchStations(any()) } returns stations

        // When
        val result = repository.searchStations(name = "test")

        // Then
        assertTrue(result.isSuccess)
        assertEquals(stations, result.getOrNull())
    }

    @Test
    fun `searchStations returns failure on exception`() = runTest {
        // Given
        coEvery { radioApi.searchStations(any()) } throws RuntimeException("Network error")

        // When
        val result = repository.searchStations(name = "test")

        // Then
        assertTrue(result.isFailure)
        assertEquals("Network error", result.exceptionOrNull()?.message)
    }

    // ============================================
    // Top Voted Tests
    // ============================================

    @Test
    fun `getTopVoted returns stations successfully`() = runTest {
        // Given
        val stations = listOf(testBrowserStation)
        coEvery { radioApi.getTopVoted(any()) } returns stations

        // When
        val result = repository.getTopVoted(20)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(stations, result.getOrNull())
        coVerify { radioApi.getTopVoted(20) }
    }

    @Test
    fun `getTopVoted returns failure on exception`() = runTest {
        // Given
        coEvery { radioApi.getTopVoted(any()) } throws RuntimeException("Error")

        // When
        val result = repository.getTopVoted()

        // Then
        assertTrue(result.isFailure)
    }

    // ============================================
    // Popular Tests
    // ============================================

    @Test
    fun `getPopular returns stations successfully`() = runTest {
        // Given
        val stations = listOf(testBrowserStation)
        coEvery { radioApi.getPopular(any()) } returns stations

        // When
        val result = repository.getPopular(30)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(stations, result.getOrNull())
        coVerify { radioApi.getPopular(30) }
    }

    // ============================================
    // By Country Tests
    // ============================================

    @Test
    fun `getByCountry returns stations for country code`() = runTest {
        // Given
        val stations = listOf(testBrowserStation)
        coEvery { radioApi.getByCountry("ES", 50) } returns stations

        // When
        val result = repository.getByCountry("ES", 50)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(stations, result.getOrNull())
    }

    // ============================================
    // By Tag Tests
    // ============================================

    @Test
    fun `getByTag returns stations for tag`() = runTest {
        // Given
        val stations = listOf(testBrowserStation)
        coEvery { radioApi.getByTag("rock", 50) } returns stations

        // When
        val result = repository.getByTag("rock", 50)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(stations, result.getOrNull())
    }

    // ============================================
    // Tags Tests
    // ============================================

    @Test
    fun `getTags returns available tags`() = runTest {
        // Given
        val tags = listOf(
            RadioBrowserTag("rock", 1000),
            RadioBrowserTag("pop", 800)
        )
        coEvery { radioApi.getTags(any()) } returns tags

        // When
        val result = repository.getTags(50)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(tags, result.getOrNull())
    }

    // ============================================
    // Countries Tests
    // ============================================

    @Test
    fun `getCountries returns available countries`() = runTest {
        // Given
        val countries = listOf(
            RadioBrowserCountry("Spain", "ES", 500),
            RadioBrowserCountry("United States", "US", 2000)
        )
        coEvery { radioApi.getCountries() } returns countries

        // When
        val result = repository.getCountries()

        // Then
        assertTrue(result.isSuccess)
        assertEquals(countries, result.getOrNull())
    }

    // ============================================
    // Favorites Tests
    // ============================================

    @Test
    fun `getFavorites returns user favorites`() = runTest {
        // Given
        val favorites = listOf(testRadioStation)
        coEvery { radioApi.getFavorites() } returns favorites

        // When
        val result = repository.getFavorites()

        // Then
        assertTrue(result.isSuccess)
        assertEquals(favorites, result.getOrNull())
    }

    @Test
    fun `saveFavorite saves station and returns result`() = runTest {
        // Given
        coEvery { radioApi.saveFavoriteFromApi(any()) } returns testRadioStation

        // When
        val result = repository.saveFavorite(testBrowserStation)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(testRadioStation, result.getOrNull())
        coVerify { radioApi.saveFavoriteFromApi(any()) }
    }

    @Test
    fun `deleteFavorite deletes station successfully`() = runTest {
        // Given
        coEvery { radioApi.deleteFavorite("1") } returns Unit

        // When
        val result = repository.deleteFavorite("1")

        // Then
        assertTrue(result.isSuccess)
        coVerify { radioApi.deleteFavorite("1") }
    }

    @Test
    fun `deleteFavorite returns failure on error`() = runTest {
        // Given
        coEvery { radioApi.deleteFavorite(any()) } throws RuntimeException("Not found")

        // When
        val result = repository.deleteFavorite("999")

        // Then
        assertTrue(result.isFailure)
    }

    // ============================================
    // Custom Station Tests
    // ============================================

    @Test
    fun `createCustomStation creates station successfully`() = runTest {
        // Given
        val customStation = testRadioStation.copy(source = "custom")
        coEvery { radioApi.createCustomStation(any()) } returns customStation

        // When
        val result = repository.createCustomStation(
            name = "My Radio",
            url = "http://myradio.com/stream",
            favicon = "http://myradio.com/icon.png",
            country = "Spain"
        )

        // Then
        assertTrue(result.isSuccess)
        assertEquals(customStation, result.getOrNull())
    }

    // ============================================
    // Server Configuration Tests
    // ============================================

    @Test
    fun `repository throws when no active server`() = runTest {
        // Given
        every { serverPreferences.activeServer } returns flowOf(null)
        repository = RadioRepository(apiClientFactory, serverPreferences)

        // When
        val result = repository.getFavorites()

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalStateException)
    }

    @Test
    fun `repository uses correct server URL`() = runTest {
        // Given
        coEvery { radioApi.getFavorites() } returns emptyList()

        // When
        repository.getFavorites()

        // Then
        coVerify { apiClientFactory.getClient("http://localhost:3000") }
    }
}
