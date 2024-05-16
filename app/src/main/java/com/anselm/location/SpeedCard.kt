package com.anselm.location

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
private fun Front(speedInKilometersPerHour: Double,
    averageSpeedInKilometersPerHour: Double,
    maxSpeedInKilometersPerHour: Double
) {
    Row (
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "%.1f".format(speedInKilometersPerHour),
            style = MaterialTheme.typography.displayLarge,
        )
    }
    Row (
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text("Average: %.1f".format(averageSpeedInKilometersPerHour))
        Text("Maximum: %.1f".format(maxSpeedInKilometersPerHour))
    }
}

@Composable
private fun Back() {
    val recording = RecordingManager.get().lastRecording()

    if ( recording == null ) {
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "No recording available"
            )
        }
    } else {
        val speed = recording.extractSpeed()
        val time = recording.extractTime()

        Graph(
            modifier = Modifier.fillMaxSize(),
            xValues = time,
            yValues = speed,
            graphAppearance = GraphAppearance(
                graphColor = Color.Blue,
                graphAxisColor = MaterialTheme.colorScheme.primary,
                graphThickness = 3f,
                isColorAreaUnderChart = true,
                colorAreaUnderChart = Color.Green,
                isCircleVisible = false,
                circleColor = MaterialTheme.colorScheme.secondary,
                backgroundColor = MaterialTheme.colorScheme.background
            )
        )
    }
}
@Composable
fun SpeedCard(speedInKilometersPerHour: Double,
              averageSpeedInKilometersPerHour: Double,
              maxSpeedInKilometersPerHour: Double
) {
    FlipCard(
        title = "Speed",
        modifier = Modifier.padding(horizontal = 0.dp, vertical = 4.dp),
        front = {
            Front(
                speedInKilometersPerHour,
                averageSpeedInKilometersPerHour,
                maxSpeedInKilometersPerHour
            )
        },
        back = {
            Back()
        }
    )
}
