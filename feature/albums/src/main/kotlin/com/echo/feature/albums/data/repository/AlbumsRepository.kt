package com.echo.feature.albums.data.repository

import com.echo.core.datastore.preferences.ServerPreferences
import com.echo.core.network.api.ApiClientFactory
import com.echo.feature.albums.data.api.AlbumsApi
import com.echo.feature.albums.data.dto.AlbumDto
import com.echo.feature.albums.data.dto.TrackDto
import com.echo.feature.albums.domain.model.Album
import com.echo.feature.albums.domain.model.AlbumWithTracks
import com.echo.feature.albums.domain.model.Track
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlbumsRepository @Inject constructor(
    private val apiClientFactory: ApiClientFactory,
    private val serverPreferences: ServerPreferences
) {
    private suspend fun getApi(): AlbumsApi {
        val server = serverPreferences.activeServer.first()
            ?: throw IllegalStateException("No active server")
        return apiClientFactory.getClient(server.url).create(AlbumsApi::class.java)
    }

    private fun buildCoverUrl(baseUrl: String, albumId: String): String {
        return "$baseUrl/api/albums/$albumId/cover"
    }

    private suspend fun AlbumDto.toDomain(): Album {
        val server = serverPreferences.activeServer.first()
        val baseUrl = server?.url ?: ""
        return Album(
            id = id,
            title = title,
            artist = artist,
            artistId = artistId,
            year = year,
            totalTracks = totalTracks,
            duration = duration,
            genres = genres,
            coverUrl = buildCoverUrl(baseUrl, id)
        )
    }

    private suspend fun TrackDto.toDomain(albumCoverUrl: String? = null): Track {
        return Track(
            id = id,
            title = title,
            artistId = artistId,
            artistName = artistName,
            albumId = albumId,
            albumName = albumName,
            trackNumber = trackNumber,
            duration = duration,
            coverUrl = albumCoverUrl
        )
    }

    suspend fun getRecentAlbums(limit: Int = 10): Result<List<Album>> = runCatching {
        getApi().getRecentAlbums(limit).map { it.toDomain() }
    }

    suspend fun getTopPlayedAlbums(limit: Int = 10): Result<List<Album>> = runCatching {
        getApi().getTopPlayedAlbums(limit).map { it.toDomain() }
    }

    suspend fun getRecentlyPlayedAlbums(limit: Int = 10): Result<List<Album>> = runCatching {
        getApi().getRecentlyPlayedAlbums(limit).map { it.toDomain() }
    }

    suspend fun getFavoriteAlbums(limit: Int = 10): Result<List<Album>> = runCatching {
        getApi().getFavoriteAlbums(limit).map { it.toDomain() }
    }

    suspend fun getFeaturedAlbum(): Result<Album?> = runCatching {
        getApi().getFeaturedAlbum()?.toDomain()
    }

    suspend fun getAlbums(skip: Int = 0, take: Int = 20): Result<List<Album>> = runCatching {
        getApi().getAlbums(skip, take).data.map { it.toDomain() }
    }

    suspend fun getAlbum(albumId: String): Result<Album> = runCatching {
        getApi().getAlbum(albumId).toDomain()
    }

    suspend fun getAlbumWithTracks(albumId: String): Result<AlbumWithTracks> = runCatching {
        val album = getApi().getAlbum(albumId).toDomain()
        val tracks = getApi().getAlbumTracks(albumId).map { it.toDomain(album.coverUrl) }
        AlbumWithTracks(album, tracks)
    }

    suspend fun searchAlbums(query: String, limit: Int = 20): Result<List<Album>> = runCatching {
        getApi().searchAlbums(query, limit).map { it.toDomain() }
    }
}
