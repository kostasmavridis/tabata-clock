package com.kostasmavridis.tabataclock.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary   = Color(0xFFE53935),
    secondary = Color(0xFF43A047),
    background = Color(0xFF0D0D0D),
    surface    = Color(0xFF1A1A2E),
    onPrimary  = Color.White,
    onSecondary = Color.White,
    onBackground = Color.White,
    onSurface  = Color.White
)

@Composable
fun TabataClockTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}

// Phase background gradient colours (dark base → vivid top)
object PhaseColors {
    val Prepare     = Color(0xFF0D47A1)  // Bold Blue
    val PrepareDark = Color(0xFF050E2A)
    val Work        = Color(0xFFB71C1C)  // Deep Red
    val WorkDark    = Color(0xFF1A0000)
    val Rest        = Color(0xFF1B5E20)  // Deep Green
    val RestDark    = Color(0xFF021005)
    val Done        = Color(0xFF1A237E)  // Indigo
    val DoneDark    = Color(0xFF050510)
}
