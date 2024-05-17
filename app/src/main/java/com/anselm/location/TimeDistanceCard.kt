package com.anselm.location

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@Composable
fun TimeElapsedCard(
    timeMillis: Long,
    distanceInKilometers: Double
) {
    BasicCard(
        modifier = Modifier.padding(0.dp, 4.dp)
    ) {
        Column (
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "%02d:%02d:%02d".format(
                    *(timeMillis).toDuration(DurationUnit.MILLISECONDS)
                        .toComponents { hours, minutes, seconds, _ ->
                            arrayOf(hours, minutes, seconds)
                        }
                ),
                style = MaterialTheme.typography.displayLarge,
            )
            Text(
                text = "%.2f km".format(distanceInKilometers),
                style = MaterialTheme.typography.displayLarge,
            )
        }
    }
}