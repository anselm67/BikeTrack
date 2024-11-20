package com.anselm.location.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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

@Composable
private fun Front(sample: Sample, recording: Recording?) {
    val isLive = (recording == null)
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceAround,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
        ) {
            if (isLive) {
                NumberWithUnits(
                    value = sample.altitude.formatIf("--", "%.1f") { !it.isFinite() },
                    units = "m",
                    style = MaterialTheme.typography.displayLarge,
                )
                NumberWithUnits(
                    value = sample.grade.formatIf("--", "%.1f") { !it.isFinite() },
                    units = "%",
                    style = MaterialTheme.typography.displayLarge,
                )
            } else {
                NumberWithUnits(
                    value = sample.avgAltitude.formatIf(
                        "--",
                        "%.1f"
                    ) { !it.isFinite() || it == 0.0 },
                    units = "m",
                    style = MaterialTheme.typography.displayLarge,
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            NumberWithUnits(
                value = "%3.1f".format(sample.climb),
                units = "Climb",
                style = MaterialTheme.typography.displaySmall,
            )
            NumberWithUnits(
                value = "%3.1f".format(sample.descent),
                units = "Descent",
                style = MaterialTheme.typography.displaySmall,
            )
        }
    }
}

@Composable
private fun Back(optionalRecording: Recording?) {
    val recording = optionalRecording ?: app.recordingManager.liveRecording()

    if ( recording == null || recording.size < MIN_SAMPLES_FOR_PLOT ) {
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
        }    } else {
        val altitude = recording.extractAltitude()
        var distance = recording.extractDistances()
        if ( distance.max() <= 2.5 ) {
            // Switch to meters unit for the graph.
            distance = distance.map { it * 1000 }
        }
        Graph(
            modifier = Modifier
                .fillMaxWidth()
                .height(500.dp),
            xValues = distance,
            yValues = altitude,
            graphAppearance = GraphAppearance(
                graphColor = Color.Blue,
                graphAxisColor = MaterialTheme.colorScheme.primary,
                graphThickness = 3f,
                isColorAreaUnderChart = true,
                colorAreaUnderChart = Color.Green,
                isCircleVisible = false,
                circleColor = MaterialTheme.colorScheme.secondary,
                backgroundColor = MaterialTheme.colorScheme.background,
                xLabelFormatter = { "%3.1fkm".format(it)  }
            )
        )
    }
}

@Composable
fun AltitudeCard(
    sample: Sample,
    modifier: Modifier = Modifier,
    recording: Recording? = null
) {
    FlipCard(
        key = "AltitudeCard",
        modifier = modifier
            .padding(0.dp, 4.dp)
            .fillMaxWidth()
            .defaultMinSize(minHeight = 200.dp),
        drawableId = R.drawable.ic_show_chart,
        front = {
            Front(sample, recording)
        },
        back = { Back(recording) }
    )
}
