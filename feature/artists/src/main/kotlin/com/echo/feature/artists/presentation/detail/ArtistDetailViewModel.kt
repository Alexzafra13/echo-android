package com.echo.feature.artists.presentation.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.echo.feature.artists.data.repository.ArtistsRepository
import com.echo.feature.artists.domain.model.Artist
import com.echo.feature.artists.domain.model.ArtistAlbum
import com.echo.feature.artists.domain.model.ArtistTopTrack
import com.echo.feature.artists.domain.model.RelatedArtist
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ArtistDetailState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val artist: Artist? = null,
    val albums: List<ArtistAlbum> = emptyList(),
    val topTracks: List<ArtistTopTrack> = emptyList(),
    val relatedArtists: List<RelatedArtist> = emptyList()
)

@HiltViewModel
class ArtistDetailViewModel @Inject constructor(
    private val artistsRepository: ArtistsRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val artistId: String = checkNotNull(savedStateHandle["artistId"])

    private val _state = MutableStateFlow(ArtistDetailState())
    val state: StateFlow<ArtistDetailState> = _state.asStateFlow()

    init {
        loadArtistDetails()
    }

    fun loadArtistDetails() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            artistsRepository.getArtistWithAlbums(artistId)
                .onSuccess { artistWithAlbums ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            artist = artistWithAlbums.artist,
                            albums = artistWithAlbums.albums.sortedByDescending { album -> album.year },
                            topTracks = artistWithAlbums.topTracks,
                            relatedArtists = artistWithAlbums.relatedArtists
                        )
                    }
                }
                .onFailure { exception ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = exception.localizedMessage ?: "Error loading artist"
                        )
                    }
                }
        }
    }

    fun refresh() {
        loadArtistDetails()
    }
}
