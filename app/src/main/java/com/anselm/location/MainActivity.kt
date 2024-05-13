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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import com.anselm.location.ui.theme.LocationTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlin.math.max
import kotlin.math.min
import kotlin.time.DurationUnit
import kotlin.time.toDuration

data class Sample(
    val location: Location,
    val minSpeed:  Float,
    val avgSpeed:  Float,
    val maxSpeed:  Float,
    val minAltitude: Double,
    val avgAltitude: Double,
    val maxAltitude: Double,
    val distance: Double,
    val climb: Double,
    val descent: Double,
)

class MainActivity : ComponentActivity() {
    private lateinit var locationPermissionLauncher: ActivityResultLauncher<Array<String>>

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
        disconnect()
        stopService(Intent(this, LocationTracker::class.java))
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

    private var sumSpeed = 0.0f
    private var sumAltitude = 0.0
    private var sampleCount = 0

    private fun firstSample(location: Location): Sample {
        this.startTime = location.time
        this.sumSpeed = 0f
        this.sumAltitude = 0.0
        this.sampleCount = 0
        this.lastSample = Sample(
            location = location,
            minSpeed = Float.MAX_VALUE,
            avgSpeed = 0.0f,
            maxSpeed = Float.MIN_VALUE,
            minAltitude = Double.MAX_VALUE,
            avgAltitude = 0.0,
            maxAltitude = Double.MIN_VALUE,
            distance = 0.0,
            climb = 0.0,
            descent = 0.0,
        )
        return this.lastSample!!
    }

    private fun reset() {
        this.lastSample = null
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
        return Sample(
            location = location,
            minSpeed = min(location.speed, lastSample.minSpeed),
            avgSpeed = sumSpeed / sampleCount,
            maxSpeed = max(location.speed, lastSample.maxSpeed),
            minAltitude = min(location.altitude, lastSample.minAltitude),
            avgAltitude = sumAltitude / sampleCount,
            maxAltitude = max(location.altitude, lastSample.maxAltitude),
            distance = lastSample.distance + location.distanceTo(lastSample.location),
            climb = if (verticalDistance > 0)
                lastSample.climb + verticalDistance
            else
                lastSample.climb,
            descent = if (verticalDistance < 0)
                lastSample.descent - verticalDistance
            else
                lastSample.descent,
        )
    }

    private fun onLocation(location: Location): Sample {
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
    fun WithHeader(title: String, content: @Composable () -> Unit) {
        Surface (
            shape = RoundedCornerShape(10.dp),
            //shadowElevation = 2.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(5.dp),
        ) {
            Column (
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.secondaryContainer)
                    .border(BorderStroke(1.dp, MaterialTheme.colorScheme.secondary))
                    .padding(8.dp),
            ){
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.displaySmall
                )
                Spacer(modifier = Modifier.padding(8.dp))
                content()
            }
            Spacer(modifier = Modifier.padding(8.dp))
        }
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
            WithHeader("Running Time") {
                Row (
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = "%02d:%02d:%02d".format(
                            *(location.time - startTime).toDuration(DurationUnit.MILLISECONDS)
                                .toComponents { hours, minutes, seconds, _ ->
                                    arrayOf(hours, minutes, seconds)
                                }
                        ),
                        style = MaterialTheme.typography.displayLarge,
                    )
                }
            }
            WithHeader("Distance") {
                Row (
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = "%.2f km".format(sample.distance / 1000.0),
                        style = MaterialTheme.typography.displayLarge,
                    )
                }
            }
            WithHeader("Speed") {
                Row (
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = "%.2f".format(location.speed * 3.6),
                        style = MaterialTheme.typography.displayLarge,
                    )
                }
                Row (
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("%.2f".format(sample.minSpeed * 3.6))
                    Text("%.2f".format(sample.avgSpeed * 3.6))
                    Text("%.2f".format(sample.maxSpeed * 3.6))
                }
            }
            WithHeader("Altitude") {
                Row (
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row {
                        Icon(Icons.Rounded.KeyboardArrowUp, contentDescription = "Climb")
                        Text(
                            text = "%.2f".format(sample.climb),
                            style = MaterialTheme.typography.displayLarge,
                        )
                    }
                    Row {
                        Text(
                            text = "%.2f".format(location.altitude),
                            style = MaterialTheme.typography.displaySmall,
                        )
                    }
                    Row {
                        Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = "Climb")
                        Text(
                            text = "%.2f".format(sample.descent),
                            style = MaterialTheme.typography.displayLarge,
                        )
                    }
                }
            }
            WithHeader("Other") {
                Column(
                    modifier = Modifier.fillMaxWidth(),

                ) {
                    Text(
                        "Coordinates: %.2f / %.2f".format(location.latitude, location.longitude)
                    )
                    Text("Accuracy: %.2f".format(location.accuracy))
                    Text("Bearing: %.2f".format(location.bearing))
                    Text("Sample Count: %d".format(sampleCount))
                }
            }
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
                .padding(vertical = 32.dp)
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



