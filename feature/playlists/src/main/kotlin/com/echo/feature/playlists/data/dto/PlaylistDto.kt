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
    val data: List<PlaylistDto>,
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
