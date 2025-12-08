package com.echo.feature.search.data.dto

import kotlinx.serialization.Serializable

sealed class SearchResultDto {
    @Serializable
    data class AlbumsResult(
        val data: List<AlbumSearchDto>,
        val total: Int = 0,
        val skip: Int = 0,
        val take: Int = 20
    )

    @Serializable
    data class ArtistsResult(
        val data: List<ArtistSearchDto>,
        val total: Int = 0,
        val skip: Int = 0,
        val take: Int = 20
    )

    @Serializable
    data class TracksResult(
        val data: List<TrackSearchDto>,
        val total: Int = 0,
        val skip: Int = 0,
        val take: Int = 20
    )
}

@Serializable
data class AlbumSearchDto(
    val id: String,
    val title: String,
    val artist: String,
    val artistId: String,
    val year: Int? = null,
    val coverImage: String? = null
)

@Serializable
data class ArtistSearchDto(
    val id: String,
    val name: String,
    val albumCount: Int = 0,
    val trackCount: Int = 0,
    val image: String? = null
)

@Serializable
data class TrackSearchDto(
    val id: String,
    val title: String,
    val artistId: String? = null,
    val artistName: String? = null,
    val albumId: String? = null,
    val albumName: String? = null,
    val duration: Int? = null,
    val coverImage: String? = null
)
