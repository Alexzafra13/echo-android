package com.echo.core.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.echo.core.ui.theme.EchoCoral
import com.echo.core.ui.theme.EchoGlass
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

data class MiniPlayerState(
    val isVisible: Boolean = false,
    val isPlaying: Boolean = false,
    val trackTitle: String = "",
    val artistName: String = "",
    val coverUrl: String? = null,
    val progress: Float = 0f,
    val dominantColor: Color? = null
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
    val swipeThreshold = 100f

    AnimatedVisibility(
        visible = state.isVisible,
        enter = slideInVertically(initialOffsetY = { it }),
        exit = slideOutVertically(targetOffsetY = { it }),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            // Dynamic background based on album art color
            val backgroundColor = state.dominantColor?.let { dominantColor ->
                Brush.horizontalGradient(
                    colors = listOf(
                        dominantColor.copy(alpha = 0.9f),
                        dominantColor.copy(alpha = 0.7f),
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                    )
                )
            } ?: Brush.horizontalGradient(
                colors = listOf(
                    MaterialTheme.colorScheme.surface,
                    MaterialTheme.colorScheme.surface
                )
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = 8.dp,
                        shape = RoundedCornerShape(12.dp),
                        ambientColor = state.dominantColor?.copy(alpha = 0.3f) ?: Color.Black.copy(alpha = 0.3f),
                        spotColor = state.dominantColor?.copy(alpha = 0.3f) ?: Color.Black.copy(alpha = 0.3f)
                    )
                    .clip(RoundedCornerShape(12.dp))
                    .background(backgroundColor)
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
                                            // Swipe left detected - animate out and trigger next
                                            offsetX.animateTo(
                                                targetValue = -screenWidth.toPx(),
                                                animationSpec = tween(200)
                                            )
                                            onNextClick()
                                            // Reset position instantly for next track
                                            offsetX.snapTo(0f)
                                        } else {
                                            // Snap back to original position
                                            offsetX.animateTo(
                                                targetValue = 0f,
                                                animationSpec = tween(200)
                                            )
                                        }
                                    }
                                },
                                onDragCancel = {
                                    scope.launch {
                                        offsetX.animateTo(0f, tween(200))
                                    }
                                },
                                onHorizontalDrag = { _, dragAmount ->
                                    scope.launch {
                                        // Only allow swiping left (negative direction)
                                        val newOffset = (offsetX.value + dragAmount).coerceAtMost(0f)
                                        offsetX.snapTo(newOffset)
                                    }
                                }
                            )
                        }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                            .alpha(1f - (offsetX.value.absoluteValue / 300f).coerceIn(0f, 0.5f))
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

                        // Track info
                        Column(
                            modifier = Modifier.weight(1f)
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

                        // Play/Pause button only
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
