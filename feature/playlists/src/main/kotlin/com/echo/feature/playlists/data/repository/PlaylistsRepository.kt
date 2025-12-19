package com.echo.feature.playlists.data.repository

import com.echo.core.datastore.preferences.ServerPreferences
import com.echo.core.network.api.ApiClientFactory
import com.echo.feature.playlists.data.api.AddTrackToPlaylistDto
import com.echo.feature.playlists.data.api.PlaylistsApi
import com.echo.feature.playlists.data.api.UpdatePlaylistDto
import com.echo.feature.playlists.data.dto.CreatePlaylistDto
import com.echo.feature.playlists.data.dto.PlaylistDto
import com.echo.feature.playlists.data.dto.PlaylistTrackDto
import com.echo.feature.playlists.domain.model.Playlist
import com.echo.feature.playlists.domain.model.PlaylistTrack
import com.echo.feature.playlists.domain.model.PlaylistWithTracks
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

    private suspend fun getBaseUrl(): String {
        return serverPreferences.activeServer.first()?.url ?: ""
    }

    private fun buildAlbumCoverUrl(baseUrl: String, albumId: String): String {
        return "$baseUrl/api/albums/$albumId/cover"
    }

    private fun PlaylistDto.toDomain(): Playlist {
        return Playlist(
            id = id,
            name = name,
            description = description,
            trackCount = trackCount ?: 0,
            duration = duration ?: 0,
            isPublic = isPublic ?: public ?: false,
            coverUrl = coverUrl ?: coverImageUrl,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    private suspend fun PlaylistTrackDto.toDomain(): PlaylistTrack {
        val baseUrl = getBaseUrl()
        val trackData = track
        return PlaylistTrack(
            id = id,
            trackId = trackId,
            title = trackData?.title ?: "Unknown",
            artistName = trackData?.artistName,
            albumTitle = trackData?.albumTitle ?: trackData?.albumName,
            albumId = trackData?.albumId,
            duration = trackData?.duration,
            trackNumber = trackData?.trackNumber,
            coverUrl = trackData?.albumId?.let { buildAlbumCoverUrl(baseUrl, it) },
            order = order
        )
    }

    suspend fun getPlaylists(skip: Int = 0, take: Int = 50): Result<List<Playlist>> = runCatching {
        getApi().getPlaylists(skip, take).items.map { it.toDomain() }
    }

    suspend fun getPlaylist(playlistId: String): Result<Playlist> = runCatching {
        getApi().getPlaylist(playlistId).toDomain()
    }

    suspend fun getPlaylistWithTracks(playlistId: String): Result<PlaylistWithTracks> = runCatching {
        val playlistDto = getApi().getPlaylist(playlistId)
        val tracksResponse = getApi().getPlaylistTracks(playlistId)

        val playlist = playlistDto.toDomain()
        val tracks = tracksResponse.tracks.map { it.toDomain() }.sortedBy { it.order }

        PlaylistWithTracks(playlist = playlist, tracks = tracks)
    }

    suspend fun getPlaylistTracks(playlistId: String): Result<List<PlaylistTrack>> = runCatching {
        val response = getApi().getPlaylistTracks(playlistId)
        response.tracks.map { it.toDomain() }.sortedBy { it.order }
    }

    suspend fun createPlaylist(
        name: String,
        description: String? = null,
        isPublic: Boolean = false
    ): Result<Playlist> = runCatching {
        getApi().createPlaylist(CreatePlaylistDto(name, description, isPublic)).toDomain()
    }

    suspend fun updatePlaylist(
        playlistId: String,
        name: String? = null,
        description: String? = null
    ): Result<Playlist> = runCatching {
        getApi().updatePlaylist(playlistId, UpdatePlaylistDto(name, description)).toDomain()
    }

    suspend fun deletePlaylist(playlistId: String): Result<Unit> = runCatching {
        getApi().deletePlaylist(playlistId)
    }

    suspend fun addTrackToPlaylist(playlistId: String, trackId: String): Result<Unit> = runCatching {
        getApi().addTrackToPlaylist(playlistId, AddTrackToPlaylistDto(trackId))
    }

    suspend fun removeTrackFromPlaylist(playlistId: String, trackId: String): Result<Unit> = runCatching {
        getApi().removeTrackFromPlaylist(playlistId, trackId)
    }
}
