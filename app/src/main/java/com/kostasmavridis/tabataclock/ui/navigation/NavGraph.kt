package com.kostasmavridis.tabataclock.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.kostasmavridis.tabataclock.ui.screen.SettingsScreen
import com.kostasmavridis.tabataclock.ui.screen.TimerScreen

sealed class Screen(val route: String) {
    object Timer    : Screen("timer")
    object Settings : Screen("settings")
}

@Composable
fun NavGraph(navController: NavHostController) {
    NavHost(navController = navController, startDestination = Screen.Timer.route) {
        composable(Screen.Timer.route) {
            TimerScreen(onNavigateToSettings = { navController.navigate(Screen.Settings.route) })
        }
        composable(Screen.Settings.route) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
    }
}
