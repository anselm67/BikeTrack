package com.anselm.location.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import kotlin.time.DurationUnit
import kotlin.time.toDuration


@Composable
fun RunningTime(timeMillis: Long, style: TextStyle = MaterialTheme.typography.displaySmall) {
    Text(
        text = timeMillis.toDuration(DurationUnit.MILLISECONDS)
            .toComponents { hours, minutes, seconds, _ ->
                "%02d:%02d:%02d".format(hours, minutes, seconds)
            },
        style = style
    )
}
