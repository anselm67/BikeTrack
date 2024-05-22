package com.anselm.location.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.anselm.location.LocalNavController
import com.anselm.location.LocationApplication.Companion.app
import com.anselm.location.NavigationItem
import com.anselm.location.components.AltitudeCard
import com.anselm.location.components.RecordingMetaData
import com.anselm.location.components.SpeedCard
import com.anselm.location.components.TimeElapsedCard

@Composable
fun RecordingDetailsScreen(recordingId: String?) {
    val navController = LocalNavController.current
    val recording = recordingId?.let { app.recordingManager.load(recordingId) }
    if (recording == null) {
        // This really shouldn't happen.
        navController.navigate(NavigationItem.ViewRecordings.route)
    } else {
        val lastSample = recording.lastSample()
        Column(
            modifier = Modifier.fillMaxSize()
                .padding(8.dp)
                .verticalScroll(rememberScrollState())
        ) {
            RecordingMetaData(recording)
            TimeElapsedCard(sample = lastSample)
            SpeedCard(recordingId = recordingId, sample = lastSample)
            AltitudeCard(recordingId = recordingId, sample = lastSample)
        }
    }
}