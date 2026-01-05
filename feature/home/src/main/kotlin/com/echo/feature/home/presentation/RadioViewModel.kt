package com.echo.feature.home.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.echo.core.media.model.PlayableRadioStation
import com.echo.core.media.model.RadioMetadata
import com.echo.core.media.model.RadioSignalStatus
import com.echo.core.media.radio.PlayableRadioStationBuilder
import com.echo.core.media.radio.RadioPlaybackManager
import com.echo.feature.home.data.model.RadioBrowserCountry
import com.echo.feature.home.data.model.RadioBrowserStation
import com.echo.feature.home.data.model.RadioBrowserTag
import com.echo.feature.home.data.model.RadioStation
import com.echo.feature.home.data.repository.RadioRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RadioUiState(
    val isLoading: Boolean = true,
    val selectedTab: RadioTab = RadioTab.FAVORITES,
    val favorites: List<RadioStation> = emptyList(),
    val topVoted: List<RadioBrowserStation> = emptyList(),
    val popular: List<RadioBrowserStation> = emptyList(),
    val searchResults: List<RadioBrowserStation> = emptyList(),
    val genres: List<RadioBrowserTag> = emptyList(),
    val countries: List<RadioBrowserCountry> = emptyList(),
    val selectedGenre: String? = null,
    val selectedCountry: String? = null,
    val genreStations: List<RadioBrowserStation> = emptyList(),
    val countryStations: List<RadioBrowserStation> = emptyList(),
    val searchQuery: String = "",
    val isSearching: Boolean = false,
    val error: String? = null,
    val favoriteIds: Set<String> = emptySet(),
    // Playback state
    val isRadioPlaying: Boolean = false,
    val isRadioBuffering: Boolean = false,
    val currentPlayingStation: PlayableRadioStation? = null,
    val currentMetadata: RadioMetadata? = null,
    val signalStatus: RadioSignalStatus = RadioSignalStatus.UNKNOWN
)

enum class RadioTab {
    FAVORITES,
    DISCOVER,
    BROWSE
}

@HiltViewModel
class RadioViewModel @Inject constructor(
    private val radioRepository: RadioRepository,
    private val radioPlaybackManager: RadioPlaybackManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(RadioUiState())
    val uiState: StateFlow<RadioUiState> = _uiState.asStateFlow()

    init {
        loadInitialData()
        observePlaybackState()
    }

    /**
     * Observe playback state from RadioPlaybackManager
     */
    private fun observePlaybackState() {
        viewModelScope.launch {
            radioPlaybackManager.state.collect { playbackState ->
                _uiState.update {
                    it.copy(
                        isRadioPlaying = playbackState.isPlaying,
                        isRadioBuffering = playbackState.isBuffering,
                        currentPlayingStation = playbackState.currentStation,
                        currentMetadata = playbackState.metadata,
                        signalStatus = playbackState.signalStatus
                    )
                }
            }
        }
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            // Load favorites first
            radioRepository.getFavorites()
                .onSuccess { favorites ->
                    _uiState.update {
                        it.copy(
                            favorites = favorites,
                            favoriteIds = favorites.mapNotNull { station -> station.stationUuid }.toSet()
                        )
                    }
                }

            // Load top voted and popular for discover
            launch {
                radioRepository.getTopVoted(20)
                    .onSuccess { stations ->
                        _uiState.update { it.copy(topVoted = stations) }
                    }
            }

            launch {
                radioRepository.getPopular(20)
                    .onSuccess { stations ->
                        _uiState.update { it.copy(popular = stations) }
                    }
            }

            // Load genres and countries for browsing
            launch {
                radioRepository.getTags(50)
                    .onSuccess { tags ->
                        _uiState.update { it.copy(genres = tags.sortedByDescending { it.stationcount }) }
                    }
            }

            launch {
                radioRepository.getCountries()
                    .onSuccess { countries ->
                        _uiState.update { it.copy(countries = countries.sortedByDescending { it.stationcount }) }
                    }
            }

            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun selectTab(tab: RadioTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        if (query.length >= 2) {
            searchStations(query)
        } else {
            _uiState.update { it.copy(searchResults = emptyList()) }
        }
    }

    private fun searchStations(query: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSearching = true) }

            radioRepository.searchStations(name = query, limit = 50)
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

    fun selectGenre(genre: String) {
        _uiState.update { it.copy(selectedGenre = genre, selectedCountry = null) }
        loadGenreStations(genre)
    }

    private fun loadGenreStations(genre: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            radioRepository.getByTag(genre, 50)
                .onSuccess { stations ->
                    _uiState.update { it.copy(genreStations = stations, isLoading = false) }
                }
                .onFailure {
                    _uiState.update { it.copy(isLoading = false) }
                }
        }
    }

    fun selectCountry(countryCode: String) {
        _uiState.update { it.copy(selectedCountry = countryCode, selectedGenre = null) }
        loadCountryStations(countryCode)
    }

    private fun loadCountryStations(countryCode: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            radioRepository.getByCountry(countryCode, 50)
                .onSuccess { stations ->
                    _uiState.update { it.copy(countryStations = stations, isLoading = false) }
                }
                .onFailure {
                    _uiState.update { it.copy(isLoading = false) }
                }
        }
    }

    fun clearBrowseSelection() {
        _uiState.update {
            it.copy(
                selectedGenre = null,
                selectedCountry = null,
                genreStations = emptyList(),
                countryStations = emptyList()
            )
        }
    }

    fun toggleFavorite(station: RadioBrowserStation) {
        viewModelScope.launch {
            val isFavorite = station.stationuuid in _uiState.value.favoriteIds

            if (isFavorite) {
                // Find and delete
                val favorite = _uiState.value.favorites.find { it.stationUuid == station.stationuuid }
                favorite?.id?.let { id ->
                    radioRepository.deleteFavorite(id)
                        .onSuccess {
                            _uiState.update { state ->
                                state.copy(
                                    favorites = state.favorites.filter { it.id != id },
                                    favoriteIds = state.favoriteIds - station.stationuuid
                                )
                            }
                        }
                }
            } else {
                // Add to favorites
                radioRepository.saveFavorite(station)
                    .onSuccess { savedStation ->
                        _uiState.update { state ->
                            state.copy(
                                favorites = state.favorites + savedStation,
                                favoriteIds = state.favoriteIds + station.stationuuid
                            )
                        }
                    }
            }
        }
    }

    fun deleteFavorite(station: RadioStation) {
        viewModelScope.launch {
            station.id?.let { id ->
                radioRepository.deleteFavorite(id)
                    .onSuccess {
                        _uiState.update { state ->
                            state.copy(
                                favorites = state.favorites.filter { it.id != id },
                                favoriteIds = station.stationUuid?.let { uuid ->
                                    state.favoriteIds - uuid
                                } ?: state.favoriteIds
                            )
                        }
                    }
                    .onFailure { error ->
                        _uiState.update { it.copy(error = error.message) }
                    }
            }
        }
    }

    /**
     * Play a radio station (from RadioBrowserStation or RadioStation)
     */
    fun playStation(station: Any) {
        val playable = when (station) {
            is RadioBrowserStation -> PlayableRadioStationBuilder.fromRadioBrowser(
                stationuuid = station.stationuuid,
                name = station.name,
                url = station.url,
                urlResolved = station.urlResolved,
                favicon = station.favicon,
                country = station.country,
                countrycode = station.countrycode,
                tags = station.tags,
                codec = station.codec,
                bitrate = station.bitrate,
                lastcheckok = station.lastcheckok
            )
            is RadioStation -> PlayableRadioStationBuilder.fromFavorite(
                id = station.id,
                stationUuid = station.stationUuid,
                name = station.name,
                url = station.url,
                urlResolved = station.urlResolved,
                favicon = station.favicon,
                country = station.country,
                countryCode = station.countryCode,
                tags = station.tags,
                codec = station.codec,
                bitrate = station.bitrate,
                lastCheckOk = station.lastCheckOk
            )
            else -> return
        }

        radioPlaybackManager.playStation(playable)
    }

    /**
     * Toggle play/pause for current radio station
     */
    fun togglePlayPause() {
        radioPlaybackManager.togglePlayPause()
    }

    /**
     * Stop radio playback
     */
    fun stopRadio() {
        radioPlaybackManager.stop()
    }

    /**
     * Check if a station is currently playing
     */
    fun isStationPlaying(stationUuid: String?): Boolean {
        val currentStation = _uiState.value.currentPlayingStation
        return _uiState.value.isRadioPlaying &&
               currentStation?.stationUuid == stationUuid
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun refresh() {
        loadInitialData()
    }
}
