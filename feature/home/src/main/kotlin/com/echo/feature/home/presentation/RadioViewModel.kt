package com.echo.feature.home.presentation

import android.content.Context
import androidx.compose.ui.graphics.Color
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
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

// Section types for navigation
enum class RadioSectionType {
    FAVORITES,
    LOCAL,
    INTERNATIONAL,
    GENRE
}

// Genre with gradient colors
data class GenreWithGradient(
    val tag: RadioBrowserTag,
    val gradientColors: List<Color>
)

data class RadioUiState(
    val isLoading: Boolean = true,
    // User's country (auto-detected)
    val userCountryCode: String = "ES",
    val userCountryName: String = "España",

    // === SECTIONS DATA ===
    // Favorites section
    val favorites: List<RadioStation> = emptyList(),
    val favoriteIds: Set<String> = emptySet(),

    // Local stations (user's country)
    val localStations: List<RadioBrowserStation> = emptyList(),
    val isLoadingLocal: Boolean = false,

    // International stations (selected country)
    val internationalCountryCode: String = "US",
    val internationalCountryName: String = "Estados Unidos",
    val internationalStations: List<RadioBrowserStation> = emptyList(),
    val isLoadingInternational: Boolean = false,
    val showInternationalCountryPicker: Boolean = false,

    // Genre sections (top genres with stations)
    val genreSections: List<GenreWithGradient> = emptyList(),
    val genreStationsMap: Map<String, List<RadioBrowserStation>> = emptyMap(),
    val isLoadingGenres: Boolean = false,

    // All available data
    val genres: List<RadioBrowserTag> = emptyList(),
    val countries: List<RadioBrowserCountry> = emptyList(),

    // Search
    val searchQuery: String = "",
    val searchResults: List<RadioBrowserStation> = emptyList(),
    val isSearching: Boolean = false,
    val isSearchActive: Boolean = false,

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

    // Gradient color palettes for genres (Apple Music style)
    private val genreGradients = listOf(
        listOf(Color(0xFFB4A700), Color(0xFF8B9A00)), // Olive/Yellow-green
        listOf(Color(0xFF00A39E), Color(0xFF00857F)), // Teal/Cyan
        listOf(Color(0xFFE85D75), Color(0xFFD4456B)), // Pink/Rose
        listOf(Color(0xFFD4A017), Color(0xFFB8860B)), // Gold/Amber
        listOf(Color(0xFF4A90D9), Color(0xFF357ABD)), // Blue
        listOf(Color(0xFF8B5CF6), Color(0xFF7C3AED)), // Purple
        listOf(Color(0xFFEF4444), Color(0xFFDC2626)), // Red
        listOf(Color(0xFF10B981), Color(0xFF059669)), // Green
        listOf(Color(0xFFF97316), Color(0xFFEA580C)), // Orange
        listOf(Color(0xFF06B6D4), Color(0xFF0891B2)), // Cyan
    )

    init {
        detectUserCountry()
        loadInitialData()
        observePlaybackState()
        observeFavorites()
    }

    private fun detectUserCountry() {
        val countryCode = Locale.getDefault().country.uppercase()
        val countryName = Locale("", countryCode).displayCountry

        // Set international country to a different popular country
        val defaultInternational = if (countryCode == "US") "GB" else "US"
        val internationalName = Locale("", defaultInternational).displayCountry

        _uiState.update {
            it.copy(
                userCountryCode = countryCode,
                userCountryName = countryName,
                internationalCountryCode = defaultInternational,
                internationalCountryName = internationalName
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

            // Load countries first
            launch {
                radioRepository.getCountries()
                    .onSuccess { countries ->
                        _uiState.update {
                            it.copy(countries = countries.sortedByDescending { c -> c.stationcount })
                        }
                    }
            }

            // Load genres and create sections with gradients
            launch {
                radioRepository.getTags(30)
                    .onSuccess { tags ->
                        val sortedTags = tags.sortedByDescending { it.stationcount }
                        val genresWithGradients = sortedTags.take(8).mapIndexed { index, tag ->
                            GenreWithGradient(
                                tag = tag,
                                gradientColors = genreGradients[index % genreGradients.size]
                            )
                        }
                        _uiState.update {
                            it.copy(
                                genres = sortedTags,
                                genreSections = genresWithGradients
                            )
                        }
                    }
            }

            // Load local stations (user's country)
            loadLocalStations()

            // Load international stations
            loadInternationalStations()

            _uiState.update { it.copy(isLoading = false) }
        }
    }

    private fun loadLocalStations() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingLocal = true) }

            radioRepository.getByCountry(_uiState.value.userCountryCode, 20)
                .onSuccess { stations ->
                    _uiState.update {
                        it.copy(
                            localStations = stations.sortedByDescending { s -> s.clickcount },
                            isLoadingLocal = false
                        )
                    }
                }
                .onFailure {
                    _uiState.update { it.copy(isLoadingLocal = false) }
                }
        }
    }

    private fun loadInternationalStations() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingInternational = true) }

            radioRepository.getByCountry(_uiState.value.internationalCountryCode, 20)
                .onSuccess { stations ->
                    _uiState.update {
                        it.copy(
                            internationalStations = stations.sortedByDescending { s -> s.clickcount },
                            isLoadingInternational = false
                        )
                    }
                }
                .onFailure {
                    _uiState.update { it.copy(isLoadingInternational = false) }
                }
        }
    }

    fun loadStationsForGenre(genre: String) {
        viewModelScope.launch {
            // Check if already loaded
            if (_uiState.value.genreStationsMap.containsKey(genre)) return@launch

            radioRepository.getByTag(genre, 20)
                .onSuccess { stations ->
                    _uiState.update {
                        it.copy(
                            genreStationsMap = it.genreStationsMap + (genre to stations.sortedByDescending { s -> s.clickcount })
                        )
                    }
                }
        }
    }

    fun selectInternationalCountry(country: RadioBrowserCountry) {
        _uiState.update {
            it.copy(
                internationalCountryCode = country.isoCode,
                internationalCountryName = country.name,
                showInternationalCountryPicker = false
            )
        }
        loadInternationalStations()
    }

    fun showInternationalCountryPicker() {
        _uiState.update { it.copy(showInternationalCountryPicker = true) }
    }

    fun hideInternationalCountryPicker() {
        _uiState.update { it.copy(showInternationalCountryPicker = false) }
    }

    // Get all stations for a section (for detail screen)
    fun getStationsForSection(sectionType: RadioSectionType, genreName: String? = null): List<RadioBrowserStation> {
        return when (sectionType) {
            RadioSectionType.LOCAL -> _uiState.value.localStations
            RadioSectionType.INTERNATIONAL -> _uiState.value.internationalStations
            RadioSectionType.GENRE -> genreName?.let { _uiState.value.genreStationsMap[it] } ?: emptyList()
            RadioSectionType.FAVORITES -> emptyList() // Favorites use different model
        }
    }

    // Load more stations for detail screen
    fun loadMoreStationsForSection(sectionType: RadioSectionType, genreName: String? = null) {
        viewModelScope.launch {
            when (sectionType) {
                RadioSectionType.LOCAL -> {
                    radioRepository.getByCountry(_uiState.value.userCountryCode, 100)
                        .onSuccess { stations ->
                            _uiState.update {
                                it.copy(localStations = stations.sortedByDescending { s -> s.clickcount })
                            }
                        }
                }
                RadioSectionType.INTERNATIONAL -> {
                    radioRepository.getByCountry(_uiState.value.internationalCountryCode, 100)
                        .onSuccess { stations ->
                            _uiState.update {
                                it.copy(internationalStations = stations.sortedByDescending { s -> s.clickcount })
                            }
                        }
                }
                RadioSectionType.GENRE -> {
                    genreName?.let { genre ->
                        radioRepository.getByTag(genre, 100)
                            .onSuccess { stations ->
                                _uiState.update {
                                    it.copy(
                                        genreStationsMap = it.genreStationsMap + (genre to stations.sortedByDescending { s -> s.clickcount })
                                    )
                                }
                            }
                    }
                }
                else -> {}
            }
        }
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
        loadLocalStations()
        loadInternationalStations()
    }
}
