package com.echo.core.ui.util

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Represents the dynamic padding values for content areas.
 * These values change based on which overlays are visible (MiniPlayer, BottomNav).
 */
data class ContentPadding(
    val top: Dp = 0.dp,
    val bottom: Dp = 0.dp
) {
    companion object {
        // Standard heights for overlay components
        val TOP_BAR_HEIGHT = 56.dp
        val MINI_PLAYER_HEIGHT = 72.dp
        val BOTTOM_NAV_HEIGHT = 64.dp

        /**
         * Calculate bottom padding based on visible overlays
         */
        fun calculateBottomPadding(
            showMiniPlayer: Boolean,
            showBottomNav: Boolean
        ): Dp {
            var padding = 0.dp
            if (showMiniPlayer) padding += MINI_PLAYER_HEIGHT
            if (showBottomNav) padding += BOTTOM_NAV_HEIGHT
            return padding
        }
    }
}

/**
 * CompositionLocal that provides content padding to child composables.
 * This allows screens to know how much padding to add to avoid overlays.
 */
val LocalContentPadding = compositionLocalOf { ContentPadding() }
