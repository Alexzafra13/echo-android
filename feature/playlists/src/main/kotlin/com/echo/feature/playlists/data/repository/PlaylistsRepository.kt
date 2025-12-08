package com.echo.feature.playlists.data.repository

import com.echo.core.datastore.preferences.ServerPreferences
import com.echo.core.network.api.ApiClientFactory
import com.echo.feature.playlists.data.api.PlaylistsApi
import com.echo.feature.playlists.data.dto.CreatePlaylistDto
import com.echo.feature.playlists.data.dto.PlaylistDto
import com.echo.feature.playlists.domain.model.Playlist
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaylistsRepository @Inject constructor(
    private val apiClientFactory: ApiClientFactory,
    private val serverPreferences: ServerPreferences
) {
    private suspend fun getApi(): PlaylistsApi {
        val server = serverPreferences.activeServer.first()
            ?: throw IllegalStateException("No active server")
        return apiClientFactory.getClient(server.url).create(PlaylistsApi::class.java)
    }

    private fun PlaylistDto.toDomain(): Playlist {
        return Playlist(
            id = id,
            name = name,
            description = description,
            trackCount = trackCount ?: 0,
            duration = duration ?: 0,
            isPublic = isPublic ?: false,
            coverUrl = coverUrl,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    suspend fun getPlaylists(): Result<List<Playlist>> = runCatching {
        getApi().getPlaylists().map { it.toDomain() }
    }

    suspend fun getPlaylist(playlistId: String): Result<Playlist> = runCatching {
        getApi().getPlaylist(playlistId).toDomain()
    }

    suspend fun createPlaylist(name: String, description: String? = null, isPublic: Boolean = false): Result<Playlist> = runCatching {
        getApi().createPlaylist(CreatePlaylistDto(name, description, isPublic)).toDomain()
    }

    suspend fun deletePlaylist(playlistId: String): Result<Unit> = runCatching {
        getApi().deletePlaylist(playlistId)
    }
}
