package com.anselm.location

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import com.anselm.location.LocationApplication.Companion.app
import com.anselm.location.data.Sample

@Composable
private fun Front(sample: Sample) {
    Row (
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceAround,
    ) {
        Text(
            text = "%.1f".format(sample.altitude),
            style = MaterialTheme.typography.displayLarge,
        )
        Text(
            text = sample.grade.formatIf("--", "%.1f%%") { ! it.isFinite() },
            style = MaterialTheme.typography.displayLarge,
        )
    }
    Row (
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ){
        Text(
            text = "Up: %3.1f".format(sample.climb),
            style = MaterialTheme.typography.titleLarge,
        )
        Text(
            text = "Down: %3.1f".format(sample.descent),
            style = MaterialTheme.typography.titleLarge,
        )
    }
}
@Composable
private fun Back() {
    val recording = app.recordingManager.lastRecording()
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
fun AltitudeCard(sample: Sample) {
    FlipCard(
        title = "Altitude",
        modifier = Modifier.padding(0.dp, 4.dp),
        front = {
            Front(sample)
        },
        back = { Back() }
    )
}
