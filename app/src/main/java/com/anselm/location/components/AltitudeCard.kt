package com.anselm.location.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.anselm.location.Graph
import com.anselm.location.GraphAppearance
import com.anselm.location.LocationApplication.Companion.app
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
                Text(
                    text = sample.altitude.formatIf("--", "%.1f") { !it.isFinite() },
                    style = MaterialTheme.typography.displayLarge,
                )
                Text(
                    text = sample.grade.formatIf("--", "%.1f%%") { !it.isFinite() },
                    style = MaterialTheme.typography.displayLarge,
                )
            } else {
                Text(
                    text = sample.avgAltitude.formatIf(
                        "--",
                        "%.1f"
                    ) { !it.isFinite() || it == 0.0 },
                    style = MaterialTheme.typography.displayLarge,
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row {
                Icon (
                    modifier = Modifier.size(MaterialTheme.typography.titleLarge.fontSize.value.dp)
                        .align(Alignment.Bottom)
                        .padding(2.dp),
                    painter = painterResource(id = R.drawable.ic_arrow_up),
                    contentDescription = "Climb in meters.",
                    tint = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = "%3.1f".format(sample.climb),
                    style = MaterialTheme.typography.titleLarge,
                )
            }
            Row {
                Icon (
                    modifier = Modifier.size(MaterialTheme.typography.titleLarge.fontSize.value.dp)
                        .align(Alignment.Bottom)
                        .padding(2.dp),
                    painter = painterResource(id = R.drawable.ic_arrow_down),
                    contentDescription = "Climb in meters.",
                    tint = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = "%3.1f".format(sample.descent),
                    style = MaterialTheme.typography.titleLarge,
                )
            }
        }
    }
}

@Composable
private fun Back(optionalRecording: Recording?) {
    val recording = optionalRecording ?: app.recordingManager.lastRecording()

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
                backgroundColor = MaterialTheme.colorScheme.background
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
