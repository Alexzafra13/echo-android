package com.echo.feature.search.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.echo.core.media.usecase.PlayTracksUseCase
import com.echo.core.media.usecase.TrackInfo
import com.echo.feature.search.data.repository.SearchRepository
import com.echo.feature.search.domain.model.SearchAlbum
import com.echo.feature.search.domain.model.SearchArtist
import com.echo.feature.search.domain.model.SearchResults
import com.echo.feature.search.domain.model.SearchTrack
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchRepository: SearchRepository,
    private val playTracksUseCase: PlayTracksUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(SearchResults())
    val state: StateFlow<SearchResults> = _state.asStateFlow()

    private var searchJob: Job? = null

    fun onQueryChange(query: String) {
        _state.update { it.copy(query = query) }

        // Debounce search
        searchJob?.cancel()
        if (query.length >= 2) {
            searchJob = viewModelScope.launch {
                delay(300) // Debounce 300ms
                search(query)
            }
        } else {
            // Clear results for short queries
            _state.update {
                it.copy(
                    albums = emptyList(),
                    artists = emptyList(),
                    tracks = emptyList(),
                    isLoading = false
                )
            }
        }
    }

    private fun search(query: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            val (albumsResult, artistsResult, tracksResult) = searchRepository.searchAll(query)

            _state.update {
                it.copy(
                    albums = albumsResult.getOrDefault(emptyList()),
                    artists = artistsResult.getOrDefault(emptyList()),
                    tracks = tracksResult.getOrDefault(emptyList()),
                    isLoading = false
                )
            }
        }
    }

    fun playTrack(track: SearchTrack) {
        viewModelScope.launch {
            playTracksUseCase.playTrack(
                trackId = track.id,
                title = track.title,
                artist = track.artistName ?: "Unknown",
                albumId = track.albumId ?: "",
                albumTitle = track.albumName ?: "",
                duration = (track.duration ?: 0) * 1000L,
                trackNumber = 0,
                coverUrl = track.coverUrl
            )
        }
    }

    fun playAllTracks(startIndex: Int = 0) {
        viewModelScope.launch {
            val tracks = _state.value.tracks
            if (tracks.isNotEmpty()) {
                val trackInfos = tracks.map { track ->
                    TrackInfo(
                        id = track.id,
                        title = track.title,
                        artist = track.artistName ?: "Unknown",
                        albumId = track.albumId ?: "",
                        albumTitle = track.albumName ?: "",
                        duration = (track.duration ?: 0) * 1000L,
                        trackNumber = 0,
                        coverUrl = track.coverUrl
                    )
                }
                playTracksUseCase.playTracks(trackInfos, startIndex)
            }
        }
    }

    fun clearSearch() {
        _state.update {
            SearchResults()
        }
    }
}
