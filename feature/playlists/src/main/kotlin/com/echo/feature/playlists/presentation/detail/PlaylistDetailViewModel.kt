package com.echo.feature.playlists.presentation.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.echo.core.media.player.EchoPlayer
import com.echo.core.media.usecase.PlayTracksUseCase
import com.echo.core.media.usecase.TrackInfo
import com.echo.feature.playlists.data.repository.PlaylistsRepository
import com.echo.feature.playlists.domain.model.Playlist
import com.echo.feature.playlists.domain.model.PlaylistTrack
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PlaylistDetailState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val playlist: Playlist? = null,
    val tracks: List<PlaylistTrack> = emptyList(),
    val showPlaylistMenu: Boolean = false
)

@HiltViewModel
class PlaylistDetailViewModel @Inject constructor(
    private val playlistsRepository: PlaylistsRepository,
    private val playTracksUseCase: PlayTracksUseCase,
    private val player: EchoPlayer,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val playlistId: String = checkNotNull(savedStateHandle["playlistId"])

    private val _state = MutableStateFlow(PlaylistDetailState())
    val state: StateFlow<PlaylistDetailState> = _state.asStateFlow()

    init {
        loadPlaylistDetails()
    }

    fun loadPlaylistDetails() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            playlistsRepository.getPlaylistWithTracks(playlistId)
                .onSuccess { playlistWithTracks ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            playlist = playlistWithTracks.playlist,
                            tracks = playlistWithTracks.tracks
                        )
                    }
                }
                .onFailure { exception ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = exception.localizedMessage ?: "Error loading playlist"
                        )
                    }
                }
        }
    }

    fun playPlaylist() {
        val playlist = _state.value.playlist ?: return
        val tracks = _state.value.tracks
        if (tracks.isEmpty()) return

        viewModelScope.launch {
            try {
                val trackInfos = tracks.map { it.toTrackInfo(playlist) }
                playTracksUseCase.playTracks(trackInfos, 0)
            } catch (e: Exception) {
                _state.update { it.copy(error = e.localizedMessage) }
            }
        }
    }

    fun playTrack(track: PlaylistTrack) {
        val playlist = _state.value.playlist ?: return
        val tracks = _state.value.tracks
        val startIndex = tracks.indexOf(track).coerceAtLeast(0)

        viewModelScope.launch {
            try {
                val trackInfos = tracks.map { it.toTrackInfo(playlist) }
                playTracksUseCase.playTracks(trackInfos, startIndex)
            } catch (e: Exception) {
                _state.update { it.copy(error = e.localizedMessage) }
            }
        }
    }

    fun shufflePlaylist() {
        val playlist = _state.value.playlist ?: return
        val tracks = _state.value.tracks.shuffled()
        if (tracks.isEmpty()) return

        viewModelScope.launch {
            try {
                player.setShuffleEnabled(true)
                val trackInfos = tracks.map { it.toTrackInfo(playlist) }
                playTracksUseCase.playTracks(trackInfos, 0)
            } catch (e: Exception) {
                _state.update { it.copy(error = e.localizedMessage) }
            }
        }
    }

    fun removeTrack(track: PlaylistTrack) {
        viewModelScope.launch {
            playlistsRepository.removeTrackFromPlaylist(playlistId, track.trackId)
                .onSuccess {
                    // Remove from local state
                    _state.update { currentState ->
                        currentState.copy(
                            tracks = currentState.tracks.filter { it.id != track.id },
                            playlist = currentState.playlist?.copy(
                                trackCount = (currentState.playlist.trackCount - 1).coerceAtLeast(0)
                            )
                        )
                    }
                }
                .onFailure { exception ->
                    _state.update { it.copy(error = exception.localizedMessage) }
                }
        }
    }

    fun refresh() {
        loadPlaylistDetails()
    }

    private fun PlaylistTrack.toTrackInfo(playlist: Playlist): TrackInfo {
        return TrackInfo(
            id = trackId,
            title = title,
            artist = artistName ?: "Unknown Artist",
            albumId = albumId ?: "",
            albumTitle = albumTitle ?: "Unknown Album",
            duration = (duration ?: 0) * 1000L,
            trackNumber = trackNumber ?: order,
            coverUrl = coverUrl
        )
    }

    // Menu management
    fun showPlaylistMenu() {
        _state.update { it.copy(showPlaylistMenu = true) }
    }

    fun hidePlaylistMenu() {
        _state.update { it.copy(showPlaylistMenu = false) }
    }

    // Queue operations
    fun addTrackToQueue(track: PlaylistTrack) {
        val playlist = _state.value.playlist ?: return
        viewModelScope.launch {
            try {
                playTracksUseCase.addToQueue(track.toTrackInfo(playlist))
            } catch (e: Exception) {
                _state.update { it.copy(error = e.localizedMessage) }
            }
        }
    }

    fun playTrackNext(track: PlaylistTrack) {
        val playlist = _state.value.playlist ?: return
        viewModelScope.launch {
            try {
                playTracksUseCase.playNext(track.toTrackInfo(playlist))
            } catch (e: Exception) {
                _state.update { it.copy(error = e.localizedMessage) }
            }
        }
    }

    fun addPlaylistToQueue() {
        val playlist = _state.value.playlist ?: return
        val tracks = _state.value.tracks
        if (tracks.isEmpty()) return

        viewModelScope.launch {
            try {
                tracks.forEach { track ->
                    playTracksUseCase.addToQueue(track.toTrackInfo(playlist))
                }
            } catch (e: Exception) {
                _state.update { it.copy(error = e.localizedMessage) }
            }
        }
    }
}
