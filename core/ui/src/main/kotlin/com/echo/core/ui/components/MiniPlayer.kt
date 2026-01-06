package com.echo.core.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.echo.core.ui.theme.EchoCoral
import com.echo.core.ui.theme.EchoGlass
import com.echo.core.ui.util.rememberEchoHaptics
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

data class MiniPlayerState(
    val isVisible: Boolean = false,
    val isPlaying: Boolean = false,
    val trackTitle: String = "",
    val artistName: String = "",
    val coverUrl: String? = null,
    val progress: Float = 0f,
    val dominantColor: Color? = null,
    val nextTrackTitle: String? = null,
    val nextArtistName: String? = null
)

@Composable
fun MiniPlayer(
    state: MiniPlayerState,
    onPlayerClick: () -> Unit,
    onPlayPauseClick: () -> Unit,
    onNextClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val offsetX = remember { Animatable(0f) }
    val offsetY = remember { Animatable(0f) }
    val haptics = rememberEchoHaptics()
    val density = LocalDensity.current
    val swipeThreshold = 100f
    val swipeUpThreshold = -80f // Negative because up is negative Y
    val textAreaWidth = with(density) { 250.dp.toPx() }
    val nextTrackStartOffset = with(density) { 300.dp.toPx() }

    AnimatedVisibility(
        visible = state.isVisible,
        enter = slideInVertically(initialOffsetY = { it }),
        exit = slideOutVertically(targetOffsetY = { it }),
        modifier = modifier
    ) {
        // Gradient background from album color (like Spotify) - almost fully opaque
        val dominantColor = state.dominantColor ?: Color(0xFF1E293B)
        val gradientBackground = Brush.horizontalGradient(
            colors = listOf(
                dominantColor.copy(alpha = 0.98f),
                dominantColor.copy(alpha = 0.96f),
                dominantColor.copy(alpha = 0.94f, red = dominantColor.red * 0.75f, green = dominantColor.green * 0.75f, blue = dominantColor.blue * 0.75f)
            )
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(gradientBackground)
        ) {
            // Player content with swipe gestures (horizontal for next, vertical for expand)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset { IntOffset(0, offsetY.value.roundToInt()) }
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragEnd = {
                                scope.launch {
                                    // Check if swipe up (to expand player)
                                    if (offsetY.value < swipeUpThreshold) {
                                        haptics.mediumTap()
                                        offsetY.snapTo(0f)
                                        onPlayerClick()
                                    }
                                    // Check if swipe left (next track)
                                    else if (offsetX.value < -swipeThreshold) {
                                        haptics.doubleTap()
                                        offsetX.animateTo(
                                            targetValue = -nextTrackStartOffset,
                                            animationSpec = spring(
                                                dampingRatio = Spring.DampingRatioLowBouncy,
                                                stiffness = Spring.StiffnessMedium
                                            )
                                        )
                                        onNextClick()
                                        offsetX.snapTo(0f)
                                    } else {
                                        // Reset positions
                                        offsetX.animateTo(
                                            targetValue = 0f,
                                            animationSpec = spring(
                                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                                stiffness = Spring.StiffnessMedium
                                            )
                                        )
                                    }
                                    offsetY.animateTo(
                                        targetValue = 0f,
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioMediumBouncy,
                                            stiffness = Spring.StiffnessMedium
                                        )
                                    )
                                }
                            },
                            onDragCancel = {
                                scope.launch {
                                    offsetX.animateTo(0f)
                                    offsetY.animateTo(0f)
                                }
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                scope.launch {
                                    // Determine primary drag direction
                                    if (abs(dragAmount.x) > abs(dragAmount.y)) {
                                        // Horizontal drag (next track)
                                        val newOffsetX = (offsetX.value + dragAmount.x).coerceIn(-textAreaWidth, 0f)
                                        offsetX.snapTo(newOffsetX)
                                    } else {
                                        // Vertical drag (expand player) - only allow upward
                                        val newOffsetY = (offsetY.value + dragAmount.y).coerceIn(-150f, 0f)
                                        offsetY.snapTo(newOffsetY)
                                    }
                                }
                            }
                        )
                    }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onPlayerClick)
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Album cover
                    AsyncImage(
                        model = state.coverUrl,
                        contentDescription = state.trackTitle,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    // Track info area - clipped to bounds
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clipToBounds(),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        // Current track info (slides out to the left)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                        ) {
                            Text(
                                text = state.trackTitle,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = state.artistName,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.8f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Next track info (slides in from the right)
                        if (state.nextTrackTitle != null && offsetX.value < 0) {
                            val nextTrackAlpha = ((-offsetX.value) / 100f).coerceIn(0f, 1f)

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .alpha(nextTrackAlpha)
                                    .offset { IntOffset((nextTrackStartOffset + offsetX.value).roundToInt(), 0) }
                            ) {
                                Text(
                                    text = state.nextTrackTitle,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = state.nextArtistName ?: "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.8f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // Play/Pause button
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(EchoCoral, CircleShape)
                            .clip(CircleShape)
                            .clickable {
                                haptics.lightTap()
                                onPlayPauseClick()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (state.isPlaying) "Pausar" else "Reproducir",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            // Progress bar at bottom
            LinearProgressIndicator(
                progress = { state.progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp),
                color = EchoCoral,
                trackColor = EchoGlass
            )
        }
    }
}
