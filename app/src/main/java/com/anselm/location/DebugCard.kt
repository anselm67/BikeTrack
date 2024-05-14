package com.anselm.location

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun DebugCard(
    latitude: Double,
    longitude: Double,
    accuracy: Double,
    bearing: Double,
    sampleCount: Int
) {
    BasicCard(
        title = "Debug"
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                "Coordinates: %.2f / %.2f".format(latitude, longitude)
            )
            Text("Accuracy: %.2f".format(accuracy))
            Text("Bearing: %.2f".format(bearing))
            Text("Sample Count: %d".format(sampleCount))
        }
    }

}