package com.echo.feature.albums.presentation.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.echo.core.media.player.EchoPlayer
import com.echo.core.media.usecase.PlayTracksUseCase
import com.echo.core.media.usecase.TrackInfo
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
    private val playTracksUseCase: PlayTracksUseCase,
    private val player: EchoPlayer,
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
        val album = _state.value.album ?: return
        val tracks = _state.value.tracks
        if (tracks.isEmpty()) return

        viewModelScope.launch {
            try {
                val trackInfos = tracks.map { it.toTrackInfo(album) }
                playTracksUseCase.playTracks(trackInfos, 0)
            } catch (e: Exception) {
                _state.update { it.copy(error = e.localizedMessage) }
            }
        }
    }

    fun playTrack(track: Track) {
        val album = _state.value.album ?: return
        val tracks = _state.value.tracks
        val startIndex = tracks.indexOf(track).coerceAtLeast(0)

        viewModelScope.launch {
            try {
                val trackInfos = tracks.map { it.toTrackInfo(album) }
                playTracksUseCase.playTracks(trackInfos, startIndex)
            } catch (e: Exception) {
                _state.update { it.copy(error = e.localizedMessage) }
            }
        }
    }

    fun shuffleAlbum() {
        val album = _state.value.album ?: return
        val tracks = _state.value.tracks.shuffled()
        if (tracks.isEmpty()) return

        viewModelScope.launch {
            try {
                player.setShuffleEnabled(true)
                val trackInfos = tracks.map { it.toTrackInfo(album) }
                playTracksUseCase.playTracks(trackInfos, 0)
            } catch (e: Exception) {
                _state.update { it.copy(error = e.localizedMessage) }
            }
        }
    }

    private fun Track.toTrackInfo(album: Album): TrackInfo {
        return TrackInfo(
            id = id,
            title = title,
            artist = artistName ?: album.artist,
            albumId = album.id,
            albumTitle = album.title,
            duration = (duration ?: 0) * 1000L, // Convert seconds to milliseconds
            trackNumber = trackNumber ?: 0,
            coverUrl = coverUrl ?: album.coverUrl
        )
    }
}
