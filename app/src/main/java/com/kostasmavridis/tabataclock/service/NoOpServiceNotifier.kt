package com.kostasmavridis.tabataclock.service

import com.kostasmavridis.tabataclock.model.TabataPhase

/**
 * Test/default stub: does nothing.
 * Used as the default when no real [ServiceNotifier] is injected.
 */
class NoOpServiceNotifier : ServiceNotifier {
    override fun notify(phase: TabataPhase, secondsLeft: Int, round: Int) = Unit
    override fun stop() = Unit
}
