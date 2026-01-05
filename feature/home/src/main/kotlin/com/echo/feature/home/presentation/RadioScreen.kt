package com.echo.feature.home.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.layout.offset
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.echo.core.media.model.RadioSignalStatus
import com.echo.core.ui.theme.EchoCoral
import com.echo.feature.home.data.model.RadioBrowserCountry
import com.echo.feature.home.data.model.RadioBrowserStation
import com.echo.feature.home.data.model.RadioBrowserTag
import com.echo.feature.home.data.model.RadioStation

// Colors
private val LiveGreen = Color(0xFF00C853)
private val GlassWhite = Color.White.copy(alpha = 0.08f)
private val GlassWhiteBorder = Color.White.copy(alpha = 0.12f)

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
        ) {
            // Tabs with modern design
            TabRow(
                selectedTabIndex = state.selectedTab.ordinal,
                containerColor = Color.Transparent,
                contentColor = EchoCoral,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[state.selectedTab.ordinal]),
                        color = EchoCoral,
                        height = 3.dp
                    )
                },
                divider = {}
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
                                fontWeight = if (state.selectedTab == tab) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 14.sp
                            )
                        },
                        selectedContentColor = EchoCoral,
                        unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (state.isLoading && state.favorites.isEmpty() && state.topVoted.isEmpty()) {
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
    }
}

/**
 * Live indicator badge with pulse animation
 */
@Composable
private fun LiveBadge(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "live")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Row(
        modifier = modifier
            .background(LiveGreen.copy(alpha = 0.9f), RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .alpha(alpha)
                .background(Color.White, CircleShape)
        )
        Text(
            text = "EN VIVO",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            fontSize = 10.sp
        )
    }
}

/**
 * Modern pill-shaped search bar
 */
@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    isSearching: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(GlassWhite)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.weight(1f),
                textStyle = TextStyle(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 16.sp
                ),
                singleLine = true,
                cursorBrush = SolidColor(EchoCoral),
                decorationBox = { innerTextField ->
                    Box {
                        if (query.isEmpty()) {
                            Text(
                                text = "Buscar estaciones...",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 16.sp
                            )
                        }
                        innerTextField()
                    }
                }
            )

            if (isSearching) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = EchoCoral
                )
            } else if (query.isNotEmpty()) {
                IconButton(
                    onClick = { onQueryChange("") },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Limpiar",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
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
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(32.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .background(GlassWhite, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Radio,
                        contentDescription = null,
                        tint = EchoCoral.copy(alpha = 0.7f),
                        modifier = Modifier.size(48.dp)
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Sin estaciones favoritas",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Explora y guarda tus estaciones preferidas",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
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
    isCurrentlyPlaying: Boolean,
    onPlay: () -> Unit,
    onDelete: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        label = "scale"
    )

    val cardColor by animateColorAsState(
        targetValue = if (isCurrentlyPlaying) EchoCoral.copy(alpha = 0.15f) else GlassWhite,
        label = "cardColor"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onPlay
            ),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column {
            // Cover image
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                if (station.favicon != null) {
                    AsyncImage(
                        model = station.favicon,
                        contentDescription = station.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Radio,
                            contentDescription = null,
                            tint = EchoCoral.copy(alpha = 0.5f),
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }

                // Live badge
                if (isCurrentlyPlaying) {
                    LiveBadge(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                    )
                }

                // Play button overlay
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                        .size(40.dp)
                        .shadow(8.dp, CircleShape)
                        .background(EchoCoral, CircleShape)
                        .clickable(onClick = onPlay),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isCurrentlyPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Info
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Text(
                    text = station.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    station.countryCode?.let { code ->
                        Text(
                            text = countryCodeToEmoji(code),
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Text(
                        text = station.country ?: "Internacional",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Quality info
                if (station.codec != null || station.bitrate != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = listOfNotNull(
                            station.codec,
                            station.bitrate?.let { "${it}kbps" }
                        ).joinToString(" • "),
                        style = MaterialTheme.typography.labelSmall,
                        color = EchoCoral.copy(alpha = 0.8f)
                    )
                }
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
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Search bar
        item {
            SearchBar(
                query = searchQuery,
                onQueryChange = onSearchQueryChange,
                isSearching = isSearching
            )
        }

        // Search results
        if (searchQuery.length >= 2) {
            if (searchResults.isNotEmpty()) {
                item {
                    SectionHeader(
                        icon = Icons.Default.Search,
                        title = "Resultados",
                        iconColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                items(searchResults.take(10)) { station ->
                    val isCurrentlyPlaying = isPlaying && station.stationuuid == currentPlayingUuid
                    StationListItem(
                        station = station,
                        isFavorite = station.stationuuid in favoriteIds,
                        isCurrentlyPlaying = isCurrentlyPlaying,
                        onPlay = { onPlay(station) },
                        onToggleFavorite = { onToggleFavorite(station) }
                    )
                }
            } else if (!isSearching) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No se encontraron estaciones",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            // Top Voted Section
            if (topVoted.isNotEmpty()) {
                item {
                    SectionHeader(
                        icon = Icons.Default.Star,
                        title = "Más votadas",
                        iconColor = Color(0xFFFFD700)
                    )
                }
                item {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(topVoted) { station ->
                            val isCurrentlyPlaying = isPlaying && station.stationuuid == currentPlayingUuid
                            StationCard(
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
                    SectionHeader(
                        icon = Icons.Default.TrendingUp,
                        title = "Populares",
                        iconColor = EchoCoral
                    )
                }
                item {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(popular) { station ->
                            val isCurrentlyPlaying = isPlaying && station.stationuuid == currentPlayingUuid
                            StationCard(
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
private fun SectionHeader(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    iconColor: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun StationCard(
    station: RadioBrowserStation,
    isFavorite: Boolean,
    isCurrentlyPlaying: Boolean,
    onPlay: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        label = "scale"
    )

    val cardColor by animateColorAsState(
        targetValue = if (isCurrentlyPlaying) EchoCoral.copy(alpha = 0.15f) else GlassWhite,
        label = "cardColor"
    )

    Card(
        modifier = Modifier
            .width(160.dp)
            .scale(scale)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onPlay
            ),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column {
            // Cover
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                if (station.favicon.isNotEmpty()) {
                    AsyncImage(
                        model = station.favicon,
                        contentDescription = station.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Radio,
                            contentDescription = null,
                            tint = EchoCoral.copy(alpha = 0.5f),
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }

                // Live badge
                if (isCurrentlyPlaying) {
                    LiveBadge(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                    )
                }

                // Favorite button
                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(32.dp)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = null,
                        tint = if (isFavorite) Color(0xFFFF4081) else Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Play button
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                        .size(36.dp)
                        .shadow(4.dp, CircleShape)
                        .background(EchoCoral, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isCurrentlyPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Info
            Column(
                modifier = Modifier.padding(10.dp)
            ) {
                Text(
                    text = station.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (station.countrycode.isNotEmpty()) {
                        Text(
                            text = countryCodeToEmoji(station.countrycode),
                            fontSize = 11.sp
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Text(
                        text = station.country.ifEmpty { "Internacional" },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Quality badge
                if (station.bitrate > 0) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${station.codec} • ${station.bitrate}kbps",
                        style = MaterialTheme.typography.labelSmall,
                        color = EchoCoral.copy(alpha = 0.8f),
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun StationListItem(
    station: RadioBrowserStation,
    isFavorite: Boolean,
    isCurrentlyPlaying: Boolean,
    onPlay: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    val cardColor by animateColorAsState(
        targetValue = if (isCurrentlyPlaying) EchoCoral.copy(alpha = 0.12f) else Color.Transparent,
        label = "cardColor"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(cardColor)
            .clickable(onClick = onPlay)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Cover
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(GlassWhite)
        ) {
            if (station.favicon.isNotEmpty()) {
                AsyncImage(
                    model = station.favicon,
                    contentDescription = station.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Radio,
                        contentDescription = null,
                        tint = EchoCoral.copy(alpha = 0.6f),
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        // Info
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
                    LiveBadge()
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (station.countrycode.isNotEmpty()) {
                    Text(
                        text = countryCodeToEmoji(station.countrycode),
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Text(
                    text = station.country.ifEmpty { "Internacional" },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (station.bitrate > 0) {
                    Text(
                        text = " • ${station.bitrate}kbps",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (station.displayTags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = station.displayTags.take(2).joinToString(" • "),
                    style = MaterialTheme.typography.labelSmall,
                    color = EchoCoral.copy(alpha = 0.8f)
                )
            }
        }

        // Actions
        IconButton(
            onClick = onPlay,
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                imageVector = if (isCurrentlyPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = null,
                tint = EchoCoral
            )
        }

        IconButton(
            onClick = onToggleFavorite,
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = null,
                tint = if (isFavorite) Color(0xFFFF4081) else MaterialTheme.colorScheme.onSurfaceVariant
            )
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
    AnimatedVisibility(
        visible = selectedGenre != null || selectedCountry != null,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
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
                    text = selectedGenre?.replaceFirstChar { it.uppercase() }
                        ?: selectedCountry ?: "",
                    style = MaterialTheme.typography.titleLarge,
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
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(stations) { station ->
                        val isCurrentlyPlaying = isPlaying && station.stationuuid == currentPlayingUuid
                        StationCard(
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

    AnimatedVisibility(
        visible = selectedGenre == null && selectedCountry == null,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Genres section
            item {
                SectionHeader(
                    icon = Icons.Default.MusicNote,
                    title = "Géneros",
                    iconColor = EchoCoral
                )
            }
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(genres.take(25)) { genre ->
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
                SectionHeader(
                    icon = Icons.Default.Public,
                    title = "Países",
                    iconColor = EchoCoral
                )
            }
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
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

// Genre chip colors
private val genreColors = listOf(
    Color(0xFFFF6B6B) to Color(0xFFFF8E8E),
    Color(0xFF4ECDC4) to Color(0xFF7FDED8),
    Color(0xFFFFE66D) to Color(0xFFFFED8A),
    Color(0xFF95E1D3) to Color(0xFFB8EDE3),
    Color(0xFFF38181) to Color(0xFFF7A5A5),
    Color(0xFFAA96DA) to Color(0xFFC4B5E8),
    Color(0xFFFCBAD3) to Color(0xFFFDD4E3),
    Color(0xFFA8D8EA) to Color(0xFFC5E5F1),
)

@Composable
private fun GenreChip(
    name: String,
    count: Int,
    onClick: () -> Unit
) {
    val colorIndex = name.hashCode().let { kotlin.math.abs(it) % genreColors.size }
    val (startColor, endColor) = genreColors[colorIndex]

    Card(
        modifier = Modifier.clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(16.dp)
    ) {
        Box(
            modifier = Modifier
                .background(
                    Brush.linearGradient(listOf(startColor, endColor)),
                    RoundedCornerShape(16.dp)
                )
                .padding(horizontal = 20.dp, vertical = 14.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = name.replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "$count",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.85f)
                )
            }
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
        modifier = Modifier
            .width(90.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = GlassWhite),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = countryCodeToEmoji(code),
                fontSize = 32.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = name,
                style = MaterialTheme.typography.labelMedium,
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

private fun countryCodeToEmoji(countryCode: String): String {
    if (countryCode.length != 2) return "🌍"
    val firstLetter = Character.codePointAt(countryCode.uppercase(), 0) - 0x41 + 0x1F1E6
    val secondLetter = Character.codePointAt(countryCode.uppercase(), 1) - 0x41 + 0x1F1E6
    return String(Character.toChars(firstLetter)) + String(Character.toChars(secondLetter))
}
