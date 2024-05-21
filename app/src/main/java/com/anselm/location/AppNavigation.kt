package com.anselm.location

enum class Screen {
    PERMISSION,
    RECORDING,
    VIEW_RECORDINGS,
}

sealed class NavigationItem(val route: String) {
    data object Permission: NavigationItem(Screen.PERMISSION.name)
    data object Recording : NavigationItem(Screen.RECORDING.name)
    data object ViewRecordings: NavigationItem(Screen.VIEW_RECORDINGS.name)
}

