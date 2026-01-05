package com.echo.core.media.player

import com.echo.core.media.model.PlayableRadioStation
import com.echo.core.media.model.PlayableTrack
import com.echo.core.media.model.RadioMetadata
import com.echo.core.media.model.RadioSignalStatus

/**
 * Represents the current state of the media player
 */
data class PlayerState(
    // Track playback state
    val isPlaying: Boolean = false,
    val currentTrack: PlayableTrack? = null,
    val queue: List<PlayableTrack> = emptyList(),
    val currentIndex: Int = 0,
    val position: Long = 0L, // in milliseconds
    val duration: Long = 0L, // in milliseconds
    val bufferedPosition: Long = 0L,
    val playbackSpeed: Float = 1f,
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val shuffleEnabled: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,

    // Radio playback state
    val isRadioMode: Boolean = false,
    val currentRadioStation: PlayableRadioStation? = null,
    val radioMetadata: RadioMetadata? = null,
    val radioSignalStatus: RadioSignalStatus = RadioSignalStatus.UNKNOWN
) {
    val progress: Float
        get() = if (duration > 0) (position.toFloat() / duration) else 0f

    val hasNext: Boolean
        get() = currentIndex < queue.lastIndex || repeatMode == RepeatMode.ALL

    val hasPrevious: Boolean
        get() = currentIndex > 0 || repeatMode == RepeatMode.ALL

    /**
     * Get display title for current media (track or radio)
     */
    val displayTitle: String
        get() = when {
            isRadioMode -> radioMetadata?.title ?: currentRadioStation?.name ?: ""
            else -> currentTrack?.title ?: ""
        }

    /**
     * Get display artist/subtitle for current media
     */
    val displayArtist: String
        get() = when {
            isRadioMode -> radioMetadata?.artist ?: currentRadioStation?.country ?: "Radio"
            else -> currentTrack?.artist ?: ""
        }

    /**
     * Get artwork URL for current media
     */
    val artworkUrl: String?
        get() = when {
            isRadioMode -> currentRadioStation?.favicon
            else -> currentTrack?.coverUrl
        }
}

enum class RepeatMode {
    OFF,
    ONE,
    ALL
}
