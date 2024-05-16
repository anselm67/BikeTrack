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

@Composable
private fun Front(
    gradePercent: Double,
    climbInMeters: Double,
    descentInMeters: Double,
) {
    Row (
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
    ) {
        Text(
            text = if ( gradePercent.isNaN() ) "--" else "%.1f%%".format(gradePercent),
            style = MaterialTheme.typography.displayLarge,
        )
    }
    Row (
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ){
        Text(
            text = "Up: %3.1f".format(climbInMeters),
            style = MaterialTheme.typography.titleLarge,
        )
        Text(
            text = "Down: %3.1f".format(descentInMeters),
            style = MaterialTheme.typography.titleLarge,
        )
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
        val altitude = recording.extractAltitude()
        val time = recording.extractDistances()

        Graph(
            modifier = Modifier
                .fillMaxWidth()
                .height(500.dp),
            xValues = time,
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
    gradePercent: Double,
    climbInMeters: Double,
    descentInMeters: Double
) {
    FlipCard(
        title = "Altitude",
        modifier = Modifier.padding(0.dp, 4.dp),
        front = {
            Front(
                gradePercent,
                climbInMeters,
                descentInMeters
            )
        },
        back = { Back() }
    )
}
