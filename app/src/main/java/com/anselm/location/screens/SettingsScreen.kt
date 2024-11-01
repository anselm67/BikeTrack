package com.anselm.location.screens

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.anselm.location.LocationApplication.Companion.app
import com.anselm.location.R
import com.anselm.location.TAG
import com.anselm.location.asYYYYMMDD
import com.anselm.location.components.LoadingDisplay
import com.anselm.location.models.LocalAppViewModel
import com.anselm.location.models.SettingsScreenModel
import kotlinx.coroutines.launch
import java.time.LocalDate

@Composable
private fun DatabaseAction(title: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 8.dp, bottom = 4.dp, top = 4.dp)
            .clickable { onClick() },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title)
        Icon(
            painter = painterResource(id = R.drawable.ic_right),
            contentDescription = "Run this action.",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
fun ImportFiles(viewModel: SettingsScreenModel) {
    val recordingManager = app.recordingManager

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri: Uri? ->
            if (uri == null) {
                app.toast("No file selected.")
            } else {
                viewModel.showProgress = true
                app.applicationScope.launch {
                    try {
                        recordingManager.importZipFile(uri) { viewModel.progress = it }
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to import rides.", e)
                    }
                }.invokeOnCompletion {
                    // We're running on the application lifecycle scope, so this view that we're
                    viewModel.showProgress = false
                    app.toast("All rides imported.")
                }
            }
        }
    )

    DatabaseAction(title = "Import Rides") {
        launcher.launch("*/*")
    }
}

@Composable
fun ExportFiles(viewModel: SettingsScreenModel) {
    val recordingManager = app.recordingManager

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip"),
        onResult = { uri: Uri? ->
            if (uri == null) {
                app.toast("No destination file selected.")
            } else {
                viewModel.showProgress = true
                app.applicationScope.launch {
                    try {
                        recordingManager.exportFiles(uri) { viewModel.progress = it }
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to export rides.", e)
                    }
                }.invokeOnCompletion {
                    // We're running on the application lifecycle scope, so this view that we're
                    viewModel.showProgress = false
                    app.toast("All rides exported.")
                }
            }
        }
    )

    DatabaseAction(title = "Export Rides") {
            launcher.launch("${asYYYYMMDD(LocalDate.now())}-rides.zip")
    }
}

@Composable
fun CheckTags(viewModel: SettingsScreenModel) {
    val recordingManager = app.recordingManager
    val howMany = 25

    DatabaseAction(title = "Tag Rides") {
        viewModel.showProgress = true
        app.launch {
            try {
                recordingManager.checkTags(howMany) { done ->
                    viewModel.progress = done.toFloat() / howMany
                }
            } finally {
                viewModel.showProgress = false
                app.toast("$howMany rides tagged.")
            }
        }
    }
}

@Composable
fun RebuildCatalog(viewModel: SettingsScreenModel) {
    val recordingManager = app.recordingManager

    DatabaseAction(title = "Rebuild Catalog") {
        viewModel.showProgress = true
        app.launch {
            try {
                recordingManager.rebuildCatalog { viewModel.progress = it }
            } finally {
                viewModel.showProgress = false
                app.toast("${recordingManager.list().size} rides saved.")
            }
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
fun ImportExport(viewModel: SettingsScreenModel) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.Start
        ) {
            Text("Manage Database")
        }
        ImportFiles(viewModel)
        ExportFiles(viewModel)
        RebuildCatalog(viewModel)
        CheckTags(viewModel)
    }
}
@Composable
fun SettingsScreen(viewModel: SettingsScreenModel = viewModel()) {

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
        ImportExport(viewModel)
        HorizontalDivider(
            modifier = Modifier.padding(
                horizontal = 16.dp,
                vertical = 8.dp
            )
        )
        if ( viewModel.showProgress ) {
            LoadingDisplay() { viewModel.progress }
        }
    }
}