package com.echo.feature.home.presentation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
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
    viewModel: RadioViewModel = hiltViewModel(),
    onNavigateToDetail: (RadioSectionType, String?, String?) -> Unit = { _, _, _ -> }
) {
    val state by viewModel.uiState.collectAsState()
    val sheetState = rememberModalBottomSheetState()

    // Load genre stations when genres are available
    LaunchedEffect(state.genreSections) {
        state.genreSections.forEach { genreWithGradient ->
            viewModel.loadStationsForGenre(genreWithGradient.tag.name)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = 120.dp,
                bottom = 100.dp
            ),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Search bar
            item {
                SearchBar(
                    query = state.searchQuery,
                    onQueryChange = viewModel::onSearchQueryChange,
                    onClear = viewModel::clearSearch,
                    isSearching = state.isSearching,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            // Search results (if searching)
            if (state.isSearchActive && state.searchResults.isNotEmpty()) {
                item {
                    SectionHeader(
                        title = "Resultados de búsqueda",
                        showArrow = false
                    )
                }

                item {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp)
                    ) {
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
                    }
                }
            } else {
                // === FAVORITES SECTION ===
                if (state.favorites.isNotEmpty()) {
                    item {
                        SectionHeader(
                            title = "Mis favoritas",
                            trailing = "${state.favorites.size}",
                            onSeeAllClick = {
                                onNavigateToDetail(RadioSectionType.FAVORITES, null, "Mis favoritas")
                            }
                        )
                    }

                    item {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp)
                        ) {
                            items(state.favorites) { station ->
                                val isPlaying = state.isRadioPlaying &&
                                    station.stationUuid == state.currentPlayingStation?.stationUuid
                                FavoriteStationCard(
                                    station = station,
                                    isPlaying = isPlaying,
                                    onPlay = { viewModel.playStation(station) }
                                )
                            }
                        }
                    }
                }

                // === LOCAL STATIONS SECTION ===
                item {
                    SectionHeader(
                        title = "Emisoras locales",
                        subtitle = state.userCountryName,
                        countryCode = state.userCountryCode,
                        onSeeAllClick = {
                            onNavigateToDetail(RadioSectionType.LOCAL, null, "Emisoras locales")
                        }
                    )
                }

                item {
                    if (state.isLoadingLocal) {
                        LoadingRow()
                    } else if (state.localStations.isEmpty()) {
                        EmptyRow(message = "No hay emisoras disponibles")
                    } else {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp)
                        ) {
                            items(state.localStations.take(10)) { station ->
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

                // === INTERNATIONAL STATIONS SECTION ===
                item {
                    SectionHeader(
                        title = "Emisoras internacionales",
                        subtitle = state.internationalCountryName,
                        countryCode = state.internationalCountryCode,
                        showCountryPicker = true,
                        onCountryPickerClick = { viewModel.showInternationalCountryPicker() },
                        onSeeAllClick = {
                            onNavigateToDetail(RadioSectionType.INTERNATIONAL, null, "Emisoras internacionales")
                        }
                    )
                }

                item {
                    if (state.isLoadingInternational) {
                        LoadingRow()
                    } else if (state.internationalStations.isEmpty()) {
                        EmptyRow(message = "No hay emisoras disponibles")
                    } else {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp)
                        ) {
                            items(state.internationalStations.take(10)) { station ->
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

                // === GENRE SECTIONS ===
                item {
                    Text(
                        text = "Emisoras por género",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }

                item {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp)
                    ) {
                        items(state.genreSections) { genreWithGradient ->
                            GenreCard(
                                genreName = genreWithGradient.tag.name,
                                stationCount = genreWithGradient.tag.stationcount,
                                gradientColors = genreWithGradient.gradientColors,
                                onClick = {
                                    onNavigateToDetail(
                                        RadioSectionType.GENRE,
                                        genreWithGradient.tag.name,
                                        genreWithGradient.tag.name.replaceFirstChar { it.uppercase() }
                                    )
                                }
                            )
                        }
                    }
                }

                // === TOP STATIONS PER GENRE (preview) ===
                state.genreSections.take(4).forEach { genreWithGradient ->
                    val genreName = genreWithGradient.tag.name
                    val stations = state.genreStationsMap[genreName] ?: emptyList()

                    if (stations.isNotEmpty()) {
                        item {
                            SectionHeader(
                                title = genreName.replaceFirstChar { it.uppercase() },
                                trailing = "${genreWithGradient.tag.stationcount} emisoras",
                                onSeeAllClick = {
                                    onNavigateToDetail(
                                        RadioSectionType.GENRE,
                                        genreName,
                                        genreName.replaceFirstChar { it.uppercase() }
                                    )
                                }
                            )
                        }

                        item {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp)
                            ) {
                                items(stations.take(8)) { station ->
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
                }
            }
        }

        // International country picker bottom sheet
        if (state.showInternationalCountryPicker) {
            ModalBottomSheet(
                onDismissRequest = { viewModel.hideInternationalCountryPicker() },
                sheetState = sheetState,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                CountryPickerContent(
                    countries = state.countries,
                    selectedCountryCode = state.internationalCountryCode,
                    userCountryCode = state.userCountryCode,
                    onSelectCountry = viewModel::selectInternationalCountry
                )
            }
        }
    }
}

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
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
                                text = "Buscar emisoras...",
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
private fun SectionHeader(
    title: String,
    subtitle: String? = null,
    countryCode: String? = null,
    trailing: String? = null,
    showArrow: Boolean = true,
    showCountryPicker: Boolean = false,
    onCountryPickerClick: () -> Unit = {},
    onSeeAllClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = showArrow) { onSeeAllClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Country flag if provided
        if (countryCode != null) {
            Text(
                text = countryCodeToEmoji(countryCode),
                fontSize = 24.sp,
                modifier = Modifier.padding(end = 8.dp)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            if (subtitle != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = if (showCountryPicker) Modifier.clickable { onCountryPickerClick() } else Modifier
                ) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (showCountryPicker) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Cambiar país",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        if (trailing != null) {
            Text(
                text = trailing,
                style = MaterialTheme.typography.bodySmall,
                color = EchoCoral,
                modifier = Modifier.padding(end = 4.dp)
            )
        }

        if (showArrow) {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Ver más",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun GenreCard(
    genreName: String,
    stationCount: Int,
    gradientColors: List<Color>,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(160.dp)
            .aspectRatio(1f)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = gradientColors
                    )
                )
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Spacer(modifier = Modifier.weight(1f))

                Text(
                    text = genreName.replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = "$stationCount emisoras",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
private fun FavoriteStationCard(
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
            .width(140.dp)
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

            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = station.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = "Radio Browser",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
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
            .width(140.dp)
            .scale(scale)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onPlay
            ),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        shape = RoundedCornerShape(12.dp)
    ) {
        Box {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
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

                    // Gradient overlay
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Black.copy(alpha = 0.3f),
                                        Color.Transparent,
                                        Color.Transparent
                                    )
                                )
                            )
                    )

                    if (isPlaying) {
                        LiveBadge(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(6.dp)
                        )
                    }
                }

                Column(modifier = Modifier.padding(10.dp)) {
                    Text(
                        text = station.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = if (station.displayTags.isNotEmpty()) {
                            station.displayTags.first().replaceFirstChar { it.uppercase() }
                        } else {
                            "Radio Browser"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }

            // Favorite button
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.4f))
                    .clickable(onClick = onToggleFavorite),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = null,
                    tint = if (isFavorite) Color(0xFFFF4081) else Color.White.copy(alpha = 0.9f),
                    modifier = Modifier.size(14.dp)
                )
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
            .padding(horizontal = 6.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Box(
            modifier = Modifier
                .size(5.dp)
                .alpha(alpha)
                .background(Color.White, CircleShape)
        )
        Text(
            text = "EN VIVO",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            fontSize = 9.sp
        )
    }
}

@Composable
private fun LoadingRow() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = EchoCoral)
    }
}

@Composable
private fun EmptyRow(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
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
            // Filter out user's country for international section
            items(countries.filter { it.isoCode != userCountryCode }.take(50)) { country ->
                CountryItem(
                    country = country,
                    isSelected = selectedCountryCode == country.isoCode,
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
            Text(
                text = country.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
            Text(
                text = "${country.stationcount} emisoras",
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
