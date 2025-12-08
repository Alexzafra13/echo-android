package com.echo.feature.search.data.api

import com.echo.feature.search.data.dto.AlbumSearchDto
import com.echo.feature.search.data.dto.ArtistSearchDto
import com.echo.feature.search.data.dto.TrackSearchDto
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface SearchApi {

    @GET("albums/search/{query}")
    suspend fun searchAlbums(
        @Path("query") query: String,
        @Query("limit") limit: Int = 20
    ): List<AlbumSearchDto>

    @GET("artists/search/{query}")
    suspend fun searchArtists(
        @Path("query") query: String,
        @Query("limit") limit: Int = 20
    ): List<ArtistSearchDto>

    @GET("tracks/search/{query}")
    suspend fun searchTracks(
        @Path("query") query: String,
        @Query("limit") limit: Int = 20
    ): List<TrackSearchDto>
}
