package com.anselm.location
// REDACTED
import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.anselm.location.LocationApplication.Companion.app
import com.anselm.location.components.AltitudeCard
import com.anselm.location.components.DebugCard
import com.anselm.location.components.HomeScreen
import com.anselm.location.components.LoadingDisplay
import com.anselm.location.components.RecordingsScreen
import com.anselm.location.components.SpeedCard
import com.anselm.location.components.TimeElapsedCard
import com.anselm.location.data.DataManager
import com.anselm.location.data.LocationStub
import com.anselm.location.data.Sample
import com.anselm.location.data.defaultSample
import com.anselm.location.ui.theme.LocationTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn


class MainActivity : ComponentActivity() {
    private lateinit var locationPermissionLauncher: ActivityResultLauncher<Array<String>>
    private var trackerConnection: TrackerConnection? = null
    private val isGranted = mutableStateOf(false)

    private val liveContext: DataManager.Context?
        get() = trackerConnection?.binder?.liveContext

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Requests permissions if needed.
//        locationPermissionLauncher = registerForActivityResult(
//            ActivityResultContracts.RequestMultiplePermissions()) { it ->
//                isGranted.value = it.all { it.value }
//                Log.d(TAG, "isGranted ${isGranted.value}")
//                if ( isGranted.value ) {
//                    trackerConnection = connect()
//                }
//        }
//        isGranted.value = requestPermissions()
//        if ( isGranted.value ) {
//            trackerConnection = connect()
//        }
        // Sets up the UI.
        enableEdgeToEdge()
        setShowWhenLocked(true)
        setContent {
            LocationTheme {
                MainScreen()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        trackerConnection?.disconnect()
        trackerConnection = null
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

    private val allPermissions = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.FOREGROUND_SERVICE,
        Manifest.permission.FOREGROUND_SERVICE_LOCATION,
        Manifest.permission.POST_NOTIFICATIONS,
    )

    private fun checkPermissions(): Boolean {
        return allPermissions.all {
            ActivityCompat.checkSelfPermission(
                this,
                it
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestPermissions(): Boolean {
        if ( ! checkPermissions() ) {
            Log.d(TAG, "Launch permission prompt.")
            locationPermissionLauncher.launch(allPermissions)
            return false
        } else {
            return true
        }
    }

    var isTrackerBound = mutableStateOf(false)

    inner class TrackerConnection : ServiceConnection {
        var binder: LocationTracker.TrackerBinder? = null
        var flow: StateFlow<Sample>? = null

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            binder = (service as LocationTracker.TrackerBinder)
            flow = binder!!.flow
                .stateIn(
                    CoroutineScope(this@MainActivity.lifecycleScope.coroutineContext),
                    SharingStarted.Eagerly,
                    defaultSample.copy(location = LocationStub())
                )
            isTrackerBound.value = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            disconnect()
        }

        fun disconnect() {
            binder?.close()
            binder = null
            flow = null
            isTrackerBound.value = false
            this@MainActivity.unbindService(this)
        }
    }

    private fun connect(): TrackerConnection {
        val connection = TrackerConnection()
        val intent = Intent(this@MainActivity, LocationTracker::class.java)
        startService(intent)
        bindService(intent, connection, Context.BIND_AUTO_CREATE)
        return connection
    }

    private fun stopRecording() {
        liveContext?.stopRecording()
    }

    private fun startRecording() {
        liveContext?.startRecording()
    }

    @Composable
    fun LocationDisplay() {
        val sample = trackerConnection?.flow?.collectAsState()?.value
        if ( sample == null ) {
            LoadingDisplay()
            return
        }
        // We're on pause? Skip everything.
        Column (
            modifier = Modifier
                .padding(8.dp, 8.dp)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.Top,
        ) {
            TimeElapsedCard(sample)
            SpeedCard(sample)
            AltitudeCard(sample)
            DebugCard(liveContext?.isAutoPause?.value ?: false, sample)
        }
    }

    @Composable
    private fun PermissionPrompt() {
        Column (
            modifier = Modifier
                .padding(16.dp, 32.dp)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.Top,
        ) {
            Text(
                text = "Permissions are required.",
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text = "In order to use this application, you must grant it location permission" +
                        " while using the app.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Button(
                modifier = Modifier
                    .padding(top = 16.dp)
                    .align(Alignment.CenterHorizontally),
                onClick = {
                    val intent = Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    )
                    intent.data = Uri.fromParts("package", applicationContext.packageName, null)
                    startActivity(intent)
                },
            ) {
                Text("Grant Permissions")
            }
        }
    }



    @Composable
    private fun DisplayScreen() {
        val isRecording = liveContext?.isRecording?.value ?: false
        Column (
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .verticalScroll(rememberScrollState())
        ) {
            LocationDisplay()
            Row (
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
            ) {
                Button (
                    onClick = { app.quit() }
                ) {
                    Text("Quit")
                }
                IconButton(
                    onClick = {
                        if ( isRecording ) stopRecording() else startRecording()
                    }  ,
                    colors = IconButtonDefaults.iconButtonColors(
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                ) {
                    Icon(
                        painter = painterResource(
                            id = if ( isRecording )
                                R.drawable.ic_stop_recording
                            else
                                R.drawable.ic_start_recording
                        ),
                        contentDescription = "Toggle recording.",
                        tint = if ( liveContext?.isAutoPause?.value == true )
                            Color.Red
                        else
                            MaterialTheme.colorScheme.primary,
                    )
                }
                Button (
                    onClick = { liveContext?.reset() }
                ) {
                    Text("Reset")
                }
            }
        }
    }

    @Composable
    private fun TopBarActions() {
        val isRecording = liveContext?.isRecording?.value

        if ( ! isTrackerBound.value ) {
            // We don't display any action buttons.
            return
        }
        if ( isRecording == true ) {
            IconButton(
                onClick = { stopRecording() }
            ) {
                Icon(
                    painterResource(
                        id = R.drawable.ic_stop_recording
                    ),
                    contentDescription = "Recording / paused status.",
                    tint = if ( liveContext?.isAutoPause?.value == true )
                        Color.Red
                    else
                        MaterialTheme.colorScheme.primary,
                )
            }
        } else {
            IconButton(
                onClick = { startRecording() }
            ) {
                Icon(
                    painterResource(
                        id = R.drawable.ic_start_recording
                    ),
                    contentDescription = "Recording / paused status.",
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun TopBar(navController: NavController) {
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
                    text = getString(R.string.app_name),
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
                        Log.d(TAG, "Navigate to HOME")
                        navController.navigate(NavigationItem.Home.route)
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
                        Log.d(TAG, "Navigate to RECORDINGS")
                        navController.navigate(NavigationItem.Recordings.route)
                    }
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_list),
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
        val navController =  rememberNavController()

        Scaffold(
            topBar = { TopBar(navController) },
            bottomBar = { BottomBar(navController = navController) }
        ) { innerPadding ->
            NavHost(
                modifier = Modifier.padding(innerPadding),
                navController = navController,
                startDestination =
                    if ( checkPermissions() )
                        NavigationItem.Home.route
                    else
                        NavigationItem.Permission.route,
            ) {
                composable(NavigationItem.Permission.route) {
                    PermissionScreen(navController)
                }
                composable(NavigationItem.Home.route) {
                    HomeScreen(navController)
                }
                composable(NavigationItem.Recordings.route) {
                    RecordingsScreen(navController)
                }
            }
//            Column(modifier = Modifier.padding(innerPadding)) {
//                if (isGranted.value && isTrackerBound.value) {
//                    DisplayScreen()
//                } else if (isGranted.value) {
//                    LoadingDisplay()
//                } else {
//                    PermissionPrompt()
//                }
//            }
        }
    }
}



