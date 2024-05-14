package com.anselm.location

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp

@Composable
fun StartStopIcon(onStart: () -> Unit, onStop: () -> Unit) {
    var isStarted by remember { mutableStateOf(false) }

    IconButton(
        onClick = {
            if (isStarted) onStop() else onStart()
            isStarted = ! isStarted
        }  ,
        colors = IconButtonDefaults.iconButtonColors(
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    ) {
        Icon(
            painter = painterResource(
                id = if (isStarted)
                    R.drawable.ic_stop_recording
                else
                    R.drawable.ic_start_recording
            ),
            contentDescription = "Toggle recording.",
            tint = MaterialTheme.colorScheme.primary,
        )
    }
}