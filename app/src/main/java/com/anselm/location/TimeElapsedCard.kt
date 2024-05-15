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
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@Composable
fun TimeElapsedCard(timeMillis: Long) {
    BasicCard(
        title = "Running Time",
        modifier = Modifier.padding(0.dp, 4.dp)
    ) {
        Row (
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
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
        }
    }
}