package com.echo.core.media.tracking

import android.util.Log
import com.echo.core.datastore.preferences.ServerPreferences
import com.echo.core.network.api.ApiClientFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service for tracking play events and updating playback state.
 * Integrates with the backend for analytics and social "listening now" feature.
 */
@Singleton
class PlayTrackingService @Inject constructor(
    private val apiClientFactory: ApiClientFactory,
    private val serverPreferences: ServerPreferences
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var currentTrackId: String? = null
    private var currentContext: PlayContext = PlayContext.DIRECT
    private var currentSourceId: String? = null
    private var currentSourceType: SourceType? = null
    private var playStartTime: Long = 0
    private var lastPositionMs: Long = 0

    private suspend fun getApi(): PlayTrackingApi? {
        return try {
            val server = serverPreferences.activeServer.first() ?: return null
            apiClientFactory.getClient(server.url).create(PlayTrackingApi::class.java)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get API", e)
            null
        }
    }

    /**
     * Set the play context for the current/next track
     */
    fun setPlayContext(
        context: PlayContext,
        sourceId: String? = null,
        sourceType: SourceType? = null
    ) {
        currentContext = context
        currentSourceId = sourceId
        currentSourceType = sourceType
    }

    /**
     * Called when a new track starts playing
     */
    fun onTrackStarted(trackId: String) {
        // Record previous track if there was one playing
        if (currentTrackId != null && currentTrackId != trackId) {
            val listenedMs = lastPositionMs
            val wasPlaying = playStartTime > 0
            if (wasPlaying && listenedMs > 0) {
                // Record the previous track's play
                recordPlayInternal(currentTrackId!!, listenedMs)
            }
        }

        currentTrackId = trackId
        playStartTime = System.currentTimeMillis()
        lastPositionMs = 0

        // Update playback state for social "listening now"
        updatePlaybackStateInternal(isPlaying = true, trackId = trackId)
    }

    /**
     * Called when playback position changes
     */
    fun onPositionChanged(positionMs: Long) {
        lastPositionMs = positionMs
    }

    /**
     * Called when track completes
     */
    fun onTrackCompleted(trackId: String, durationMs: Long) {
        if (trackId == currentTrackId) {
            recordPlayInternal(trackId, durationMs, completionRate = 1.0f)
            currentTrackId = null
            playStartTime = 0
            lastPositionMs = 0
        }
    }

    /**
     * Called when user skips to next track
     */
    fun onTrackSkipped(trackId: String, positionMs: Long, durationMs: Long) {
        if (trackId == currentTrackId && durationMs > 0) {
            val completionRate = positionMs.toFloat() / durationMs.toFloat()

            scope.launch {
                try {
                    getApi()?.recordSkip(
                        RecordSkipData(
                            trackId = trackId,
                            playContext = currentContext.toApiValue(),
                            completionRate = completionRate,
                            sourceId = currentSourceId,
                            sourceType = currentSourceType?.toApiValue()
                        )
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to record skip", e)
                }
            }
        }
    }

    /**
     * Called when playback is paused
     */
    fun onPlaybackPaused() {
        updatePlaybackStateInternal(isPlaying = false, trackId = currentTrackId)
    }

    /**
     * Called when playback is resumed
     */
    fun onPlaybackResumed() {
        updatePlaybackStateInternal(isPlaying = true, trackId = currentTrackId)
    }

    /**
     * Called when playback is stopped
     */
    fun onPlaybackStopped() {
        // Record partial play if there was one
        if (currentTrackId != null && lastPositionMs > 0) {
            recordPlayInternal(currentTrackId!!, lastPositionMs)
        }

        updatePlaybackStateInternal(isPlaying = false, trackId = null)

        currentTrackId = null
        playStartTime = 0
        lastPositionMs = 0
    }

    private fun recordPlayInternal(
        trackId: String,
        listenedMs: Long,
        completionRate: Float? = null
    ) {
        // Only record if listened for at least 10 seconds
        if (listenedMs < 10_000) return

        scope.launch {
            try {
                getApi()?.recordPlay(
                    RecordPlayData(
                        trackId = trackId,
                        playContext = currentContext.toApiValue(),
                        completionRate = completionRate,
                        sourceId = currentSourceId,
                        sourceType = currentSourceType?.toApiValue()
                    )
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to record play", e)
            }
        }
    }

    private fun updatePlaybackStateInternal(isPlaying: Boolean, trackId: String?) {
        scope.launch {
            try {
                getApi()?.updatePlaybackState(
                    PlaybackStateData(
                        isPlaying = isPlaying,
                        currentTrackId = trackId
                    )
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update playback state", e)
            }
        }
    }

    /**
     * Get play history
     */
    suspend fun getPlayHistory(limit: Int = 50): Result<List<PlayHistoryEntry>> = runCatching {
        getApi()?.getPlayHistory(limit) ?: emptyList()
    }

    /**
     * Get top tracks
     */
    suspend fun getTopTracks(
        limit: Int = 20,
        timeRange: String = "all"
    ): Result<List<TopTrack>> = runCatching {
        getApi()?.getTopTracks(limit, timeRange) ?: emptyList()
    }

    /**
     * Get recently played
     */
    suspend fun getRecentlyPlayed(limit: Int = 20): Result<List<RecentlyPlayed>> = runCatching {
        getApi()?.getRecentlyPlayed(limit) ?: emptyList()
    }

    /**
     * Get play summary
     */
    suspend fun getPlaySummary(): Result<PlaySummary> = runCatching {
        getApi()?.getPlaySummary() ?: throw IllegalStateException("No server connection")
    }

    companion object {
        private const val TAG = "PlayTrackingService"
    }
}
