package com.anselm.location.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import com.anselm.location.LocationApplication.Companion.app
import com.anselm.location.data.Recording
import com.anselm.location.data.Sample
import com.anselm.location.timeFormat
import kotlinx.coroutines.delay
import kotlin.time.DurationUnit
import kotlin.time.toDuration

private fun getCurrentTime(): String {
    return timeFormat.format(System.currentTimeMillis())
}

@Composable fun LiveRunningTime(timeMillis: Long) {
    val alpha by rememberInfiniteTransition(label = "Blinking text.").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 2000
                0.25f at  500
                1f at 1500
            },
            repeatMode = RepeatMode.Restart
        ), label = "Blinking text."
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "%02d:%02d:%02d".format(
                *(timeMillis).toDuration(DurationUnit.MILLISECONDS)
                    .toComponents { hours, minutes, seconds, _ ->
                        arrayOf(hours, minutes, seconds)
                    }
            ),
            style = MaterialTheme.typography.displaySmall,
            modifier = Modifier.alpha(alpha)
        )
        Text(
            text = "Auto-Paused",
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun RunningTime(timeMillis: Long) {
    Text(
        text = "%02d:%02d:%02d".format(
            *(timeMillis).toDuration(DurationUnit.MILLISECONDS)
                .toComponents { hours, minutes, seconds, _ ->
                    arrayOf(hours, minutes, seconds)
                }
        ),
        style = MaterialTheme.typography.displaySmall,
    )
}

@Composable
fun TimeElapsedCard(
    sample: Sample,
    modifier: Modifier = Modifier,
    recording: Recording? = null,
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
                if ( isLive ) {
                    LiveRunningTime(timeMillis)
                } else {
                    RunningTime(timeMillis)
                }
                if ( isLive ) {
                    // Displays the current gps accuracy.
                    GradientCircle(
                        accuracy = sample.location.accuracy,
                        modifier = Modifier.defaultMinSize(minHeight = 50.dp, minWidth = 50.dp)
                    )
                    // Displays the current time.
                    var currentTime by remember { mutableStateOf(getCurrentTime()) }
                    LaunchedEffect(Unit) {
                        while (true) {
                            currentTime = getCurrentTime()
                            if ( ! app.isAutoPaused.value ) {
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
                NumberWithUnits(
                    value = "%.2f".format(distanceInKilometers),
                    units = "km",
                    style = MaterialTheme.typography.displayLarge,
                )
            }
        }
    }
}