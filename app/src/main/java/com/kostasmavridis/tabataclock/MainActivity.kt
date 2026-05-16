package com.kostasmavridis.tabataclock

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.rememberNavController
import com.kostasmavridis.tabataclock.ui.navigation.NavGraph
import com.kostasmavridis.tabataclock.ui.theme.TabataClockTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TabataClockTheme {
                val navController = rememberNavController()
                NavGraph(navController = navController)
            }
        }
    }
}
