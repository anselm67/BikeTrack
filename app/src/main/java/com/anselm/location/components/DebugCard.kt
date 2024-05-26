package com.anselm.location.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.anselm.location.data.Sample

@Composable
fun DebugCard(
    sample: Sample,
    isAutoPaused: MutableState<Boolean>? = null,
) {
    val location = sample.location
    BasicCard(
        key = "DebugCard",
        title = "Debug",
        foldable = true,
        modifier = Modifier.padding(0.dp, 4.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if ( isAutoPaused?.value == true ) "Paused" else "Running")
            Text(
                "Coordinates: %.2f / %.2f".format(location.latitude, location.longitude)
            )
            Text("Accuracy: %.2f".format(location.accuracy))
            Text("Bearing: %.2f".format(location.bearing))
            Text("Sample Count: %d".format(sample.seqno))
        }
    }

}