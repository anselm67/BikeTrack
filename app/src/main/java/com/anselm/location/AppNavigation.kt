package com.anselm.location

enum class Screen {
    PERMISSION,
    RECORDING,
    VIEW_RECORDINGS,
    RECORDING_DETAILS,
}

sealed class NavigationItem(val route: String) {
    data object Permission: NavigationItem(Screen.PERMISSION.name)
    data object Recording : NavigationItem(Screen.RECORDING.name)
    data object ViewRecordings: NavigationItem(Screen.VIEW_RECORDINGS.name)
    data object RecordingDetails: NavigationItem(Screen.RECORDING_DETAILS.name)
}

