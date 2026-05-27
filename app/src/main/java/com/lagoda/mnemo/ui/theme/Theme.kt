package com.lagoda.mnemo.ui.theme

import android.app.Activity
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = Accent,
    onPrimary = Surface,
    secondary = BrandPrimary,
    onSecondary = Surface,
    background = Background,
    surface = Surface,
    surfaceVariant = SurfaceVariant,
    surfaceContainer = SurfaceElevated,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    onSurfaceVariant = TextTertiary,
    error = Error,
    outline = Outline,
)

@Composable
fun MnemoTheme(content: @Composable () -> Unit) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.setDecorFitsSystemWindows(window, false)
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
        }
    }
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = MnemoTypography,
        content = content
    )
}
