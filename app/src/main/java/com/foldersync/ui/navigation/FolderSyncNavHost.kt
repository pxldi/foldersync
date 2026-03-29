package com.foldersync.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.foldersync.ui.connections.ConnectionsScreen
import com.foldersync.ui.editor.ProfileEditorScreen
import com.foldersync.ui.history.SyncHistoryScreen
import com.foldersync.ui.home.HomeScreen
import com.foldersync.ui.settings.SettingsScreen

@Composable
fun FolderSyncNavHost(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToEditor = { profileId ->
                    navController.navigate(Screen.Editor.createRoute(profileId))
                },
                onNavigateToHistory = { profileId ->
                    navController.navigate(Screen.History.createRoute(profileId))
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                },
            )
        }

        composable(Screen.Editor.route) {
            ProfileEditorScreen(
                onNavigateBack = { navController.popBackStack() },
            )
        }

        composable(Screen.History.route) {
            SyncHistoryScreen(
                onNavigateBack = { navController.popBackStack() },
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToConnections = { navController.navigate(Screen.Connections.route) },
            )
        }

        composable(Screen.Connections.route) {
            ConnectionsScreen(
                onNavigateBack = { navController.popBackStack() },
            )
        }
    }
}