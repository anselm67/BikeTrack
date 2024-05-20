package com.anselm.location

enum class Screen {
    HOME,
    RECORDINGS,
}

sealed class NavigationItem(val route: String) {
    data object Home : NavigationItem(Screen.HOME.name)
    data object Recordings: NavigationItem(Screen.RECORDINGS.name)
}

