package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

/**
 * Material Theme configured strictly around the user's provided palette:
 * - PaletteDeepSage (#8C9F85)
 * - PaletteMediumSage (#B3C2AC)
 * - PalettePaleCeladon (#D3DDCF)
 * - PaletteMintFoam (#DFEAE0)
 * - PaletteCreamIvory (#FDF8EB)
 * - PaletteDarkText (#212C1E) for crystal-clear visible dark text
 */

private val CleanColorScheme = lightColorScheme(
  primary = PaletteDeepSage,
  onPrimary = PaletteCreamIvory,
  primaryContainer = PaletteMintFoam,
  onPrimaryContainer = PaletteDarkText,
  secondary = PaletteMediumSage,
  onSecondary = PaletteDarkText,
  secondaryContainer = PalettePaleCeladon,
  onSecondaryContainer = PaletteDarkText,
  tertiary = PaletteDeepSage,
  onTertiary = PaletteCreamIvory,
  tertiaryContainer = PaletteMintFoam,
  onTertiaryContainer = PaletteDarkText,
  background = PaletteCreamIvory,
  onBackground = PaletteDarkText,
  surface = PaletteCreamIvory,
  onSurface = PaletteDarkText,
  surfaceVariant = PaletteMintFoam,
  onSurfaceVariant = PaletteDarkText,
  outline = PaletteMediumSage,
  outlineVariant = PalettePaleCeladon,
  error = PaletteDeepSage,
  onError = PaletteCreamIvory
)

@Composable
fun StudyPilotTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false, // Forced to false to strictly honor user's palette
  content: @Composable () -> Unit,
) {
  MaterialTheme(
    colorScheme = CleanColorScheme,
    typography = Typography,
    content = content
  )
}

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit
) = StudyPilotTheme(darkTheme = darkTheme, dynamicColor = false, content = content)
