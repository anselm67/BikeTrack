package com.anselm.location.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import com.anselm.location.LocalNavController
import com.anselm.location.LocationApplication.Companion.app
import com.anselm.location.NavigationItem
import com.anselm.location.data.Entry
import com.anselm.location.dateFormat
import com.anselm.location.models.LocalAppViewModel
import kotlin.time.DurationUnit
import kotlin.time.toDuration

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
    Box (
        modifier = Modifier
            .padding(8.dp)
            .clickable {
                navController.navigate(
                    "${NavigationItem.RecordingDetails.route}/${entry.id}"
                )
            },
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.Top,
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start
            ) {
                Row (
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column( ) {
                        Text(
                            text = entry.title.ifEmpty { "Untitled" },
                            style = MaterialTheme.typography.titleLarge,
                            fontStyle =
                                if ( entry.title.isEmpty() )
                                    FontStyle.Italic
                                else
                                    FontStyle.Normal
                        )
                        Text(
                            text = dateFormat.format(entry.time),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    Text(
                        text = "%02d:%02d:%02d".format(
                            *(entry.lastSample.elapsedTime).toDuration(DurationUnit.MILLISECONDS)
                                .toComponents { hours, minutes, seconds, _ ->
                                    arrayOf(hours, minutes, seconds)
                                }
                        ),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.align(Alignment.Top)
                    )
                }
            }
            Spacer(modifier = Modifier.size(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
            ) {
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
            HorizontalDivider(modifier = Modifier.padding(4.dp))
        }
    }
}


@Composable
fun ViewRecordingsScreen() {
    val appViewModel = LocalAppViewModel.current
    appViewModel
        .updateTitle(title = "Your rides")
        .setShowOnLockScreen(false)

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