package com.anselm.location

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kotlin.math.max

@Composable
fun SpeedCard(speedInKilometersPerHour: Double,
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