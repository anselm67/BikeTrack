package com.anselm.location.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.anselm.location.data.Recording
import com.anselm.location.data.Sample
import com.anselm.location.timeFormat
import kotlinx.coroutines.delay
import kotlin.time.DurationUnit
import kotlin.time.toDuration

private fun getCurrentTime(): String {
    return timeFormat.format(System.currentTimeMillis())
}

@Composable
fun TimeElapsedCard(
    sample: Sample,
    modifier: Modifier = Modifier,
    recording: Recording? = null,
    isAutoPaused: MutableState<Boolean>? = null,
) {
    val isLive = (recording == null)

    var timeMillis by remember { mutableLongStateOf(sample.elapsedTime) }
    val distanceInKilometers = sample.totalDistance / 1000.0

    BasicCard(
        key = "TimeElapsedCard",
        modifier = modifier.padding(0.dp, 4.dp)
    ) {
        Column (
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row (
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
            ) {
                Text(
                    text = "%02d:%02d:%02d".format(
                        *(timeMillis).toDuration(DurationUnit.MILLISECONDS)
                            .toComponents { hours, minutes, seconds, _ ->
                                arrayOf(hours, minutes, seconds)
                            }
                    ),
                    style = MaterialTheme.typography.displaySmall,
                )
                if ( isLive ) {
                    var currentTime by remember { mutableStateOf(getCurrentTime()) }
                    LaunchedEffect(Unit) {
                        while (true) {
                            currentTime = getCurrentTime()
                            if (isAutoPaused?.value != true) {
                                timeMillis += 1000
                            }
                            delay(1000)
                        }
                    }
                    Text(
                        text = currentTime,
                        style = MaterialTheme.typography.displaySmall,
                    )
                }
            }
            Row (
                modifier = Modifier.padding(top = 12.dp)
            ){
                Text(
                    text = "%.2f".format(distanceInKilometers),
                    style = MaterialTheme.typography.displayLarge,
                    modifier = Modifier.alignByBaseline(),
                )
                Text(
                    modifier = Modifier.alignByBaseline(),
                    text = "km",
                    style = MaterialTheme.typography.titleLarge,
                )
            }
        }
    }
}