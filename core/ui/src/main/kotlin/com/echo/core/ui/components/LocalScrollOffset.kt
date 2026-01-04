package com.echo.core.ui.components

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * CompositionLocal to provide scroll offset state to screens and header.
 * Screens should update this value when they scroll.
 */
val LocalScrollOffset = staticCompositionLocalOf<MutableState<Int>> {
    mutableIntStateOf(0)
}
