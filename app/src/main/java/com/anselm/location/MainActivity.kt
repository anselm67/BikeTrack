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
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
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
import com.anselm.location.data.AutoPauseListener
import com.anselm.location.data.AverageFilter
import com.anselm.location.data.DataManager
import com.anselm.location.data.GradeFilter
import com.anselm.location.data.LocationStub
import com.anselm.location.data.RecordingManager
import com.anselm.location.data.Sample
import com.anselm.location.data.defaultSample
import com.anselm.location.ui.theme.LocationTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transform


class MainActivity : ComponentActivity() {
    private lateinit var locationPermissionLauncher: ActivityResultLauncher<Array<String>>
    private val recordingManager by lazy {
        RecordingManager.getInstance(applicationContext!!.filesDir)
    }
    private val dataManager by lazy {
        DataManager(recordingManager)
    }
    private val isFlowAvailable = mutableStateOf(false)
    private val isRecording = mutableStateOf(false)
    private val isAutoPause = mutableStateOf(false)
    private val isGranted = mutableStateOf(false)

    private val autoPauseListener = object : AutoPauseListener {
        override fun onAutoPause(onOff: Boolean) {
            isAutoPause.value = onOff
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Configures the data manager
        dataManager
            .addAutoPauseListener(autoPauseListener)
            // Averages the altitude values.
            .addFilter(object: AverageFilter(3) {
                override fun output(sample: Sample, value: Double) {
                    sample.location.altitude = value
                }
            }).addFilter(GradeFilter())

        // Requests permissions if needed.
        locationPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()) { it ->
                isGranted.value = it.all { it.value }
                Log.d(TAG, "isGranted ${isGranted.value}")
                if ( isGranted.value ) {
                    startService(Intent(this, LocationTracker::class.java))
                    connect()
                }
        }
        isGranted.value = requestPermissions()
        if ( isGranted.value ) {
            startService(Intent(this, LocationTracker::class.java))
            connect()
        }
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
        dataManager.removeAutoPauseListener(autoPauseListener)
        recordingManager.stop()
        disconnect()
        stopService(Intent(this, LocationTracker::class.java))
    }

    companion object {
        const val EXIT_ACTION = "Exit"
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        when(intent.action) {
            EXIT_ACTION -> {
                finishAndRemoveTask()
            }
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

    private var flow: StateFlow<Sample>? = null
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val locationFlow = (service as LocationTracker.TrackerBinder).getFlow()!!
            flow = locationFlow
                .transform { location ->
                    emit(dataManager.onLocation(location))
                }
                .stateIn(
                    CoroutineScope(Dispatchers.Main),
                    SharingStarted.Eagerly,
                    defaultSample.copy(location = LocationStub())
                )
            isFlowAvailable.value = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            isFlowAvailable.value = false
            flow = null
        }
    }

    private fun stopRecording() {
        recordingManager.stop()
        isRecording.value = false
    }

    private fun startRecording() {
        dataManager.reset()
        recordingManager.start()
        isRecording.value = true
    }

    private fun connect() {
        val intent = Intent(this@MainActivity, LocationTracker::class.java)
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    private fun disconnect() {
        unbindService(serviceConnection)
        flow = null
    }

    @Composable
    fun LocationDisplay() {
        val sample = flow?.collectAsState()?.value
        if ( sample == null ) {
            LoadingDisplay()
            return
        }
        val location = sample.location
        // We're on pause? Skip everything.
        Column (
            modifier = Modifier
                .padding(8.dp, 8.dp)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.Top,
        ) {
            TimeElapsedCard(
                timeMillis = sample.elapsedTime,
                distanceInKilometers = sample.totalDistance / 1000.0
            )
            SpeedCard(
                speedInKilometersPerHour = location.speed * 3.6,
                averageSpeedInKilometersPerHour = sample.avgSpeed * 3.6,
                maxSpeedInKilometersPerHour = sample.maxSpeed * 3.6,
            )
            AltitudeCard(
                gradePercent = sample.grade,
                climbInMeters = sample.climb,
                descentInMeters = sample.descent,
            )
            DebugCard(isAutoPause.value, sample)
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
    private fun LoadingDisplay() {
        Column (
            modifier = Modifier
                .padding(8.dp, 8.dp)
                .fillMaxWidth()
                .defaultMinSize(minHeight = 200.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CircularProgressIndicator(
                modifier = Modifier.width(96.dp),
                color = MaterialTheme.colorScheme.secondary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                strokeWidth = 7.dp
            )
            Text(
                text = "Loading...",
                style = MaterialTheme.typography.titleSmall,
            )
        }
    }
    @Composable
    private fun DisplayScreen() {
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
                    onClick = { finishAndRemoveTask() }
                ) {
                    Text("Quit")
                }
                IconButton(
                    onClick = {
                        if ( isRecording.value ) stopRecording() else startRecording()
                    }  ,
                    colors = IconButtonDefaults.iconButtonColors(
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                ) {
                    Icon(
                        painter = painterResource(
                            id = if ( isRecording.value )
                                R.drawable.ic_stop_recording
                            else
                                R.drawable.ic_start_recording
                        ),
                        contentDescription = "Toggle recording.",
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
                Button (
                    onClick = { dataManager.reset() }
                ) {
                    Text("Reset")
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun MainScreen() {
        Scaffold(
            topBar = {
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
                        if ( isRecording.value ) {
                            IconButton(
                                onClick = { stopRecording() }
                            ) {
                                Icon(
                                    painterResource(
                                        id = R.drawable.ic_stop_recording
                                    ),
                                    contentDescription = "Recording / paused status.",
                                    tint = if (isAutoPause.value)
                                        Color.Red
                                    else
                                        MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }
                )
            }
        ) { innerPadding ->
            Column(modifier = Modifier.padding(innerPadding)) {
                if (isGranted.value && isFlowAvailable.value) {
                    DisplayScreen()
                } else if (isGranted.value) {
                    LoadingDisplay()
                } else {
                    PermissionPrompt()
                }
            }
        }
    }
}



