package com.anselm.location

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun SampleGraphCard() {
    Box (
        modifier = Modifier.fillMaxWidth().height(200.dp)
    ) {
        val recording = RecordingManager.get().load("recording-2024-05-14-10-14-37.json")
        val altitude = recording.extractAltitude().map(Double::toFloat)
        val time = recording.extractDistances().map { it.toFloat()  }

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