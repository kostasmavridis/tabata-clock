package com.kostasmavridis.tabataclock.model

import com.kostasmavridis.tabataclock.ui.theme.PhaseColors
import androidx.compose.ui.graphics.Color

enum class TabataPhase(val label: String) {
    PREPARE("Get Ready"),
    WORK("WORK"),
    REST("REST"),
    DONE("Done!")
}

/**
 * Returns the (topColor, bottomColor) gradient pair for each phase.
 * Centralises the mapping so [TimerScreen] does not need two parallel
 * when-blocks that must be kept in sync.
 */
fun TabataPhase.gradientColors(): Pair<Color, Color> = when (this) {
    TabataPhase.PREPARE -> PhaseColors.Prepare     to PhaseColors.PrepareDark
    TabataPhase.WORK    -> PhaseColors.Work         to PhaseColors.WorkDark
    TabataPhase.REST    -> PhaseColors.Rest         to PhaseColors.RestDark
    TabataPhase.DONE    -> PhaseColors.Done         to PhaseColors.DoneDark
}
