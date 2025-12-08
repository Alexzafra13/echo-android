package com.echo.core.ui.theme

import androidx.compose.ui.graphics.Color

// Echo Brand Colors - Coral Primary
val EchoCoral = Color(0xFFED6842)           // Primary coral
val EchoCoralHover = Color(0xFFE04D28)      // Darker coral for pressed states
val EchoCoralDark = Color(0xFFC6401D)       // Even darker
val EchoCoralLight = Color(0xFFED6842).copy(alpha = 0.15f)

// Secondary - Teal/Cyan
val EchoTeal = Color(0xFF22D3EE)
val EchoTealDark = Color(0xFF06B6D4)

// Dark Mode Backgrounds (Slate scale)
val EchoDarkBackground = Color(0xFF0F172A)      // Base background
val EchoDarkSurface = Color(0xFF1E293B)         // Elevated surfaces
val EchoDarkSurfaceVariant = Color(0xFF334155)  // Cards, inputs
val EchoDarkSurfaceHigh = Color(0xFF475569)     // Higher elevation

// Light Mode Backgrounds
val EchoLightBackground = Color(0xFFF8FAFC)
val EchoLightSurface = Color(0xFFFFFFFF)
val EchoLightSurfaceVariant = Color(0xFFF1F5F9)

// Text Colors - Dark Mode
val EchoTextPrimary = Color(0xFFF8FAFC)         // White-ish
val EchoTextSecondary = Color(0xFF94A3B8)       // Slate 400
val EchoTextTertiary = Color(0xFF64748B)        // Slate 500

// Text Colors - Light Mode
val EchoTextPrimaryLight = Color(0xFF0F172A)    // Slate 900
val EchoTextSecondaryLight = Color(0xFF475569)  // Slate 600
val EchoTextTertiaryLight = Color(0xFF64748B)   // Slate 500

// Borders and Dividers
val EchoBorderDark = Color(0xFFFFFFFF).copy(alpha = 0.1f)
val EchoBorderLight = Color(0xFF000000).copy(alpha = 0.1f)

// Overlay Colors
val EchoOverlayLight = Color(0xFFFFFFFF)
val EchoOverlayDark = Color(0xFF000000)

// Status Colors
val EchoSuccess = Color(0xFF10B981)     // Emerald
val EchoWarning = Color(0xFFF59E0B)     // Amber
val EchoError = Color(0xFFEF4444)       // Red
val EchoInfo = Color(0xFF22D3EE)        // Cyan (same as secondary)

// Player specific
val EchoPlayerBackground = Color(0xFF141414).copy(alpha = 0.95f)
val EchoProgressTrack = Color(0xFFFFFFFF).copy(alpha = 0.1f)
val EchoProgressIndicator = EchoCoral

// Glassmorphism
val EchoGlass = Color(0xFFFFFFFF).copy(alpha = 0.08f)
val EchoGlassBorder = Color(0xFFFFFFFF).copy(alpha = 0.15f)
val EchoGlassHover = Color(0xFFFFFFFF).copy(alpha = 0.12f)
