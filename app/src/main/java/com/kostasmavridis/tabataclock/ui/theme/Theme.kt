package com.kostasmavridis.tabataclock.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary      = Color(0xFFFF4D6D),
    secondary    = Color(0xFF00C896),
    background   = Color(0xFF06060F),
    surface      = Color(0xFF111827),
    onPrimary    = Color.White,
    onSecondary  = Color.White,
    onBackground = Color.White,
    onSurface    = Color.White
)

@Composable
fun TabataClockTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content     = content
    )
}

/**
 * Phase background gradient colours — kept in sync with the web dashboard.
 *
 * Rest is deep emerald (#00C896) rather than cyan (#00D4FF):
 *   - cyan read as "cold/icy" rather than "recovery"
 *   - emerald contrasts clearly with Work coral and Prepare blue
 *   - still vivid enough to glow well against the #06060F background
 */
object PhaseColors {
    // Work — coral
    val Work        = Color(0xFFFF4D6D)
    val WorkDark    = Color(0xFF1A0010)

    // Rest — deep emerald (was cyan #00D4FF)
    val Rest        = Color(0xFF00C896)
    val RestDark    = Color(0xFF001A0F)

    // Prepare — royal blue
    val Prepare     = Color(0xFF4169E1)
    val PrepareDark = Color(0xFF04091A)

    // Done — purple
    val Done        = Color(0xFF9B59B6)
    val DoneDark    = Color(0xFF0D0018)
}
