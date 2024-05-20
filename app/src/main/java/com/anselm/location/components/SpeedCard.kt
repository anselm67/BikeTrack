package com.anselm.location.components

import androidx.compose.foundation.background
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
import com.anselm.location.Graph
import com.anselm.location.GraphAppearance
import com.anselm.location.LocationApplication.Companion.app
import com.anselm.location.data.Sample
import com.anselm.location.formatIf

private const val MIN_SPEED = 0.0001

@Composable
private fun Front(sample: Sample) {
    val speedInKilometersPerHour = sample.location.speed * 3.6
    val averageSpeedInKilometersPerHour = sample.avgSpeed * 3.6
    val maxSpeedInKilometersPerHour = sample.maxSpeed * 3.6

    Row (
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
    ) {
        Text(
            text = speedInKilometersPerHour.formatIf("--", "%.1f") { it <= MIN_SPEED },
            style = MaterialTheme.typography.displayLarge,
        )
    }
    Row (
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = averageSpeedInKilometersPerHour.formatIf(
                "Average: --",
                "Average: %.1f"
            ) { ! it.isFinite() || it <= MIN_SPEED },
            style = MaterialTheme.typography.titleLarge,
        )
        Text(
            text = maxSpeedInKilometersPerHour.formatIf(
                "Maximum: --",
                "Maximum: %.1f"
            ) { ! it.isFinite() || it < MIN_SPEED },
            style = MaterialTheme.typography.titleLarge,
        )
    }
}

@Composable
private fun Back() {
    val recording = app.recordingManager.lastRecording()

    if ( recording == null ) {
        Column(
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.errorContainer),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "No recording available",
                style = MaterialTheme.typography.titleLarge,
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
fun SpeedCard(sample: Sample) {
    FlipCard(
        title = "Speed",
        modifier = Modifier.padding(horizontal = 0.dp, vertical = 4.dp),
        front = {
            Front(sample)
        },
        back = {
            Back()
        }
    )
}
