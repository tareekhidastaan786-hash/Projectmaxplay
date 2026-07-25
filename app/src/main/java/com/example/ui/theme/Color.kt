package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Luxurious Purple Gradient Palette
val PurpleStart = Color(0xFF6A11CB)
val PurpleMid = Color(0xFF8E2DE2)
val PurpleEnd = Color(0xFFB517FF)

val BackgroundDark = Color(0xFF050A18)
val SurfaceDark = Color(0xFF121A2D)
val CardSurfaceDark = Color(0xFF1A263E) // Slightly lighter for card depth
val BorderWhite5 = Color(0x0DFFFFFF)   // 5% white border for glassmorphism
val GlassBackground = Color(0x1A121A2D) // Glassmorphism translucent background

val PureWhite = Color(0xFFFFFFFF)
val SecondaryTextDark = Color(0xFFB8C0D4)

// Legacy / M3 system mapping compatibility
val PrimaryPurple = Color(0xFF8E2DE2)
val PrimaryPurpleDark = Color(0xFF6A11CB)
val AccentPurple = Color(0xFFB517FF)

// Alias/Legacy references mapping for instant theme migration
val NeonGreen = AccentPurple       // Glowing purple accent instead of neon green!
val NeonGreenDark = PrimaryPurple   // Primary purple
val DeepNavyBlack = BackgroundDark
val DarkSurface = SurfaceDark
val CardSurface = CardSurfaceDark
val SecondaryText = SecondaryTextDark
val SlateGray = Color(0xFF475569)

// M3 compatibility mappings
val Purple80 = AccentPurple
val PurpleGrey80 = SurfaceDark
val Pink80 = Color(0xFFE2E8F0)

val Purple40 = PrimaryPurpleDark
val PurpleGrey40 = BackgroundDark
val Pink40 = PrimaryPurple
