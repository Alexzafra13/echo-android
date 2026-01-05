package com.echo.feature.home.data.repository

import com.echo.core.database.dao.FavoriteRadioStationDao
import com.echo.core.database.entity.FavoriteRadioStationEntity
import com.echo.feature.home.data.api.RadioBrowserApiService
import com.echo.feature.home.data.model.RadioBrowserCountry
import com.echo.feature.home.data.model.RadioBrowserStation
import com.echo.feature.home.data.model.RadioBrowserTag
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.eq
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RadioRepositoryTest {

    private lateinit var radioBrowserApi: RadioBrowserApiService
    private lateinit var favoriteRadioStationDao: FavoriteRadioStationDao
    private lateinit var repository: RadioRepository

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

    private val testFavoriteEntity = FavoriteRadioStationEntity(
        id = 1,
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
        radioBrowserApi = mockk(relaxed = true)
        favoriteRadioStationDao = mockk(relaxed = true)
        repository = RadioRepository(radioBrowserApi, favoriteRadioStationDao)
    }

    // ============================================
    // Search Stations Tests
    // ============================================

    @Test
    fun `searchStations builds correct query params with name only`() = runTest {
        // Given
        val paramsSlot = slot<Map<String, String>>()
        coEvery { radioBrowserApi.searchStations(capture(paramsSlot)) } returns listOf(testBrowserStation)

        // When
        repository.searchStations(name = "jazz")

        // Then
        val params = paramsSlot.captured
        assertEquals("jazz", params["name"])
        assertEquals("50", params["limit"])
        assertEquals("true", params["hidebroken"])
    }

    @Test
    fun `searchStations builds correct query params with all parameters`() = runTest {
        // Given
        val paramsSlot = slot<Map<String, String>>()
        coEvery { radioBrowserApi.searchStations(capture(paramsSlot)) } returns listOf(testBrowserStation)

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
        coEvery { radioBrowserApi.searchStations(any()) } returns stations

        // When
        val result = repository.searchStations(name = "test")

        // Then
        assertTrue(result.isSuccess)
        assertEquals(stations, result.getOrNull())
    }

    @Test
    fun `searchStations returns failure on exception`() = runTest {
        // Given
        coEvery { radioBrowserApi.searchStations(any()) } throws RuntimeException("Network error")

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
        coEvery { radioBrowserApi.getTopVoted(any()) } returns stations

        // When
        val result = repository.getTopVoted(20)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(stations, result.getOrNull())
        coVerify { radioBrowserApi.getTopVoted(20) }
    }

    @Test
    fun `getTopVoted returns failure on exception`() = runTest {
        // Given
        coEvery { radioBrowserApi.getTopVoted(any()) } throws RuntimeException("Error")

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
        coEvery { radioBrowserApi.getPopular(any()) } returns stations

        // When
        val result = repository.getPopular(30)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(stations, result.getOrNull())
        coVerify { radioBrowserApi.getPopular(30) }
    }

    // ============================================
    // By Country Tests
    // ============================================

    @Test
    fun `getByCountry returns stations for country code`() = runTest {
        // Given
        val stations = listOf(testBrowserStation)
        coEvery { radioBrowserApi.getByCountry(eq("ES"), any(), any(), any(), any()) } returns stations

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
        coEvery { radioBrowserApi.getByTag(eq("rock"), any(), any(), any(), any()) } returns stations

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
        coEvery { radioBrowserApi.getTags(any(), any(), any(), any()) } returns tags

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
        coEvery { radioBrowserApi.getCountries(any(), any(), any()) } returns countries

        // When
        val result = repository.getCountries()

        // Then
        assertTrue(result.isSuccess)
        assertEquals(countries, result.getOrNull())
    }

    // ============================================
    // Local Favorites Tests
    // ============================================

    @Test
    fun `getFavorites returns favorites from local database`() = runTest {
        // Given
        val favorites = listOf(testFavoriteEntity)
        coEvery { favoriteRadioStationDao.getAllFavoritesList() } returns favorites

        // When
        val result = repository.getFavorites()

        // Then
        assertTrue(result.isSuccess)
        val stations = result.getOrNull()!!
        assertEquals(1, stations.size)
        assertEquals("Test Radio", stations[0].name)
        assertEquals("test-uuid", stations[0].stationUuid)
    }

    @Test
    fun `saveFavorite saves station to local database`() = runTest {
        // Given
        coEvery { favoriteRadioStationDao.insert(any()) } returns 1L

        // When
        val result = repository.saveFavorite(testBrowserStation)

        // Then
        assertTrue(result.isSuccess)
        val savedStation = result.getOrNull()!!
        assertEquals("Test Radio", savedStation.name)
        assertEquals("test-uuid", savedStation.stationUuid)
        coVerify { favoriteRadioStationDao.insert(any()) }
    }

    @Test
    fun `deleteFavorite deletes station from local database`() = runTest {
        // Given
        coEvery { favoriteRadioStationDao.deleteById(1L) } returns Unit

        // When
        val result = repository.deleteFavorite("1")

        // Then
        assertTrue(result.isSuccess)
        coVerify { favoriteRadioStationDao.deleteById(1L) }
    }

    @Test
    fun `deleteFavoriteByUuid deletes station by UUID`() = runTest {
        // Given
        coEvery { favoriteRadioStationDao.deleteByStationUuid("test-uuid") } returns Unit

        // When
        val result = repository.deleteFavoriteByUuid("test-uuid")

        // Then
        assertTrue(result.isSuccess)
        coVerify { favoriteRadioStationDao.deleteByStationUuid("test-uuid") }
    }

    @Test
    fun `isFavorite returns true when station is favorite`() = runTest {
        // Given
        coEvery { favoriteRadioStationDao.isFavorite("test-uuid") } returns true

        // When
        val result = repository.isFavorite("test-uuid")

        // Then
        assertTrue(result)
    }

    @Test
    fun `createCustomStation creates station in local database`() = runTest {
        // Given
        coEvery { favoriteRadioStationDao.insert(any()) } returns 1L

        // When
        val result = repository.createCustomStation(
            name = "My Radio",
            url = "http://myradio.com/stream",
            favicon = "http://myradio.com/icon.png",
            country = "Spain"
        )

        // Then
        assertTrue(result.isSuccess)
        val station = result.getOrNull()!!
        assertEquals("My Radio", station.name)
        assertEquals("http://myradio.com/stream", station.url)
        coVerify { favoriteRadioStationDao.insert(any()) }
    }

    @Test
    fun `observeFavorites returns flow of favorites`() = runTest {
        // Given
        val favorites = listOf(testFavoriteEntity)
        coEvery { favoriteRadioStationDao.getAllFavorites() } returns flowOf(favorites)

        // When
        val flow = repository.observeFavorites()

        // Then
        val stations = flow.first()
        assertEquals(1, stations.size)
        assertEquals("Test Radio", stations[0].name)
    }
}
