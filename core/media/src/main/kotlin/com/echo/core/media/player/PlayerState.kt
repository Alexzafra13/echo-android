package com.echo.core.media.player

import com.echo.core.media.model.PlayableTrack

/**
 * Represents the current state of the media player
 */
data class PlayerState(
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
    val error: String? = null
) {
    val progress: Float
        get() = if (duration > 0) (position.toFloat() / duration) else 0f

    val hasNext: Boolean
        get() = currentIndex < queue.lastIndex || repeatMode == RepeatMode.ALL

    val hasPrevious: Boolean
        get() = currentIndex > 0 || repeatMode == RepeatMode.ALL
}

enum class RepeatMode {
    OFF,
    ONE,
    ALL
}
