package com.echo.feature.player.presentation

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.echo.core.media.player.RepeatMode
import com.echo.core.ui.theme.EchoCoral
import com.echo.core.ui.util.ColorExtractor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    onNavigateBack: () -> Unit,
    onNavigateToQueue: () -> Unit,
    viewModel: PlayerViewModel = hiltViewModel()
) {
    val state by viewModel.playerState.collectAsState()
    val currentTrack = state.currentTrack
    val context = LocalContext.current

    // Extract dominant color from album art
    var dominantColor by remember { mutableStateOf<Color?>(null) }
    LaunchedEffect(currentTrack?.coverUrl) {
        val coverUrl = currentTrack?.coverUrl
        if (coverUrl != null) {
            dominantColor = ColorExtractor.extractDominantColor(
                context = context,
                imageUrl = coverUrl
            )
        } else {
            dominantColor = null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Reproduciendo de",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = currentTrack?.albumTitle ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Medium
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Cerrar",
                            modifier = Modifier.size(32.dp)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToQueue) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                            contentDescription = "Cola"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            // Background with dominant color gradient
            val baseColor = dominantColor ?: MaterialTheme.colorScheme.background
            val backgroundColor = MaterialTheme.colorScheme.background

            // Base background
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(backgroundColor)
            )

            // Diagonal gradient from top-left corner - more organic feel
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                baseColor.copy(alpha = 0.6f),
                                baseColor.copy(alpha = 0.3f),
                                Color.Transparent
                            ),
                            start = androidx.compose.ui.geometry.Offset(0f, 0f),
                            end = androidx.compose.ui.geometry.Offset(800f, 1000f)
                        )
                    )
            )

            // Secondary blob from top-right - creates asymmetry
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(500.dp)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                baseColor.copy(alpha = 0.4f),
                                baseColor.copy(alpha = 0.15f),
                                Color.Transparent
                            ),
                            center = androidx.compose.ui.geometry.Offset(900f, 100f),
                            radius = 600f
                        )
                    )
            )

            // Glow behind album art - slightly off-center
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(450.dp)
                    .padding(top = 50.dp)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                baseColor.copy(alpha = 0.5f),
                                baseColor.copy(alpha = 0.2f),
                                Color.Transparent
                            ),
                            center = androidx.compose.ui.geometry.Offset(500f, 350f),
                            radius = 450f
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(24.dp))

                // Album Art with colored shadow
                AsyncImage(
                    model = currentTrack?.coverUrl,
                    contentDescription = currentTrack?.title,
                    modifier = Modifier
                        .size(300.dp)
                        .shadow(
                            elevation = 40.dp,
                            shape = RoundedCornerShape(12.dp),
                            ambientColor = baseColor.copy(alpha = 0.5f),
                            spotColor = baseColor.copy(alpha = 0.5f)
                        )
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Track Info
                Text(
                    text = currentTrack?.title ?: "Sin reproducir",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = currentTrack?.artist ?: "",
                    style = MaterialTheme.typography.bodyLarge,
                    color = EchoCoral,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Progress Slider
                ProgressSlider(
                    progress = state.progress,
                    position = state.position,
                    duration = state.duration,
                    onSeek = { viewModel.seekToProgress(it) }
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Playback Controls
                PlaybackControls(
                    isPlaying = state.isPlaying,
                    repeatMode = state.repeatMode,
                    shuffleEnabled = state.shuffleEnabled,
                    onPlayPauseClick = { viewModel.togglePlayPause() },
                    onPreviousClick = { viewModel.skipToPrevious() },
                    onNextClick = { viewModel.skipToNext() },
                    onRepeatClick = { viewModel.toggleRepeatMode() },
                    onShuffleClick = { viewModel.toggleShuffle() }
                )

                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ProgressSlider(
    progress: Float,
    position: Long,
    duration: Long,
    onSeek: (Float) -> Unit
) {
    var isDragging by remember { mutableStateOf(false) }
    var dragProgress by remember { mutableFloatStateOf(0f) }

    val displayProgress = if (isDragging) dragProgress else progress

    Column(modifier = Modifier.fillMaxWidth()) {
        Slider(
            value = displayProgress,
            onValueChange = {
                isDragging = true
                dragProgress = it
            },
            onValueChangeFinished = {
                onSeek(dragProgress)
                isDragging = false
            },
            colors = SliderDefaults.colors(
                thumbColor = EchoCoral,
                activeTrackColor = EchoCoral,
                inactiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formatDuration(if (isDragging) (duration * dragProgress).toLong() else position),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = formatDuration(duration),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PlaybackControls(
    isPlaying: Boolean,
    repeatMode: RepeatMode,
    shuffleEnabled: Boolean,
    onPlayPauseClick: () -> Unit,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit,
    onRepeatClick: () -> Unit,
    onShuffleClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Shuffle
        IconButton(onClick = onShuffleClick) {
            Icon(
                imageVector = Icons.Default.Shuffle,
                contentDescription = "Aleatorio",
                tint = if (shuffleEnabled) EchoCoral else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.size(24.dp)
            )
        }

        // Previous
        IconButton(
            onClick = onPreviousClick,
            modifier = Modifier.size(56.dp)
        ) {
            Icon(
                imageVector = Icons.Default.SkipPrevious,
                contentDescription = "Anterior",
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(36.dp)
            )
        }

        // Play/Pause
        Box(
            modifier = Modifier
                .shadow(
                    elevation = 12.dp,
                    shape = CircleShape,
                    ambientColor = EchoCoral.copy(alpha = 0.4f),
                    spotColor = EchoCoral.copy(alpha = 0.4f)
                )
                .size(72.dp)
                .background(EchoCoral, CircleShape)
                .clickable(onClick = onPlayPauseClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isPlaying) "Pausar" else "Reproducir",
                tint = Color.White,
                modifier = Modifier.size(40.dp)
            )
        }

        // Next
        IconButton(
            onClick = onNextClick,
            modifier = Modifier.size(56.dp)
        ) {
            Icon(
                imageVector = Icons.Default.SkipNext,
                contentDescription = "Siguiente",
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(36.dp)
            )
        }

        // Repeat
        IconButton(onClick = onRepeatClick) {
            Icon(
                imageVector = when (repeatMode) {
                    RepeatMode.ONE -> Icons.Default.RepeatOne
                    else -> Icons.Default.Repeat
                },
                contentDescription = "Repetir",
                tint = when (repeatMode) {
                    RepeatMode.OFF -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    else -> EchoCoral
                },
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

private fun formatDuration(millis: Long): String {
    val totalSeconds = millis / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "$minutes:${seconds.toString().padStart(2, '0')}"
}
