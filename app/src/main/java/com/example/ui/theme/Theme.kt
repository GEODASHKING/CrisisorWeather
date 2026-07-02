package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme =
  darkColorScheme(
    primary = EmergencyAmber,
    secondary = WarningYellow,
    tertiary = DangerRed,
    background = ObsidianBlack,
    surface = DarkSteel,
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onBackground = TextLight,
    onSurface = TextLight,
    surfaceVariant = CardSteel,
    onSurfaceVariant = TextMuted
  )

@Composable
fun CrisisorWeatherTheme(
  content: @Composable () -> Unit,
) {
  MaterialTheme(colorScheme = DarkColorScheme, typography = Typography, content = content)
}
