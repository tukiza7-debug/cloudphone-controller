package com.cloudcontroller.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val ConsoleBlack = Color(0xFF0A0A0A)
val ConsoleSurface = Color(0xFF1A1A1A)
val AccentGreen = Color(0xFF34D058)
val AccentRed = Color(0xFFE5484D)
val AccentAmber = Color(0xFFF0B429)
val ButtonFace = Color(0xFF2A2A2A)
val ButtonFacePressed = Color(0xFF3D3D3D)
val TextPrimary = Color(0xFFEDEDED)
val TextSecondary = Color(0xFF9A9A9A)

private val ConsoleColorScheme = darkColorScheme(
    primary = AccentGreen,
    secondary = AccentAmber,
    error = AccentRed,
    background = ConsoleBlack,
    surface = ConsoleSurface,
    onPrimary = ConsoleBlack,
    onBackground = TextPrimary,
    onSurface = TextPrimary
)

@Composable
fun CloudControllerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ConsoleColorScheme,
        content = content
    )
}
