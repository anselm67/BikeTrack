package com.anselm.location

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
        title = "Running Time",
        modifier = Modifier.padding(0.dp, 4.dp)
    ) {
        Column (
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "%02dh:%02dm:%02ds".format(
                    *(timeMillis).toDuration(DurationUnit.MILLISECONDS)
                        .toComponents { hours, minutes, seconds, _ ->
                            arrayOf(hours, minutes, seconds)
                        }
                ),
                style = MaterialTheme.typography.displayLarge,
            )
            Text(
                text = "%.1f km".format(distanceInKilometers),
                style = MaterialTheme.typography.displayLarge,
            )
        }
    }
}