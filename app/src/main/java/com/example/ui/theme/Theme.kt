package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val TvColorScheme = darkColorScheme(
    primary = TvAccentCyan,
    onPrimary = TvBackground,
    primaryContainer = TvAccentBlue,
    onPrimaryContainer = TvTextPrimary,
    secondary = TvAccentEmerald,
    onSecondary = TvBackground,
    tertiary = TvAccentAmber,
    background = TvBackground,
    onBackground = TvTextPrimary,
    surface = TvSurfaceDark,
    onSurface = TvTextPrimary,
    surfaceVariant = TvSurfaceElevated,
    onSurfaceVariant = TvTextSecondary
)

@Composable
fun LuminaTvTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = TvColorScheme,
        typography = Typography,
        content = content
    )
}
