package com.anselm.location
// To setup google maps, follow this *exactly* don't skip a beat
// https://developers.google.com/maps/documentation/android-sdk/config?hl=en#kotlin_3

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.anselm.location.LocationApplication.Companion.app
import com.anselm.location.components.AppBottomBar
import com.anselm.location.components.AppTopBar
import com.anselm.location.models.ApplicationViewModel
import com.anselm.location.models.LocalAppViewModel
import com.anselm.location.screens.PermissionScreen
import com.anselm.location.screens.RecordingDetailsScreen
import com.anselm.location.screens.RecordingScreen
import com.anselm.location.screens.SettingsScreen
import com.anselm.location.screens.ViewRecordingsScreen
import com.anselm.location.ui.theme.LocationTheme

val LocalNavController = compositionLocalOf<NavHostController> {
    error("No NavController found!")
}

class MainActivity : ComponentActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Sets up the UI.
        enableEdgeToEdge()
        setShowWhenLocked(true)
        setContent {
            LocationTheme {
                val navController = rememberNavController()
                val appViewModel : ApplicationViewModel
                        = viewModel(factory = ApplicationViewModel.Factory(::setShowWhenLocked))
                CompositionLocalProvider(LocalAppViewModel provides appViewModel) {
                    CompositionLocalProvider(LocalNavController provides navController) {
                        MainScreen()
                    }
                }
            }
        }
    }

    companion object {
        const val EXIT_ACTION = "Exit"
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        when(intent.action) {
            EXIT_ACTION -> { app.quit() }
        }
    }

    @Composable
    private fun MainScreen() {
        val appViewModel = LocalAppViewModel.current
        val navController = LocalNavController.current
        Scaffold(
            topBar = { AppTopBar(appViewModel) },
            bottomBar = { AppBottomBar(appViewModel) }
        ) { innerPadding ->
            NavHost(
                modifier = Modifier.padding(innerPadding),
                navController = navController,
                startDestination =
                    if ( app.checkPermissions() )
                        NavigationItem.ViewRecordings.route
                    else
                        NavigationItem.Permission.route,
            ) {
                composable(NavigationItem.Permission.route) {
                    PermissionScreen()
                }
                composable(NavigationItem.Recording.route) {
                    RecordingScreen()
                }
                composable(NavigationItem.ViewRecordings.route) {
                    ViewRecordingsScreen()
                }
                composable(
                    "${NavigationItem.RecordingDetails.route}/{recordingId}",
                    arguments = listOf(navArgument("recordingId") { type = NavType.StringType }),
                ) {recordingId ->
                    RecordingDetailsScreen(recordingId.arguments?.getString("recordingId"))
                }
                composable(NavigationItem.Settings.route) {
                    SettingsScreen()
                }
            }
        }
    }
}



