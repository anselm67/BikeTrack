package com.anselm.location.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.paint
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.anselm.location.LocalNavController
import com.anselm.location.LocationApplication.Companion.app
import com.anselm.location.NavigationItem
import com.anselm.location.R
import com.anselm.location.components.AltitudeCard
import com.anselm.location.components.DebugCard
import com.anselm.location.components.LoadingDisplay
import com.anselm.location.components.SpeedCard
import com.anselm.location.components.TimeElapsedCard
import com.anselm.location.components.YesNoDialog
import com.anselm.location.data.Entry
import com.anselm.location.data.RecordingTagger
import com.anselm.location.data.Sample
import com.anselm.location.models.LocalAppViewModel
import com.anselm.location.models.RecordingViewModel
import kotlinx.coroutines.flow.StateFlow

private fun finishRecording(
    entry: Entry?,
    isSaving: MutableState<Boolean>,
    navController: NavHostController,
) {
    if ( entry == null ) {
        app.toast("Ride discarded because it is too short.")
        navController.navigate(NavigationItem.ViewRecordings.route)
    } else {
        // Tag this ride.
        isSaving.value = true
        app.recordingManager.load(entry.id)?.let {
            RecordingTagger(it).tag {
                it.save()
                // Navigate to the details screen.
                app.postOnUiThread {
                    navController.navigate(
                        NavigationItem.RecordingDetails.route + "/${entry.id}"
                    ) {
                        popUpTo(NavigationItem.ViewRecordings.route) {
                            inclusive = true
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LocationDisplay(sampleFlow: StateFlow<Sample>) {
    val sharedPreferences = LocalContext.current.getSharedPreferences(
        "LocationPreferences",
        Context.MODE_PRIVATE)
    val showDebugCard by remember {
        mutableStateOf(sharedPreferences.getBoolean("showDebugCard", false))
    }

    val sample = sampleFlow.collectAsState()

    // We're on pause? Skip everything.
    Column (
        modifier = Modifier
            .padding(8.dp, 8.dp)
            .fillMaxHeight(),
        verticalArrangement = Arrangement.Top,
    ) {
        TimeElapsedCard(
            sample = sample.value,
        )
        SpeedCard(
            sample = sample.value,
            modifier = Modifier.defaultMinSize(minHeight = 250.dp)
        )
        AltitudeCard(
            sample = sample.value,
            modifier = Modifier.defaultMinSize(minHeight = 250.dp)
        )
        if ( showDebugCard ) {
            DebugCard(sample.value)
        }
    }
}

@Composable
private fun DisplayScreen(
    sampleFlow: StateFlow<Sample>,
    showStopRecordingDialog: MutableState<Boolean>,
) {
    Column (
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .verticalScroll(rememberScrollState())
    ) {
        LocationDisplay(sampleFlow)
        Column (
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            IconButton(
                onClick = {
                    if (app.isRecording.value) {
                        showStopRecordingDialog.value = true
                    }
                },
                colors = IconButtonDefaults.iconButtonColors(
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
                modifier = Modifier.size(96.dp)
            ) {
                Icon(
                    painter = painterResource(
                        id =
                        if (app.isRecording.value)
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

@Composable
fun SavingRide() {
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
        Text(
            modifier = Modifier
                .background(
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(25.dp),
                )
                .padding(24.dp),
            text = "Saving ride...",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onPrimary,
        )
    }
}


@Composable
fun RecordingScreen() {
    val showStopRecordingDialog = remember { mutableStateOf(false) }

    val navController = LocalNavController.current
    val appViewModel = LocalAppViewModel.current
    appViewModel
        .updateTitle(title = "Enjoy your ride!")

    val viewModel = viewModel<RecordingViewModel>()
    viewModel.connect()

    // Setting isSaving to false is the very last thing we want to do on this screen.
    DisposableEffect(key1 = viewModel) {
        onDispose {
            viewModel.isSaving.value = false
        }
    }

    val isConnected by viewModel.isConnected
    val isRecording by app.isRecording

    Column(
        modifier = Modifier.fillMaxSize(),
    ) {
        if ( viewModel.isSaving.value ) {
            SavingRide()
        } else if ( isConnected ) {
            if ( isRecording ) {
                appViewModel.updateApplicationState {
                    it.copy(
                        hideBottomBar = true,
                        hideTopBar = true,
                        showOnLockScreen = true
                    )
                }
                DisplayScreen(
                    viewModel.sampleFlow!!,
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
                            viewModel.startRecording()
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
                        finishRecording(
                            viewModel.stopRecording(),
                            viewModel.isSaving,
                            navController
                        )
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