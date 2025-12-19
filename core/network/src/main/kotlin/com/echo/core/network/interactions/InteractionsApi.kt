package com.echo.core.network.interactions

import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface InteractionsApi {

    /**
     * Toggle like on an item
     */
    @POST("interactions/like")
    suspend fun toggleLike(@Body request: ToggleLikeRequest): ToggleLikeResponse

    /**
     * Toggle dislike on an item
     */
    @POST("interactions/dislike")
    suspend fun toggleDislike(@Body request: ToggleLikeRequest): ToggleLikeResponse

    /**
     * Set rating (1-5 stars)
     */
    @POST("interactions/rating")
    suspend fun setRating(@Body request: SetRatingRequest): UserInteraction

    /**
     * Remove rating
     */
    @DELETE("interactions/rating/{itemType}/{itemId}")
    suspend fun removeRating(
        @Path("itemType") itemType: String,
        @Path("itemId") itemId: String
    )

    /**
     * Get user's interactions
     */
    @GET("interactions/me")
    suspend fun getUserInteractions(): UserInteractions

    /**
     * Get interaction summary for an item
     */
    @GET("interactions/item/{itemType}/{itemId}")
    suspend fun getItemInteractions(
        @Path("itemType") itemType: String,
        @Path("itemId") itemId: String
    ): ItemInteractionSummary
}
