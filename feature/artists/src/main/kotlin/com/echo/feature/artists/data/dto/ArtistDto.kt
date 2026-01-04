package com.echo.feature.artists.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class ArtistDto(
    val id: String,
    val name: String,
    val imageUrl: String? = null,
    val albumCount: Int? = null,
    val songCount: Int? = null,
    val biography: String? = null
)

@Serializable
data class ArtistStatsDto(
    val artistId: String,
    val totalPlays: Int? = null,
    val uniqueListeners: Int? = null,
    val avgCompletionRate: Double? = null,
    val skipRate: Double? = null
)

@Serializable
data class ArtistsPageDto(
    val data: List<ArtistDto>,
    val total: Int,
    val skip: Int,
    val take: Int
)

@Serializable
data class ArtistAlbumDto(
    val id: String,
    val name: String,
    val year: Int? = null,
    val trackCount: Int? = null,
    val duration: Int? = null,
    val genres: List<String>? = null
)

@Serializable
data class ArtistAlbumsPageDto(
    val data: List<ArtistAlbumDto>,
    val total: Int? = null
)

@Serializable
data class ArtistsSearchPageDto(
    val data: List<ArtistDto>,
    val total: Int? = null
)

@Serializable
data class ArtistTopTrackDto(
    val trackId: String,
    val title: String,
    val albumId: String? = null,
    val albumName: String? = null,
    val duration: Int? = null,
    val playCount: Int? = null,
    val uniqueListeners: Int? = null
)

@Serializable
data class ArtistTopTracksResponseDto(
    val data: List<ArtistTopTrackDto>,
    val artistId: String,
    val limit: Int? = null,
    val days: Int? = null
)

@Serializable
data class RelatedArtistDto(
    val id: String,
    val name: String,
    val albumCount: Int? = null,
    val songCount: Int? = null,
    val matchScore: Int? = null
)

@Serializable
data class RelatedArtistsResponseDto(
    val data: List<RelatedArtistDto>,
    val artistId: String,
    val limit: Int? = null,
    val source: String? = null
)
