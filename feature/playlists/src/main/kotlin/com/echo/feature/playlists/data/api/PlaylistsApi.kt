package com.echo.feature.playlists.data.api

import com.echo.feature.playlists.data.dto.CreatePlaylistDto
import com.echo.feature.playlists.data.dto.PlaylistDto
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface PlaylistsApi {
    @GET("playlists")
    suspend fun getPlaylists(): List<PlaylistDto>

    @GET("playlists/{id}")
    suspend fun getPlaylist(@Path("id") playlistId: String): PlaylistDto

    @POST("playlists")
    suspend fun createPlaylist(@Body request: CreatePlaylistDto): PlaylistDto

    @DELETE("playlists/{id}")
    suspend fun deletePlaylist(@Path("id") playlistId: String)
}
