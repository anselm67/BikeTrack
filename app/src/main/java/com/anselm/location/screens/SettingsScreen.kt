package com.anselm.location.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.anselm.location.LocationApplication.Companion.app
import com.anselm.location.components.LoadingDisplay
import com.anselm.location.models.LocalAppViewModel

@Composable
fun SettingsScreen() {
    val recordingManager = app.recordingManager
    val rebuildingCatalog = remember { mutableStateOf(false) }

    val appViewModel = LocalAppViewModel.current
    appViewModel
        .updateTitle("Settings")
        .setShowOnLockScreen(false)

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (rebuildingCatalog.value) {
            LoadingDisplay()
        } else {
            Button(
                onClick = {
                    rebuildingCatalog.value = true
                    app.launch {
                        try {
                            recordingManager.rebuildCatalog()
                        } finally {
                            rebuildingCatalog.value = false
                            app.toast("${recordingManager.list().size} rides saved.")
                        }
                    }
                }
            ) {
                Text("Rebuild Catalog")
            }
        }
    }
}