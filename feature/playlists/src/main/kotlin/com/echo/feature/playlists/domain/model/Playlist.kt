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
) {
    val formattedDuration: String
        get() {
            val hours = duration / 3600
            val minutes = (duration % 3600) / 60
            return when {
                hours > 0 -> "${hours}h ${minutes}m"
                minutes > 0 -> "$minutes min"
                else -> ""
            }
        }
}

data class PlaylistTrack(
    val id: String,
    val trackId: String,
    val title: String,
    val artistName: String?,
    val albumTitle: String?,
    val albumId: String?,
    val duration: Int?,
    val trackNumber: Int?,
    val coverUrl: String?,
    val order: Int
) {
    val formattedDuration: String
        get() {
            val totalSeconds = duration ?: return ""
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return "$minutes:${seconds.toString().padStart(2, '0')}"
        }
}

data class PlaylistWithTracks(
    val playlist: Playlist,
    val tracks: List<PlaylistTrack>
)
