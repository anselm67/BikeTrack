package com.anselm.location.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.anselm.location.LocalNavController
import com.anselm.location.NavigationItem
import com.anselm.location.components.AltitudeCard
import com.anselm.location.components.RecordingMetaData
import com.anselm.location.components.SpeedCard
import com.anselm.location.components.TimeElapsedCard
import com.anselm.location.models.RecordingDetailsViewModel

@Composable
fun RecordingDetailsScreen(recordingId: String?) {
    val navController = LocalNavController.current

    if (recordingId == null) {
        // This really shouldn't happen.
        navController.navigate(NavigationItem.ViewRecordings.route)
        return
    }
    val viewModel :  RecordingDetailsViewModel
        = viewModel(factory = RecordingDetailsViewModel.Factory(recordingId))
    val recording = viewModel.recordingState.collectAsState().value
    val lastSample = recording.lastSample()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp)
            .verticalScroll(rememberScrollState())
    ) {
        RecordingMetaData(viewModel)
        TimeElapsedCard(sample = lastSample)
        SpeedCard(recording = recording, sample = lastSample)
        AltitudeCard(recording = recording, sample = lastSample)
    }
}