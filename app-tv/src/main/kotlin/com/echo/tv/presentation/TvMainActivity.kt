package com.echo.tv.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.echo.tv.theme.EchoTvTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Main Activity for Echo TV app.
 *
 * This is a skeleton implementation for the TV interface.
 * TODO: Implement full TV UI when ready:
 * - Home screen with content rows (albums, artists, playlists)
 * - Playback screen with full-screen album art
 * - Search functionality with voice input
 * - Settings screen
 * - D-pad navigation throughout
 */
@AndroidEntryPoint
class TvMainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            EchoTvTheme {
                TvHomeScreen()
            }
        }
    }
}

/**
 * Placeholder home screen for TV.
 * Replace with full implementation using Compose for TV components.
 */
@Composable
fun TvHomeScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1A1A2E)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = null,
                tint = Color(0xFFFF6B6B),
                modifier = Modifier.size(120.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Echo TV",
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Tu música en la gran pantalla",
                fontSize = 24.sp,
                color = Color(0xFFB0B0B0)
            )

            Spacer(modifier = Modifier.height(48.dp))

            Text(
                text = "Próximamente...",
                fontSize = 18.sp,
                color = Color(0xFF808080)
            )
        }
    }
}
