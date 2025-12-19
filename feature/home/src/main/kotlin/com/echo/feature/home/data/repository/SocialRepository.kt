package com.echo.feature.home.data.repository

import com.echo.core.datastore.preferences.ServerPreferences
import com.echo.core.network.api.ApiClientFactory
import com.echo.core.network.sse.SseClient
import com.echo.core.network.sse.SseConnectionState
import com.echo.core.network.sse.SseEvent
import com.echo.feature.home.data.api.SocialApi
import com.echo.feature.home.data.model.ActivityItem
import com.echo.feature.home.data.model.Friend
import com.echo.feature.home.data.model.FriendRequestBody
import com.echo.feature.home.data.model.Friendship
import com.echo.feature.home.data.model.ListeningUpdate
import com.echo.feature.home.data.model.ListeningUser
import com.echo.feature.home.data.model.PendingRequests
import com.echo.feature.home.data.model.SearchUserResult
import com.echo.feature.home.data.model.SocialOverview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SocialRepository @Inject constructor(
    private val apiClientFactory: ApiClientFactory,
    private val sseClient: SseClient,
    private val serverPreferences: ServerPreferences,
    private val json: Json
) {

    private suspend fun getApi(): SocialApi {
        val server = serverPreferences.activeServer.first()
            ?: throw IllegalStateException("No active server")
        return apiClientFactory.getClient(server.url).create(SocialApi::class.java)
    }

    /**
     * Get complete social overview
     */
    suspend fun getSocialOverview(): Result<SocialOverview> = runCatching {
        getApi().getSocialOverview()
    }

    /**
     * Get all friends
     */
    suspend fun getFriends(): Result<List<Friend>> = runCatching {
        getApi().getFriends()
    }

    /**
     * Send a friend request
     */
    suspend fun sendFriendRequest(addresseeId: String): Result<Friendship> = runCatching {
        getApi().sendFriendRequest(FriendRequestBody(addresseeId))
    }

    /**
     * Accept a friend request
     */
    suspend fun acceptFriendRequest(friendshipId: String): Result<Friendship> = runCatching {
        getApi().acceptFriendRequest(friendshipId)
    }

    /**
     * Remove or reject a friendship
     */
    suspend fun removeFriendship(friendshipId: String): Result<Unit> = runCatching {
        getApi().removeFriendship(friendshipId)
    }

    /**
     * Get pending friend requests
     */
    suspend fun getPendingRequests(): Result<PendingRequests> = runCatching {
        getApi().getPendingRequests()
    }

    /**
     * Get friends who are currently listening
     */
    suspend fun getListeningFriends(): Result<List<ListeningUser>> = runCatching {
        getApi().getListeningFriends()
    }

    /**
     * Get friends' activity feed
     */
    suspend fun getFriendsActivity(limit: Int? = null): Result<List<ActivityItem>> = runCatching {
        getApi().getFriendsActivity(limit)
    }

    /**
     * Search users
     */
    suspend fun searchUsers(query: String, limit: Int? = null): Result<List<SearchUserResult>> = runCatching {
        getApi().searchUsers(query, limit)
    }

    /**
     * Connect to listening now SSE stream.
     * Returns a Flow of ListeningUpdate events.
     */
    suspend fun connectToListeningStream(
        userId: String,
        onConnectionState: (SseConnectionState) -> Unit = {}
    ): Flow<ListeningUpdate> {
        val server = serverPreferences.activeServer.first()
            ?: throw IllegalStateException("No active server")

        val baseUrl = server.url.trimEnd('/')
        val sseUrl = "$baseUrl/api/social/listening/stream?userId=${java.net.URLEncoder.encode(userId, "UTF-8")}"

        return sseClient.connectWithReconnect(
            url = sseUrl,
            maxReconnectAttempts = -1, // Infinite reconnects
            initialDelayMs = 1000,
            maxDelayMs = 30000,
            onConnectionState = onConnectionState
        ).mapNotNull { event ->
            parseListeningEvent(event)
        }
    }

    private fun parseListeningEvent(event: SseEvent): ListeningUpdate? {
        return when (event.event) {
            "listening-update" -> {
                try {
                    json.decodeFromString<ListeningUpdate>(event.data)
                } catch (e: Exception) {
                    null
                }
            }
            "connected", "keepalive" -> null
            else -> null
        }
    }
}
