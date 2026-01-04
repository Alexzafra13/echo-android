package com.echo.feature.artists.domain.model

data class Artist(
    val id: String,
    val name: String,
    val imageUrl: String? = null,
    val backgroundUrl: String? = null,
    val biography: String? = null,
    val albumCount: Int = 0,
    val trackCount: Int = 0,
    val playCount: Int = 0,
    val listenerCount: Int = 0
)

data class ArtistAlbum(
    val id: String,
    val title: String,
    val artistId: String,
    val artistName: String,
    val year: Int? = null,
    val trackCount: Int = 0,
    val duration: Int? = null,
    val genres: List<String> = emptyList(),
    val coverUrl: String? = null
) {
    val formattedDuration: String
        get() {
            val totalSeconds = duration ?: return ""
            val hours = totalSeconds / 3600
            val minutes = (totalSeconds % 3600) / 60
            return if (hours > 0) {
                "${hours}h ${minutes}m"
            } else {
                "${minutes} min"
            }
        }
}

data class ArtistTopTrack(
    val trackId: String,
    val title: String,
    val albumId: String? = null,
    val albumName: String? = null,
    val duration: Int? = null,
    val playCount: Int = 0,
    val uniqueListeners: Int = 0,
    val coverUrl: String? = null
) {
    val formattedDuration: String
        get() {
            val totalSeconds = duration ?: return ""
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return "$minutes:${seconds.toString().padStart(2, '0')}"
        }
}

data class RelatedArtist(
    val id: String,
    val name: String,
    val albumCount: Int = 0,
    val songCount: Int = 0,
    val matchScore: Int = 0,
    val imageUrl: String? = null
)

data class ArtistWithAlbums(
    val artist: Artist,
    val albums: List<ArtistAlbum>,
    val topTracks: List<ArtistTopTrack> = emptyList(),
    val relatedArtists: List<RelatedArtist> = emptyList()
)
