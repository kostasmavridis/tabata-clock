package com.kostasmavridis.tabataclock

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.navigation.compose.rememberNavController
import com.kostasmavridis.tabataclock.ui.navigation.NavGraph
import com.kostasmavridis.tabataclock.ui.theme.TabataClockTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    // Tracks whether to show the in-app rationale dialog before the system
    // permission prompt. Compose state so the dialog survives recomposition.
    private var showNotificationRationale by mutableStateOf(false)

    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            // Permission granted or denied — timer works either way,
            // but the foreground service notification won't show if denied.
            showNotificationRationale = false
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestPostNotificationsIfNeeded()
        setContent {
            TabataClockTheme {
                // Show rationale dialog before launching the system prompt.
                // This only appears when the user has previously denied the
                // permission once (shouldShowRequestPermissionRationale == true).
                if (showNotificationRationale) {
                    AlertDialog(
                        onDismissRequest = { showNotificationRationale = false },
                        title = { Text("Timer notifications") },
                        text  = {
                            Text(
                                "Tabata Clock needs notification permission to show " +
                                "the current phase and time remaining while you work out " +
                                "with your screen off."
                            )
                        },
                        confirmButton = {
                            TextButton(onClick = {
                                showNotificationRationale = false
                                requestNotificationPermission
                                    .launch(Manifest.permission.POST_NOTIFICATIONS)
                            }) { Text("Allow") }
                        },
                        dismissButton = {
                            TextButton(onClick = { showNotificationRationale = false }) {
                                Text("Not now")
                            }
                        }
                    )
                }

                val navController = rememberNavController()
                NavGraph(navController = navController)
            }
        }
    }

    private fun requestPostNotificationsIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        when {
            ContextCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED -> {
                // Already granted — nothing to do.
            }
            shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                // User denied once. Show our rationale dialog first, then
                // the system prompt is launched from the dialog's confirm button.
                showNotificationRationale = true
            }
            else -> {
                // First-time ask — launch system dialog directly.
                requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}
