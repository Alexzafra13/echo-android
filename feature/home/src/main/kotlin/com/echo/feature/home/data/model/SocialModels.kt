package com.echo.feature.home.data.model

import kotlinx.serialization.Serializable

@Serializable
data class UserBasic(
    val id: String,
    val username: String,
    val name: String? = null,
    val avatarUrl: String? = null
)

@Serializable
data class Friend(
    val id: String,
    val username: String,
    val name: String? = null,
    val avatarUrl: String? = null,
    val isPublicProfile: Boolean = false,
    val friendshipId: String,
    val friendsSince: String
)

@Serializable
data class Friendship(
    val id: String,
    val requesterId: String,
    val addresseeId: String,
    val status: String, // "pending", "accepted", "blocked"
    val createdAt: String,
    val updatedAt: String
)

@Serializable
data class PendingRequests(
    val received: List<Friend>,
    val sent: List<Friend>,
    val count: Int
)

@Serializable
data class ListeningTrack(
    val id: String,
    val title: String,
    val artistName: String,
    val albumName: String,
    val albumId: String,
    val coverUrl: String? = null
)

@Serializable
data class ListeningUser(
    val id: String,
    val username: String,
    val name: String? = null,
    val avatarUrl: String? = null,
    val isPlaying: Boolean,
    val currentTrack: ListeningTrack? = null,
    val updatedAt: String
)

@Serializable
data class ActivityItem(
    val id: String,
    val user: UserBasic,
    val actionType: String,
    val targetType: String,
    val targetId: String,
    val targetName: String,
    val targetExtra: String? = null,
    val targetCoverUrl: String? = null,
    val targetAlbumId: String? = null,
    val targetAlbumIds: List<String>? = null,
    val secondUser: UserBasic? = null,
    val createdAt: String
)

@Serializable
data class SearchUserResult(
    val id: String,
    val username: String,
    val name: String? = null,
    val avatarUrl: String? = null,
    val friendshipStatus: String? = null // "pending", "accepted", "blocked", null
)

@Serializable
data class SocialOverview(
    val friends: List<Friend>,
    val pendingRequests: PendingRequests,
    val listeningNow: List<ListeningUser>,
    val recentActivity: List<ActivityItem>
)

@Serializable
data class FriendRequestBody(
    val addresseeId: String
)

/**
 * SSE listening update event
 */
@Serializable
data class ListeningUpdate(
    val userId: String,
    val isPlaying: Boolean,
    val currentTrackId: String? = null,
    val timestamp: String
)
