package com.echo.core.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.echo.core.ui.theme.EchoCoral

data class TrackMenuActions(
    val onAddToQueue: (() -> Unit)? = null,
    val onPlayNext: (() -> Unit)? = null,
    val onAddToPlaylist: (() -> Unit)? = null,
    val onGoToAlbum: (() -> Unit)? = null,
    val onGoToArtist: (() -> Unit)? = null,
    val onRemoveFromPlaylist: (() -> Unit)? = null
)

@Composable
fun TrackOptionsMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    actions: TrackMenuActions,
    modifier: Modifier = Modifier
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        modifier = modifier
    ) {
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

        // Play next
        actions.onPlayNext?.let { action ->
            DropdownMenuItem(
                text = { Text("Reproducir siguiente") },
                onClick = {
                    onDismiss()
                    action()
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.PlaylistPlay,
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

        // Go to album
        actions.onGoToAlbum?.let { action ->
            DropdownMenuItem(
                text = { Text("Ir al album") },
                onClick = {
                    onDismiss()
                    action()
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Album,
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

        // Remove from playlist (danger action)
        actions.onRemoveFromPlaylist?.let { action ->
            DropdownMenuItem(
                text = {
                    Text(
                        text = "Eliminar de playlist",
                        color = MaterialTheme.colorScheme.error
                    )
                },
                onClick = {
                    onDismiss()
                    action()
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            )
        }
    }
}
