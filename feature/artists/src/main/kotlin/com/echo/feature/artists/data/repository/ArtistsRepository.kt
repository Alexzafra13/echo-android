package com.echo.feature.artists.data.repository

import com.echo.core.datastore.preferences.ServerPreferences
import com.echo.core.network.api.ApiClientFactory
import com.echo.feature.artists.data.api.ArtistsApi
import com.echo.feature.artists.data.dto.ArtistAlbumDto
import com.echo.feature.artists.data.dto.ArtistDto
import com.echo.feature.artists.domain.model.Artist
import com.echo.feature.artists.domain.model.ArtistAlbum
import com.echo.feature.artists.domain.model.ArtistWithAlbums
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ArtistsRepository @Inject constructor(
    private val apiClientFactory: ApiClientFactory,
    private val serverPreferences: ServerPreferences
) {
    private suspend fun getApi(): ArtistsApi {
        val server = serverPreferences.activeServer.first()
            ?: throw IllegalStateException("No active server")
        return apiClientFactory.getClient(server.url).create(ArtistsApi::class.java)
    }

    private suspend fun getBaseUrl(): String {
        return serverPreferences.activeServer.first()?.url ?: ""
    }

    private fun buildImageUrl(baseUrl: String, artistId: String, type: String = "profile"): String {
        return "$baseUrl/api/images/artists/$artistId/$type"
    }

    private fun buildAlbumCoverUrl(baseUrl: String, albumId: String): String {
        return "$baseUrl/api/albums/$albumId/cover"
    }

    private suspend fun ArtistDto.toDomain(): Artist {
        val baseUrl = getBaseUrl()
        return Artist(
            id = id,
            name = name,
            imageUrl = buildImageUrl(baseUrl, id, "profile"),
            backgroundUrl = buildImageUrl(baseUrl, id, "background"),
            biography = biography,
            albumCount = albumCount ?: 0,
            trackCount = trackCount ?: 0
        )
    }

    private suspend fun ArtistAlbumDto.toDomain(artistId: String, artistName: String): ArtistAlbum {
        val baseUrl = getBaseUrl()
        return ArtistAlbum(
            id = id,
            title = name,
            artistId = artistId,
            artistName = artistName,
            year = year,
            trackCount = trackCount ?: 0,
            duration = duration,
            genres = genres ?: emptyList(),
            coverUrl = buildAlbumCoverUrl(baseUrl, id)
        )
    }

    suspend fun getArtists(skip: Int = 0, take: Int = 50): Result<List<Artist>> = runCatching {
        val artists = getApi().getArtists(skip, take).data
        artists.map { it.toDomain() }
    }

    suspend fun getArtist(artistId: String): Result<Artist> = runCatching {
        getApi().getArtist(artistId).toDomain()
    }

    suspend fun getArtistWithAlbums(artistId: String): Result<ArtistWithAlbums> = runCatching {
        val artistDto = getApi().getArtist(artistId)
        val artist = artistDto.toDomain()
        val albumDtos = getApi().getArtistAlbums(artistId).data
        val albums = albumDtos.map { it.toDomain(artistId, artist.name) }
        ArtistWithAlbums(artist = artist, albums = albums)
    }

    suspend fun getArtistAlbums(artistId: String): Result<List<ArtistAlbum>> = runCatching {
        val artist = getApi().getArtist(artistId)
        val albumDtos = getApi().getArtistAlbums(artistId).data
        albumDtos.map { it.toDomain(artistId, artist.name) }
    }

    suspend fun searchArtists(query: String, limit: Int = 20): Result<List<Artist>> = runCatching {
        val artists = getApi().searchArtists(query, limit).data
        artists.map { it.toDomain() }
    }
}
