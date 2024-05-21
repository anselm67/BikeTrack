package com.anselm.location.components

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.paint
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
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
fun TopBarActionButton(liveContext: DataManager.Context) {
    if (liveContext.isRecording.value ) {
        IconButton(
            onClick = { liveContext.stopRecording() }
        ) {
            Icon(
                painterResource(
                    id = R.drawable.ic_stop_recording
                ),
                contentDescription = "Recording / paused status.",
                tint = if (liveContext.isAutoPause.value)
                    Color.Red
                else
                    MaterialTheme.colorScheme.primary,
            )
        }
    } else {
        IconButton(
            onClick = { liveContext.startRecording() }
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
        DebugCard(trackerConnection.binder?.isAutoPause?.value ?: false, sample)
    }
}

@Composable
private fun DisplayScreen(trackerConnection: LocationApplication.TrackerConnection) {
    val liveContext = trackerConnection.binder ?: return
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
            IconButton(
                onClick = {
                    if ( liveContext.isRecording.value )
                        liveContext.stopRecording()
                    else
                        liveContext.startRecording()
                }  ,
                colors = IconButtonDefaults.iconButtonColors(
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
                modifier = Modifier.size(96.dp)
            ) {
                Icon(
                    painter = painterResource(
                        id = if ( liveContext.isRecording.value )
                            R.drawable.ic_stop_recording
                        else
                            R.drawable.ic_start_recording,
                    ),
                    contentDescription = "Toggle recording.",
                    tint = if ( liveContext.isAutoPause.value )
                        Color.Red
                    else
                        MaterialTheme.colorScheme.primary,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

private var trackerConnection: LocationApplication.TrackerConnection? = null

@Composable
fun RecordingScreen(
    navController: NavHostController,
) {
    val bottomBarVisible = app.hideBottomBar.value

    DisposableEffect(LocalContext.current) {
        Log.d(TAG, "RecordingScreen.connect()")
        trackerConnection = app.connect()

        onDispose {
            Log.d(TAG, "RecordingScreen.close")
            trackerConnection?.close()
            trackerConnection = null
            app.hideBottomBar.value = bottomBarVisible
        }
    }
    Column(
        modifier = Modifier.fillMaxSize(),
    ) {
        Log.d(TAG, "Recording.connected? ${app.isTrackerBound.value}")
        if ( app.isTrackerBound.value ) {
            if (trackerConnection?.binder?.isRecording?.value == true) {
                app.hideBottomBar.value = true
                DisplayScreen(trackerConnection!!)
            } else {
                app.hideBottomBar.value = false
                Column(
                    modifier = Modifier.fillMaxSize()
                        .paint(
                            painter = painterResource(id = R.drawable.cyclist_start),
                            contentScale = ContentScale.FillHeight,
                            alpha = 0.45f
                        ),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Button(
                        onClick = { trackerConnection?.binder?.startRecording() }
                    ) {
                        Text("Start Recording")
                    }
                }
            }
        } else {
            LoadingDisplay()
        }
    }
}