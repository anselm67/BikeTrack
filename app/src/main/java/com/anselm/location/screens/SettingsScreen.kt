package com.anselm.location.screens

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.anselm.location.LocationApplication.Companion.app
import com.anselm.location.components.LoadingDisplay
import com.anselm.location.models.LocalAppViewModel

@Composable
fun RebuildCatalog() {
    val recordingManager = app.recordingManager
    val rebuildingCatalog = remember { mutableStateOf(false) }

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

@Composable
fun EditBooleanPreference(
    title: String,
    key: String,
) {
    val sharedPreferences = LocalContext.current.getSharedPreferences(
        "LocationPreferences",
        Context.MODE_PRIVATE)
    var show by remember { mutableStateOf(sharedPreferences.getBoolean(key, false)) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
        )
        Switch(
            checked = show,
            onCheckedChange = {
                show = it
                sharedPreferences.edit().putBoolean(key, show).apply()
            }
        )
    }
}

@Composable
fun SettingsScreen() {

    val appViewModel = LocalAppViewModel.current
    appViewModel
        .updateTitle("Settings")
        .setShowOnLockScreen(false)

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        EditBooleanPreference(
            title = "Show debug card",
            key = "showDebugCard"
        )
        HorizontalDivider(
            modifier = Modifier.padding(
                horizontal = 16.dp,
                vertical = 8.dp
            )
        )
        EditBooleanPreference(
            title = "Show accuracy meter",
            key = "showAccuracyMeter"
        )
        HorizontalDivider(
            modifier = Modifier.padding(
                horizontal = 16.dp,
                vertical = 8.dp
            )
        )
        RebuildCatalog()
    }
}