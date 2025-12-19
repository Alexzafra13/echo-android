package com.echo.feature.home.data.api

import com.echo.feature.home.data.model.CreateCustomStationDto
import com.echo.feature.home.data.model.RadioBrowserCountry
import com.echo.feature.home.data.model.RadioBrowserStation
import com.echo.feature.home.data.model.RadioBrowserTag
import com.echo.feature.home.data.model.RadioStation
import com.echo.feature.home.data.model.SaveApiStationDto
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.QueryMap

interface RadioApi {

    // ============================================
    // Radio Browser API endpoints
    // ============================================

    /**
     * Search stations in Radio Browser API
     */
    @GET("radio/search")
    suspend fun searchStations(
        @QueryMap params: Map<String, String>
    ): List<RadioBrowserStation>

    /**
     * Get top voted stations
     */
    @GET("radio/top-voted")
    suspend fun getTopVoted(
        @Query("limit") limit: Int = 20
    ): List<RadioBrowserStation>

    /**
     * Get popular stations
     */
    @GET("radio/popular")
    suspend fun getPopular(
        @Query("limit") limit: Int = 20
    ): List<RadioBrowserStation>

    /**
     * Get stations by country
     */
    @GET("radio/by-country/{countryCode}")
    suspend fun getByCountry(
        @Path("countryCode") countryCode: String,
        @Query("limit") limit: Int = 50
    ): List<RadioBrowserStation>

    /**
     * Get stations by tag (genre)
     */
    @GET("radio/by-tag/{tag}")
    suspend fun getByTag(
        @Path("tag") tag: String,
        @Query("limit") limit: Int = 50
    ): List<RadioBrowserStation>

    /**
     * Get available tags (genres)
     */
    @GET("radio/tags")
    suspend fun getTags(
        @Query("limit") limit: Int = 100
    ): List<RadioBrowserTag>

    /**
     * Get available countries
     */
    @GET("radio/countries")
    suspend fun getCountries(): List<RadioBrowserCountry>

    // ============================================
    // Favorites endpoints
    // ============================================

    /**
     * Get user's favorite stations
     */
    @GET("radio/favorites")
    suspend fun getFavorites(): List<RadioStation>

    /**
     * Save a station from Radio Browser API as favorite
     */
    @POST("radio/favorites/from-api")
    suspend fun saveFavoriteFromApi(@Body stationData: SaveApiStationDto): RadioStation

    /**
     * Create a custom radio station
     */
    @POST("radio/favorites/custom")
    suspend fun createCustomStation(@Body stationData: CreateCustomStationDto): RadioStation

    /**
     * Delete a favorite station
     */
    @DELETE("radio/favorites/{stationId}")
    suspend fun deleteFavorite(@Path("stationId") stationId: String)
}
