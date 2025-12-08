package com.echo.feature.albums.presentation.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.echo.feature.albums.data.repository.AlbumsRepository
import com.echo.feature.albums.domain.model.Album
import com.echo.feature.albums.domain.model.Track
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AlbumDetailState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val album: Album? = null,
    val tracks: List<Track> = emptyList()
)

@HiltViewModel
class AlbumDetailViewModel @Inject constructor(
    private val albumsRepository: AlbumsRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val albumId: String = checkNotNull(savedStateHandle["albumId"])

    private val _state = MutableStateFlow(AlbumDetailState())
    val state: StateFlow<AlbumDetailState> = _state.asStateFlow()

    init {
        loadAlbumDetails()
    }

    fun loadAlbumDetails() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            albumsRepository.getAlbumWithTracks(albumId)
                .onSuccess { albumWithTracks ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            album = albumWithTracks.album,
                            tracks = albumWithTracks.tracks
                        )
                    }
                }
                .onFailure { exception ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = exception.localizedMessage ?: "Error loading album"
                        )
                    }
                }
        }
    }

    fun playAlbum() {
        // TODO: Implement playback
    }

    fun playTrack(track: Track) {
        // TODO: Implement playback
    }

    fun shuffleAlbum() {
        // TODO: Implement shuffle playback
    }
}
