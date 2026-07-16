package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val ProfessionalLightColorScheme = lightColorScheme(
    primary = BeigePrimary,
    onPrimary = Color.White,
    primaryContainer = BeigeAccent,
    onPrimaryContainer = BeigeOnBackground,
    secondary = BeigeSecondary,
    onSecondary = Color.White,
    background = BeigeBackgroundLight,
    onBackground = BeigeOnBackground,
    surface = BeigeSurfaceLight,
    onSurface = BeigeOnBackground,
    surfaceVariant = BeigeAccent.copy(alpha = 0.2f),
    onSurfaceVariant = BeigeSecondary
)

private val ProfessionalDarkColorScheme = darkColorScheme(
    primary = BeigeAccentDark,
    onPrimary = BeigeBackgroundDark,
    primaryContainer = BeigeSurfaceVariantDark,
    onPrimaryContainer = BeigePrimaryDark,
    secondary = E2eeGreen,
    onSecondary = BeigeBackgroundDark,
    background = BeigeBackgroundDark,
    onBackground = BeigePrimaryDark,
    surface = BeigeSurfaceDark,
    onSurface = BeigePrimaryDark,
    surfaceVariant = BeigeSurfaceVariantDark,
    onSurfaceVariant = BeigeSecondaryDark
)

private val HighContrastColorScheme = darkColorScheme(
    primary = Color.White,
    onPrimary = Color.Black,
    primaryContainer = Color.Black,
    onPrimaryContainer = Color.White,
    secondary = HighContrastPrimary,
    onSecondary = Color.Black,
    background = HighContrastBgDark,
    onBackground = Color.White,
    surface = HighContrastBgDark,
    onSurface = Color.White,
    surfaceVariant = Color.Black,
    onSurfaceVariant = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    highContrastMode: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        highContrastMode -> HighContrastColorScheme
        darkTheme -> ProfessionalDarkColorScheme
        else -> ProfessionalLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
