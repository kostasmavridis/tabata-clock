package com.kostasmavridis.tabataclock.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary      = Color(0xFFFF4D6D),
    secondary    = Color(0xFF00D4FF),
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
 * Phase background gradient colours — kept in sync with the web dashboard
 * CSS variables in web/index.html:
 *   --work-a/#FF4D6D  --work-b/#FF8C69
 *   --rest-a/#00D4FF  --rest-b/#7B61FF
 *   --prep-a/#4169E1  --prep-b/#00CFFD
 *   --done-a/#9B59B6  --done-b/#6C3483
 *
 * *Top* is the vivid accent; *Dark* is the deep background end of the gradient.
 */
object PhaseColors {
    // Work — coral
    val Work        = Color(0xFFFF4D6D)
    val WorkDark    = Color(0xFF1A0010)

    // Rest — cyan / violet
    val Rest        = Color(0xFF00D4FF)
    val RestDark    = Color(0xFF06001A)

    // Prepare — royal blue
    val Prepare     = Color(0xFF4169E1)
    val PrepareDark = Color(0xFF04091A)

    // Done — purple
    val Done        = Color(0xFF9B59B6)
    val DoneDark    = Color(0xFF0D0018)
}
