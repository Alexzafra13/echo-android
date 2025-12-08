package com.echo.feature.playlists.domain.model

data class Playlist(
    val id: String,
    val name: String,
    val description: String? = null,
    val trackCount: Int = 0,
    val duration: Int = 0,
    val isPublic: Boolean = false,
    val coverUrl: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null
)
