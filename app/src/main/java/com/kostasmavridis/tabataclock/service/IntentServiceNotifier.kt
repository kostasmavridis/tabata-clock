package com.kostasmavridis.tabataclock.service

import android.app.Application
import android.content.Intent
import com.kostasmavridis.tabataclock.model.TabataPhase
import javax.inject.Inject

/**
 * Production implementation: forwards phase updates to [TabataForegroundService]
 * via startForegroundService / stopService.
 */
class IntentServiceNotifier @Inject constructor(
    private val application: Application
) : ServiceNotifier {

    override fun notify(phase: TabataPhase, secondsLeft: Int, round: Int) {
        val intent = Intent(application, TabataForegroundService::class.java).apply {
            putExtra(TabataForegroundService.EXTRA_PHASE,   phase.label)
            putExtra(TabataForegroundService.EXTRA_SECONDS, secondsLeft)
            putExtra(TabataForegroundService.EXTRA_ROUND,   round)
        }
        application.startForegroundService(intent)
    }

    override fun stop() {
        application.stopService(
            Intent(application, TabataForegroundService::class.java)
        )
    }
}
