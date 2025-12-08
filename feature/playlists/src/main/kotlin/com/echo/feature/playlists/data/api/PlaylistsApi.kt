package com.echo.feature.playlists.data.api

import com.echo.feature.playlists.data.dto.CreatePlaylistDto
import com.echo.feature.playlists.data.dto.PlaylistDto
import com.echo.feature.playlists.data.dto.PlaylistsPageDto
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface PlaylistsApi {
    @GET("playlists")
    suspend fun getPlaylists(
        @Query("skip") skip: Int = 0,
        @Query("take") take: Int = 50
    ): PlaylistsPageDto

    @GET("playlists/{id}")
    suspend fun getPlaylist(@Path("id") playlistId: String): PlaylistDto

    @POST("playlists")
    suspend fun createPlaylist(@Body request: CreatePlaylistDto): PlaylistDto

    @DELETE("playlists/{id}")
    suspend fun deletePlaylist(@Path("id") playlistId: String)
}
