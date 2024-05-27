package com.anselm.location.screens

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
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
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.paint
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.anselm.location.LocalNavController
import com.anselm.location.LocationApplication
import com.anselm.location.LocationApplication.Companion.app
import com.anselm.location.LocationTracker
import com.anselm.location.NavigationItem
import com.anselm.location.R
import com.anselm.location.components.AltitudeCard
import com.anselm.location.components.DebugCard
import com.anselm.location.components.LoadingDisplay
import com.anselm.location.components.SpeedCard
import com.anselm.location.components.TimeElapsedCard
import com.anselm.location.components.YesNoDialog
import com.anselm.location.models.LocalAppViewModel

private const val TAG = "com.anselm.location.components.HomeScreen"

private fun stopRecording(
    liveContext: LocationTracker.TrackerBinder,
    navController: NavHostController,
) {
    val entry = liveContext.stopRecording()
    app.onRecordingChanged(false)
    if ( entry == null ) {
        app.toast("Ride discarded because it is too short.")
        navController.navigate(NavigationItem.ViewRecordings.route)
    } else {
        navController.navigate(
            NavigationItem.RecordingDetails.route + "/${entry.id}"
        ) {
            popUpTo(NavigationItem.ViewRecordings.route) {
                inclusive = true
            }
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
        TimeElapsedCard(
            sample = sample,
            isAutoPaused = trackerConnection.binder?.isAutoPause,
        )
        SpeedCard(
            sample = sample,
            modifier = Modifier.defaultMinSize(minHeight = 250.dp)
        )
        AltitudeCard(
            sample = sample,
            modifier = Modifier.defaultMinSize(minHeight = 250.dp)
        )
        DebugCard(
            sample = sample,
            isAutoPaused = trackerConnection.binder?.isAutoPause,
        )
    }
}

@Composable
private fun DisplayScreen(
    trackerConnection: LocationApplication.TrackerConnection,
    showStopRecordingDialog: MutableState<Boolean>,
) {
    val liveContext = trackerConnection.binder ?: return
    Column (
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .verticalScroll(rememberScrollState())
    ) {
        if ( liveContext.isAutoPause.value ) {
            Column (
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primary),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Auto-Paused",
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.padding(4.dp)
                )
            }
        }
        LocationDisplay(trackerConnection)
        Row (
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
        ) {
            IconButton(
                onClick = {
                    if ( liveContext.isRecording.value ) {
                        showStopRecordingDialog.value = true
                    }
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
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

private var trackerConnection: LocationApplication.TrackerConnection? = null

@Composable
fun RecordingScreen() {
    val showStopRecordingDialog = remember { mutableStateOf(false) }
    val navController = LocalNavController.current
    val appViewModel = LocalAppViewModel.current
    appViewModel
        .updateTitle(title = "Enjoy your ride!")
    DisposableEffect(LocalContext.current) {
        Log.d(TAG, "RecordingScreen.connect()")
        trackerConnection = app.connect()

        onDispose {
            Log.d(TAG, "RecordingScreen.close")
            trackerConnection?.close()
            trackerConnection = null
        }
    }
    Column(
        modifier = Modifier.fillMaxSize(),
    ) {
        Log.d(TAG, "Recording.connected? ${app.isTrackerBound.value}")
        if ( app.isTrackerBound.value ) {
            if (trackerConnection?.binder?.isRecording?.value == true) {
                appViewModel.updateApplicationState {
                    it.copy(
                        hideBottomBar = true,
                        hideTopBar = true,
                        showOnLockScreen = true
                    )
                }
                DisplayScreen(
                    trackerConnection!!,
                    showStopRecordingDialog,
                )
            } else {
                appViewModel.updateApplicationState {
                    it.copy(
                        hideBottomBar = false,
                        hideTopBar = false,
                        showOnLockScreen = false
                    )
                }
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .paint(
                            painter = painterResource(id = R.drawable.cyclist_start),
                            contentScale = ContentScale.FillHeight,
                            alpha = 0.45f
                        ),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Button(
                        onClick = {
                            trackerConnection?.binder?.startRecording()
                            app.onRecordingChanged(true)
                        },
                    ) {
                        Text(
                            text = "Start Recording",
                            style = MaterialTheme.typography.headlineLarge,
                            modifier = Modifier.padding(16.dp, 8.dp)
                        )
                    }
                }
            }
            if ( showStopRecordingDialog.value ) {
                YesNoDialog(
                    onDismiss = {
                        showStopRecordingDialog.value = false
                    },
                    onConfirm = {
                        showStopRecordingDialog.value = false
                        stopRecording(trackerConnection!!.binder!!, navController)
                    },
                    title = "Save this ride ?",
                    text = "If you have finished your ride, press Yes to save your ride. " +
                            "Otherwise, cancel will resume tracking."
                )
            }
        } else {
            LoadingDisplay()
        }
    }
}