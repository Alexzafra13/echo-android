package com.echo.feature.artists.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class ArtistDto(
    val id: String,
    val name: String,
    val imageUrl: String? = null,
    val albumCount: Int? = null,
    val trackCount: Int? = null
)

@Serializable
data class ArtistsPageDto(
    val data: List<ArtistDto>,
    val total: Int,
    val skip: Int,
    val take: Int
)
