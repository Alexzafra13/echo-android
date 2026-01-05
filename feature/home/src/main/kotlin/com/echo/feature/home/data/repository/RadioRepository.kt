package com.echo.feature.home.data.repository

import android.util.Log
import com.echo.core.database.dao.FavoriteRadioStationDao
import com.echo.core.database.entity.FavoriteRadioStationEntity
import com.echo.feature.home.data.api.RadioBrowserApiService
import com.echo.feature.home.data.model.RadioBrowserCountry
import com.echo.feature.home.data.model.RadioBrowserStation
import com.echo.feature.home.data.model.RadioBrowserTag
import com.echo.feature.home.data.model.RadioStation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RadioRepository @Inject constructor(
    private val radioBrowserApi: RadioBrowserApiService,
    private val favoriteRadioStationDao: FavoriteRadioStationDao
) {

    companion object {
        private const val TAG = "RadioRepository"
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
    // Local Favorites (stored in Room database)
    // ============================================

    /**
     * Get all favorite stations as Flow
     */
    fun observeFavorites(): Flow<List<RadioStation>> {
        return favoriteRadioStationDao.getAllFavorites().map { entities ->
            entities.map { it.toRadioStation() }
        }
    }

    /**
     * Get all favorite station IDs as Flow
     */
    fun observeFavoriteIds(): Flow<List<String>> {
        return favoriteRadioStationDao.observeAllFavoriteIds()
    }

    /**
     * Get user's favorite stations
     */
    suspend fun getFavorites(): Result<List<RadioStation>> = runCatching {
        favoriteRadioStationDao.getAllFavoritesList().map { it.toRadioStation() }
    }

    /**
     * Check if a station is favorite
     */
    suspend fun isFavorite(stationUuid: String): Boolean {
        return favoriteRadioStationDao.isFavorite(stationUuid)
    }

    /**
     * Save a station from Radio Browser API as favorite
     */
    suspend fun saveFavorite(station: RadioBrowserStation): Result<RadioStation> = runCatching {
        val entity = station.toFavoriteEntity()
        val id = favoriteRadioStationDao.insert(entity)
        entity.copy(id = id).toRadioStation()
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
        val entity = FavoriteRadioStationEntity(
            stationUuid = "custom_${System.currentTimeMillis()}",
            name = name,
            url = url,
            homepage = homepage,
            favicon = favicon,
            country = country,
            tags = tags
        )
        val id = favoriteRadioStationDao.insert(entity)
        entity.copy(id = id).toRadioStation()
    }

    /**
     * Delete a favorite station by ID
     */
    suspend fun deleteFavorite(stationId: String): Result<Unit> = runCatching {
        favoriteRadioStationDao.deleteById(stationId.toLong())
    }

    /**
     * Delete a favorite station by station UUID
     */
    suspend fun deleteFavoriteByUuid(stationUuid: String): Result<Unit> = runCatching {
        favoriteRadioStationDao.deleteByStationUuid(stationUuid)
    }

    // ============================================
    // Conversion functions
    // ============================================

    private fun FavoriteRadioStationEntity.toRadioStation(): RadioStation {
        return RadioStation(
            id = id.toString(),
            stationUuid = stationUuid,
            name = name,
            url = url,
            urlResolved = urlResolved,
            homepage = homepage,
            favicon = favicon,
            country = country,
            countryCode = countryCode,
            state = state,
            language = language,
            tags = tags,
            codec = codec,
            bitrate = bitrate,
            votes = votes,
            clickCount = clickCount,
            lastCheckOk = lastCheckOk,
            source = "local",
            isFavorite = true
        )
    }

    private fun RadioBrowserStation.toFavoriteEntity(): FavoriteRadioStationEntity {
        return FavoriteRadioStationEntity(
            stationUuid = stationuuid,
            name = name,
            url = url,
            urlResolved = urlResolved.ifEmpty { null },
            homepage = homepage.ifEmpty { null },
            favicon = favicon.ifEmpty { null },
            country = country.ifEmpty { null },
            countryCode = countrycode.ifEmpty { null },
            state = state.ifEmpty { null },
            language = language.ifEmpty { null },
            tags = tags.ifEmpty { null },
            codec = codec.ifEmpty { null },
            bitrate = if (bitrate > 0) bitrate else null,
            votes = if (votes > 0) votes else null,
            clickCount = if (clickcount > 0) clickcount else null,
            lastCheckOk = lastcheckok == 1
        )
    }
}
