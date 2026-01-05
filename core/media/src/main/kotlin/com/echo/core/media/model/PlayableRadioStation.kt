package com.echo.core.media.model

import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata

/**
 * Represents a radio station that can be played by the media player.
 * This is a playback-focused model, separate from the API models.
 */
data class PlayableRadioStation(
    val id: String,
    val stationUuid: String?,
    val name: String,
    val url: String,
    val urlResolved: String?,
    val favicon: String?,
    val country: String?,
    val countryCode: String?,
    val tags: List<String>,
    val codec: String?,
    val bitrate: Int?,
    val isOnline: Boolean = true
) {
    /**
     * Get the best stream URL (resolved URL preferred)
     */
    val streamUrl: String
        get() = urlResolved?.takeIf { it.isNotBlank() } ?: url

    /**
     * Convert to a Media3 MediaItem for playback
     */
    fun toMediaItem(): MediaItem {
        val metadata = MediaMetadata.Builder()
            .setTitle(name)
            .setArtist(country ?: "Radio")
            .setStation(name)
            .setGenre(tags.firstOrNull())
            .setArtworkUri(favicon?.let { Uri.parse(it) })
            .setIsPlayable(true)
            .build()

        return MediaItem.Builder()
            .setMediaId("radio:${stationUuid ?: id}")
            .setUri(streamUrl)
            .setMediaMetadata(metadata)
            .setLiveConfiguration(
                MediaItem.LiveConfiguration.Builder()
                    .setMaxPlaybackSpeed(1.02f)
                    .build()
            )
            .build()
    }
}

/**
 * Radio stream metadata from ICY headers
 */
data class RadioMetadata(
    val stationUuid: String,
    val title: String?,
    val artist: String?,
    val song: String?,
    val timestamp: Long = System.currentTimeMillis()
) {
    /**
     * Get a formatted display string for the current song
     */
    val displayText: String
        get() = when {
            artist != null && title != null -> "$artist - $title"
            song != null -> song
            title != null -> title
            else -> ""
        }

    val hasMetadata: Boolean
        get() = !title.isNullOrBlank() || !artist.isNullOrBlank() || !song.isNullOrBlank()
}

/**
 * Signal quality status for radio streams
 */
enum class RadioSignalStatus {
    /** Stream is playing smoothly */
    GOOD,
    /** Stream is buffering or experiencing issues */
    WEAK,
    /** Stream failed to connect or errored */
    ERROR,
    /** Status unknown (not playing) */
    UNKNOWN
}
