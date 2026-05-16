package com.kostasmavridis.tabataclock.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFE53935),
    secondary = Color(0xFF43A047),
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White
)

@Composable
fun TabataClockTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}

// Phase-specific background colors
object PhaseColors {
    val Prepare = Color(0xFF1565C0)  // Deep Blue
    val Work    = Color(0xFFB71C1C)  // Deep Red
    val Rest    = Color(0xFF1B5E20)  // Deep Green
    val Done    = Color(0xFF212121)  // Near Black
}
