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
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.echo.core.ui.theme.EchoCoral
import com.echo.feature.home.data.model.RadioBrowserCountry
import com.echo.feature.home.data.model.RadioBrowserStation
import com.echo.feature.home.data.model.RadioStation

// Colors
private val LiveGreen = Color(0xFF00C853)
private val GlassWhite = Color.White.copy(alpha = 0.08f)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RadioScreen(
    viewModel: RadioViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val sheetState = rememberModalBottomSheetState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 120.dp, // Space for top bar
                bottom = 100.dp // Space for mini player
            ),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Country selector + Search bar
            item(span = { GridItemSpan(2) }) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Country selector
                    CountrySelector(
                        countryCode = state.selectedCountryCode,
                        countryName = state.selectedCountryName,
                        onClick = { viewModel.showCountryPicker() }
                    )

                    // Search bar
                    SearchBar(
                        query = state.searchQuery,
                        onQueryChange = viewModel::onSearchQueryChange,
                        onClear = viewModel::clearSearch,
                        isSearching = state.isSearching
                    )
                }
            }

            // Genre chips
            item(span = { GridItemSpan(2) }) {
                GenreChipsRow(
                    genres = state.genres.take(15).map { it.name },
                    selectedGenre = state.selectedGenre,
                    onSelectGenre = viewModel::selectGenre
                )
            }

            // Search results (if searching)
            if (state.isSearchActive && state.searchResults.isNotEmpty()) {
                item(span = { GridItemSpan(2) }) {
                    SectionTitle(title = "Resultados de búsqueda")
                }

                items(state.searchResults) { station ->
                    val isPlaying = state.isRadioPlaying &&
                        state.currentPlayingStation?.stationUuid == station.stationuuid
                    StationCard(
                        station = station,
                        isFavorite = station.stationuuid in state.favoriteIds,
                        isPlaying = isPlaying,
                        onPlay = { viewModel.playStation(station) },
                        onToggleFavorite = { viewModel.toggleFavorite(station) }
                    )
                }
            } else {
                // Favorites section (if any)
                if (state.favorites.isNotEmpty()) {
                    item(span = { GridItemSpan(2) }) {
                        SectionTitle(
                            title = "Mis favoritas",
                            trailing = "${state.favorites.size}"
                        )
                    }

                    item(span = { GridItemSpan(2) }) {
                        FavoritesRow(
                            favorites = state.favorites,
                            currentPlayingUuid = state.currentPlayingStation?.stationUuid,
                            isPlaying = state.isRadioPlaying,
                            onPlay = { viewModel.playStation(it) }
                        )
                    }
                }

                // Main stations grid
                item(span = { GridItemSpan(2) }) {
                    SectionTitle(
                        title = if (state.selectedGenre != null)
                            "${state.selectedGenre?.replaceFirstChar { it.uppercase() }} en ${state.selectedCountryName}"
                        else
                            "Top en ${state.selectedCountryName}"
                    )
                }

                if (state.isLoading) {
                    item(span = { GridItemSpan(2) }) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = EchoCoral)
                        }
                    }
                } else if (state.stations.isEmpty()) {
                    item(span = { GridItemSpan(2) }) {
                        EmptyState(
                            message = if (state.selectedGenre != null)
                                "No hay estaciones de ${state.selectedGenre} en ${state.selectedCountryName}"
                            else
                                "No hay estaciones disponibles"
                        )
                    }
                } else {
                    items(state.stations) { station ->
                        val isPlaying = state.isRadioPlaying &&
                            state.currentPlayingStation?.stationUuid == station.stationuuid
                        StationCard(
                            station = station,
                            isFavorite = station.stationuuid in state.favoriteIds,
                            isPlaying = isPlaying,
                            onPlay = { viewModel.playStation(station) },
                            onToggleFavorite = { viewModel.toggleFavorite(station) }
                        )
                    }
                }
            }
        }

        // Country picker bottom sheet
        if (state.showCountryPicker) {
            ModalBottomSheet(
                onDismissRequest = { viewModel.hideCountryPicker() },
                sheetState = sheetState,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                CountryPickerContent(
                    countries = state.countries,
                    selectedCountryCode = state.selectedCountryCode,
                    userCountryCode = state.userCountryCode,
                    onSelectCountry = viewModel::selectCountry
                )
            }
        }
    }
}

@Composable
private fun CountrySelector(
    countryCode: String,
    countryName: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(GlassWhite)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = countryCodeToEmoji(countryCode),
            fontSize = 28.sp
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Escuchando en",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = countryName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
        Icon(
            imageVector = Icons.Default.KeyboardArrowDown,
            contentDescription = "Cambiar país",
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
    isSearching: Boolean
) {
    Box(
        modifier = Modifier
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
                    onClick = onClear,
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
private fun GenreChipsRow(
    genres: List<String>,
    selectedGenre: String?,
    onSelectGenre: (String?) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 4.dp)
    ) {
        // "All" chip
        item {
            FilterChip(
                selected = selectedGenre == null,
                onClick = { onSelectGenre(null) },
                label = {
                    Text(
                        text = "Todas",
                        fontWeight = if (selectedGenre == null) FontWeight.Bold else FontWeight.Normal
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = EchoCoral,
                    selectedLabelColor = Color.White
                )
            )
        }

        // Genre chips
        items(genres) { genre ->
            FilterChip(
                selected = selectedGenre == genre,
                onClick = { onSelectGenre(if (selectedGenre == genre) null else genre) },
                label = {
                    Text(
                        text = genre.replaceFirstChar { it.uppercase() },
                        fontWeight = if (selectedGenre == genre) FontWeight.Bold else FontWeight.Normal
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = EchoCoral,
                    selectedLabelColor = Color.White
                )
            )
        }
    }
}

@Composable
private fun SectionTitle(
    title: String,
    trailing: String? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )
        trailing?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = EchoCoral
            )
        }
    }
}

@Composable
private fun FavoritesRow(
    favorites: List<RadioStation>,
    currentPlayingUuid: String?,
    isPlaying: Boolean,
    onPlay: (RadioStation) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(favorites) { station ->
            val isCurrentlyPlaying = isPlaying && station.stationUuid == currentPlayingUuid
            FavoriteCard(
                station = station,
                isPlaying = isCurrentlyPlaying,
                onPlay = { onPlay(station) }
            )
        }
    }
}

@Composable
private fun FavoriteCard(
    station: RadioStation,
    isPlaying: Boolean,
    onPlay: () -> Unit
) {
    val cardColor by animateColorAsState(
        targetValue = if (isPlaying) EchoCoral.copy(alpha = 0.2f) else GlassWhite,
        label = "cardColor"
    )

    Card(
        modifier = Modifier
            .width(120.dp)
            .clickable(onClick = onPlay),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
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
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                if (isPlaying) {
                    LiveBadge(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(6.dp)
                    )
                }
            }

            Text(
                text = station.name,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(8.dp)
            )
        }
    }
}

@Composable
private fun StationCard(
    station: RadioBrowserStation,
    isFavorite: Boolean,
    isPlaying: Boolean,
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
        targetValue = if (isPlaying) EchoCoral.copy(alpha = 0.15f) else GlassWhite,
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
                if (isPlaying) {
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
            }

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

                if (station.displayTags.isNotEmpty()) {
                    Text(
                        text = station.displayTags.take(2).joinToString(" • "),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (station.bitrate > 0) {
                    Spacer(modifier = Modifier.height(2.dp))
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

@Composable
private fun EmptyState(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Radio,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CountryPickerContent(
    countries: List<RadioBrowserCountry>,
    selectedCountryCode: String,
    userCountryCode: String,
    onSelectCountry: (RadioBrowserCountry) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 32.dp)
    ) {
        Text(
            text = "Seleccionar país",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
        )

        // User's country first
        countries.find { it.isoCode == userCountryCode }?.let { userCountry ->
            CountryItem(
                country = userCountry,
                isSelected = selectedCountryCode == userCountry.isoCode,
                isUserCountry = true,
                onClick = { onSelectCountry(userCountry) }
            )
        }

        // Popular countries section
        Text(
            text = "Populares",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
        )

        LazyColumn(
            modifier = Modifier.height(400.dp)
        ) {
            items(countries.filter { it.isoCode != userCountryCode }.take(50)) { country ->
                CountryItem(
                    country = country,
                    isSelected = selectedCountryCode == country.isoCode,
                    isUserCountry = false,
                    onClick = { onSelectCountry(country) }
                )
            }
        }
    }
}

@Composable
private fun CountryItem(
    country: RadioBrowserCountry,
    isSelected: Boolean,
    isUserCountry: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(
                if (isSelected) EchoCoral.copy(alpha = 0.1f) else Color.Transparent
            )
            .padding(horizontal = 24.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = countryCodeToEmoji(country.isoCode),
            fontSize = 24.sp
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = country.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
                if (isUserCountry) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Tu país",
                        style = MaterialTheme.typography.labelSmall,
                        color = EchoCoral,
                        modifier = Modifier
                            .background(EchoCoral.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            Text(
                text = "${country.stationcount} estaciones",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = EchoCoral
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
