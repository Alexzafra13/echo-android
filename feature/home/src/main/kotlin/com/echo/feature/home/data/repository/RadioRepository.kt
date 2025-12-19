package com.echo.feature.home.data.repository

import com.echo.core.datastore.preferences.ServerPreferences
import com.echo.core.network.api.ApiClientFactory
import com.echo.feature.home.data.api.RadioApi
import com.echo.feature.home.data.model.CreateCustomStationDto
import com.echo.feature.home.data.model.RadioBrowserCountry
import com.echo.feature.home.data.model.RadioBrowserStation
import com.echo.feature.home.data.model.RadioBrowserTag
import com.echo.feature.home.data.model.RadioStation
import com.echo.feature.home.data.model.toSaveDto
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RadioRepository @Inject constructor(
    private val apiClientFactory: ApiClientFactory,
    private val serverPreferences: ServerPreferences
) {

    private suspend fun getApi(): RadioApi {
        val server = serverPreferences.activeServer.first()
            ?: throw IllegalStateException("No active server")
        return apiClientFactory.getClient(server.url).create(RadioApi::class.java)
    }

    // ============================================
    // Radio Browser API
    // ============================================

    /**
     * Search stations
     */
    suspend fun searchStations(
        name: String? = null,
        country: String? = null,
        countryCode: String? = null,
        tag: String? = null,
        limit: Int = 50
    ): Result<List<RadioBrowserStation>> = runCatching {
        val params = buildMap {
            name?.let { put("name", it) }
            country?.let { put("country", it) }
            countryCode?.let { put("countrycode", it) }
            tag?.let { put("tag", it) }
            put("limit", limit.toString())
            put("hidebroken", "true")
        }
        getApi().searchStations(params)
    }

    /**
     * Get top voted stations
     */
    suspend fun getTopVoted(limit: Int = 20): Result<List<RadioBrowserStation>> = runCatching {
        getApi().getTopVoted(limit)
    }

    /**
     * Get popular stations
     */
    suspend fun getPopular(limit: Int = 20): Result<List<RadioBrowserStation>> = runCatching {
        getApi().getPopular(limit)
    }

    /**
     * Get stations by country
     */
    suspend fun getByCountry(countryCode: String, limit: Int = 50): Result<List<RadioBrowserStation>> = runCatching {
        getApi().getByCountry(countryCode, limit)
    }

    /**
     * Get stations by tag (genre)
     */
    suspend fun getByTag(tag: String, limit: Int = 50): Result<List<RadioBrowserStation>> = runCatching {
        getApi().getByTag(tag, limit)
    }

    /**
     * Get available tags (genres)
     */
    suspend fun getTags(limit: Int = 100): Result<List<RadioBrowserTag>> = runCatching {
        getApi().getTags(limit)
    }

    /**
     * Get available countries
     */
    suspend fun getCountries(): Result<List<RadioBrowserCountry>> = runCatching {
        getApi().getCountries()
    }

    // ============================================
    // Favorites
    // ============================================

    /**
     * Get user's favorite stations
     */
    suspend fun getFavorites(): Result<List<RadioStation>> = runCatching {
        getApi().getFavorites()
    }

    /**
     * Save a station from Radio Browser API as favorite
     */
    suspend fun saveFavorite(station: RadioBrowserStation): Result<RadioStation> = runCatching {
        getApi().saveFavoriteFromApi(station.toSaveDto())
    }

    /**
     * Create a custom radio station
     */
    suspend fun createCustomStation(
        name: String,
        url: String,
        homepage: String? = null,
        favicon: String? = null,
        country: String? = null,
        tags: String? = null
    ): Result<RadioStation> = runCatching {
        getApi().createCustomStation(
            CreateCustomStationDto(
                name = name,
                url = url,
                homepage = homepage,
                favicon = favicon,
                country = country,
                tags = tags
            )
        )
    }

    /**
     * Delete a favorite station
     */
    suspend fun deleteFavorite(stationId: String): Result<Unit> = runCatching {
        getApi().deleteFavorite(stationId)
    }
}
