package com.anselm.location

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun AltitudeCard(
    gradePercent: Double,
    climbInMeters: Double,
    descentInMeters: Double
) {

    BasicCard("Altitude") {
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

}