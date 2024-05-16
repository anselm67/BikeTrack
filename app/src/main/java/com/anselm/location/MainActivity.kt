package com.anselm.location

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.location.Location
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import com.anselm.location.ui.theme.LocationTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlin.math.max

data class Sample(
    val location: Location,
    val avgSpeed:  Double,
    val maxSpeed:  Double,
    val totalDistance: Double,
    val distance: Double,
    val verticalDistance: Double,
    val climb: Double,
    val descent: Double,
)

class MainActivity : ComponentActivity() {
    private lateinit var locationPermissionLauncher: ActivityResultLauncher<Array<String>>
    private val recordingManager by lazy {
        RecordingManager.getInstance(applicationContext!!.filesDir)
    }
    private val isFlowAvailable = mutableStateOf(false)
    private val isGranted = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
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
        setContent {
            LocationTheme {
                MainScreen()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
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

    private var lastSample: Sample? = null
    private var startTime = 0L

    private var sumSpeed = 0.0
    private var sumAltitude = 0.0
    private var sampleCount = 0

    private fun firstSample(location: Location): Sample {
        this.startTime = location.time
        this.sumSpeed = 0.0
        this.sumAltitude = 0.0
        this.sampleCount = 0
        this.lastSample = Sample(
            location = location,
            avgSpeed = 0.0,
            maxSpeed = 0.0,
            totalDistance = 0.0,
            distance = 0.0,
            climb = 0.0,
            descent = 0.0,
            verticalDistance = 0.0,
        )
        return this.lastSample!!
    }

    private fun reset() {
        sampleCount = 0
        lastSample = null
    }

    private fun update(location: Location): Sample {
        // This really can't happen.
        if ( lastSample == null ) {
            throw Exception("No sample available")
        }
        val lastSample = lastSample!!
        this.sampleCount += 1
        this.sumSpeed += location.speed
        this.sumAltitude += location.altitude
        val verticalDistance = location.altitude - lastSample.location.altitude
        val distance = location.distanceTo(lastSample.location).toDouble()
        val nextSample = Sample(
            location = location,
            avgSpeed = sumSpeed / sampleCount,
            maxSpeed = max(location.speed.toDouble(), lastSample.maxSpeed),
            totalDistance = lastSample.totalDistance + distance,
            distance = distance,
            climb = if (verticalDistance > 0)
                lastSample.climb + verticalDistance
            else
                lastSample.climb,
            descent = if (verticalDistance < 0)
                lastSample.descent - verticalDistance
            else
                lastSample.descent,
            verticalDistance = verticalDistance,
        )
        this.lastSample = nextSample
        return nextSample
    }

    private fun onLocation(location: Location): Sample {
        recordingManager.record(location)
        return if (lastSample == null) firstSample(location) else update(location)
    }

    private var flow: StateFlow<Location?>? = null
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            flow = (service as LocationTracker.TrackerBinder).getFlow()!!
                .stateIn(CoroutineScope(Dispatchers.Main), SharingStarted.Eagerly, null)
            isFlowAvailable.value = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            isFlowAvailable.value = false
            flow = null
        }
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
        val location = flow?.collectAsState()?.value
        if ( location == null ) {
            LoadingDisplay()
            return
        }
        val sample = onLocation(location)
        Column (
            modifier = Modifier
                .padding(8.dp, 8.dp)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.Top,
        ) {
            TimeElapsedCard(
                timeMillis = location.time - startTime,
                distanceInKilometers = sample.totalDistance / 1000.0
            )
            SpeedCard(
                speedInKilometersPerHour = location.speed * 3.6,
                averageSpeedInKilometersPerHour = sample.avgSpeed * 3.6,
                maxSpeedInKilometersPerHour = sample.maxSpeed * 3.6,
            )
            AltitudeCard(
                gradePercent = 100.0 * sample.verticalDistance / sample.distance,
                climbInMeters = sample.climb,
                descentInMeters = sample.descent,
            )
            DebugCard(
                altitude = location.altitude,
                latitude = location.latitude,
                longitude = location.longitude,
                accuracy = location.accuracy.toDouble(),
                bearing = location.bearing.toDouble(),
                sampleCount = sampleCount
            )
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
                StartStopIcon(
                    onStart = {
                        reset()
                        this@MainActivity.recordingManager.start()
                    },
                    onStop = {  this@MainActivity.recordingManager.stop() },
                )
                Button (
                    onClick = { reset() }
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



