package com.anselm.location

enum class Screen {
    PERMISSION,
    HOME,
    RECORDINGS,
}

sealed class NavigationItem(val route: String) {
    data object Permission: NavigationItem(Screen.PERMISSION.name)
    data object Home : NavigationItem(Screen.HOME.name)
    data object Recordings: NavigationItem(Screen.RECORDINGS.name)
}

