package com.example.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Exact palette from user's provided 5-band swatch image:
 * Band 1 (Deepest Sage): #8C9F85
 * Band 2 (Medium Sage): #B3C2AC
 * Band 3 (Pale Celadon): #D3DDCF
 * Band 4 (Soft Mint Foam): #DFEAE0
 * Band 5 (Warm Cream Ivory): #FDF8EB
 *
 * Text & High-Contrast: Deep Forest Olive (#212C1E) derived directly from Band 1 luminance
 * for ultra-crisp, readable, visible dark text exceeding WCAG AAA standards.
 */

val PaletteDeepSage = Color(0xFF8C9F85)     // Band 1: Primary Accent & Buttons
val PaletteMediumSage = Color(0xFFB3C2AC)   // Band 2: Borders & Secondary Accents
val PalettePaleCeladon = Color(0xFFD3DDCF)  // Band 3: Elevated Cards & Headers
val PaletteMintFoam = Color(0xFFDFEAE0)     // Band 4: Event Containers & Light Cards
val PaletteCreamIvory = Color(0xFFFDF8EB)   // Band 5: Main Screen Background / Ivory

// High-contrast, visible dark text strictly harmonized with the palette
val PaletteDarkText = Color(0xFF212C1E)     // Deep Forest Olive: 13.7:1 contrast on Ivory

// Backward-compatible semantic aliases mapped strictly to these 5 colors
val PaletteMint = PaletteMintFoam
val PaletteParchment = PalettePaleCeladon
val PalettePeachTan = PaletteMediumSage
val PaletteCoral = PaletteDeepSage          // Replaced coral with Band 1 Deep Sage
val PaletteWarmTerracotta = PaletteDeepSage
val PaletteCreamOffWhite = PaletteCreamIvory
val PaletteWarmDark = PaletteDarkText
val PaletteDeepNavy = PaletteDarkText

// Modern Material tokens
val PrimarySage = PaletteDeepSage
val SecondarySage = PaletteMediumSage
val TertiaryCeladon = PalettePaleCeladon
val SurfaceMint = PaletteMintFoam
val BackgroundIvory = PaletteCreamIvory

// Semantic UI aliases strictly bound to the 5 photo colors
val CyanAccent = PaletteDeepSage
val AmberAccent = PaletteDeepSage
val EmeraldSuccess = PaletteDeepSage
val RoseError = PaletteDeepSage
val PurpleAccent = PaletteMediumSage

val BluePrimaryLight = PaletteDeepSage
val BluePrimaryDark = PaletteDeepSage
val BlueContainerLight = PaletteMintFoam
val BlueContainerDark = PalettePaleCeladon

val Navy900 = PaletteDarkText
val Navy800 = PaletteDarkText
val Navy700 = PaletteDarkText
val Navy50 = PaletteCreamIvory
val Navy100 = PaletteMintFoam
