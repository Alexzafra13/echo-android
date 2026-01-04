package com.echo.core.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.echo.core.ui.theme.EchoCoral
import com.echo.core.ui.theme.EchoGlass
import kotlinx.coroutines.launch
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
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val density = LocalDensity.current
    val swipeThreshold = 100f
    val textAreaWidth = with(density) { 250.dp.toPx() } // Width for swipe limit
    val nextTrackStartOffset = with(density) { 300.dp.toPx() } // Start position for next track (further right)

    AnimatedVisibility(
        visible = state.isVisible,
        enter = slideInVertically(initialOffsetY = { it }),
        exit = slideOutVertically(targetOffsetY = { it }),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            // Dynamic gradient background based on album art color - no borders, just fades
            val gradientBackground = state.dominantColor?.let { dominantColor ->
                Brush.horizontalGradient(
                    colors = listOf(
                        dominantColor.copy(alpha = 0.55f),
                        dominantColor.copy(alpha = 0.2f),
                        Color.Transparent
                    )
                )
            } ?: Brush.horizontalGradient(
                colors = listOf(
                    Color(0xFF1E293B).copy(alpha = 0.5f),
                    Color.Transparent
                )
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(gradientBackground)
                    .padding(horizontal = 8.dp)
            ) {
                // Player content with swipe gesture
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .pointerInput(Unit) {
                            detectHorizontalDragGestures(
                                onDragEnd = {
                                    scope.launch {
                                        if (offsetX.value < -swipeThreshold) {
                                            // Swipe left detected - animate to final position smoothly
                                            offsetX.animateTo(
                                                targetValue = -nextTrackStartOffset,
                                                animationSpec = spring(
                                                    dampingRatio = Spring.DampingRatioLowBouncy,
                                                    stiffness = Spring.StiffnessMedium
                                                )
                                            )
                                            onNextClick()
                                            // Reset position instantly for next track
                                            offsetX.snapTo(0f)
                                        } else {
                                            // Snap back to original position with spring
                                            offsetX.animateTo(
                                                targetValue = 0f,
                                                animationSpec = spring(
                                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                                    stiffness = Spring.StiffnessMedium
                                                )
                                            )
                                        }
                                    }
                                },
                                onDragCancel = {
                                    scope.launch {
                                        offsetX.animateTo(
                                            targetValue = 0f,
                                            animationSpec = spring(
                                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                                stiffness = Spring.StiffnessMedium
                                            )
                                        )
                                    }
                                },
                                onHorizontalDrag = { _, dragAmount ->
                                    scope.launch {
                                        // Only allow swiping left (negative direction)
                                        val newOffset = (offsetX.value + dragAmount).coerceIn(-textAreaWidth, 0f)
                                        offsetX.snapTo(newOffset)
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

                            // Next track info (slides in from the right - only visible when swiping)
                            if (state.nextTrackTitle != null && offsetX.value < 0) {
                                // Fade in gradually as user swipes (0 to 1 over first 100px of swipe)
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

                        // Play/Pause button (stays in place)
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(EchoCoral, CircleShape)
                                .clip(CircleShape)
                                .clickable(onClick = onPlayPauseClick),
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
}
