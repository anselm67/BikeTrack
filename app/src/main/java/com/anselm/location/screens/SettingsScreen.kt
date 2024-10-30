package com.anselm.location.screens

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.anselm.location.LocationApplication.Companion.app
import com.anselm.location.TAG
import com.anselm.location.components.LoadingDisplay
import com.anselm.location.models.LocalAppViewModel
import kotlinx.coroutines.launch

@Composable
fun ImportFiles() {
    val recordingManager = app.recordingManager
    val importingFiles = remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri: Uri? ->
            if (uri == null) {
                app.toast("No file selected.")
            } else {
                importingFiles.value = true
                app.applicationScope.launch {
                    try {
                        recordingManager.importZipFile(uri) { progress = it }
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to import rides.", e)
                    }
                }.invokeOnCompletion {
                    // We're running on the application lifecycle scope, so this view that we're
                    importingFiles.value = false
                    app.toast("All rides imported.")
                }
            }
        }
    )

    if (importingFiles.value) {
        LoadingDisplay { progress }
    } else {
        Button(
            onClick = {
                launcher.launch("*/*")
            }
        ) {
            Text("Import Rides")
        }
    }
}

@Composable
fun ExportFiles() {
    val recordingManager = app.recordingManager
    val exportingFiles = remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip"),
        onResult = { uri: Uri? ->
            if (uri == null) {
                app.toast("No destination file selected.")
            } else {
                exportingFiles.value = true
                app.applicationScope.launch {
                    try {
                        recordingManager.exportFiles(uri) { progress = it }
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to export rides.", e)
                    }
                }.invokeOnCompletion {
                    // We're running on the application lifecycle scope, so this view that we're
                    exportingFiles.value = false
                    app.toast("All rides exported.")
                }
            }
        }
    )

    if (exportingFiles.value) {
        LoadingDisplay { progress }
    } else {
        Button(
            onClick = {
                launcher.launch("rides.zip")
            }
        ) {
            Text("Export Rides")
        }
    }
}

@Composable
fun CheckTags() {
    val recordingManager = app.recordingManager
    val checkingTags = remember { mutableStateOf(false) }
    val howMany = 25
    var progress by remember { mutableFloatStateOf(0f) }

    if (checkingTags.value) {
        LoadingDisplay() { progress }
    } else {
        Button(
            onClick = {
                checkingTags.value = true
                app.launch {
                    try {
                        recordingManager.checkTags(howMany) { done ->
                            progress = done.toFloat() / howMany
                        }
                    } finally {
                        checkingTags.value = false
                        app.toast("$howMany rides tagged.")
                    }
                }
            }
        ) {
            Text("Tag rides")
        }
    }
}

@Composable
fun RebuildCatalog() {
    val recordingManager = app.recordingManager
    val rebuildingCatalog = remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }

    if (rebuildingCatalog.value) {
        LoadingDisplay() { progress }
    } else {
        Button(
            onClick = {
                rebuildingCatalog.value = true
                app.launch {
                    try {
                        recordingManager.rebuildCatalog { progress = it }
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
        CheckTags()
        ExportFiles()
        ImportFiles()
    }
}