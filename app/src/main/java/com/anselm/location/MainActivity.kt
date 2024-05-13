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
import android.widget.Toast
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import com.anselm.location.ui.theme.LocationTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val isGranted = mutableStateOf(false)
        locationPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()) { it ->
                isGranted.value = it.all { it.value }
                Log.d(TAG, "isGranted ${isGranted.value}")
                if ( ! isGranted.value ) {
                    toast("Permission is required for this application.")
                } else {
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
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    shadowElevation = 2.dp,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    if ( isGranted.value && isFlowAvailable.value ) {
                        LocationDisplay()
                    } else if ( isGranted.value ) {
                        LoadingDisplay()
                    } else {
                        PermissionPrompt()
                    }
                }
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

    private fun firstSample(location: Location): Sample {
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

    private var sumSpeed = 0.0f
    private var sumAltitude = 0.0
    private var sampleCount = 0

    private fun update(location: Location): Sample {
        // This really can't happen.
        if ( lastSample == null ) {
            throw Exception("No sample available")
        }
        this.sampleCount += 1
        this.sumSpeed += location.speed
        this.sumAltitude += location.altitude
        val verticalDistance = location.altitude - lastSample!!.location.altitude
        return Sample(
            location = location,
            minSpeed = min(location.speed, lastSample!!.minSpeed),
            avgSpeed = sumSpeed / sampleCount,
            maxSpeed = max(location.speed, lastSample!!.maxSpeed),
            minAltitude = min(location.altitude, lastSample!!.minAltitude),
            avgAltitude = sumAltitude / sampleCount,
            maxAltitude = max(location.altitude, lastSample!!.maxAltitude),
            distance = lastSample!!.distance + location.distanceTo(lastSample!!.location),
            climb = if ( verticalDistance > 0 ) lastSample!!.climb + verticalDistance else lastSample!!.climb,
            descent = if ( verticalDistance < 0 ) lastSample!!.descent - verticalDistance else lastSample!!.descent,
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
            Log.d(TAG, "onServiceConnected ${flow}")
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            Log.d(TAG, "onServiceDisconnected")
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

    private val applicationScope = CoroutineScope(SupervisorJob())
    private fun toast(msg: String) {
        applicationScope.launch(Dispatchers.Main) {
            Toast.makeText(applicationContext, msg, Toast.LENGTH_LONG).show()
        }
    }

    @Composable
    fun WithHeader(title: String, content: @Composable () -> Unit) {
        Surface (
            shape = RoundedCornerShape(5.dp),
            shadowElevation = 2.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(5.dp),
        ) {
            Column (
                modifier = Modifier
                    .background(Color(0x99, 0x099, 0x99, 0xcc))
                    .border(BorderStroke(1.dp, Color(0xdd, 0xdd, 0xdd, 0xdd)))
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
        val location = flow?.collectAsState()?.value ?: return
        val sample = onLocation(location)

        Column (
            modifier = Modifier
                .padding(8.dp, 8.dp)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.Top,
        ) {
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
                .fillMaxHeight(),
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
}



