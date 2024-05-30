package com.anselm.location.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateValue
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.anselm.location.LocalNavController
import com.anselm.location.LocationApplication.Companion.app
import com.anselm.location.NavigationItem
import com.anselm.location.R
import com.anselm.location.models.ApplicationViewModel

@Composable
private fun PulseRecordButton(onClick: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_button")

    // Animated size for the pulsing effect
    val pulsingSize by infiniteTransition.animateValue(
        initialValue = 32.dp,
        targetValue = 48.dp,
        typeConverter = Dp.VectorConverter,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "pulse_button"
    )

    IconButton(
        onClick =  onClick
    ) {
        Icon(
            painter = painterResource(
                id = R.drawable.ic_start_recording
            ),
            contentDescription = "Navigate to the recording screen.",
            modifier = Modifier.size(pulsingSize),
            tint = MaterialTheme.colorScheme.primary,

            )
    }
}

@Composable
private fun RecordButton(onClick: () -> Unit ){
    IconButton(
        onClick = onClick
    ) {
        Icon(
            painter = painterResource(
                id = R.drawable.ic_start_recording
            ),
            contentDescription = "Navigate to the recording screen.",
            tint = MaterialTheme.colorScheme.primary,
        )
    }

}

@Composable
fun AppBottomBar(viewModel: ApplicationViewModel) {
    val state by viewModel.applicationState.collectAsState()
    val navController = LocalNavController.current

    if ( state.hideBottomBar ) {
        return
    }

    val isRecording by app.isRecording

    BottomAppBar (
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.primary,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
        ) {
            IconButton(
                onClick = {
                    navController.navigate(NavigationItem.ViewRecordings.route)
                }
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_home),
                    contentDescription = "Navigate to the home screen.",
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
            if ( isRecording ) {
                PulseRecordButton(
                    onClick = {
                        navController.navigate(NavigationItem.Recording.route)
                    }
                )
            } else {
                RecordButton(
                    onClick = {
                        navController.navigate(NavigationItem.Recording.route)
                    }
                )
            }
            IconButton(
                onClick = {
                    navController.navigate(NavigationItem.Settings.route)
                }
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_settings),
                    contentDescription = "Navigate to the settings screen.",
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}
