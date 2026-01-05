package com.echo.feature.home.data.repository

import android.util.Log
import com.echo.core.datastore.preferences.ServerPreferences
import com.echo.core.network.api.ApiClientFactory
import com.echo.feature.home.data.api.RadioApi
import com.echo.feature.home.data.api.RadioBrowserApiService
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
    private val serverPreferences: ServerPreferences,
    private val radioBrowserApi: RadioBrowserApiService
) {

    companion object {
        private const val TAG = "RadioRepository"
    }

    private suspend fun getApi(): RadioApi {
        val server = serverPreferences.activeServer.first()
            ?: throw IllegalStateException("No active server")
        return apiClientFactory.getClient(server.url).create(RadioApi::class.java)
    }

    // ============================================
    // Radio Browser API (Direct calls to public API)
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
            put("order", "clickcount")
            put("reverse", "true")
        }
        radioBrowserApi.searchStations(params)
    }.onFailure { Log.e(TAG, "Error searching stations", it) }

    /**
     * Get top voted stations
     */
    suspend fun getTopVoted(limit: Int = 20): Result<List<RadioBrowserStation>> = runCatching {
        radioBrowserApi.getTopVoted(limit)
    }.onFailure { Log.e(TAG, "Error getting top voted", it) }

    /**
     * Get popular stations
     */
    suspend fun getPopular(limit: Int = 20): Result<List<RadioBrowserStation>> = runCatching {
        radioBrowserApi.getPopular(limit)
    }.onFailure { Log.e(TAG, "Error getting popular", it) }

    /**
     * Get stations by country
     */
    suspend fun getByCountry(countryCode: String, limit: Int = 50): Result<List<RadioBrowserStation>> = runCatching {
        radioBrowserApi.getByCountry(countryCode, limit)
    }.onFailure { Log.e(TAG, "Error getting by country", it) }

    /**
     * Get stations by tag (genre)
     */
    suspend fun getByTag(tag: String, limit: Int = 50): Result<List<RadioBrowserStation>> = runCatching {
        radioBrowserApi.getByTag(tag, limit)
    }.onFailure { Log.e(TAG, "Error getting by tag", it) }

    /**
     * Get available tags (genres)
     */
    suspend fun getTags(limit: Int = 100): Result<List<RadioBrowserTag>> = runCatching {
        radioBrowserApi.getTags(limit)
    }.onFailure { Log.e(TAG, "Error getting tags", it) }

    /**
     * Get available countries
     */
    suspend fun getCountries(): Result<List<RadioBrowserCountry>> = runCatching {
        radioBrowserApi.getCountries()
    }.onFailure { Log.e(TAG, "Error getting countries", it) }

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
