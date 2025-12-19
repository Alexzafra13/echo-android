package com.echo.feature.home.data.api

import com.echo.feature.home.data.model.ActivityItem
import com.echo.feature.home.data.model.Friend
import com.echo.feature.home.data.model.FriendRequestBody
import com.echo.feature.home.data.model.Friendship
import com.echo.feature.home.data.model.ListeningUser
import com.echo.feature.home.data.model.PendingRequests
import com.echo.feature.home.data.model.SearchUserResult
import com.echo.feature.home.data.model.SocialOverview
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface SocialApi {

    /**
     * Get social overview (main page data)
     */
    @GET("social")
    suspend fun getSocialOverview(): SocialOverview

    /**
     * Get all friends
     */
    @GET("social/friends")
    suspend fun getFriends(): List<Friend>

    /**
     * Send a friend request
     */
    @POST("social/friends/request")
    suspend fun sendFriendRequest(@Body body: FriendRequestBody): Friendship

    /**
     * Accept a friend request
     */
    @POST("social/friends/accept/{friendshipId}")
    suspend fun acceptFriendRequest(@Path("friendshipId") friendshipId: String): Friendship

    /**
     * Remove a friendship or reject a request
     */
    @DELETE("social/friends/{friendshipId}")
    suspend fun removeFriendship(@Path("friendshipId") friendshipId: String)

    /**
     * Get pending friend requests
     */
    @GET("social/friends/pending")
    suspend fun getPendingRequests(): PendingRequests

    /**
     * Get friends who are currently listening
     */
    @GET("social/listening")
    suspend fun getListeningFriends(): List<ListeningUser>

    /**
     * Get friends' activity feed
     */
    @GET("social/activity")
    suspend fun getFriendsActivity(@Query("limit") limit: Int? = null): List<ActivityItem>

    /**
     * Search users
     */
    @GET("social/users/search")
    suspend fun searchUsers(
        @Query("q") query: String,
        @Query("limit") limit: Int? = null
    ): List<SearchUserResult>
}
