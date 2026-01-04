package com.echo.feature.artists.presentation.detail

import androidx.compose.animation.animateContentSize
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.echo.core.ui.theme.EchoCoral
import com.echo.core.ui.theme.EchoDarkSurfaceVariant
import com.echo.feature.artists.domain.model.Artist
import com.echo.feature.artists.domain.model.ArtistAlbum
import com.echo.feature.artists.domain.model.ArtistTopTrack
import com.echo.feature.artists.domain.model.RelatedArtist

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArtistDetailScreen(
    onNavigateBack: () -> Unit,
    onNavigateToAlbum: (String) -> Unit,
    onNavigateToArtist: (String) -> Unit = {},
    viewModel: ArtistDetailViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val listState = rememberLazyListState()

    // Detect when scrolled past the artist name in header
    val showGlassBar by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            state.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = EchoCoral)
                }
            }
            state.error != null -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = state.error ?: "Error desconocido",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            else -> {
                state.artist?.let { artist ->
                    ArtistDetailContent(
                        artist = artist,
                        albums = state.albums,
                        topTracks = state.topTracks,
                        relatedArtists = state.relatedArtists,
                        listState = listState,
                        onAlbumClick = onNavigateToAlbum,
                        onArtistClick = onNavigateToArtist
                    )
                }
            }
        }

        // Glass effect TopAppBar overlay
        GlassTopAppBar(
            artistName = state.artist?.name ?: "",
            showGlass = showGlassBar,
            onNavigateBack = onNavigateBack
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GlassTopAppBar(
    artistName: String,
    showGlass: Boolean,
    onNavigateBack: () -> Unit
) {
    val backgroundColor = if (showGlass) {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
    } else {
        Color.Transparent
    }

    TopAppBar(
        title = {
            if (showGlass) {
                Text(
                    text = artistName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        navigationIcon = {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Volver"
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = backgroundColor
        )
    )
}

@Composable
private fun ArtistDetailContent(
    artist: Artist,
    albums: List<ArtistAlbum>,
    topTracks: List<ArtistTopTrack>,
    relatedArtists: List<RelatedArtist>,
    listState: androidx.compose.foundation.lazy.LazyListState,
    onAlbumClick: (String) -> Unit,
    onArtistClick: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = listState,
        contentPadding = PaddingValues(bottom = 100.dp)
    ) {
        // Header with artist info
        item {
            ArtistHeader(artist = artist)
        }

        // Stats row
        item {
            ArtistStats(artist = artist, albums = albums)
        }

        // Top Tracks section
        if (topTracks.isNotEmpty()) {
            item {
                Text(
                    text = "Canciones Populares",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
                )
            }

            item {
                TopTracksSection(topTracks = topTracks)
            }
        }

        // Albums section
        if (albums.isNotEmpty()) {
            item {
                Text(
                    text = "Discografía",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
                )
            }

            item {
                AlbumsGrid(
                    albums = albums,
                    onAlbumClick = onAlbumClick
                )
            }
        }

        // Related Artists section
        if (relatedArtists.isNotEmpty()) {
            item {
                Text(
                    text = "Artistas Similares",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
                )
            }

            item {
                RelatedArtistsSection(
                    relatedArtists = relatedArtists,
                    onArtistClick = onArtistClick
                )
            }
        }

        // Biography at the end (if available)
        artist.biography?.let { bio ->
            if (bio.isNotBlank()) {
                item {
                    ArtistBiography(biography = bio)
                }
            }
        }
    }
}

@Composable
private fun ArtistHeader(artist: Artist) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(380.dp)
    ) {
        // Main artist image filling the entire header
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(EchoDarkSurfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            // Fallback icon
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(120.dp)
            )
            // Artist profile image (full width)
            AsyncImage(
                model = artist.imageUrl,
                contentDescription = artist.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        // Gradient overlay at the bottom for fade effect
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Transparent,
                            MaterialTheme.colorScheme.background.copy(alpha = 0.3f),
                            MaterialTheme.colorScheme.background.copy(alpha = 0.7f),
                            MaterialTheme.colorScheme.background
                        ),
                        startY = 0f,
                        endY = Float.POSITIVE_INFINITY
                    )
                )
        )

        // Artist Name at the bottom
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.Bottom,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = artist.name,
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ArtistStats(artist: Artist, albums: List<ArtistAlbum>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Main stats row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatItem(
                value = artist.albumCount.toString(),
                label = "Álbumes"
            )
            StatItem(
                value = artist.trackCount.toString(),
                label = "Canciones"
            )
            if (artist.playCount > 0) {
                StatItem(
                    value = artist.playCount.toString(),
                    label = "Reproducciones"
                )
            }
            if (artist.listenerCount > 0) {
                StatItem(
                    value = artist.listenerCount.toString(),
                    label = if (artist.listenerCount == 1) "Oyente" else "Oyentes"
                )
            }
        }
    }
}

@Composable
private fun StatItem(
    value: String,
    label: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold
            ),
            color = EchoCoral
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ArtistBiography(biography: String) {
    var isExpanded by remember { mutableStateOf(false) }
    val needsExpansion = biography.length > 200

    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .then(
                if (needsExpansion) {
                    Modifier.clickable { isExpanded = !isExpanded }
                } else {
                    Modifier
                }
            )
    ) {
        Text(
            text = "Biografía",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = biography,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = if (isExpanded) Int.MAX_VALUE else 4,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.animateContentSize()
        )
        if (needsExpansion) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (isExpanded) "Ver menos" else "Ver más",
                style = MaterialTheme.typography.bodySmall,
                color = EchoCoral,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun AlbumsGrid(
    albums: List<ArtistAlbum>,
    onAlbumClick: (String) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(albums) { album ->
            AlbumItem(
                album = album,
                onClick = { onAlbumClick(album.id) }
            )
        }
    }
}

@Composable
private fun AlbumItem(
    album: ArtistAlbum,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(150.dp)
            .clickable(onClick = onClick)
    ) {
        // Album Cover
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(8.dp))
                .background(EchoDarkSurfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(40.dp)
            )
            AsyncImage(
                model = album.coverUrl,
                contentDescription = album.title,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Album Title
        Text(
            text = album.title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        // Year
        album.year?.let { year ->
            Text(
                text = year.toString(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TopTracksSection(topTracks: List<ArtistTopTrack>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        topTracks.take(5).forEachIndexed { index, track ->
            TopTrackItem(
                track = track,
                position = index + 1
            )
        }
    }
}

@Composable
private fun TopTrackItem(
    track: ArtistTopTrack,
    position: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(EchoDarkSurfaceVariant.copy(alpha = 0.5f))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Position number
        Text(
            text = position.toString(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(24.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        // Album cover
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(EchoDarkSurfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
            track.coverUrl?.let {
                AsyncImage(
                    model = it,
                    contentDescription = track.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Track info
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = track.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            track.albumName?.let { albumName ->
                Text(
                    text = albumName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Duration
        if (track.formattedDuration.isNotEmpty()) {
            Text(
                text = track.formattedDuration,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun RelatedArtistsSection(
    relatedArtists: List<RelatedArtist>,
    onArtistClick: (String) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(relatedArtists) { artist ->
            RelatedArtistItem(
                artist = artist,
                onClick = { onArtistClick(artist.id) }
            )
        }
    }
}

@Composable
private fun RelatedArtistItem(
    artist: RelatedArtist,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(100.dp)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Circular artist image
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(EchoDarkSurfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(40.dp)
            )
            artist.imageUrl?.let {
                AsyncImage(
                    model = it,
                    contentDescription = artist.name,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Artist name
        Text(
            text = artist.name,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}
