package com.echo.feature.home.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.echo.feature.albums.data.repository.AlbumsRepository
import com.echo.feature.albums.domain.model.Album
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val featuredAlbum: Album? = null,
    val recentAlbums: List<Album> = emptyList(),
    val topPlayedAlbums: List<Album> = emptyList(),
    val recentlyPlayedAlbums: List<Album> = emptyList()
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val albumsRepository: AlbumsRepository
) : ViewModel() {

    private val _state = MutableStateFlow(HomeState())
    val state: StateFlow<HomeState> = _state.asStateFlow()

    init {
        loadHomeData()
    }

    fun loadHomeData() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            try {
                // Load all sections in parallel
                val featuredResult = albumsRepository.getFeaturedAlbum()
                val recentResult = albumsRepository.getRecentAlbums(10)
                val topPlayedResult = albumsRepository.getTopPlayedAlbums(10)
                val recentlyPlayedResult = albumsRepository.getRecentlyPlayedAlbums(10)

                _state.update { currentState ->
                    currentState.copy(
                        isLoading = false,
                        featuredAlbum = featuredResult.getOrNull(),
                        recentAlbums = recentResult.getOrDefault(emptyList()),
                        topPlayedAlbums = topPlayedResult.getOrDefault(emptyList()),
                        recentlyPlayedAlbums = recentlyPlayedResult.getOrDefault(emptyList())
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = e.localizedMessage ?: "Error loading data"
                    )
                }
            }
        }
    }

    fun refresh() {
        loadHomeData()
    }
}
