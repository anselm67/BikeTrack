package com.anselm.location.models

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

val LocalAppViewModel = compositionLocalOf<ApplicationViewModel> {
    error("No NavController found!")
}

interface AppAction {
    @Composable
    fun Action()
}

data class ApplicationState(
    val hideTopBar : Boolean = false,
    val hideBottomBar : Boolean = false,
    val title: String = "Bike Tracking",
    val showOnLockScreen: Boolean = false,
    val actions: List<AppAction> = emptyList()
)
class ApplicationViewModel(
    private val showOnLockScreen: (Boolean) -> Unit,
): ViewModel() {

    private val applicationStateFlow = MutableStateFlow(ApplicationState())

    val applicationState = applicationStateFlow.asStateFlow()

    fun setShowOnLockScreen(show: Boolean) {
        showOnLockScreen(show)
    }

    fun updateApplicationState(change: (state: ApplicationState) -> ApplicationState)
        : ApplicationViewModel
    {
        val oldState = applicationStateFlow.value
        val newState = change(oldState)
        applicationStateFlow.value = newState
        setShowOnLockScreen(newState.showOnLockScreen)
        return this
    }

    fun updateTitle(title: String, usePrevState: Boolean? = false)
        : ApplicationViewModel
    {
        updateApplicationState {
            if ( usePrevState == true )
                it
            else
                defaultApplicationState
            .copy(title = title)
        }
        return this
    }

    fun hideBottomBar(usePrevState: Boolean? = true)
        : ApplicationViewModel
    {
        updateApplicationState {
            val state = if ( usePrevState == true ) it else defaultApplicationState
            state.copy(hideBottomBar = true)
        }
        return this
    }

    fun showBottomBar(usePrevState: Boolean? = true)
        : ApplicationViewModel
    {
        updateApplicationState {
            val state = if ( usePrevState == true ) it else defaultApplicationState
            state.copy(hideBottomBar = false)
        }
        return this
    }

    companion object {
        val defaultApplicationState = ApplicationState (
            hideTopBar = false,
            hideBottomBar = false,
            title = "Bike Tracker"
        )
    }

    class Factory(private val showOnLockScreen: (Boolean) -> Unit) :
        ViewModelProvider.NewInstanceFactory() {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return requireNotNull(value = ApplicationViewModel(showOnLockScreen) as? T) {
                "Cannot create an instance of $modelClass"
            }
        }
    }
}