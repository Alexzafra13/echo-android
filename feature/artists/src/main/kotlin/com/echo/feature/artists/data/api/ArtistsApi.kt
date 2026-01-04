package com.echo.feature.artists.data.api

import com.echo.feature.artists.data.dto.ArtistAlbumsPageDto
import com.echo.feature.artists.data.dto.ArtistDto
import com.echo.feature.artists.data.dto.ArtistStatsDto
import com.echo.feature.artists.data.dto.ArtistTopTracksResponseDto
import com.echo.feature.artists.data.dto.ArtistsPageDto
import com.echo.feature.artists.data.dto.ArtistsSearchPageDto
import com.echo.feature.artists.data.dto.RelatedArtistsResponseDto
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
    ): ArtistsSearchPageDto

    @GET("artists/{id}")
    suspend fun getArtist(@Path("id") artistId: String): ArtistDto

    @GET("artists/{id}/albums")
    suspend fun getArtistAlbums(@Path("id") artistId: String): ArtistAlbumsPageDto

    @GET("artists/{id}/stats")
    suspend fun getArtistStats(@Path("id") artistId: String): ArtistStatsDto

    @GET("artists/{id}/top-tracks")
    suspend fun getArtistTopTracks(
        @Path("id") artistId: String,
        @Query("limit") limit: Int = 10
    ): ArtistTopTracksResponseDto

    @GET("artists/{id}/related")
    suspend fun getRelatedArtists(
        @Path("id") artistId: String,
        @Query("limit") limit: Int = 10
    ): RelatedArtistsResponseDto
}
