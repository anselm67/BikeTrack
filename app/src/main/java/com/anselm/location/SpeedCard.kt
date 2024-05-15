package com.anselm.location

import android.health.connect.datatypes.SpeedRecord
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
private fun Front(speedInKilometersPerHour: Double,
    averageSpeedInKilometersPerHour: Double,
    maxSpeedInKilometersPerHour: Double
) {
    BasicCard("Speed") {
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
}

@Composable
private fun Back() {
    val recording = RecordingManager.get().load("recording-2024-05-14-10-14-37.json")
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
@Composable
fun SpeedCard(speedInKilometersPerHour: Double,
              averageSpeedInKilometersPerHour: Double,
              maxSpeedInKilometersPerHour: Double
) {
    var cardFace by remember { mutableStateOf(CardFace.Front) }
    FlipCard(
        cardFace = cardFace ,
        onClick = { cardFace = cardFace.next },
        //modifier = Modifier.fillMaxWidth(),
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
