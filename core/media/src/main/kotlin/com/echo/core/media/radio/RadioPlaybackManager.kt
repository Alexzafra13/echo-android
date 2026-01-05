package com.echo.core.media.radio

import androidx.annotation.OptIn
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import com.echo.core.media.model.PlayableRadioStation
import com.echo.core.media.model.RadioMetadata
import com.echo.core.media.model.RadioSignalStatus
import com.echo.core.media.player.EchoPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

/**
 * State for radio playback
 */
data class RadioPlaybackState(
    val isRadioMode: Boolean = false,
    val currentStation: PlayableRadioStation? = null,
    val metadata: RadioMetadata? = null,
    val signalStatus: RadioSignalStatus = RadioSignalStatus.UNKNOWN,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val error: String? = null
)

/**
 * Manages radio station playback using EchoPlayer.
 * Handles stream URL resolution, playback state, and metadata.
 */
@Singleton
@OptIn(UnstableApi::class)
class RadioPlaybackManager @Inject constructor(
    private val echoPlayer: EchoPlayer,
    private val metadataService: RadioMetadataService,
    private val mediaItemFactory: MediaItemFactory
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _state = MutableStateFlow(RadioPlaybackState())
    val state: StateFlow<RadioPlaybackState> = _state.asStateFlow()

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            if (_state.value.isRadioMode) {
                _state.update { it.copy(isPlaying = isPlaying) }
            }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (!_state.value.isRadioMode) return

            when (playbackState) {
                Player.STATE_BUFFERING -> {
                    _state.update {
                        it.copy(
                            isBuffering = true,
                            signalStatus = RadioSignalStatus.WEAK
                        )
                    }
                }
                Player.STATE_READY -> {
                    _state.update {
                        it.copy(
                            isBuffering = false,
                            signalStatus = RadioSignalStatus.GOOD,
                            error = null
                        )
                    }
                }
                Player.STATE_ENDED -> {
                    // For live streams, this usually means connection lost
                    _state.update {
                        it.copy(
                            signalStatus = RadioSignalStatus.ERROR,
                            error = "Stream ended unexpectedly"
                        )
                    }
                }
                Player.STATE_IDLE -> {
                    _state.update {
                        it.copy(
                            isBuffering = false,
                            signalStatus = RadioSignalStatus.UNKNOWN
                        )
                    }
                }
            }
        }

        override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
            if (_state.value.isRadioMode) {
                _state.update {
                    it.copy(
                        signalStatus = RadioSignalStatus.ERROR,
                        error = error.localizedMessage ?: "Playback error",
                        isPlaying = false
                    )
                }
            }
        }
    }

    init {
        echoPlayer.exoPlayer.addListener(playerListener)
        observeMetadata()
    }

    /**
     * Observe metadata updates from the SSE service
     */
    private fun observeMetadata() {
        scope.launch {
            metadataService.metadata.collect { metadata ->
                if (metadata != null && _state.value.isRadioMode) {
                    updateMetadata(metadata)
                }
            }
        }
    }

    /**
     * Play a radio station
     */
    fun playStation(station: PlayableRadioStation) {
        // Disconnect from any previous metadata stream
        metadataService.disconnect()

        // Clear any current track queue
        echoPlayer.clearQueue()

        // Create media item for the radio station using factory
        val mediaItem = mediaItemFactory.createFromStation(station)

        // Update state first
        _state.update {
            RadioPlaybackState(
                isRadioMode = true,
                currentStation = station,
                metadata = null,
                signalStatus = RadioSignalStatus.WEAK, // Start as weak until buffered
                isPlaying = false,
                isBuffering = true,
                error = null
            )
        }

        // Set media and play
        echoPlayer.exoPlayer.setMediaItem(mediaItem)
        echoPlayer.exoPlayer.prepare()
        echoPlayer.exoPlayer.play()

        // Connect to metadata stream if station has UUID
        station.stationUuid?.let { uuid ->
            metadataService.connect(uuid, station.streamUrl)
        }
    }

    /**
     * Stop radio playback
     */
    fun stop() {
        // Disconnect metadata stream
        metadataService.disconnect()

        echoPlayer.stop()
        _state.update {
            RadioPlaybackState(
                isRadioMode = false,
                currentStation = null,
                metadata = null,
                signalStatus = RadioSignalStatus.UNKNOWN,
                isPlaying = false,
                isBuffering = false,
                error = null
            )
        }
    }

    /**
     * Pause radio playback
     */
    fun pause() {
        if (_state.value.isRadioMode) {
            echoPlayer.pause()
        }
    }

    /**
     * Resume radio playback
     */
    fun resume() {
        if (_state.value.isRadioMode && _state.value.currentStation != null) {
            echoPlayer.play()
        }
    }

    /**
     * Toggle play/pause
     */
    fun togglePlayPause() {
        if (_state.value.isPlaying) {
            pause()
        } else {
            resume()
        }
    }

    /**
     * Update radio metadata from SSE stream
     */
    fun updateMetadata(metadata: RadioMetadata) {
        if (_state.value.isRadioMode) {
            _state.update { it.copy(metadata = metadata) }

            // Also update the MediaSession metadata for notifications
            _state.value.currentStation?.let { station ->
                updateMediaSessionMetadata(station, metadata)
            }
        }
    }

    /**
     * Clear metadata
     */
    fun clearMetadata() {
        _state.update { it.copy(metadata = null) }
    }

    /**
     * Check if currently in radio mode
     */
    val isRadioMode: Boolean
        get() = _state.value.isRadioMode

    /**
     * Get current station
     */
    val currentStation: PlayableRadioStation?
        get() = _state.value.currentStation

    /**
     * Update MediaSession with current metadata for notifications
     */
    private fun updateMediaSessionMetadata(station: PlayableRadioStation, metadata: RadioMetadata?) {
        val newMetadata = mediaItemFactory.createMetadata(station, metadata)

        // Update the current media item's metadata
        echoPlayer.exoPlayer.currentMediaItem?.let { currentItem ->
            val updatedItem = currentItem.buildUpon()
                .setMediaMetadata(newMetadata)
                .build()
            echoPlayer.exoPlayer.replaceMediaItem(0, updatedItem)
        }
    }

    /**
     * Clean up resources
     */
    fun release() {
        metadataService.release()
        echoPlayer.exoPlayer.removeListener(playerListener)
    }
}
