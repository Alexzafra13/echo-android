package com.echo.feature.home.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.SignalCellular4Bar
import androidx.compose.material.icons.filled.SignalCellularConnectedNoInternet0Bar
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.echo.core.media.model.RadioSignalStatus
import com.echo.core.ui.theme.EchoCoral
import com.echo.feature.home.data.model.RadioBrowserCountry
import com.echo.feature.home.data.model.RadioBrowserStation
import com.echo.feature.home.data.model.RadioBrowserTag
import com.echo.feature.home.data.model.RadioStation

@Composable
fun RadioScreen(
    viewModel: RadioViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 72.dp)
                .padding(bottom = if (state.currentPlayingStation != null) 80.dp else 0.dp)
        ) {
            // Header
            Text(
                text = "Radio",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(16.dp)
            )

            // Tabs
            TabRow(
                selectedTabIndex = state.selectedTab.ordinal,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = EchoCoral,
                indicator = { tabPositions ->
                    TabRowDefaults.Indicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[state.selectedTab.ordinal]),
                        color = EchoCoral
                    )
                }
            ) {
                RadioTab.entries.forEach { tab ->
                    Tab(
                        selected = state.selectedTab == tab,
                        onClick = { viewModel.selectTab(tab) },
                        text = {
                            Text(
                                text = when (tab) {
                                    RadioTab.FAVORITES -> "Favoritos"
                                    RadioTab.DISCOVER -> "Descubrir"
                                    RadioTab.BROWSE -> "Explorar"
                                },
                                fontWeight = if (state.selectedTab == tab) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        selectedContentColor = EchoCoral,
                        unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (state.isLoading && state.favorites.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = EchoCoral)
                }
            } else {
                when (state.selectedTab) {
                    RadioTab.FAVORITES -> FavoritesTab(
                        favorites = state.favorites,
                        currentPlayingUuid = state.currentPlayingStation?.stationUuid,
                        isPlaying = state.isRadioPlaying,
                        onPlay = { viewModel.playStation(it) },
                        onDelete = { viewModel.deleteFavorite(it) }
                    )
                    RadioTab.DISCOVER -> DiscoverTab(
                        searchQuery = state.searchQuery,
                        searchResults = state.searchResults,
                        topVoted = state.topVoted,
                        popular = state.popular,
                        isSearching = state.isSearching,
                        favoriteIds = state.favoriteIds,
                        currentPlayingUuid = state.currentPlayingStation?.stationUuid,
                        isPlaying = state.isRadioPlaying,
                        onSearchQueryChange = viewModel::onSearchQueryChange,
                        onPlay = { viewModel.playStation(it) },
                        onToggleFavorite = viewModel::toggleFavorite
                    )
                    RadioTab.BROWSE -> BrowseTab(
                        genres = state.genres,
                        countries = state.countries,
                        selectedGenre = state.selectedGenre,
                        selectedCountry = state.selectedCountry,
                        genreStations = state.genreStations,
                        countryStations = state.countryStations,
                        favoriteIds = state.favoriteIds,
                        isLoading = state.isLoading,
                        currentPlayingUuid = state.currentPlayingStation?.stationUuid,
                        isPlaying = state.isRadioPlaying,
                        onSelectGenre = viewModel::selectGenre,
                        onSelectCountry = viewModel::selectCountry,
                        onClearSelection = viewModel::clearBrowseSelection,
                        onPlay = { viewModel.playStation(it) },
                        onToggleFavorite = viewModel::toggleFavorite
                    )
                }
            }
        }

        // Mini Player Bar
        state.currentPlayingStation?.let { station ->
            RadioMiniPlayer(
                stationName = station.name,
                metadata = state.currentMetadata?.displayText,
                favicon = station.favicon,
                isPlaying = state.isRadioPlaying,
                isBuffering = state.isRadioBuffering,
                signalStatus = state.signalStatus,
                onPlayPause = viewModel::togglePlayPause,
                onStop = viewModel::stopRadio,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

/**
 * Mini player bar shown at the bottom when radio is playing
 */
@Composable
private fun RadioMiniPlayer(
    stationName: String,
    metadata: String?,
    favicon: String?,
    isPlaying: Boolean,
    isBuffering: Boolean,
    signalStatus: RadioSignalStatus,
    onPlayPause: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Station icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(EchoCoral.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                if (favicon != null) {
                    AsyncImage(
                        model = favicon,
                        contentDescription = stationName,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Radio,
                        contentDescription = null,
                        tint = EchoCoral,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Station info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stationName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!metadata.isNullOrBlank()) {
                    Text(
                        text = metadata,
                        style = MaterialTheme.typography.bodySmall,
                        color = EchoCoral,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Signal status indicator
            SignalIndicator(
                status = signalStatus,
                isBuffering = isBuffering
            )

            Spacer(modifier = Modifier.width(4.dp))

            // Play/Pause button
            IconButton(onClick = onPlayPause) {
                if (isBuffering) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                        color = EchoCoral
                    )
                } else {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pausar" else "Reproducir",
                        tint = EchoCoral
                    )
                }
            }

            // Stop button
            IconButton(onClick = onStop) {
                Icon(
                    imageVector = Icons.Default.Stop,
                    contentDescription = "Detener",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Signal quality indicator
 */
@Composable
private fun SignalIndicator(
    status: RadioSignalStatus,
    isBuffering: Boolean
) {
    val (icon, color) = when {
        isBuffering -> Icons.Default.SignalCellular4Bar to Color.Yellow
        status == RadioSignalStatus.GOOD -> Icons.Default.SignalCellular4Bar to Color.Green
        status == RadioSignalStatus.WEAK -> Icons.Default.SignalCellular4Bar to Color.Yellow
        status == RadioSignalStatus.ERROR -> Icons.Default.SignalCellularConnectedNoInternet0Bar to Color.Red
        else -> Icons.Default.SignalCellular4Bar to MaterialTheme.colorScheme.onSurfaceVariant
    }

    Icon(
        imageVector = icon,
        contentDescription = "Estado de señal",
        tint = color,
        modifier = Modifier.size(20.dp)
    )
}

@Composable
private fun FavoritesTab(
    favorites: List<RadioStation>,
    currentPlayingUuid: String?,
    isPlaying: Boolean,
    onPlay: (RadioStation) -> Unit,
    onDelete: (RadioStation) -> Unit
) {
    if (favorites.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Radio,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Sin estaciones favoritas",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Explora y agrega estaciones",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(favorites) { station ->
                val isCurrentlyPlaying = isPlaying && station.stationUuid == currentPlayingUuid
                FavoriteStationCard(
                    station = station,
                    isCurrentlyPlaying = isCurrentlyPlaying,
                    onPlay = { onPlay(station) },
                    onDelete = { onDelete(station) }
                )
            }
        }
    }
}

@Composable
private fun FavoriteStationCard(
    station: RadioStation,
    isCurrentlyPlaying: Boolean = false,
    onPlay: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onPlay),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrentlyPlaying)
                EchoCoral.copy(alpha = 0.1f)
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Station icon/favicon with playing indicator
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(EchoCoral.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                if (station.favicon != null) {
                    AsyncImage(
                        model = station.favicon,
                        contentDescription = station.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Radio,
                        contentDescription = null,
                        tint = EchoCoral,
                        modifier = Modifier.size(28.dp)
                    )
                }
                // Playing overlay
                if (isCurrentlyPlaying) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Radio,
                            contentDescription = "Reproduciendo",
                            tint = EchoCoral,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = station.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (isCurrentlyPlaying) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "EN VIVO",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            modifier = Modifier
                                .background(EchoCoral, RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                station.country?.let { country ->
                    Text(
                        text = country,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (station.displayTags.isNotEmpty()) {
                    Text(
                        text = station.displayTags.take(2).joinToString(" • "),
                        style = MaterialTheme.typography.labelSmall,
                        color = EchoCoral,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            IconButton(onClick = onPlay) {
                Icon(
                    imageVector = if (isCurrentlyPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isCurrentlyPlaying) "Pausar" else "Reproducir",
                    tint = EchoCoral
                )
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Eliminar",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun DiscoverTab(
    searchQuery: String,
    searchResults: List<RadioBrowserStation>,
    topVoted: List<RadioBrowserStation>,
    popular: List<RadioBrowserStation>,
    isSearching: Boolean,
    favoriteIds: Set<String>,
    currentPlayingUuid: String?,
    isPlaying: Boolean,
    onSearchQueryChange: (String) -> Unit,
    onPlay: (RadioBrowserStation) -> Unit,
    onToggleFavorite: (RadioBrowserStation) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Search field
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Buscar estaciones...") },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Search, contentDescription = null)
                },
                trailingIcon = {
                    if (isSearching) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = EchoCoral
                        )
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = EchoCoral,
                    cursorColor = EchoCoral
                )
            )
        }

        // Search results
        if (searchQuery.length >= 2 && searchResults.isNotEmpty()) {
            item {
                Text(
                    text = "Resultados de búsqueda",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            items(searchResults.take(10)) { station ->
                val isCurrentlyPlaying = isPlaying && station.stationuuid == currentPlayingUuid
                RadioBrowserStationCard(
                    station = station,
                    isFavorite = station.stationuuid in favoriteIds,
                    isCurrentlyPlaying = isCurrentlyPlaying,
                    onPlay = { onPlay(station) },
                    onToggleFavorite = { onToggleFavorite(station) }
                )
            }
        } else {
            // Top Voted Section
            if (topVoted.isNotEmpty()) {
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = Color(0xFFFFD700),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Más votadas",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                item {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(topVoted) { station ->
                            val isCurrentlyPlaying = isPlaying && station.stationuuid == currentPlayingUuid
                            CompactRadioCard(
                                station = station,
                                isFavorite = station.stationuuid in favoriteIds,
                                isCurrentlyPlaying = isCurrentlyPlaying,
                                onPlay = { onPlay(station) },
                                onToggleFavorite = { onToggleFavorite(station) }
                            )
                        }
                    }
                }
            }

            // Popular Section
            if (popular.isNotEmpty()) {
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.TrendingUp,
                            contentDescription = null,
                            tint = EchoCoral,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Populares",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                item {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(popular) { station ->
                            val isCurrentlyPlaying = isPlaying && station.stationuuid == currentPlayingUuid
                            CompactRadioCard(
                                station = station,
                                isFavorite = station.stationuuid in favoriteIds,
                                isCurrentlyPlaying = isCurrentlyPlaying,
                                onPlay = { onPlay(station) },
                                onToggleFavorite = { onToggleFavorite(station) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactRadioCard(
    station: RadioBrowserStation,
    isFavorite: Boolean,
    isCurrentlyPlaying: Boolean = false,
    onPlay: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(160.dp)
            .clickable(onClick = onPlay),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrentlyPlaying)
                EchoCoral.copy(alpha = 0.15f)
            else
                MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(EchoCoral.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                if (station.favicon.isNotEmpty()) {
                    AsyncImage(
                        model = station.favicon,
                        contentDescription = station.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Radio,
                        contentDescription = null,
                        tint = EchoCoral,
                        modifier = Modifier.size(28.dp)
                    )
                }
                // Playing indicator overlay
                if (isCurrentlyPlaying) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Radio,
                            contentDescription = "Reproduciendo",
                            tint = EchoCoral,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = station.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = if (isCurrentlyPlaying) 1 else 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
            }

            if (isCurrentlyPlaying) {
                Text(
                    text = "EN VIVO",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .background(EchoCoral, RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            } else {
                Text(
                    text = station.country.ifEmpty { "Internacional" },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onPlay,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (isCurrentlyPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isCurrentlyPlaying) "Pausar" else "Reproducir",
                        tint = EchoCoral
                    )
                }
                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = if (isFavorite) "Quitar de favoritos" else "Agregar a favoritos",
                        tint = if (isFavorite) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun RadioBrowserStationCard(
    station: RadioBrowserStation,
    isFavorite: Boolean,
    isCurrentlyPlaying: Boolean = false,
    onPlay: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onPlay),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrentlyPlaying)
                EchoCoral.copy(alpha = 0.1f)
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(EchoCoral.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                if (station.favicon.isNotEmpty()) {
                    AsyncImage(
                        model = station.favicon,
                        contentDescription = station.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Radio,
                        contentDescription = null,
                        tint = EchoCoral
                    )
                }
                // Playing indicator overlay
                if (isCurrentlyPlaying) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Radio,
                            contentDescription = "Reproduciendo",
                            tint = EchoCoral,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = station.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (isCurrentlyPlaying) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "EN VIVO",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            modifier = Modifier
                                .background(EchoCoral, RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Text(
                    text = station.country.ifEmpty { "Internacional" },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (station.displayTags.isNotEmpty()) {
                    Text(
                        text = station.displayTags.take(3).joinToString(" • "),
                        style = MaterialTheme.typography.labelSmall,
                        color = EchoCoral
                    )
                }
            }

            IconButton(onClick = onPlay) {
                Icon(
                    imageVector = if (isCurrentlyPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isCurrentlyPlaying) "Pausar" else "Reproducir",
                    tint = EchoCoral
                )
            }

            IconButton(onClick = onToggleFavorite) {
                Icon(
                    imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = if (isFavorite) "Quitar de favoritos" else "Agregar a favoritos",
                    tint = if (isFavorite) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun BrowseTab(
    genres: List<RadioBrowserTag>,
    countries: List<RadioBrowserCountry>,
    selectedGenre: String?,
    selectedCountry: String?,
    genreStations: List<RadioBrowserStation>,
    countryStations: List<RadioBrowserStation>,
    favoriteIds: Set<String>,
    isLoading: Boolean,
    currentPlayingUuid: String?,
    isPlaying: Boolean,
    onSelectGenre: (String) -> Unit,
    onSelectCountry: (String) -> Unit,
    onClearSelection: () -> Unit,
    onPlay: (RadioBrowserStation) -> Unit,
    onToggleFavorite: (RadioBrowserStation) -> Unit
) {
    // If a genre or country is selected, show stations
    if (selectedGenre != null || selectedCountry != null) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Back header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onClearSelection)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Volver",
                    tint = EchoCoral
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = selectedGenre ?: selectedCountry ?: "",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = EchoCoral)
                }
            } else {
                val stations = if (selectedGenre != null) genreStations else countryStations
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(stations) { station ->
                        val isCurrentlyPlaying = isPlaying && station.stationuuid == currentPlayingUuid
                        RadioBrowserStationCard(
                            station = station,
                            isFavorite = station.stationuuid in favoriteIds,
                            isCurrentlyPlaying = isCurrentlyPlaying,
                            onPlay = { onPlay(station) },
                            onToggleFavorite = { onToggleFavorite(station) }
                        )
                    }
                }
            }
        }
    } else {
        // Show genres and countries grid
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Genres section
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = EchoCoral,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Géneros",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(genres.take(20)) { genre ->
                        GenreChip(
                            name = genre.name,
                            count = genre.stationcount,
                            onClick = { onSelectGenre(genre.name) }
                        )
                    }
                }
            }

            // Countries section
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Public,
                        contentDescription = null,
                        tint = EchoCoral,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Países",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(countries.take(30)) { country ->
                        CountryChip(
                            name = country.name,
                            code = country.isoCode,
                            count = country.stationcount,
                            onClick = { onSelectCountry(country.isoCode) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GenreChip(
    name: String,
    count: Int,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = EchoCoral.copy(alpha = 0.15f)
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = name.replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = EchoCoral
            )
            Text(
                text = "$count estaciones",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CountryChip(
    name: String,
    code: String,
    count: Int,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = countryCodeToEmoji(code),
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = name,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "$count",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Convert country code (e.g., "US") to flag emoji
 */
private fun countryCodeToEmoji(countryCode: String): String {
    if (countryCode.length != 2) return "🌍"

    val firstLetter = Character.codePointAt(countryCode.uppercase(), 0) - 0x41 + 0x1F1E6
    val secondLetter = Character.codePointAt(countryCode.uppercase(), 1) - 0x41 + 0x1F1E6

    return String(Character.toChars(firstLetter)) + String(Character.toChars(secondLetter))
}
