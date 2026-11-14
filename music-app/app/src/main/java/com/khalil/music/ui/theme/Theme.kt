package com.khalil.music.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Samsung One UI blue.
val Accent = Color(0xFF3E91FF)
val AccentOnDark = Color(0xFF6FB1FF)
val YouTubeRed = Color(0xFFFF0033)

private val LightColors = lightColorScheme(
    primary = Accent,
    onPrimary = Color.White,
    secondary = Accent,
    background = Color(0xFFFCFCFC),
    onBackground = Color(0xFF111111),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF111111),
    surfaceVariant = Color(0xFFEDEDED),
    onSurfaceVariant = Color(0xFF7A7A7A),
    surfaceContainerLow = Color(0xFFF3F3F3),
    surfaceContainer = Color(0xFFF3F3F3),
    surfaceContainerHigh = Color(0xFFFFFFFF),
    outlineVariant = Color(0xFFE2E2E2),
)

private val DarkColors = darkColorScheme(
    primary = AccentOnDark,
    onPrimary = Color.White,
    secondary = AccentOnDark,
    background = Color(0xFF000000),
    onBackground = Color(0xFFF2F2F2),
    surface = Color(0xFF000000),
    onSurface = Color(0xFFF2F2F2),
    surfaceVariant = Color(0xFF242424),
    onSurfaceVariant = Color(0xFF9A9A9A),
    surfaceContainerLow = Color(0xFF171717),
    surfaceContainer = Color(0xFF171717),
    surfaceContainerHigh = Color(0xFF1F1F1F),
    outlineVariant = Color(0xFF2A2A2A),
)

@Composable
fun MusicTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content,
    )
}
