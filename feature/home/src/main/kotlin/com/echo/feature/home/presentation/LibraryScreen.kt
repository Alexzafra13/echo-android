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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.echo.core.ui.theme.EchoCoral
import com.echo.core.ui.theme.EchoDarkSurfaceVariant
import com.echo.feature.albums.domain.model.Album
import com.echo.feature.artists.domain.model.Artist
import com.echo.feature.playlists.domain.model.Playlist

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

    // Create Playlist Dialog
    if (state.showCreatePlaylistDialog) {
        CreatePlaylistDialog(
            isLoading = state.isCreatingPlaylist,
            error = state.createPlaylistError,
            onDismiss = { viewModel.hideCreatePlaylistDialog() },
            onCreate = { name, description ->
                viewModel.createPlaylist(name, description)
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 72.dp)
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
                artists = state.artists,
                isLoading = state.isLoadingArtists,
                onArtistClick = onNavigateToArtist
            )
            2 -> PlaylistsTab(
                playlists = state.playlists,
                isLoading = state.isLoadingPlaylists,
                onPlaylistClick = onNavigateToPlaylist,
                onCreateClick = { viewModel.showCreatePlaylistDialog() }
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
            SortOption.RECENT -> albums
            SortOption.NAME_ASC -> albums.sortedBy { it.title.lowercase() }
            SortOption.NAME_DESC -> albums.sortedByDescending { it.title.lowercase() }
            SortOption.ARTIST -> albums.sortedBy { it.artist.lowercase() }
            SortOption.YEAR -> albums.sortedByDescending { it.year ?: 0 }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
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
    artists: List<Artist>,
    isLoading: Boolean,
    onArtistClick: (String) -> Unit
) {
    if (isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = EchoCoral)
        }
    } else if (artists.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "No hay artistas",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 100.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(artists, key = { it.id }) { artist ->
                ArtistItem(
                    artist = artist,
                    onClick = { onArtistClick(artist.id) }
                )
            }
        }
    }
}

@Composable
private fun ArtistItem(
    artist: Artist,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Circular image with fallback icon behind
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(EchoDarkSurfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            // Fallback icon (shown if image fails or while loading)
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(40.dp)
            )
            // Actual image on top
            AsyncImage(
                model = artist.imageUrl,
                contentDescription = artist.name,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = artist.name,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        if (artist.albumCount > 0) {
            Text(
                text = "${artist.albumCount} albums",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PlaylistsTab(
    playlists: List<Playlist>,
    isLoading: Boolean,
    onPlaylistClick: (String) -> Unit,
    onCreateClick: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        if (isLoading) {
            CircularProgressIndicator(
                color = EchoCoral,
                modifier = Modifier.align(Alignment.Center)
            )
        } else if (playlists.isEmpty()) {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.QueueMusic,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "No hay playlists",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Crea tu primera playlist",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(playlists, key = { it.id }) { playlist ->
                    PlaylistItem(
                        playlist = playlist,
                        onClick = { onPlaylistClick(playlist.id) }
                    )
                }
            }
        }

        // FAB for creating playlist
        FloatingActionButton(
            onClick = onCreateClick,
            containerColor = EchoCoral,
            contentColor = Color.White,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 100.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Crear playlist"
            )
        }
    }
}

@Composable
private fun PlaylistItem(
    playlist: Playlist,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(EchoDarkSurfaceVariant)
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Playlist cover or icon
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.surface),
            contentAlignment = Alignment.Center
        ) {
            if (playlist.coverUrl != null) {
                AsyncImage(
                    model = playlist.coverUrl,
                    contentDescription = playlist.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = EchoCoral,
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = playlist.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = "${playlist.trackCount} canciones",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
