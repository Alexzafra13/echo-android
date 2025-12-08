package com.echo.feature.artists.domain.model

data class Artist(
    val id: String,
    val name: String,
    val imageUrl: String? = null,
    val albumCount: Int = 0,
    val trackCount: Int = 0
)
