package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val MaxPlayDarkColorScheme = darkColorScheme(
    primary = PrimaryPurple,
    onPrimary = Color.White,
    primaryContainer = PrimaryPurpleDark,
    onPrimaryContainer = Color.White,
    secondary = SurfaceDark,
    onSecondary = PureWhite,
    secondaryContainer = BackgroundDark,
    onSecondaryContainer = SecondaryTextDark,
    tertiary = AccentPurple,
    background = BackgroundDark,
    onBackground = PureWhite,
    surface = SurfaceDark,
    onSurface = PureWhite,
    surfaceVariant = CardSurfaceDark,
    onSurfaceVariant = PureWhite,
    outline = BorderWhite5
)

private val MaxPlayLightColorScheme = lightColorScheme(
    primary = PrimaryPurpleDark,
    onPrimary = Color.White,
    primaryContainer = PrimaryPurple,
    onPrimaryContainer = Color.Black,
    secondary = Color(0xFFF1F5F9),
    onSecondary = Color.Black,
    secondaryContainer = Color.White,
    onSecondaryContainer = Color.DarkGray,
    tertiary = AccentPurple,
    background = Color(0xFFF8FAFC),
    onBackground = Color.Black,
    surface = Color.White,
    onSurface = Color.Black,
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = Color.DarkGray,
    outline = Color(0xFFE2E8F0)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Force Dark as premium standard, but support selection
    useDynamicColors: Boolean = false, // Configurable via Settings
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        useDynamicColors && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> MaxPlayDarkColorScheme
        else -> MaxPlayLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
