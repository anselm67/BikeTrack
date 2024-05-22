package com.anselm.location
// REDACTED
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.anselm.location.LocationApplication.Companion.app
import com.anselm.location.screens.PermissionScreen
import com.anselm.location.screens.RecordingDetailsScreen
import com.anselm.location.screens.RecordingScreen
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
                CompositionLocalProvider(LocalNavController provides navController) {
                    MainScreen()
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
    private fun TopBarActions() {
        if ( ! app.isTrackerBound.value ) {
            // We don't display any action buttons.
            return
        }

    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun TopBar() {
        if ( app.hideTopBar.value ) {
            return
        }
        CenterAlignedTopAppBar(
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                titleContentColor = MaterialTheme.colorScheme.primary,
            ),
            title = {
                Text(
                    text = app.appBarTitle.value,
                    maxLines = 1,
                )
            },
            actions = {
                TopBarActions()
            }
        )
    }
    @Composable
    private fun BottomBar(navController: NavController) {
        if ( app.hideBottomBar.value ) {
            return
        }
        BottomAppBar (
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.primary,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
            ) {
                IconButton(
                    onClick = {
                        navController.navigate(NavigationItem.ViewRecordings.route)
                    }
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_home),
                        contentDescription = "Navigate to the home screen.",
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
                IconButton(
                    onClick = {
                        navController.navigate(NavigationItem.Recording.route)
                    }
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_start_recording),
                        contentDescription = "Navigate to the home screen.",
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
                IconButton(
                    onClick = { }
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_settings),
                        contentDescription = "Navigate to the home screen.",
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
    @Composable
    private fun MainScreen() {
        val navController = LocalNavController.current
        Scaffold(
            topBar = { TopBar() },
            bottomBar = { BottomBar(navController = navController) }
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
            }
        }
    }
}



