package com.echo.feature.home.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.echo.core.media.usecase.PlayTracksUseCase
import com.echo.core.media.usecase.TrackInfo
import com.echo.feature.albums.data.repository.AlbumsRepository
import com.echo.feature.albums.domain.model.Album
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LibraryState(
    val albums: List<Album> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val albumsRepository: AlbumsRepository,
    private val playTracksUseCase: PlayTracksUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(LibraryState())
    val state: StateFlow<LibraryState> = _state.asStateFlow()

    init {
        loadAlbums()
    }

    private fun loadAlbums() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            albumsRepository.getAlbums(skip = 0, take = 100)
                .onSuccess { albums ->
                    _state.update {
                        it.copy(
                            albums = albums,
                            isLoading = false,
                            error = null
                        )
                    }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = error.message
                        )
                    }
                }
        }
    }

    fun playAlbum(album: Album) {
        viewModelScope.launch {
            albumsRepository.getAlbumWithTracks(album.id)
                .onSuccess { albumWithTracks ->
                    val trackInfos = albumWithTracks.tracks.map { track ->
                        TrackInfo(
                            id = track.id,
                            title = track.title,
                            artist = track.artistName ?: album.artist,
                            albumId = album.id,
                            albumTitle = album.title,
                            duration = (track.duration ?: 0) * 1000L,
                            trackNumber = track.trackNumber ?: 0,
                            coverUrl = album.coverUrl
                        )
                    }
                    if (trackInfos.isNotEmpty()) {
                        playTracksUseCase.playTracks(trackInfos, 0)
                    }
                }
        }
    }

    fun refresh() {
        loadAlbums()
    }
}
