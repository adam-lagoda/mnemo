package com.lagoda.mnemo.ui.theme

import androidx.compose.ui.graphics.Color

// ── Surfaces — warm whites, shift from off-white to beige
val Background      = Color(0xFFFDFDFD)  // Off-White canvas
val Surface         = Color(0xFFFFFFFF)  // Clean card surface
val SurfaceVariant  = Color(0xFFFAF3E0)  // Cream — subtle surface variation
val SurfaceElevated = Color(0xFFF5E1C8)  // Warm Beige — dialogs, sheets

// ── Text — four levels over Charcoal
val TextPrimary   = Color(0xFF333333)   // Charcoal
val TextSecondary = Color(0xC7333333)   // ~78 %
val TextTertiary  = Color(0x8C333333)   // ~55 %
val TextMuted     = Color(0x72333333)   // ~45 %

// Legacy aliases used by existing screens
val OnSurface        = TextPrimary
val OnSurfaceVariant = TextTertiary

// ── Brand primary — Deep Olive Green
val BrandPrimary    = Color(0xFF4B5320)
val BrandPrimaryDim = Color(0xFF6B7530)  // lighter olive for tints

// ── Terracotta accent — primary actions, active states
val Accent    = Color(0xFFE2725B)  // Terracotta
val AccentDim = Color(0xFFC85C47)  // Deeper terracotta

// ── Semantic
val Error   = Color(0xFFD94C37)   // Warm red
val Outline = Color(0xFFE8E2DA)   // Warm light border

// ── Community colors — mid-saturation, readable on light surfaces
val CommunityColors = listOf(
    Color(0xFF0891B2),  // 0  teal
    Color(0xFF16A34A),  // 1  green
    Color(0xFFD97706),  // 2  amber
    Color(0xFFEA580C),  // 3  orange
    Color(0xFFDB2777),  // 4  pink
    Color(0xFF7C3AED),  // 5  violet
    Color(0xFF2563EB),  // 6  blue
    Color(0xFF0284C7),  // 7  sky
)

fun communityColor(id: Int): Color =
    if (id < 0) TextTertiary
    else CommunityColors[id % CommunityColors.size]
