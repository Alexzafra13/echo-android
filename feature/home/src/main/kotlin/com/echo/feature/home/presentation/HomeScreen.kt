package com.echo.feature.home.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.echo.core.ui.components.AlbumCard
import com.echo.core.ui.theme.EchoCoral
import com.echo.feature.albums.domain.model.Album

@Composable
fun HomeScreen(
    onNavigateToAlbum: (String) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        if (state.isLoading && state.recentAlbums.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = EchoCoral)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 100.dp)
            ) {
                // Hero Section - extends under header for seamless background
                state.featuredAlbum?.let { album ->
                    item {
                        HeroSection(
                            album = album,
                            onClick = { onNavigateToAlbum(album.id) },
                            onPlayClick = { viewModel.playAlbum(album) }
                        )
                    }
                }

                // Spacer if no hero to account for header
                if (state.featuredAlbum == null) {
                    item {
                        Spacer(modifier = Modifier.height(72.dp))
                    }
                }

                // Recently Added
                if (state.recentAlbums.isNotEmpty()) {
                    item {
                        AlbumSection(
                            title = "Añadidos recientemente",
                            albums = state.recentAlbums,
                            onAlbumClick = onNavigateToAlbum,
                            onPlayAlbum = { viewModel.playAlbum(it) }
                        )
                    }
                }

                // Top Played
                if (state.topPlayedAlbums.isNotEmpty()) {
                    item {
                        AlbumSection(
                            title = "Más escuchados",
                            albums = state.topPlayedAlbums,
                            onAlbumClick = onNavigateToAlbum,
                            onPlayAlbum = { viewModel.playAlbum(it) }
                        )
                    }
                }

                // Recently Played
                if (state.recentlyPlayedAlbums.isNotEmpty()) {
                    item {
                        AlbumSection(
                            title = "Escuchados recientemente",
                            albums = state.recentlyPlayedAlbums,
                            onAlbumClick = onNavigateToAlbum,
                            onPlayAlbum = { viewModel.playAlbum(it) }
                        )
                    }
                }

                // Error state
                state.error?.let { error ->
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = error,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroSection(
    album: Album,
    onClick: () -> Unit,
    onPlayClick: () -> Unit
) {
    // Height includes space for header (72dp) so background extends under it
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(350.dp)
            .clickable(onClick = onClick)
    ) {
        // Background Image with blur - extends to top
        AsyncImage(
            model = album.coverUrl,
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .blur(radius = 20.dp),
            contentScale = ContentScale.Crop
        )

        // Dark overlay with gradient
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.3f),
                            Color.Transparent,
                            MaterialTheme.colorScheme.background.copy(alpha = 0.8f),
                            MaterialTheme.colorScheme.background
                        ),
                        startY = 0f,
                        endY = Float.POSITIVE_INFINITY
                    )
                )
        )

        // Content - positioned below header area
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 80.dp, start = 24.dp, end = 24.dp, bottom = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Album Cover
            AsyncImage(
                model = album.coverUrl,
                contentDescription = album.title,
                modifier = Modifier
                    .size(140.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .shadow(elevation = 16.dp, shape = RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(20.dp))

            // Album Info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = album.artist.uppercase(),
                    style = MaterialTheme.typography.labelMedium.copy(
                        letterSpacing = 2.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = album.title,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = EchoCoral,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                album.year?.let { year ->
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = year.toString(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Play Button
                Box(
                    modifier = Modifier
                        .shadow(
                            elevation = 8.dp,
                            shape = CircleShape,
                            ambientColor = EchoCoral.copy(alpha = 0.4f),
                            spotColor = EchoCoral.copy(alpha = 0.4f)
                        )
                        .size(48.dp)
                        .background(EchoCoral, CircleShape)
                        .clickable(onClick = onPlayClick),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Reproducir",
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun AlbumSection(
    title: String,
    albums: List<Album>,
    onAlbumClick: (String) -> Unit,
    onPlayAlbum: (Album) -> Unit
) {
    Column(
        modifier = Modifier.padding(vertical = 16.dp)
    ) {
        // Section Title
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold
            ),
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Albums Row
        LazyRow(
            contentPadding = PaddingValues(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(albums) { album ->
                AlbumCard(
                    title = album.title,
                    artist = album.artist,
                    coverUrl = album.coverUrl,
                    onClick = { onAlbumClick(album.id) },
                    modifier = Modifier.width(150.dp)
                )
            }
        }
    }
}
