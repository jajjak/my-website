package com.linnan.hayaophoto.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

val AccentStart = Color(0xFF6A5AE0)
val AccentEnd = Color(0xFFFF8A65)
val AccentSolid = Color(0xFF6A5AE0)

private val LightColors = lightColorScheme(
    primary = Color(0xFF6A5AE0),
    onPrimary = Color.White,
    secondary = Color(0xFFFF8A65),
    background = Color(0xFFF7F7FA),
    surface = Color.White,
    surfaceVariant = Color(0xFFEFEFF4),
    onSurface = Color(0xFF1C1C1E),
    onBackground = Color(0xFF1C1C1E),
    error = Color(0xFFE05353)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF9B8FFF),
    onPrimary = Color(0xFF1C1C1E),
    secondary = Color(0xFFFFAB91),
    background = Color(0xFF000000),
    surface = Color(0xFF1C1C1E),
    surfaceVariant = Color(0xFF2C2C2E),
    onSurface = Color(0xFFF2F2F7),
    onBackground = Color(0xFFF2F2F7),
    error = Color(0xFFFF6B6B)
)

@Composable
fun HayaoPhotoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = HayaoTypography,
        content = content
    )
}
