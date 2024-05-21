package com.anselm.location.components

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.anselm.location.LocationApplication
import com.anselm.location.LocationApplication.Companion.app
import com.anselm.location.R
import com.anselm.location.data.DataManager

private const val TAG = "com.anselm.location.components.HomeScreen"

@Composable
fun LocationDisplay(trackerConnection: LocationApplication.TrackerConnection) {
    val sample = trackerConnection.flow?.collectAsState()?.value
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
        DebugCard(trackerConnection.binder?.liveContext?.isAutoPause?.value ?: false, sample)
    }
}

private fun stopRecording(context: DataManager.Context) {
    context.stopRecording()
}

private fun startRecording(context: DataManager.Context) {
    context.startRecording()
}

@Composable
private fun DisplayScreen(trackerConnection: LocationApplication.TrackerConnection) {
    val liveContext = trackerConnection.binder?.liveContext!!
    val isRecording = liveContext.isRecording.value
    Column (
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .verticalScroll(rememberScrollState())
    ) {
        LocationDisplay(trackerConnection)
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
                    if ( isRecording ) stopRecording(liveContext) else startRecording(liveContext)
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
                    tint = if ( liveContext.isAutoPause.value )
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

private var trackerConnection: LocationApplication.TrackerConnection? = null

@Composable
fun HomeScreen(
    navController: NavHostController,
) {
    DisposableEffect(LocalContext.current) {
        Log.d(TAG, "HomeScreen.connect()")
        trackerConnection = app.connect()

        onDispose {
            Log.d(TAG, "HomeScreen.close")
            trackerConnection?.close()
            trackerConnection = null
        }
    }
    Column(
        modifier = Modifier.fillMaxSize(),
    ) {
        Log.d(TAG, "HomeScreen.connected? ${app.isTrackerBound.value}")
        if ( app.isTrackerBound.value ) {
            DisplayScreen(trackerConnection!!)
        } else {
            LoadingDisplay()
        }
    }
}