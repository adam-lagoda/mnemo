package com.mnemo.ui.theme

import androidx.compose.ui.graphics.Color

// Dark-first monochromatic developer palette with cyan accent
val Background = Color(0xFF0A0A0A)
val Surface = Color(0xFF141414)
val SurfaceVariant = Color(0xFF1E1E1E)
val OnSurface = Color(0xFFE8E8E8)
val OnSurfaceVariant = Color(0xFF9A9A9A)
val Accent = Color(0xFF00C8C8)       // Cyan
val AccentDim = Color(0xFF008888)
val Error = Color(0xFFCF6679)
val Outline = Color(0xFF2E2E2E)

// Community colors — used for graph node coloring
val CommunityColors = listOf(
    Color(0xFF00C8C8), // cyan
    Color(0xFF00C87A), // green
    Color(0xFFC8A000), // amber
    Color(0xFFC86400), // orange
    Color(0xFFAA00C8), // purple
    Color(0xFFC80050), // red
    Color(0xFF0064C8), // blue
    Color(0xFF64C800), // lime
)

fun communityColor(id: Int): Color =
    if (id < 0) OnSurfaceVariant
    else CommunityColors[id % CommunityColors.size]
