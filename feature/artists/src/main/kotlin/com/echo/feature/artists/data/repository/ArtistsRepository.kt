package com.echo.feature.artists.data.repository

import com.echo.core.datastore.preferences.ServerPreferences
import com.echo.core.network.api.ApiClientFactory
import com.echo.feature.artists.data.api.ArtistsApi
import com.echo.feature.artists.data.dto.ArtistDto
import com.echo.feature.artists.domain.model.Artist
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

    private fun buildImageUrl(baseUrl: String, artistId: String): String {
        return "$baseUrl/api/images/artists/$artistId/profile"
    }

    private suspend fun ArtistDto.toDomain(): Artist {
        val server = serverPreferences.activeServer.first()
        val baseUrl = server?.url ?: ""
        return Artist(
            id = id,
            name = name,
            imageUrl = buildImageUrl(baseUrl, id),
            albumCount = albumCount ?: 0,
            trackCount = trackCount ?: 0
        )
    }

    suspend fun getArtists(skip: Int = 0, take: Int = 50): Result<List<Artist>> = runCatching {
        val artists = getApi().getArtists(skip, take).data
        artists.map { it.toDomain() }
    }

    suspend fun getArtist(artistId: String): Result<Artist> = runCatching {
        getApi().getArtist(artistId).toDomain()
    }

    suspend fun searchArtists(query: String, limit: Int = 20): Result<List<Artist>> = runCatching {
        val artists = getApi().searchArtists(query, limit)
        artists.map { it.toDomain() }
    }
}
