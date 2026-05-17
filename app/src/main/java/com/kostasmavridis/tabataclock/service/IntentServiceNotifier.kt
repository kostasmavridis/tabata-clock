package com.kostasmavridis.tabataclock.service

import android.Manifest
import android.app.Application
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.kostasmavridis.tabataclock.model.TabataPhase
import javax.inject.Inject

/**
 * Production implementation: forwards phase updates to [TabataForegroundService]
 * via startForegroundService / stopService.
 *
 * On Android 13+ (TIRAMISU) we must hold POST_NOTIFICATIONS before starting a
 * foreground service that posts a notification. If the permission has not been
 * granted the timer still runs — the service just won't start, so there will be
 * no persistent notification while the screen is off. The UI layer (TimerScreen)
 * is responsible for prompting the user and showing a snackbar if denied.
 */
class IntentServiceNotifier @Inject constructor(
    private val application: Application
) : ServiceNotifier {

    override fun notify(phase: TabataPhase, secondsLeft: Int, round: Int) {
        if (!hasNotificationPermission()) {
            Log.w(TAG, "POST_NOTIFICATIONS not granted — skipping foreground service start")
            return
        }
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

    private fun hasNotificationPermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            application,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    private companion object {
        const val TAG = "IntentServiceNotifier"
    }
}
