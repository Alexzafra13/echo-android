package com.echo.feature.playlists.data.api

import com.echo.feature.playlists.data.dto.AddTrackToPlaylistDto
import com.echo.feature.playlists.data.dto.CreatePlaylistDto
import com.echo.feature.playlists.data.dto.PlaylistDto
import com.echo.feature.playlists.data.dto.PlaylistTracksResponseDto
import com.echo.feature.playlists.data.dto.PlaylistsPageDto
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
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

    @GET("playlists/{id}/tracks")
    suspend fun getPlaylistTracks(@Path("id") playlistId: String): PlaylistTracksResponseDto

    @POST("playlists")
    suspend fun createPlaylist(@Body request: CreatePlaylistDto): PlaylistDto

    @PATCH("playlists/{id}")
    suspend fun updatePlaylist(
        @Path("id") playlistId: String,
        @Body request: UpdatePlaylistDto
    ): PlaylistDto

    @DELETE("playlists/{id}")
    suspend fun deletePlaylist(@Path("id") playlistId: String)

    @POST("playlists/{id}/tracks")
    suspend fun addTrackToPlaylist(
        @Path("id") playlistId: String,
        @Body request: AddTrackToPlaylistDto
    )

    @DELETE("playlists/{playlistId}/tracks/{trackId}")
    suspend fun removeTrackFromPlaylist(
        @Path("playlistId") playlistId: String,
        @Path("trackId") trackId: String
    )
}

@kotlinx.serialization.Serializable
data class UpdatePlaylistDto(
    val name: String? = null,
    val description: String? = null
)

@kotlinx.serialization.Serializable
data class AddTrackToPlaylistDto(
    val trackId: String
)
