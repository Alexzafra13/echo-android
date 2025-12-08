package com.echo.feature.search.data.dto

import kotlinx.serialization.Serializable

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
