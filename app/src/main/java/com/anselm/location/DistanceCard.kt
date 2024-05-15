package com.anselm.location

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun DistanceCard(distanceInKilometers: Double) {
    BasicCard(
        title = "Distance",
        modifier = Modifier.padding(0.dp, 4.dp)
    ) {
        Row (
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
        ) {
            Text(
                text = "%.1f km".format(distanceInKilometers),
                style = MaterialTheme.typography.displayLarge,
            )
        }
    }
}