package com.echo.feature.search.data.repository

import com.echo.core.datastore.preferences.ServerPreferences
import com.echo.core.network.api.ApiClientFactory
import com.echo.feature.search.data.api.SearchApi
import com.echo.feature.search.data.dto.AlbumSearchDto
import com.echo.feature.search.data.dto.ArtistSearchDto
import com.echo.feature.search.data.dto.TrackSearchDto
import com.echo.feature.search.domain.model.SearchAlbum
import com.echo.feature.search.domain.model.SearchArtist
import com.echo.feature.search.domain.model.SearchTrack
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SearchRepository @Inject constructor(
    private val apiClientFactory: ApiClientFactory,
    private val serverPreferences: ServerPreferences
) {
    private suspend fun getApi(): SearchApi {
        val server = serverPreferences.activeServer.first()
            ?: throw IllegalStateException("No active server")
        return apiClientFactory.getClient(server.url).create(SearchApi::class.java)
    }

    private suspend fun getBaseUrl(): String {
        return serverPreferences.activeServer.first()?.url ?: ""
    }

    private fun buildCoverUrl(baseUrl: String, albumId: String): String {
        return "$baseUrl/api/albums/$albumId/cover"
    }

    private fun buildArtistImageUrl(baseUrl: String, artistId: String): String {
        return "$baseUrl/api/artists/$artistId/image"
    }

    private suspend fun AlbumSearchDto.toDomain(): SearchAlbum {
        val baseUrl = getBaseUrl()
        return SearchAlbum(
            id = id,
            title = title,
            artist = artist,
            artistId = artistId,
            year = year,
            coverUrl = buildCoverUrl(baseUrl, id)
        )
    }

    private suspend fun ArtistSearchDto.toDomain(): SearchArtist {
        val baseUrl = getBaseUrl()
        return SearchArtist(
            id = id,
            name = name,
            albumCount = albumCount,
            trackCount = trackCount,
            imageUrl = buildArtistImageUrl(baseUrl, id)
        )
    }

    private suspend fun TrackSearchDto.toDomain(): SearchTrack {
        val baseUrl = getBaseUrl()
        return SearchTrack(
            id = id,
            title = title,
            artistId = artistId,
            artistName = artistName,
            albumId = albumId,
            albumName = albumName,
            duration = duration,
            coverUrl = albumId?.let { buildCoverUrl(baseUrl, it) }
        )
    }

    suspend fun searchAlbums(query: String, limit: Int = 20): Result<List<SearchAlbum>> = runCatching {
        getApi().searchAlbums(query, limit).data.map { it.toDomain() }
    }

    suspend fun searchArtists(query: String, limit: Int = 20): Result<List<SearchArtist>> = runCatching {
        getApi().searchArtists(query, limit).data.map { it.toDomain() }
    }

    suspend fun searchTracks(query: String, limit: Int = 20): Result<List<SearchTrack>> = runCatching {
        getApi().searchTracks(query, limit).data.map { it.toDomain() }
    }

    suspend fun searchAll(query: String, limit: Int = 10): Triple<
        Result<List<SearchAlbum>>,
        Result<List<SearchArtist>>,
        Result<List<SearchTrack>>
    > {
        return Triple(
            searchAlbums(query, limit),
            searchArtists(query, limit),
            searchTracks(query, limit)
        )
    }
}
