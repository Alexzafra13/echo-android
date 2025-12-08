package com.echo.feature.albums.data.api

import com.echo.feature.albums.data.dto.AlbumDto
import com.echo.feature.albums.data.dto.PaginatedResponse
import com.echo.feature.albums.data.dto.TrackDto
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface AlbumsApi {

    @GET("albums")
    suspend fun getAlbums(
        @Query("skip") skip: Int = 0,
        @Query("take") take: Int = 20
    ): PaginatedResponse<AlbumDto>

    @GET("albums/recent")
    suspend fun getRecentAlbums(
        @Query("limit") limit: Int = 10
    ): List<AlbumDto>

    @GET("albums/top-played")
    suspend fun getTopPlayedAlbums(
        @Query("limit") limit: Int = 10
    ): List<AlbumDto>

    @GET("albums/recently-played")
    suspend fun getRecentlyPlayedAlbums(
        @Query("limit") limit: Int = 10
    ): List<AlbumDto>

    @GET("albums/favorites")
    suspend fun getFavoriteAlbums(
        @Query("limit") limit: Int = 10
    ): List<AlbumDto>

    @GET("albums/featured")
    suspend fun getFeaturedAlbum(): AlbumDto?

    @GET("albums/search/{query}")
    suspend fun searchAlbums(
        @Path("query") query: String,
        @Query("limit") limit: Int = 20
    ): List<AlbumDto>

    @GET("albums/{id}")
    suspend fun getAlbum(
        @Path("id") albumId: String
    ): AlbumDto

    @GET("albums/{id}/tracks")
    suspend fun getAlbumTracks(
        @Path("id") albumId: String
    ): List<TrackDto>
}
