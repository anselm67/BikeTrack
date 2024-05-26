package com.anselm.location.components

import android.util.Log
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.compose.currentBackStackEntryAsState
import com.anselm.location.LocalNavController
import com.anselm.location.NavigationItem
import com.anselm.location.models.ApplicationViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(viewModel: ApplicationViewModel) {
    val state by viewModel.applicationState.collectAsState()
    val navController = LocalNavController.current

    Log.d("com.anselm.location.AppTopBar", "state: $state")
    if ( state.hideTopBar ) {
        return
    }

    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            titleContentColor = MaterialTheme.colorScheme.primary,
        ),
        title = {
            Text(
                text = state.title,
                maxLines = 1,
            )
        },
        actions = {
            state.actions.forEach { action -> action.Action() }
        },
        navigationIcon = {
            val currentBackStackEntry by navController.currentBackStackEntryAsState()
            val canNavigateBack = currentBackStackEntry?.destination?.route != NavigationItem.ViewRecordings.route
            if ( canNavigateBack ) {
                IconButton(onClick = { navController.navigateUp() }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                    )
                }
            }
        }
    )
}