package com.lockedapp.vault.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val VaultDarkColors = darkColorScheme(
    primary = Color(0xFFD0BCFF),
    onPrimary = Color(0xFF381E72),
    background = Color(0xFF1C1B1F),
    surface = Color(0xFF211F26),
    onBackground = Color(0xFFE6E1E5),
    onSurface = Color(0xFFE6E1E5)
)

private val VaultLightColors = lightColorScheme(
    primary = Color(0xFF6750A4),
    onPrimary = Color(0xFFFFFFFF),
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE)
)

@Composable
fun LockedVaultTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) VaultDarkColors else VaultLightColors
    MaterialTheme(colorScheme = colors, content = content)
}
