package com.echo.feature.artists.data.api

import com.echo.feature.artists.data.dto.ArtistAlbumDto
import com.echo.feature.artists.data.dto.ArtistDto
import com.echo.feature.artists.data.dto.ArtistsPageDto
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface ArtistsApi {
    @GET("artists")
    suspend fun getArtists(
        @Query("skip") skip: Int = 0,
        @Query("take") take: Int = 20
    ): ArtistsPageDto

    @GET("artists/search/{query}")
    suspend fun searchArtists(
        @Path("query") query: String,
        @Query("limit") limit: Int = 20
    ): List<ArtistDto>

    @GET("artists/{id}")
    suspend fun getArtist(@Path("id") artistId: String): ArtistDto

    @GET("artists/{id}/albums")
    suspend fun getArtistAlbums(@Path("id") artistId: String): List<ArtistAlbumDto>
}
