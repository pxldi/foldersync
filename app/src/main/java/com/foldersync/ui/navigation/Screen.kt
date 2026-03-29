package com.foldersync.ui.navigation

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Editor : Screen("editor/{profileId}") {
        fun createRoute(profileId: Long? = null) =
            "editor/${profileId ?: "new"}"
    }
    data object History : Screen("history/{profileId}") {
        fun createRoute(profileId: Long) = "history/$profileId"
    }
    data object Settings : Screen("settings")

    data object Connections : Screen("connections")
}