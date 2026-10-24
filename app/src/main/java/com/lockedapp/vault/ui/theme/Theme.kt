package com.lockedapp.vault.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

private val VaultColorScheme = darkColorScheme(
    primary = AuroraViolet,
    onPrimary = Color.White,
    secondary = AuroraPink,
    background = VaultVoid,
    onBackground = TextPrimary,
    surface = VaultSurface,
    onSurface = TextPrimary,
    surfaceVariant = VaultSurfaceElevated,
    onSurfaceVariant = TextSecondary,
    outline = VaultOutline,
    error = DangerRed
)

/** The signature "aurora" mesh-style gradient used behind the PIN screens. */
fun auroraBrush(): Brush = Brush.linearGradient(
    colors = listOf(VaultVoid, Color(0xFF231A3D), Color(0xFF1A1030), VaultVoid)
)

@Composable
fun LockedVaultTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = VaultColorScheme, content = content)
}
