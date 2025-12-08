package com.echo.feature.albums.domain.model

data class Album(
    val id: String,
    val title: String,
    val artist: String,
    val artistId: String,
    val year: Int? = null,
    val totalTracks: Int = 0,
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

data class Track(
    val id: String,
    val title: String,
    val artistId: String? = null,
    val artistName: String? = null,
    val albumId: String? = null,
    val albumName: String? = null,
    val trackNumber: Int? = null,
    val duration: Int? = null,
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

data class AlbumWithTracks(
    val album: Album,
    val tracks: List<Track>
)
