package com.anselm.location.components

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import com.anselm.location.LocationApplication.Companion.app

private const val TAG = "com.anselm.location.components.RecordingsScreen"

@Composable
fun RecordingsScreen(navController: NavHostController) {
    Log.d(TAG, "RecordingScreen")
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("All Rides")
        app.recordingManager.recordings.forEach {
            Text(
                text = it
            )
        }
    }
}