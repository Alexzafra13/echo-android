package com.echo.core.network.interactions

import com.echo.core.datastore.preferences.ServerPreferences
import com.echo.core.network.api.ApiClientFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InteractionsRepository @Inject constructor(
    private val apiClientFactory: ApiClientFactory,
    private val serverPreferences: ServerPreferences
) {
    // Cache of liked items by type
    private val _likedTracks = MutableStateFlow<Set<String>>(emptySet())
    val likedTracks: StateFlow<Set<String>> = _likedTracks.asStateFlow()

    private val _likedAlbums = MutableStateFlow<Set<String>>(emptySet())
    val likedAlbums: StateFlow<Set<String>> = _likedAlbums.asStateFlow()

    private val _likedArtists = MutableStateFlow<Set<String>>(emptySet())
    val likedArtists: StateFlow<Set<String>> = _likedArtists.asStateFlow()

    private suspend fun getApi(): InteractionsApi {
        val server = serverPreferences.activeServer.first()
            ?: throw IllegalStateException("No active server")
        return apiClientFactory.getClient(server.url).create(InteractionsApi::class.java)
    }

    /**
     * Load user's liked items to cache
     */
    suspend fun loadUserInteractions(): Result<Unit> = runCatching {
        val interactions = getApi().getUserInteractions()

        val likedByType = interactions.liked.groupBy { it.itemType }

        _likedTracks.value = likedByType["track"]?.map { it.itemId }?.toSet() ?: emptySet()
        _likedAlbums.value = likedByType["album"]?.map { it.itemId }?.toSet() ?: emptySet()
        _likedArtists.value = likedByType["artist"]?.map { it.itemId }?.toSet() ?: emptySet()
    }

    /**
     * Check if an item is liked
     */
    fun isLiked(itemType: ItemType, itemId: String): Boolean {
        return when (itemType) {
            ItemType.TRACK -> itemId in _likedTracks.value
            ItemType.ALBUM -> itemId in _likedAlbums.value
            ItemType.ARTIST -> itemId in _likedArtists.value
            ItemType.PLAYLIST -> false // Could add playlist likes if needed
        }
    }

    /**
     * Toggle like on an item
     */
    suspend fun toggleLike(itemType: ItemType, itemId: String): Result<Boolean> = runCatching {
        val response = getApi().toggleLike(
            ToggleLikeRequest(
                itemType = itemType.toApiValue(),
                itemId = itemId
            )
        )

        // Update cache
        when (itemType) {
            ItemType.TRACK -> {
                _likedTracks.value = if (response.liked) {
                    _likedTracks.value + itemId
                } else {
                    _likedTracks.value - itemId
                }
            }
            ItemType.ALBUM -> {
                _likedAlbums.value = if (response.liked) {
                    _likedAlbums.value + itemId
                } else {
                    _likedAlbums.value - itemId
                }
            }
            ItemType.ARTIST -> {
                _likedArtists.value = if (response.liked) {
                    _likedArtists.value + itemId
                } else {
                    _likedArtists.value - itemId
                }
            }
            ItemType.PLAYLIST -> { /* No-op for now */ }
        }

        response.liked
    }

    /**
     * Toggle dislike on an item
     */
    suspend fun toggleDislike(itemType: ItemType, itemId: String): Result<ToggleLikeResponse> = runCatching {
        getApi().toggleDislike(
            ToggleLikeRequest(
                itemType = itemType.toApiValue(),
                itemId = itemId
            )
        )
    }

    /**
     * Set rating (1-5 stars)
     */
    suspend fun setRating(itemType: ItemType, itemId: String, rating: Int): Result<UserInteraction> = runCatching {
        require(rating in 1..5) { "Rating must be between 1 and 5" }
        getApi().setRating(
            SetRatingRequest(
                itemType = itemType.toApiValue(),
                itemId = itemId,
                rating = rating
            )
        )
    }

    /**
     * Remove rating
     */
    suspend fun removeRating(itemType: ItemType, itemId: String): Result<Unit> = runCatching {
        getApi().removeRating(itemType.toApiValue(), itemId)
    }

    /**
     * Get interaction summary for an item
     */
    suspend fun getItemInteractions(itemType: ItemType, itemId: String): Result<ItemInteractionSummary> = runCatching {
        getApi().getItemInteractions(itemType.toApiValue(), itemId)
    }

    /**
     * Get all user interactions
     */
    suspend fun getUserInteractions(): Result<UserInteractions> = runCatching {
        getApi().getUserInteractions()
    }
}
