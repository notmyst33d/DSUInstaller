package com.myst33d.dsuinstaller.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable

private val DarkColorPalette = darkColors(
    primary = BluePrimary,
    primaryVariant = BlueVariant,
    secondary = BlueSecondary
)

private val LightColorPalette = lightColors(
    primary = BluePrimary,
    primaryVariant = BlueVariant,
    secondary = BlueSecondary
)

@Composable
fun DSUInstallerTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colors = if (darkTheme) DarkColorPalette else LightColorPalette,
        typography = if (darkTheme) DarkTypography else LightTypography,
        shapes = Shapes,
        content = content
    )
}