package com.echo.core.media.tracking

import kotlinx.serialization.Serializable

/**
 * Play context types - determines how a track was initiated for weighted scoring
 */
enum class PlayContext {
    DIRECT,         // Direct play from track list (weight: 1.0)
    SEARCH,         // From search results (weight: 0.9)
    PLAYLIST,       // From playlist (weight: 0.8)
    ARTIST,         // From artist page (weight: 0.75)
    QUEUE,          // From queue (weight: 0.7)
    ALBUM,          // From album view (weight: 0.6)
    RECOMMENDATION, // From recommendations (weight: 0.7)
    RADIO,          // From radio/discovery (weight: 0.4)
    SHUFFLE;        // From shuffle mode (weight: 0.2)

    fun toApiValue(): String = name.lowercase()
}

/**
 * Source type for play tracking
 */
enum class SourceType {
    PLAYLIST,
    ALBUM,
    ARTIST,
    SEARCH,
    QUEUE,
    RECOMMENDATION;

    fun toApiValue(): String = name.lowercase()
}

/**
 * Play event data to send to backend
 */
@Serializable
data class RecordPlayData(
    val trackId: String,
    val playContext: String,
    val completionRate: Float? = null,
    val sourceId: String? = null,
    val sourceType: String? = null
)

/**
 * Skip event data
 */
@Serializable
data class RecordSkipData(
    val trackId: String,
    val playContext: String,
    val completionRate: Float,
    val sourceId: String? = null,
    val sourceType: String? = null
)

/**
 * Play history entry
 */
@Serializable
data class PlayHistoryEntry(
    val id: String,
    val userId: String,
    val trackId: String,
    val playedAt: String,
    val playContext: String,
    val completionRate: Float? = null,
    val skipped: Boolean,
    val sourceId: String? = null,
    val sourceType: String? = null,
    val track: PlayHistoryTrack? = null
)

@Serializable
data class PlayHistoryTrack(
    val id: String,
    val title: String,
    val artistName: String? = null,
    val albumName: String? = null,
    val duration: Int? = null
)

/**
 * User play summary statistics
 */
@Serializable
data class PlaySummary(
    val totalPlays: Int,
    val uniqueTracks: Int,
    val totalListeningTime: Long, // in seconds
    val avgCompletionRate: Float? = null,
    val topPlayContext: String? = null,
    val recentActivity: RecentActivity
)

@Serializable
data class RecentActivity(
    val last7Days: Int,
    val last30Days: Int
)

/**
 * Top track with play stats
 */
@Serializable
data class TopTrack(
    val trackId: String,
    val playCount: Int,
    val weightedPlayCount: Float,
    val avgCompletionRate: Float? = null,
    val skipCount: Int,
    val lastPlayedAt: String,
    val track: PlayHistoryTrack? = null
)

/**
 * Recently played track
 */
@Serializable
data class RecentlyPlayed(
    val trackId: String,
    val playedAt: String,
    val playContext: String,
    val completionRate: Float? = null,
    val track: PlayHistoryTrack? = null
)

/**
 * Playback state data for social "listening now" feature
 */
@Serializable
data class PlaybackStateData(
    val isPlaying: Boolean,
    val currentTrackId: String? = null
)
