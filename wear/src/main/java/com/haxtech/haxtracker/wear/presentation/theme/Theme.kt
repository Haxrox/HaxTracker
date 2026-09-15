package com.haxtech.haxtracker.wear.presentation.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.wear.compose.material3.ColorScheme
import androidx.wear.compose.material3.MaterialTheme

val PitchBlack = Color(0xFF090A0F)
val DarkSurface = Color(0xFF12141C)
val DarkSurfaceElevated = Color(0xFF1E2230)
val DarkBorder = Color(0xFF2A2F45)

val VoltGreen = Color(0xFF00FF87)
val VoltCyan = Color(0xFF00E5FF)
val VoltGold = Color(0xFFFFD700)
val VoltCoral = Color(0xFFFF4D4D)

val TextPrimary = Color(0xFFF0F2F8)
val TextSecondary = Color(0xFF8C93A8)
val TextDisabled = Color(0xFF4A5065)

@Composable
fun HaxTrackerTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = ColorScheme(
            primary = VoltGreen,
            onPrimary = PitchBlack,
            secondary = VoltCyan,
            onSecondary = PitchBlack,
            background = PitchBlack,
            onBackground = TextPrimary,
            surfaceContainer = DarkSurface,
            onSurface = TextPrimary,
            onSurfaceVariant = TextSecondary,
            error = VoltCoral,
            onError = PitchBlack,
        ),
        content = content,
    )
}
