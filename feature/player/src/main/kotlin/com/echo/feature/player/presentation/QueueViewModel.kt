package com.echo.feature.player.presentation

import androidx.lifecycle.ViewModel
import com.echo.core.media.player.EchoPlayer
import com.echo.core.media.player.PlayerState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class QueueViewModel @Inject constructor(
    private val player: EchoPlayer
) : ViewModel() {

    val playerState: StateFlow<PlayerState> = player.state

    fun skipToQueueItem(index: Int) {
        player.skipToQueueItem(index)
    }

    fun removeFromQueue(index: Int) {
        player.removeFromQueue(index)
    }

    fun clearQueue() {
        player.clearQueue()
    }
}
