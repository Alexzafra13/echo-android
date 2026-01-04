package com.echo.core.media.player

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import com.echo.core.media.model.PlayableTrack
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
@OptIn(UnstableApi::class)
class EchoPlayer @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var positionUpdateJob: Job? = null

    private val _state = MutableStateFlow(PlayerState())
    val state: StateFlow<PlayerState> = _state.asStateFlow()

    private var _exoPlayer: ExoPlayer? = null
    val exoPlayer: ExoPlayer
        get() = _exoPlayer ?: createPlayer().also { _exoPlayer = it }

    private var currentQueue: List<PlayableTrack> = emptyList()

    private fun createPlayer(): ExoPlayer {
        // Configure larger buffers for smoother streaming
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                30_000,  // Min buffer before playback starts (30s)
                60_000,  // Max buffer size (60s)
                2_500,   // Buffer for playback to start (2.5s)
                5_000    // Buffer for rebuffering (5s)
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()

        // Audio attributes for music playback
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()

        return ExoPlayer.Builder(context)
            .setLoadControl(loadControl)
            .setAudioAttributes(audioAttributes, true) // true = handle audio focus
            .setHandleAudioBecomingNoisy(true)
            .build()
            .apply {
                addListener(playerListener)
                playWhenReady = true
            }
    }

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _state.update { it.copy(isPlaying = isPlaying) }
            if (isPlaying) {
                startPositionUpdates()
            } else {
                stopPositionUpdates()
            }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            val isLoading = playbackState == Player.STATE_BUFFERING
            _state.update { it.copy(isLoading = isLoading) }

            if (playbackState == Player.STATE_READY) {
                _state.update {
                    it.copy(duration = exoPlayer.duration.coerceAtLeast(0))
                }
            }
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            mediaItem?.let { item ->
                val index = exoPlayer.currentMediaItemIndex
                val track = currentQueue.getOrNull(index)
                _state.update {
                    it.copy(
                        currentTrack = track,
                        currentIndex = index,
                        position = 0L
                    )
                }
            }
        }

        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
            _state.update { it.copy(shuffleEnabled = shuffleModeEnabled) }
        }

        override fun onRepeatModeChanged(repeatMode: Int) {
            val mode = when (repeatMode) {
                Player.REPEAT_MODE_ONE -> RepeatMode.ONE
                Player.REPEAT_MODE_ALL -> RepeatMode.ALL
                else -> RepeatMode.OFF
            }
            _state.update { it.copy(repeatMode = mode) }
        }

        override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
            _state.update { it.copy(error = error.localizedMessage) }
        }
    }

    private fun startPositionUpdates() {
        positionUpdateJob?.cancel()
        positionUpdateJob = scope.launch {
            while (isActive) {
                _state.update {
                    it.copy(
                        position = exoPlayer.currentPosition.coerceAtLeast(0),
                        bufferedPosition = exoPlayer.bufferedPosition.coerceAtLeast(0)
                    )
                }
                delay(250) // Update every 250ms
            }
        }
    }

    private fun stopPositionUpdates() {
        positionUpdateJob?.cancel()
        positionUpdateJob = null
    }

    fun play() {
        exoPlayer.play()
    }

    fun pause() {
        exoPlayer.pause()
    }

    fun togglePlayPause() {
        if (exoPlayer.isPlaying) pause() else play()
    }

    fun seekTo(position: Long) {
        exoPlayer.seekTo(position)
        _state.update { it.copy(position = position) }
    }

    fun seekToNext() {
        if (exoPlayer.hasNextMediaItem()) {
            exoPlayer.seekToNextMediaItem()
        }
    }

    fun seekToPrevious() {
        // If more than 3 seconds in, restart current track
        if (exoPlayer.currentPosition > 3000) {
            exoPlayer.seekTo(0)
        } else if (exoPlayer.hasPreviousMediaItem()) {
            exoPlayer.seekToPreviousMediaItem()
        } else {
            exoPlayer.seekTo(0)
        }
    }

    fun setRepeatMode(mode: RepeatMode) {
        exoPlayer.repeatMode = when (mode) {
            RepeatMode.OFF -> Player.REPEAT_MODE_OFF
            RepeatMode.ONE -> Player.REPEAT_MODE_ONE
            RepeatMode.ALL -> Player.REPEAT_MODE_ALL
        }
    }

    fun toggleRepeatMode() {
        val nextMode = when (_state.value.repeatMode) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
        setRepeatMode(nextMode)
    }

    fun setShuffleEnabled(enabled: Boolean) {
        exoPlayer.shuffleModeEnabled = enabled
    }

    fun toggleShuffle() {
        exoPlayer.shuffleModeEnabled = !exoPlayer.shuffleModeEnabled
    }

    fun shuffleAll() {
        // Enable shuffle mode and restart from beginning if there's a queue
        if (currentQueue.isNotEmpty()) {
            exoPlayer.shuffleModeEnabled = true
            exoPlayer.seekTo(0, 0)
            exoPlayer.play()
        }
    }

    fun playTracksShuffled(tracks: List<PlayableTrack>) {
        if (tracks.isEmpty()) return
        val shuffled = tracks.shuffled()
        playTracks(shuffled, 0)
        exoPlayer.shuffleModeEnabled = true
    }

    fun playTrack(track: PlayableTrack) {
        playTracks(listOf(track), 0)
    }

    fun playTracks(tracks: List<PlayableTrack>, startIndex: Int = 0) {
        currentQueue = tracks
        _state.update {
            it.copy(
                queue = tracks,
                currentIndex = startIndex,
                currentTrack = tracks.getOrNull(startIndex)
            )
        }

        val mediaItems = tracks.map { it.toMediaItem() }
        exoPlayer.setMediaItems(mediaItems, startIndex, 0)
        exoPlayer.prepare()
        exoPlayer.play()
    }

    fun addToQueue(track: PlayableTrack) {
        currentQueue = currentQueue + track
        _state.update { it.copy(queue = currentQueue) }
        exoPlayer.addMediaItem(track.toMediaItem())
    }

    fun addToQueue(tracks: List<PlayableTrack>) {
        currentQueue = currentQueue + tracks
        _state.update { it.copy(queue = currentQueue) }
        tracks.forEach { exoPlayer.addMediaItem(it.toMediaItem()) }
    }

    fun playNext(track: PlayableTrack) {
        val insertIndex = exoPlayer.currentMediaItemIndex + 1
        currentQueue = currentQueue.toMutableList().apply {
            add(insertIndex.coerceAtMost(size), track)
        }
        _state.update { it.copy(queue = currentQueue) }
        exoPlayer.addMediaItem(insertIndex, track.toMediaItem())
    }

    fun removeFromQueue(index: Int) {
        if (index in currentQueue.indices) {
            currentQueue = currentQueue.toMutableList().apply { removeAt(index) }
            _state.update { it.copy(queue = currentQueue) }
            exoPlayer.removeMediaItem(index)
        }
    }

    fun clearQueue() {
        currentQueue = emptyList()
        _state.update {
            it.copy(
                queue = emptyList(),
                currentTrack = null,
                currentIndex = 0
            )
        }
        exoPlayer.clearMediaItems()
    }

    fun skipToQueueItem(index: Int) {
        if (index in currentQueue.indices) {
            exoPlayer.seekTo(index, 0)
        }
    }

    fun stop() {
        exoPlayer.stop()
        _state.update {
            it.copy(
                isPlaying = false,
                position = 0L
            )
        }
    }

    fun release() {
        stopPositionUpdates()
        _exoPlayer?.removeListener(playerListener)
        _exoPlayer?.release()
        _exoPlayer = null
    }
}
