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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.echo.core.ui.theme.EchoCoral
import com.echo.core.ui.theme.EchoDarkSurfaceVariant
import com.echo.feature.albums.domain.model.Album

enum class SortOption(val label: String) {
    RECENT("Reciente"),
    NAME_ASC("Nombre A-Z"),
    NAME_DESC("Nombre Z-A"),
    ARTIST("Artista"),
    YEAR("Año")
}

@Composable
fun LibraryScreen(
    onNavigateToAlbum: (String) -> Unit,
    onNavigateToArtist: (String) -> Unit,
    onNavigateToPlaylist: (String) -> Unit,
    viewModel: LibraryViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    var sortOption by remember { mutableStateOf(SortOption.RECENT) }
    val tabs = listOf("Albums", "Artistas", "Playlists")

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Header
        Text(
            text = "Biblioteca",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
        )

        // Tabs
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.background,
            contentColor = EchoCoral,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = EchoCoral
                )
            }
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            text = title,
                            fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    selectedContentColor = EchoCoral,
                    unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Content based on tab
        when (selectedTab) {
            0 -> AlbumsTab(
                albums = state.albums,
                isLoading = state.isLoading,
                sortOption = sortOption,
                onSortChange = { sortOption = it },
                onAlbumClick = onNavigateToAlbum
            )
            1 -> ArtistsTab(
                onArtistClick = onNavigateToArtist
            )
            2 -> PlaylistsTab(
                onPlaylistClick = onNavigateToPlaylist
            )
        }
    }
}

@Composable
private fun AlbumsTab(
    albums: List<Album>,
    isLoading: Boolean,
    sortOption: SortOption,
    onSortChange: (SortOption) -> Unit,
    onAlbumClick: (String) -> Unit
) {
    val sortedAlbums = remember(albums, sortOption) {
        when (sortOption) {
            SortOption.RECENT -> albums // Already sorted by recent from API
            SortOption.NAME_ASC -> albums.sortedBy { it.title.lowercase() }
            SortOption.NAME_DESC -> albums.sortedByDescending { it.title.lowercase() }
            SortOption.ARTIST -> albums.sortedBy { it.artist.lowercase() }
            SortOption.YEAR -> albums.sortedByDescending { it.year ?: 0 }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Sort selector
        SortSelector(
            currentSort = sortOption,
            onSortChange = onSortChange,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = EchoCoral)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 100.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(sortedAlbums, key = { it.id }) { album ->
                    LibraryAlbumItem(
                        album = album,
                        onClick = { onAlbumClick(album.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SortSelector(
    currentSort: SortOption,
    onSortChange: (SortOption) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        TextButton(
            onClick = { expanded = true }
        ) {
            Text(
                text = "Ordenar: ${currentSort.label}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            SortOption.entries.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = option.label,
                            color = if (option == currentSort) EchoCoral
                                    else MaterialTheme.colorScheme.onSurface
                        )
                    },
                    onClick = {
                        onSortChange(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun LibraryAlbumItem(
    album: Album,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        AsyncImage(
            model = album.coverUrl,
            contentDescription = album.title,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(8.dp))
                .background(EchoDarkSurfaceVariant),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = album.title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Text(
            text = album.artist,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ArtistsTab(
    onArtistClick: (String) -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Album,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Artistas proximamente",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PlaylistsTab(
    onPlaylistClick: (String) -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Album,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Playlists proximamente",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
