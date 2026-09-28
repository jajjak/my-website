package com.linnan.hayaocamera.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val HayaoColorScheme = darkColorScheme(
    primary = HayaoGold,
    onPrimary = HayaoBlack,
    primaryContainer = HayaoGoldDim,
    onPrimaryContainer = HayaoWhite,
    secondary = HayaoGoldLight,
    onSecondary = HayaoBlack,
    background = HayaoBlack,
    onBackground = HayaoWhite,
    surface = HayaoCharcoal,
    onSurface = HayaoWhite,
    surfaceVariant = HayaoCharcoalLight,
    onSurfaceVariant = HayaoWhiteMuted,
    surfaceContainerHighest = HayaoSurfaceElevated,
    outline = HayaoDivider,
    error = HayaoDanger,
    onError = HayaoBlack
)

/**
 * The app always renders in this fixed dark palette regardless of the system theme,
 * matching the behaviour of dedicated camera hardware / premium camera apps.
 */
@Composable
fun HayaoCameraTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = HayaoColorScheme,
        typography = HayaoTypography,
        content = content
    )
}
