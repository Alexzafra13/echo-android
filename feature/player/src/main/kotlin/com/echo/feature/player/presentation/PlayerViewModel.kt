package com.echo.feature.player.presentation

import androidx.lifecycle.ViewModel
import com.echo.core.media.player.EchoPlayer
import com.echo.core.media.player.PlayerState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val player: EchoPlayer
) : ViewModel() {

    val playerState: StateFlow<PlayerState> = player.state

    fun togglePlayPause() {
        player.togglePlayPause()
    }

    fun seekTo(position: Long) {
        player.seekTo(position)
    }

    fun seekToProgress(progress: Float) {
        val duration = playerState.value.duration
        val position = (duration * progress).toLong()
        player.seekTo(position)
    }

    fun skipToNext() {
        player.seekToNext()
    }

    fun skipToPrevious() {
        player.seekToPrevious()
    }

    fun toggleRepeatMode() {
        player.toggleRepeatMode()
    }

    fun toggleShuffle() {
        player.toggleShuffle()
    }

    fun skipToQueueItem(index: Int) {
        player.skipToQueueItem(index)
    }

    fun removeFromQueue(index: Int) {
        player.removeFromQueue(index)
    }
}
