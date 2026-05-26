package com.mnemo.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Design uses Inter, JetBrains Mono, and Instrument Serif.
// Mapped to system families; swap for bundled/downloadable fonts when assets are added.
val InterFamily         = FontFamily.SansSerif
val JetBrainsMonoFamily = FontFamily.Monospace
val InstrumentSerif     = FontFamily.Serif

val MnemoTypography = Typography(
    // App wordmark / onboarding headline
    displaySmall = TextStyle(
        fontFamily = InterFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 30.sp,
        letterSpacing = (-1.2).sp,
        lineHeight = 34.sp,
    ),
    // Page/section headers
    headlineMedium = TextStyle(
        fontFamily = InterFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 15.sp,
        letterSpacing = (-0.6).sp,
        lineHeight = 20.sp,
    ),
    // Card titles, list primary text
    titleMedium = TextStyle(
        fontFamily = InterFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        letterSpacing = 0.sp,
        lineHeight = 18.sp,
    ),
    // Body copy
    bodyMedium = TextStyle(
        fontFamily = InterFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        letterSpacing = 0.sp,
        lineHeight = 22.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = InterFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        letterSpacing = 0.sp,
        lineHeight = 18.sp,
    ),
    // Mono metadata — chips, timestamps, IDs
    labelSmall = TextStyle(
        fontFamily = JetBrainsMonoFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 9.sp,
        letterSpacing = 0.8.sp,
        lineHeight = 13.sp,
    ),
    // Navigation tab labels
    labelMedium = TextStyle(
        fontFamily = JetBrainsMonoFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 8.sp,
        letterSpacing = 1.sp,
        lineHeight = 12.sp,
    ),
)

// ── Extra styles applied directly (not in MaterialTheme slots)

// Detail screen screenshot title
val SerifTitle = TextStyle(
    fontFamily = InstrumentSerif,
    fontWeight = FontWeight.Normal,
    fontStyle = FontStyle.Normal,
    fontSize = 28.sp,
    letterSpacing = (-0.4).sp,
    lineHeight = 34.sp,
)

// Search screen suggestion labels
val SerifItalic = TextStyle(
    fontFamily = InstrumentSerif,
    fontWeight = FontWeight.Normal,
    fontStyle = FontStyle.Italic,
    fontSize = 17.sp,
    letterSpacing = 0.sp,
    lineHeight = 24.sp,
)
