package com.echo.feature.home.presentation

import android.content.Context
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
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

data class RadioUiState(
    val isLoading: Boolean = true,
    // User's country (auto-detected)
    val userCountryCode: String = "ES",
    val userCountryName: String = "España",
    // Currently selected country for browsing
    val selectedCountryCode: String = "ES",
    val selectedCountryName: String = "España",
    // Selected genre filter (null = all)
    val selectedGenre: String? = null,
    // Data
    val favorites: List<RadioStation> = emptyList(),
    val stations: List<RadioBrowserStation> = emptyList(),
    val genres: List<RadioBrowserTag> = emptyList(),
    val countries: List<RadioBrowserCountry> = emptyList(),
    // Search
    val searchQuery: String = "",
    val searchResults: List<RadioBrowserStation> = emptyList(),
    val isSearching: Boolean = false,
    val isSearchActive: Boolean = false,
    // Country picker
    val showCountryPicker: Boolean = false,
    // Favorites tracking
    val favoriteIds: Set<String> = emptySet(),
    // Playback state
    val isRadioPlaying: Boolean = false,
    val isRadioBuffering: Boolean = false,
    val currentPlayingStation: PlayableRadioStation? = null,
    val currentMetadata: RadioMetadata? = null,
    val signalStatus: RadioSignalStatus = RadioSignalStatus.UNKNOWN,
    // Error
    val error: String? = null
)

@HiltViewModel
class RadioViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val radioRepository: RadioRepository,
    private val radioPlaybackManager: RadioPlaybackManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(RadioUiState())
    val uiState: StateFlow<RadioUiState> = _uiState.asStateFlow()

    init {
        detectUserCountry()
        loadInitialData()
        observePlaybackState()
        observeFavorites()
    }

    private fun detectUserCountry() {
        val countryCode = Locale.getDefault().country.uppercase()
        val countryName = Locale("", countryCode).displayCountry

        _uiState.update {
            it.copy(
                userCountryCode = countryCode,
                userCountryName = countryName,
                selectedCountryCode = countryCode,
                selectedCountryName = countryName
            )
        }
    }

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

    private fun observeFavorites() {
        viewModelScope.launch {
            radioRepository.observeFavorites().collect { favorites ->
                _uiState.update {
                    it.copy(
                        favorites = favorites,
                        favoriteIds = favorites.mapNotNull { station -> station.stationUuid }.toSet()
                    )
                }
            }
        }
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            // Load genres
            launch {
                radioRepository.getTags(30)
                    .onSuccess { tags ->
                        _uiState.update {
                            it.copy(genres = tags.sortedByDescending { tag -> tag.stationcount })
                        }
                    }
            }

            // Load countries
            launch {
                radioRepository.getCountries()
                    .onSuccess { countries ->
                        _uiState.update {
                            it.copy(countries = countries.sortedByDescending { c -> c.stationcount })
                        }
                    }
            }

            // Load stations for user's country
            loadStationsForCountry(_uiState.value.selectedCountryCode)
        }
    }

    private fun loadStationsForCountry(countryCode: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            radioRepository.getByCountry(countryCode, 100)
                .onSuccess { stations ->
                    _uiState.update {
                        it.copy(
                            stations = stations.sortedByDescending { s -> s.clickcount },
                            isLoading = false
                        )
                    }
                }
                .onFailure {
                    _uiState.update { it.copy(isLoading = false) }
                }
        }
    }

    fun selectCountry(country: RadioBrowserCountry) {
        _uiState.update {
            it.copy(
                selectedCountryCode = country.isoCode,
                selectedCountryName = country.name,
                showCountryPicker = false,
                selectedGenre = null
            )
        }
        loadStationsForCountry(country.isoCode)
    }

    fun selectGenre(genre: String?) {
        _uiState.update { it.copy(selectedGenre = genre) }

        if (genre != null) {
            // Filter current stations by genre or load new ones
            viewModelScope.launch {
                _uiState.update { it.copy(isLoading = true) }

                // Search stations with both country and tag
                radioRepository.searchStations(
                    countryCode = _uiState.value.selectedCountryCode,
                    tag = genre,
                    limit = 100
                ).onSuccess { stations ->
                    _uiState.update {
                        it.copy(
                            stations = stations.sortedByDescending { s -> s.clickcount },
                            isLoading = false
                        )
                    }
                }.onFailure {
                    _uiState.update { it.copy(isLoading = false) }
                }
            }
        } else {
            // Reload all stations for country
            loadStationsForCountry(_uiState.value.selectedCountryCode)
        }
    }

    fun showCountryPicker() {
        _uiState.update { it.copy(showCountryPicker = true) }
    }

    fun hideCountryPicker() {
        _uiState.update { it.copy(showCountryPicker = false) }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query, isSearchActive = query.isNotEmpty()) }

        if (query.length >= 2) {
            searchStations(query)
        } else {
            _uiState.update { it.copy(searchResults = emptyList()) }
        }
    }

    fun clearSearch() {
        _uiState.update {
            it.copy(
                searchQuery = "",
                searchResults = emptyList(),
                isSearchActive = false
            )
        }
    }

    private fun searchStations(query: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSearching = true) }

            radioRepository.searchStations(name = query, limit = 30)
                .onSuccess { results ->
                    _uiState.update {
                        it.copy(
                            searchResults = results.sortedByDescending { s -> s.clickcount },
                            isSearching = false
                        )
                    }
                }
                .onFailure {
                    _uiState.update { it.copy(isSearching = false) }
                }
        }
    }

    fun toggleFavorite(station: RadioBrowserStation) {
        viewModelScope.launch {
            val isFavorite = station.stationuuid in _uiState.value.favoriteIds

            if (isFavorite) {
                radioRepository.deleteFavoriteByUuid(station.stationuuid)
            } else {
                radioRepository.saveFavorite(station)
            }
        }
    }

    fun deleteFavorite(station: RadioStation) {
        viewModelScope.launch {
            station.stationUuid?.let { uuid ->
                radioRepository.deleteFavoriteByUuid(uuid)
            }
        }
    }

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

    fun togglePlayPause() {
        radioPlaybackManager.togglePlayPause()
    }

    fun stopRadio() {
        radioPlaybackManager.stop()
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun refresh() {
        loadStationsForCountry(_uiState.value.selectedCountryCode)
    }
}
