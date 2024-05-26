package com.anselm.location.components

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import com.anselm.location.LocalNavController
import com.anselm.location.NavigationItem
import com.anselm.location.R
import com.anselm.location.models.ApplicationViewModel

@Composable
fun AppBottomBar(viewModel: ApplicationViewModel) {
    val state by viewModel.applicationState.collectAsState()
    val navController = LocalNavController.current

    Log.d("com.anselm.location.AppBottomBar", "state: $state")
    if ( state.hideBottomBar ) {
        return
    }
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
            IconButton(
                onClick = {
                    navController.navigate(NavigationItem.Recording.route)
                }
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_start_recording),
                    contentDescription = "Navigate to the home screen.",
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
            IconButton(
                onClick = { }
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_settings),
                    contentDescription = "Navigate to the home screen.",
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}
