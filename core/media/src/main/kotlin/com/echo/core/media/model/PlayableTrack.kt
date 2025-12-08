package com.echo.core.media.model

import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata

/**
 * Represents a track that can be played by the media player
 */
data class PlayableTrack(
    val id: String,
    val title: String,
    val artist: String,
    val albumId: String,
    val albumTitle: String,
    val duration: Long, // in milliseconds
    val trackNumber: Int,
    val coverUrl: String?,
    val streamUrl: String
) {
    fun toMediaItem(): MediaItem {
        val metadata = MediaMetadata.Builder()
            .setTitle(title)
            .setArtist(artist)
            .setAlbumTitle(albumTitle)
            .setTrackNumber(trackNumber)
            .setArtworkUri(coverUrl?.let { Uri.parse(it) })
            .build()

        return MediaItem.Builder()
            .setMediaId(id)
            .setUri(streamUrl)
            .setMediaMetadata(metadata)
            .build()
    }

    companion object {
        fun fromMediaItem(mediaItem: MediaItem): PlayableTrack? {
            val metadata = mediaItem.mediaMetadata
            return PlayableTrack(
                id = mediaItem.mediaId,
                title = metadata.title?.toString() ?: "",
                artist = metadata.artist?.toString() ?: "",
                albumId = "", // Not stored in MediaMetadata
                albumTitle = metadata.albumTitle?.toString() ?: "",
                duration = 0, // Will be updated from player
                trackNumber = metadata.trackNumber ?: 0,
                coverUrl = metadata.artworkUri?.toString(),
                streamUrl = mediaItem.localConfiguration?.uri?.toString() ?: ""
            )
        }
    }
}
