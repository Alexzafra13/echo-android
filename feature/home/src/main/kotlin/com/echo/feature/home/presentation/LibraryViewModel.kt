package com.echo.feature.home.presentation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.echo.core.media.usecase.PlayTracksUseCase
import com.echo.core.media.usecase.TrackInfo
import com.echo.feature.albums.data.repository.AlbumsRepository
import com.echo.feature.albums.domain.model.Album
import com.echo.feature.artists.data.repository.ArtistsRepository
import com.echo.feature.artists.domain.model.Artist
import com.echo.feature.playlists.data.repository.PlaylistsRepository
import com.echo.feature.playlists.domain.model.Playlist
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LibraryState(
    val albums: List<Album> = emptyList(),
    val artists: List<Artist> = emptyList(),
    val playlists: List<Playlist> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingArtists: Boolean = false,
    val isLoadingPlaylists: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val albumsRepository: AlbumsRepository,
    private val artistsRepository: ArtistsRepository,
    private val playlistsRepository: PlaylistsRepository,
    private val playTracksUseCase: PlayTracksUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(LibraryState())
    val state: StateFlow<LibraryState> = _state.asStateFlow()

    init {
        loadAlbums()
        loadArtists()
        loadPlaylists()
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

    private fun loadArtists() {
        viewModelScope.launch {
            _state.update { it.copy(isLoadingArtists = true) }

            artistsRepository.getArtists(skip = 0, take = 100)
                .onSuccess { artists ->
                    // Sort artists alphabetically by name
                    val sortedArtists = artists.sortedBy { it.name.lowercase() }
                    _state.update {
                        it.copy(
                            artists = sortedArtists,
                            isLoadingArtists = false
                        )
                    }
                }
                .onFailure {
                    _state.update {
                        it.copy(isLoadingArtists = false)
                    }
                }
        }
    }

    private fun loadPlaylists() {
        viewModelScope.launch {
            _state.update { it.copy(isLoadingPlaylists = true) }

            playlistsRepository.getPlaylists()
                .onSuccess { playlists ->
                    Log.d("LibraryVM", "Loaded ${playlists.size} playlists")
                    _state.update {
                        it.copy(
                            playlists = playlists,
                            isLoadingPlaylists = false
                        )
                    }
                }
                .onFailure { error ->
                    Log.e("LibraryVM", "Failed to load playlists", error)
                    _state.update {
                        it.copy(isLoadingPlaylists = false)
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
        loadArtists()
        loadPlaylists()
    }
}
