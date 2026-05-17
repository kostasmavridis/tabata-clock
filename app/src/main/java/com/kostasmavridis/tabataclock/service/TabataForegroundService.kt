package com.kostasmavridis.tabataclock.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.kostasmavridis.tabataclock.MainActivity
import com.kostasmavridis.tabataclock.R

/**
 * Foreground service that keeps the Tabata timer alive when the app is in the background.
 *
 * The ViewModel drives all timer logic; this service exists solely to post a
 * persistent notification so Android won't kill the process.
 *
 * foregroundServiceType = mediaPlayback:
 *   The service plays audio cues during workout phases. This type requires only
 *   FOREGROUND_SERVICE_MEDIA_PLAYBACK — no sensor permissions needed.
 *   The 'health' type was incorrect as it mandates ACTIVITY_RECOGNITION /
 *   BODY_SENSORS / HIGH_SAMPLING_RATE_SENSORS (Android 14+).
 *
 * Start it when the user presses Play; stop it on Reset or when the cycle completes.
 *
 * Lifecycle notes
 * ───────────────
 * Returns START_NOT_STICKY: the ViewModel owns all timer state via a coroutine
 * running inside viewModelScope. If Android kills the process, both the ViewModel
 * coroutine and this service are destroyed together. There is nothing to re-display,
 * so the service must NOT be auto-restarted with a null Intent (which is what
 * START_STICKY would do, resulting in a stale notification).
 */
class TabataForegroundService : Service() {

    companion object {
        private const val TAG         = "TabataFgService"
        const val CHANNEL_ID          = "tabata_timer_channel"
        const val NOTIFICATION_ID     = 1001
        const val EXTRA_PHASE         = "extra_phase"
        const val EXTRA_SECONDS       = "extra_seconds"
        const val EXTRA_ROUND         = "extra_round"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val phase   = intent?.getStringExtra(EXTRA_PHASE)   ?: "GET READY"
        val seconds = intent?.getIntExtra(EXTRA_SECONDS, 0) ?: 0
        val round   = intent?.getIntExtra(EXTRA_ROUND,   1) ?: 1

        try {
            startForeground(NOTIFICATION_ID, buildNotification(phase, seconds, round))
        } catch (e: Exception) {
            // Safety net: catches any residual SecurityException or
            // ForegroundServiceStartNotAllowedException that slips through.
            Log.e(TAG, "startForeground failed — stopping service: ${e.message}")
            stopSelf()
            return START_NOT_STICKY
        }

        return START_NOT_STICKY
    }

    private fun buildNotification(phase: String, secondsLeft: Int, round: Int): Notification {
        val openIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_timer_notification)
            .setContentTitle("Tabata — $phase")
            .setContentText("$secondsLeft s  •  Round $round")
            .setOngoing(true)
            .setSilent(true)
            .setContentIntent(openIntent)
            .build()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Tabata Timer",
            NotificationManager.IMPORTANCE_LOW
        ).apply { description = "Shows current Tabata phase while app is in the background" }
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
            .createNotificationChannel(channel)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        stopForeground(STOP_FOREGROUND_REMOVE)
    }
}
