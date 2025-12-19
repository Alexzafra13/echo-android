package com.echo.feature.home.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.echo.core.datastore.preferences.SessionPreferences
import com.echo.core.network.sse.SseConnectionState
import com.echo.feature.home.data.model.ActivityItem
import com.echo.feature.home.data.model.Friend
import com.echo.feature.home.data.model.ListeningUser
import com.echo.feature.home.data.model.PendingRequests
import com.echo.feature.home.data.model.SearchUserResult
import com.echo.feature.home.data.repository.SocialRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SocialUiState(
    val isLoading: Boolean = true,
    val friends: List<Friend> = emptyList(),
    val pendingRequests: PendingRequests = PendingRequests(emptyList(), emptyList(), 0),
    val listeningNow: List<ListeningUser> = emptyList(),
    val recentActivity: List<ActivityItem> = emptyList(),
    val searchQuery: String = "",
    val searchResults: List<SearchUserResult> = emptyList(),
    val isSearching: Boolean = false,
    val sseConnected: Boolean = false,
    val error: String? = null,
    val selectedTab: SocialTab = SocialTab.FRIENDS
)

enum class SocialTab {
    FRIENDS,
    ACTIVITY,
    DISCOVER
}

@HiltViewModel
class SocialViewModel @Inject constructor(
    private val socialRepository: SocialRepository,
    private val sessionPreferences: SessionPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(SocialUiState())
    val uiState: StateFlow<SocialUiState> = _uiState.asStateFlow()

    private var sseJob: Job? = null

    init {
        loadSocialData()
        connectToListeningStream()
    }

    fun loadSocialData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            socialRepository.getSocialOverview()
                .onSuccess { overview ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            friends = overview.friends,
                            pendingRequests = overview.pendingRequests,
                            listeningNow = overview.listeningNow,
                            recentActivity = overview.recentActivity
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = error.message ?: "Error loading social data"
                        )
                    }
                }
        }
    }

    private fun connectToListeningStream() {
        sseJob?.cancel()
        sseJob = viewModelScope.launch {
            try {
                val session = sessionPreferences.session.first()
                val userId = session?.userId ?: return@launch

                socialRepository.connectToListeningStream(
                    userId = userId,
                    onConnectionState = { state ->
                        _uiState.update {
                            it.copy(sseConnected = state is SseConnectionState.Connected)
                        }
                    }
                ).collect { update ->
                    // When we receive an update, refresh the listening data
                    refreshListeningNow()
                }
            } catch (e: Exception) {
                // SSE connection failed, still functional via polling
                android.util.Log.e("SocialViewModel", "SSE connection failed", e)
            }
        }
    }

    private fun refreshListeningNow() {
        viewModelScope.launch {
            socialRepository.getListeningFriends()
                .onSuccess { users ->
                    _uiState.update { it.copy(listeningNow = users) }
                }
        }
    }

    fun selectTab(tab: SocialTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        if (query.length >= 2) {
            searchUsers(query)
        } else {
            _uiState.update { it.copy(searchResults = emptyList()) }
        }
    }

    private fun searchUsers(query: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSearching = true) }

            socialRepository.searchUsers(query, limit = 20)
                .onSuccess { results ->
                    _uiState.update {
                        it.copy(
                            searchResults = results,
                            isSearching = false
                        )
                    }
                }
                .onFailure {
                    _uiState.update { it.copy(isSearching = false) }
                }
        }
    }

    fun sendFriendRequest(userId: String) {
        viewModelScope.launch {
            socialRepository.sendFriendRequest(userId)
                .onSuccess {
                    // Update search results to reflect pending status
                    _uiState.update { state ->
                        state.copy(
                            searchResults = state.searchResults.map { user ->
                                if (user.id == userId) {
                                    user.copy(friendshipStatus = "pending")
                                } else {
                                    user
                                }
                            }
                        )
                    }
                    // Reload pending requests
                    loadPendingRequests()
                }
                .onFailure { error ->
                    _uiState.update { it.copy(error = error.message) }
                }
        }
    }

    fun acceptFriendRequest(friendshipId: String) {
        viewModelScope.launch {
            socialRepository.acceptFriendRequest(friendshipId)
                .onSuccess {
                    loadSocialData() // Refresh all data
                }
                .onFailure { error ->
                    _uiState.update { it.copy(error = error.message) }
                }
        }
    }

    fun rejectFriendRequest(friendshipId: String) {
        viewModelScope.launch {
            socialRepository.removeFriendship(friendshipId)
                .onSuccess {
                    loadPendingRequests()
                }
                .onFailure { error ->
                    _uiState.update { it.copy(error = error.message) }
                }
        }
    }

    fun removeFriend(friendshipId: String) {
        viewModelScope.launch {
            socialRepository.removeFriendship(friendshipId)
                .onSuccess {
                    _uiState.update { state ->
                        state.copy(
                            friends = state.friends.filter { it.friendshipId != friendshipId }
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(error = error.message) }
                }
        }
    }

    private fun loadPendingRequests() {
        viewModelScope.launch {
            socialRepository.getPendingRequests()
                .onSuccess { pending ->
                    _uiState.update { it.copy(pendingRequests = pending) }
                }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    override fun onCleared() {
        super.onCleared()
        sseJob?.cancel()
    }
}
