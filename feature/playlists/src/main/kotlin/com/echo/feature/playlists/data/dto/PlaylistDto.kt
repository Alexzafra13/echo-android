package com.echo.feature.playlists.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class PlaylistDto(
    val id: String,
    val name: String,
    val description: String? = null,
    val trackCount: Int? = null,
    val duration: Int? = null,
    val isPublic: Boolean? = null,
    val public: Boolean? = null,
    val coverUrl: String? = null,
    val coverImageUrl: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null
)

@Serializable
data class PlaylistsPageDto(
    val items: List<PlaylistDto>,
    val total: Int,
    val skip: Int,
    val take: Int
)

@Serializable
data class CreatePlaylistDto(
    val name: String,
    val description: String? = null,
    val isPublic: Boolean = false
)

@Serializable
data class PlaylistTracksResponseDto(
    val playlistId: String,
    val playlistName: String,
    val tracks: List<PlaylistTrackDto>,
    val total: Int
)

@Serializable
data class PlaylistTrackDto(
    val id: String,
    val playlistId: String,
    val trackId: String,
    val order: Int,
    val addedAt: String? = null,
    val track: TrackDto? = null
)

@Serializable
data class TrackDto(
    val id: String,
    val title: String,
    val artistId: String? = null,
    val artistName: String? = null,
    val albumId: String? = null,
    val albumName: String? = null,
    val albumTitle: String? = null,
    val trackNumber: Int? = null,
    val discNumber: Int? = null,
    val year: Int? = null,
    val duration: Int? = null,
    val coverImage: String? = null
)
