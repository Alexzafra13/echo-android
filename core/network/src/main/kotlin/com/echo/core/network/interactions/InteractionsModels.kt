package com.echo.core.network.interactions

import kotlinx.serialization.Serializable

/**
 * Item types that can be interacted with
 */
enum class ItemType {
    TRACK,
    ALBUM,
    ARTIST,
    PLAYLIST;

    fun toApiValue(): String = name.lowercase()
}

/**
 * User interaction with an item
 */
@Serializable
data class UserInteraction(
    val id: String,
    val userId: String,
    val itemType: String,
    val itemId: String,
    val liked: Boolean,
    val disliked: Boolean,
    val rating: Int? = null,
    val createdAt: String,
    val updatedAt: String
)

/**
 * Summary of interactions for an item
 */
@Serializable
data class ItemInteractionSummary(
    val itemType: String,
    val itemId: String,
    val likeCount: Int,
    val dislikeCount: Int,
    val avgRating: Float? = null,
    val ratingCount: Int,
    val userInteraction: UserInteraction? = null
)

/**
 * User's interactions list
 */
@Serializable
data class UserInteractions(
    val liked: List<UserInteraction>,
    val disliked: List<UserInteraction>,
    val rated: List<UserInteraction>
)

/**
 * Toggle like/dislike request
 */
@Serializable
data class ToggleLikeRequest(
    val itemType: String,
    val itemId: String
)

/**
 * Set rating request
 */
@Serializable
data class SetRatingRequest(
    val itemType: String,
    val itemId: String,
    val rating: Int
)

/**
 * Toggle like/dislike response
 */
@Serializable
data class ToggleLikeResponse(
    val liked: Boolean,
    val disliked: Boolean
)
