package com.anselm.location.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
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
import com.anselm.location.MIN_SAMPLES_FOR_PLOT
import com.anselm.location.R
import com.anselm.location.data.Recording
import com.anselm.location.data.Sample
import com.anselm.location.formatIf

private const val MIN_SPEED = 0.0001

@Composable
private fun Front(sample: Sample, recording: Recording? = null) {
    val isLive = (recording == null)
    val speedInKilometersPerHour =
        if ( isLive )
            sample.location.speed * 3.6
        else
            sample.avgSpeed * 3.6
    val averageSpeedInKilometersPerHour = sample.avgSpeed * 3.6
    val maxSpeedInKilometersPerHour = sample.maxSpeed * 3.6

    Column (
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceAround,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
        ) {
            NumberWithUnits(
                value = speedInKilometersPerHour.formatIf("--", "%.1f") { it <= MIN_SPEED },
                units = "km/h",
                style = MaterialTheme.typography.displayLarge,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            NumberWithUnits(
                value = averageSpeedInKilometersPerHour.formatIf(
                    "Average: --",
                    "Average: %.1f"
                ) { !it.isFinite() || it <= MIN_SPEED },
                units = "km/h",
                style = MaterialTheme.typography.titleLarge,
            )
            NumberWithUnits(
                value = maxSpeedInKilometersPerHour.formatIf(
                    "Maximum: --",
                    "Maximum: %.1f"
                ) { !it.isFinite() || it < MIN_SPEED },
                units = "km/h",
                style = MaterialTheme.typography.titleLarge,
            )
        }
    }
}

@Composable
private fun Back(optionalRecording: Recording?) {
    val recording = optionalRecording ?: app.recordingManager.liveRecording()
    if ( recording == null || recording.size < MIN_SAMPLES_FOR_PLOT) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.errorContainer),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Ride some more!",
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
fun SpeedCard(
    sample: Sample,
    modifier: Modifier = Modifier,
    recording: Recording? = null
) {
    FlipCard(
        key = "SpeedCard",
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 200.dp)
            .padding(horizontal = 0.dp, vertical = 4.dp),
        drawableId = R.drawable.ic_show_chart,
        front = {
            Front(sample, recording)
        },
        back = {
            Back(recording)
        }
    )
}
