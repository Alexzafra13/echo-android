package com.echo.core.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

data class AlbumMenuActions(
    val onPlay: (() -> Unit)? = null,
    val onShuffle: (() -> Unit)? = null,
    val onAddToQueue: (() -> Unit)? = null,
    val onAddToPlaylist: (() -> Unit)? = null,
    val onGoToArtist: (() -> Unit)? = null
)

@Composable
fun AlbumOptionsMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    actions: AlbumMenuActions,
    modifier: Modifier = Modifier
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        modifier = modifier
    ) {
        // Play album
        actions.onPlay?.let { action ->
            DropdownMenuItem(
                text = { Text("Reproducir") },
                onClick = {
                    onDismiss()
                    action()
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            )
        }

        // Shuffle album
        actions.onShuffle?.let { action ->
            DropdownMenuItem(
                text = { Text("Reproducir en aleatorio") },
                onClick = {
                    onDismiss()
                    action()
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Shuffle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            )
        }

        // Add to queue
        actions.onAddToQueue?.let { action ->
            DropdownMenuItem(
                text = { Text("Agregar a la cola") },
                onClick = {
                    onDismiss()
                    action()
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            )
        }

        // Add to playlist
        actions.onAddToPlaylist?.let { action ->
            DropdownMenuItem(
                text = { Text("Agregar a playlist") },
                onClick = {
                    onDismiss()
                    action()
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.PlaylistAdd,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            )
        }

        // Go to artist
        actions.onGoToArtist?.let { action ->
            DropdownMenuItem(
                text = { Text("Ir al artista") },
                onClick = {
                    onDismiss()
                    action()
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            )
        }
    }
}
