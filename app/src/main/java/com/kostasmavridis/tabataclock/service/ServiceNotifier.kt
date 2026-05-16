package com.kostasmavridis.tabataclock.service

import com.kostasmavridis.tabataclock.model.TabataPhase

/**
 * Abstracts all foreground-service communication so that [TabataViewModel]
 * never touches [android.content.Intent] directly, keeping it unit-testable.
 */
interface ServiceNotifier {
    fun notify(phase: TabataPhase, secondsLeft: Int, round: Int)
    fun stop()
}
