package com.anselm.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.Looper
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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import com.anselm.location.ui.theme.LocationTheme
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.callbackFlow
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
);

class MainActivity : ComponentActivity() {
    private lateinit var fusedLocationClient: FusedLocationProviderClient;
    private lateinit var locationPermissionLauncher: ActivityResultLauncher<String>;

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        locationPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
                if ( ! isGranted) {
                    toast("Permission is required for this application.");
                }
        }
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        setContent {
            LocationTheme {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    shadowElevation = 2.dp,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    LocationDisplay()
                }
            }
        }
    }

    private fun checkPermission(): Boolean {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
            return false;
        } else {
            return true;
        }
    }

    private var lastSample: Sample? = null;

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
        );
        return this.lastSample!!;
    }

    private var sumSpeed = 0.0f;
    private var sumAltitude = 0.0;
    private var sampleCount = 0;

    private fun update(location: Location): Sample {
        // This really can't happen.
        if ( lastSample == null ) {
            throw Exception("No sample available");
        }
        this.sampleCount += 1;
        this.sumSpeed += location.speed;
        this.sumAltitude += location.altitude;
        val verticalDistance = location.altitude - lastSample!!.location.altitude;
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
            descent = if ( verticalDistance < 0 ) lastSample!!.descent + verticalDistance else lastSample!!.descent,
        );
    }

    private fun onLocation(location: Location): Sample {
        return if (lastSample == null) firstSample(location) else update(location);
    }

    @SuppressLint("MissingPermission")
    private val locationFlow = callbackFlow {
        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                trySendBlocking(locationResult.lastLocation?.let { onLocation(it) });
            }
        }

        if ( checkPermission() ) {
            fusedLocationClient.requestLocationUpdates(
                LocationRequest.Builder(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    10000
                ).build(),
                locationCallback,
                Looper.getMainLooper()
            );

            awaitClose {
                fusedLocationClient.removeLocationUpdates(locationCallback);
            }
        } else {
            toast("Fine grain location permission is required to run this application.");
        }
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
            modifier = Modifier.fillMaxWidth().padding(5.dp),
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
                content();
            }
            Spacer(modifier = Modifier.padding(8.dp))
        }
    }

    @Composable
    fun LocationDisplay() {
        val sample = locationFlow.collectAsState(initial = null)

        if ( sample.value == null ) {
            Text(text = "Loading...");
        } else {
            val value = sample.value!!;
            val location = value.location;
            Column (
                modifier = Modifier.padding(8.dp, 8.dp).fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceAround,
            ) {
                WithHeader("Distance") {
                    Row (
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            text = "%.2f km".format(value.distance / 1000.0),
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
                        Text("%.2f".format(value.minSpeed * 3.6))
                        Text("%.2f".format(value.avgSpeed * 3.6))
                        Text("%.2f".format(value.maxSpeed * 3.6))
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
                                text = "%.2f".format(value.climb),
                                style = MaterialTheme.typography.displayLarge,
                            )
                        }
                        Row {
                            Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = "Climb")
                            Text(
                                text = "%.2f".format(value.descent),
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
                    }
                }


            }
        }
    }

}



