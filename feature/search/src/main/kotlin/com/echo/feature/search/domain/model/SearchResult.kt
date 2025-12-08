package com.echo.feature.search.domain.model

data class SearchResults(
    val albums: List<SearchAlbum> = emptyList(),
    val artists: List<SearchArtist> = emptyList(),
    val tracks: List<SearchTrack> = emptyList(),
    val isLoading: Boolean = false,
    val query: String = ""
)

data class SearchAlbum(
    val id: String,
    val title: String,
    val artist: String,
    val artistId: String,
    val year: Int?,
    val coverUrl: String?
)

data class SearchArtist(
    val id: String,
    val name: String,
    val albumCount: Int,
    val trackCount: Int,
    val imageUrl: String?
)

data class SearchTrack(
    val id: String,
    val title: String,
    val artistId: String?,
    val artistName: String?,
    val albumId: String?,
    val albumName: String?,
    val duration: Int?,
    val coverUrl: String?
)
