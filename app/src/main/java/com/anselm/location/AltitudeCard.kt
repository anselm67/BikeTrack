package com.anselm.location

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row {
            Icon(Icons.Rounded.KeyboardArrowUp, contentDescription = "Climb")
            Text(
                text = "%.1f".format(climbInMeters),
                style = MaterialTheme.typography.displayLarge,
            )
        }
        Row {
            Text(
                text = "%.1f".format(gradePercent.ifNaN(0.0)),
                style = MaterialTheme.typography.displaySmall,
            )
        }
        Row {
            Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = "Climb")
            Text(
                text = "%.1f".format(descentInMeters),
                style = MaterialTheme.typography.displayLarge,
            )
        }
    }
}
@Composable
private fun Back() {
    val recording = RecordingManager.get().load("recording-2024-05-14-10-14-37.json")
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
