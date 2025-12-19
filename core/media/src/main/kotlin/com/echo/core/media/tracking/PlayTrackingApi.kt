package com.echo.core.media.tracking

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Query

interface PlayTrackingApi {

    /**
     * Record a play event
     */
    @POST("play-tracking/play")
    suspend fun recordPlay(@Body data: RecordPlayData)

    /**
     * Record a skip event
     */
    @POST("play-tracking/skip")
    suspend fun recordSkip(@Body data: RecordSkipData)

    /**
     * Get user's play history
     */
    @GET("play-tracking/history")
    suspend fun getPlayHistory(@Query("limit") limit: Int = 50): List<PlayHistoryEntry>

    /**
     * Get user's top tracks
     */
    @GET("play-tracking/top-tracks")
    suspend fun getTopTracks(
        @Query("limit") limit: Int = 20,
        @Query("timeRange") timeRange: String = "all"
    ): List<TopTrack>

    /**
     * Get recently played tracks
     */
    @GET("play-tracking/recently-played")
    suspend fun getRecentlyPlayed(@Query("limit") limit: Int = 20): List<RecentlyPlayed>

    /**
     * Get user's play summary statistics
     */
    @GET("play-tracking/summary")
    suspend fun getPlaySummary(): PlaySummary

    /**
     * Update current playback state for social "listening now" feature
     */
    @PUT("play-tracking/playback-state")
    suspend fun updatePlaybackState(@Body data: PlaybackStateData)
}
