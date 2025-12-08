package com.echo.feature.albums.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class AlbumDto(
    val id: String,
    val title: String,
    val artist: String,
    val artistId: String,
    val year: Int? = null,
    val totalTracks: Int = 0,
    val duration: Int? = null,
    val genres: List<String> = emptyList(),
    val addedAt: String? = null,
    val coverImage: String? = null
)

@Serializable
data class AlbumDetailDto(
    val id: String,
    val title: String,
    val artist: String,
    val artistId: String,
    val year: Int? = null,
    val totalTracks: Int = 0,
    val duration: Int? = null,
    val genres: List<String> = emptyList(),
    val addedAt: String? = null,
    val coverImage: String? = null,
    val tracks: List<TrackDto> = emptyList()
)

@Serializable
data class TrackDto(
    val id: String,
    val title: String,
    val artistId: String? = null,
    val artistName: String? = null,
    val albumId: String? = null,
    val albumName: String? = null,
    val trackNumber: Int? = null,
    val duration: Int? = null,
    val bitRate: Int? = null,
    val suffix: String? = null,
    val path: String? = null
)

@Serializable
data class PaginatedResponse<T>(
    val data: List<T>,
    val total: Int,
    val skip: Int,
    val take: Int
)
