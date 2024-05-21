package com.anselm.location.screens

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.anselm.location.LocationApplication.Companion.app
import com.anselm.location.data.Recording

private const val TAG = "com.anselm.location.components.RecordingsScreen"

@Composable
private fun DisplayRecording(title: String, recording: Recording) {
    Card(
        modifier = Modifier.padding(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 6.dp
        )
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.Top,
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = title
                )
            }
            Row (
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "%.2f".format(recording.lastSample().avgSpeed)
                )
                Text(
                    text = "%.2f".format(recording.lastSample().totalDistance)
                )
                Column (
                    horizontalAlignment = Alignment.Start,
                ) {
                    Text(
                        text = "%.1f".format(recording.lastSample().climb)
                    )
                    Text(
                        text = "%.1f".format(recording.lastSample().descent)
                    )
                }
            }
        }
    }
}


@Composable
fun ViewRecordingsScreen(navController: NavHostController) {
    Log.d(TAG, "RecordingScreen")
    Column(
        modifier = Modifier.fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "All Rides",
            style = MaterialTheme.typography.displaySmall,

        )
        app.recordingManager.recordings.forEach {
            DisplayRecording(it, app.recordingManager.load(it))
        }
    }
}