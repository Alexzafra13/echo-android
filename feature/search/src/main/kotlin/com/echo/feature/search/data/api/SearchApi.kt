package com.echo.feature.search.data.api

import com.echo.feature.search.data.dto.SearchResultDto
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface SearchApi {

    @GET("albums/search/{query}")
    suspend fun searchAlbums(
        @Path("query") query: String,
        @Query("limit") limit: Int = 20,
        @Query("offset") offset: Int = 0
    ): SearchResultDto.AlbumsResult

    @GET("artists/search/{query}")
    suspend fun searchArtists(
        @Path("query") query: String,
        @Query("limit") limit: Int = 20,
        @Query("offset") offset: Int = 0
    ): SearchResultDto.ArtistsResult

    @GET("tracks/search/{query}")
    suspend fun searchTracks(
        @Path("query") query: String,
        @Query("limit") limit: Int = 20,
        @Query("offset") offset: Int = 0
    ): SearchResultDto.TracksResult
}
