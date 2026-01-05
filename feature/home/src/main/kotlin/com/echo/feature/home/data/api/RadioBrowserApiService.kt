package com.echo.feature.home.data.api

import com.echo.feature.home.data.model.RadioBrowserCountry
import com.echo.feature.home.data.model.RadioBrowserStation
import com.echo.feature.home.data.model.RadioBrowserTag
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.QueryMap

/**
 * Direct interface to Radio Browser public API
 * https://api.radio-browser.info/
 */
interface RadioBrowserApiService {

    /**
     * Search stations by name, country, tag, etc.
     */
    @GET("stations/search")
    suspend fun searchStations(
        @QueryMap params: Map<String, String>
    ): List<RadioBrowserStation>

    /**
     * Get top voted stations
     */
    @GET("stations/topvote/{limit}")
    suspend fun getTopVoted(
        @Path("limit") limit: Int
    ): List<RadioBrowserStation>

    /**
     * Get most clicked/popular stations
     */
    @GET("stations/topclick/{limit}")
    suspend fun getPopular(
        @Path("limit") limit: Int
    ): List<RadioBrowserStation>

    /**
     * Get stations by exact tag name
     */
    @GET("stations/bytagexact/{tag}")
    suspend fun getByTag(
        @Path("tag") tag: String,
        @Query("limit") limit: Int = 50,
        @Query("hidebroken") hideBroken: Boolean = true,
        @Query("order") order: String = "clickcount",
        @Query("reverse") reverse: Boolean = true
    ): List<RadioBrowserStation>

    /**
     * Get stations by country code
     */
    @GET("stations/bycountrycodeexact/{countryCode}")
    suspend fun getByCountry(
        @Path("countryCode") countryCode: String,
        @Query("limit") limit: Int = 50,
        @Query("hidebroken") hideBroken: Boolean = true,
        @Query("order") order: String = "clickcount",
        @Query("reverse") reverse: Boolean = true
    ): List<RadioBrowserStation>

    /**
     * Get all available tags (genres)
     */
    @GET("tags")
    suspend fun getTags(
        @Query("limit") limit: Int = 100,
        @Query("order") order: String = "stationcount",
        @Query("reverse") reverse: Boolean = true,
        @Query("hidebroken") hideBroken: Boolean = true
    ): List<RadioBrowserTag>

    /**
     * Get all available countries
     */
    @GET("countries")
    suspend fun getCountries(
        @Query("order") order: String = "stationcount",
        @Query("reverse") reverse: Boolean = true,
        @Query("hidebroken") hideBroken: Boolean = true
    ): List<RadioBrowserCountry>
}
