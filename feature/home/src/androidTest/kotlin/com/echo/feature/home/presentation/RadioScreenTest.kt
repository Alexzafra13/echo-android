package com.echo.feature.home.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SignalCellular4Bar
import androidx.compose.material.icons.filled.SignalCellularConnectedNoInternet0Bar
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.echo.core.media.model.RadioSignalStatus
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI tests for Radio screen components.
 * These tests verify the visual and interactive behavior of radio UI elements.
 */
@RunWith(AndroidJUnit4::class)
class RadioScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // ============================================
    // Mini Player Tests
    // ============================================

    @Test
    fun miniPlayer_displaysStationName() {
        composeTestRule.setContent {
            MaterialTheme {
                TestMiniPlayer(stationName = "Test FM")
            }
        }

        composeTestRule
            .onNodeWithText("Test FM")
            .assertIsDisplayed()
    }

    @Test
    fun miniPlayer_displaysMetadataWhenAvailable() {
        composeTestRule.setContent {
            MaterialTheme {
                TestMiniPlayer(
                    stationName = "Test FM",
                    metadata = "Artist - Song"
                )
            }
        }

        composeTestRule
            .onNodeWithText("Artist - Song")
            .assertIsDisplayed()
    }

    @Test
    fun miniPlayer_showsPauseWhenPlaying() {
        composeTestRule.setContent {
            MaterialTheme {
                TestMiniPlayer(isPlaying = true)
            }
        }

        composeTestRule
            .onNodeWithContentDescription("Pause")
            .assertIsDisplayed()
    }

    @Test
    fun miniPlayer_showsPlayWhenPaused() {
        composeTestRule.setContent {
            MaterialTheme {
                TestMiniPlayer(isPlaying = false)
            }
        }

        composeTestRule
            .onNodeWithContentDescription("Play")
            .assertIsDisplayed()
    }

    @Test
    fun miniPlayer_showsBufferingIndicator() {
        composeTestRule.setContent {
            MaterialTheme {
                TestMiniPlayer(isBuffering = true)
            }
        }

        composeTestRule
            .onNodeWithContentDescription("Buffering")
            .assertIsDisplayed()
    }

    @Test
    fun miniPlayer_showsGoodSignal() {
        composeTestRule.setContent {
            MaterialTheme {
                TestMiniPlayer(signalStatus = RadioSignalStatus.GOOD)
            }
        }

        composeTestRule
            .onNodeWithContentDescription("Good signal")
            .assertIsDisplayed()
    }

    @Test
    fun miniPlayer_showsWeakSignal() {
        composeTestRule.setContent {
            MaterialTheme {
                TestMiniPlayer(signalStatus = RadioSignalStatus.WEAK)
            }
        }

        composeTestRule
            .onNodeWithContentDescription("Weak signal")
            .assertIsDisplayed()
    }

    @Test
    fun miniPlayer_playPauseClicks() {
        var clicked = false
        composeTestRule.setContent {
            MaterialTheme {
                TestMiniPlayer(
                    isPlaying = true,
                    onPlayPause = { clicked = true }
                )
            }
        }

        composeTestRule
            .onNodeWithContentDescription("Pause")
            .performClick()

        assertTrue("Play/Pause should be clicked", clicked)
    }

    @Test
    fun miniPlayer_stopClicks() {
        var stopped = false
        composeTestRule.setContent {
            MaterialTheme {
                TestMiniPlayer(
                    onStop = { stopped = true }
                )
            }
        }

        composeTestRule
            .onNodeWithContentDescription("Stop")
            .performClick()

        assertTrue("Stop should be clicked", stopped)
    }

    // ============================================
    // Tab Tests
    // ============================================

    @Test
    fun tabs_displaysAllThreeTabs() {
        composeTestRule.setContent {
            MaterialTheme {
                TestRadioTabs()
            }
        }

        composeTestRule.onNodeWithText("Favoritos").assertIsDisplayed()
        composeTestRule.onNodeWithText("Descubrir").assertIsDisplayed()
        composeTestRule.onNodeWithText("Explorar").assertIsDisplayed()
    }

    @Test
    fun tabs_clickChangesSelection() {
        var selectedIndex = 0
        composeTestRule.setContent {
            MaterialTheme {
                TestRadioTabs(
                    selectedIndex = selectedIndex,
                    onTabSelected = { selectedIndex = it }
                )
            }
        }

        composeTestRule
            .onNodeWithText("Descubrir")
            .performClick()

        assertTrue("Tab should change to Discover (index 1)", selectedIndex == 1)
    }

    // ============================================
    // Station Card Tests
    // ============================================

    @Test
    fun stationCard_displaysStationInfo() {
        composeTestRule.setContent {
            MaterialTheme {
                TestStationCard(
                    name = "Jazz FM",
                    country = "USA",
                    tags = "jazz, blues"
                )
            }
        }

        composeTestRule.onNodeWithText("Jazz FM").assertIsDisplayed()
        composeTestRule.onNodeWithText("USA").assertIsDisplayed()
    }

    @Test
    fun stationCard_showsLiveBadgeWhenPlaying() {
        composeTestRule.setContent {
            MaterialTheme {
                TestStationCard(
                    name = "Jazz FM",
                    isPlaying = true
                )
            }
        }

        composeTestRule
            .onNodeWithText("EN VIVO")
            .assertIsDisplayed()
    }

    @Test
    fun stationCard_hidesLiveBadgeWhenNotPlaying() {
        composeTestRule.setContent {
            MaterialTheme {
                TestStationCard(
                    name = "Jazz FM",
                    isPlaying = false
                )
            }
        }

        composeTestRule
            .onNodeWithText("EN VIVO")
            .assertDoesNotExist()
    }

    @Test
    fun stationCard_clickTriggersCallback() {
        var clicked = false
        composeTestRule.setContent {
            MaterialTheme {
                TestStationCard(
                    name = "Jazz FM",
                    onClick = { clicked = true }
                )
            }
        }

        composeTestRule
            .onNodeWithText("Jazz FM")
            .performClick()

        assertTrue("Card should be clicked", clicked)
    }

    // ============================================
    // Signal Indicator Tests
    // ============================================

    @Test
    fun signalIndicator_goodShowsGreenIcon() {
        composeTestRule.setContent {
            MaterialTheme {
                TestSignalIndicator(RadioSignalStatus.GOOD)
            }
        }

        composeTestRule
            .onNodeWithContentDescription("Good signal")
            .assertIsDisplayed()
    }

    @Test
    fun signalIndicator_errorShowsRedIcon() {
        composeTestRule.setContent {
            MaterialTheme {
                TestSignalIndicator(RadioSignalStatus.ERROR)
            }
        }

        composeTestRule
            .onNodeWithContentDescription("Error signal")
            .assertIsDisplayed()
    }
}

// ============================================
// Test Composables (Isolated for Testing)
// ============================================

@Composable
private fun TestMiniPlayer(
    stationName: String = "Test Station",
    metadata: String? = null,
    isPlaying: Boolean = false,
    isBuffering: Boolean = false,
    signalStatus: RadioSignalStatus = RadioSignalStatus.UNKNOWN,
    onPlayPause: () -> Unit = {},
    onStop: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Station icon placeholder
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primary)
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Station info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stationName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (metadata != null) {
                    Text(
                        text = metadata,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Signal indicator
            TestSignalIndicator(signalStatus)

            Spacer(modifier = Modifier.width(8.dp))

            // Play/Pause button
            if (isBuffering) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(24.dp)
                        .semantics { contentDescription = "Buffering" },
                    strokeWidth = 2.dp
                )
            } else {
                IconButton(onClick = onPlayPause) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play"
                    )
                }
            }

            // Stop button
            IconButton(onClick = onStop) {
                Icon(
                    imageVector = Icons.Default.Stop,
                    contentDescription = "Stop"
                )
            }
        }
    }
}

@Composable
private fun TestRadioTabs(
    selectedIndex: Int = 0,
    onTabSelected: (Int) -> Unit = {}
) {
    val tabs = listOf("Favoritos", "Descubrir", "Explorar")

    TabRow(selectedTabIndex = selectedIndex) {
        tabs.forEachIndexed { index, title ->
            Tab(
                selected = selectedIndex == index,
                onClick = { onTabSelected(index) },
                text = { Text(title) }
            )
        }
    }
}

@Composable
private fun TestStationCard(
    name: String,
    country: String = "",
    tags: String = "",
    isPlaying: Boolean = false,
    onClick: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleMedium
                )
                if (country.isNotEmpty()) {
                    Text(
                        text = country,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            if (isPlaying) {
                Box(
                    modifier = Modifier
                        .background(Color.Red, CircleShape)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "EN VIVO",
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}

@Composable
private fun TestSignalIndicator(status: RadioSignalStatus) {
    val (icon, description, tint) = when (status) {
        RadioSignalStatus.GOOD -> Triple(
            Icons.Default.SignalCellular4Bar,
            "Good signal",
            Color.Green
        )
        RadioSignalStatus.WEAK -> Triple(
            Icons.Default.SignalCellularConnectedNoInternet0Bar,
            "Weak signal",
            Color.Yellow
        )
        RadioSignalStatus.ERROR -> Triple(
            Icons.Default.SignalCellularConnectedNoInternet0Bar,
            "Error signal",
            Color.Red
        )
        RadioSignalStatus.UNKNOWN -> Triple(
            Icons.Default.SignalCellularConnectedNoInternet0Bar,
            "Unknown signal",
            Color.Gray
        )
    }

    Icon(
        imageVector = icon,
        contentDescription = description,
        tint = tint,
        modifier = Modifier.size(20.dp)
    )
}
