package com.anselm.location.screens

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.anselm.location.LocalNavController
import com.anselm.location.LocationApplication.Companion.app
import com.anselm.location.NavigationItem
import com.anselm.location.data.Entry
import kotlin.time.DurationUnit
import kotlin.time.toDuration

private const val TAG = "com.anselm.location.components.RecordingsScreen"


@Composable
private fun StatBox(value: Double, units: String) {
    Column (
        horizontalAlignment = Alignment.CenterHorizontally,
    ){
        Text(
            text = "%.2f".format(value),
            style = MaterialTheme.typography.displayMedium,
        )
        Text(
            text = units,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun DisplayRecordingItem(entry: Entry) {
    val navController = LocalNavController.current
    Card(
        modifier = Modifier.padding(8.dp),
        onClick = {
            navController.navigate(
                "${NavigationItem.RecordingDetails.route}/${entry.id}"
            )
        },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 6.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalArrangement = Arrangement.Top,
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = entry.title,
                    style = MaterialTheme.typography.titleLarge,
                )
            }
            Spacer(modifier = Modifier.size(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = "Duration %02d:%02d:%02d".format(
                        *(entry.lastSample.elapsedTime).toDuration(DurationUnit.MILLISECONDS)
                            .toComponents { hours, minutes, seconds, _ ->
                                arrayOf(hours, minutes, seconds)
                            }
                    ),
                    style = MaterialTheme.typography.titleLarge
                )
            }
            Row (
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                StatBox(value = entry.lastSample.avgSpeed * 3.6, units = "km/h")
                StatBox(value = entry.lastSample.totalDistance / 1000.0, units = "km")
                Column (
                    horizontalAlignment = Alignment.Start,
                ) {
                    Text(
                        text = "%.1f up".format(entry.lastSample.climb),
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Text(
                        text = "%.1f dn".format(entry.lastSample.descent),
                        style = MaterialTheme.typography.titleLarge,
                    )
                }
            }
        }
    }
}


@Composable
fun ViewRecordingsScreen() {
    Log.d(TAG, "RecordingScreen")
    val appBarTitle = rememberSaveable { app.appBarTitle.value }

    DisposableEffect (LocalContext.current ){
        app.appBarTitle.value = "Your Rides"
        onDispose {
            app.appBarTitle.value = appBarTitle
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        app.recordingManager.list().forEach {
            DisplayRecordingItem(it)
        }
    }
}