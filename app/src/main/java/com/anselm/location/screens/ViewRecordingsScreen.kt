package com.anselm.location.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.anselm.location.LocalNavController
import com.anselm.location.LocationApplication.Companion.app
import com.anselm.location.NavigationItem
import com.anselm.location.R
import com.anselm.location.data.Entry
import com.anselm.location.data.RecordingManager
import com.anselm.location.dateFormat
import com.anselm.location.models.AppAction
import com.anselm.location.models.LocalAppViewModel
import com.anselm.location.models.ViewRecordingsModel
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@Composable
private fun StatBox(value: Double, units: String) {
    Column (
        horizontalAlignment = Alignment.CenterHorizontally,
    ){
        Text(
            text = "%.2f".format(value),
            style = MaterialTheme.typography.bodyLarge,
        )
        Text(
            text = units,
            style = MaterialTheme.typography.bodySmall,
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
                    Column {
                        Text(
                            text = entry.title.ifEmpty { "Untitled" },
                            style = MaterialTheme.typography.titleMedium,
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
                        modifier = Modifier.align(Alignment.CenterVertically)
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
                    Row {
                        Icon (
                            modifier = Modifier
                                .size(MaterialTheme.typography.bodyMedium.fontSize.value.dp)
                                .align(Alignment.Bottom)
                                .padding(2.dp),
                            painter = painterResource(id = R.drawable.ic_arrow_up),
                            contentDescription = "Climb in meters.",
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = "%.1f".format(entry.lastSample.climb),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    Row {
                        Icon (
                            modifier = Modifier
                                .size(MaterialTheme.typography.bodyMedium.fontSize.value.dp)
                                .align(Alignment.Bottom)
                                .padding(2.dp),
                            painter = painterResource(id = R.drawable.ic_arrow_down),
                            contentDescription = "Climb in meters.",
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = "%.1f".format(entry.lastSample.descent),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
            HorizontalDivider(modifier = Modifier.padding(4.dp))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SearchBox(viewModel: ViewRecordingsModel) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.Top
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(0.dp)
            ) {
                RangeSlider(
                    modifier = Modifier.fillMaxWidth(),
                    value = viewModel.queryRange,
                    valueRange = 0f..100f,
                    onValueChange = {
                        val step = 5
                        val start = (it.start / step).toInt() * step
                        val end = (it.endInclusive / step).toInt() * step
                        viewModel.queryRange = start.toFloat()..end.toFloat()
                    },
                    onValueChangeFinished = {
                        viewModel.updateQuery()
                    },
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("${viewModel.queryRange.start.toInt()}km")
                    Text("${viewModel.queryRange.endInclusive.toInt()}km")
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            Button(onClick = {
                viewModel.showBottomSheet = true
            }) {
                Text("Tags")
            }
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            FlowRow(
                modifier = Modifier.fillMaxWidth()
            ) {
                viewModel.queryTags.forEach {
                    Box (
                        modifier = Modifier
                            .padding(4.dp)
                            .clickable {
                                viewModel.queryTags -= it
                            },
                    ) {
                        Row(
                            modifier = Modifier
                                .background(
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = RoundedCornerShape(5.dp)
                                )
                                .padding(8.dp)
                                .clickable {
                                    viewModel.queryTags -= it
                                    viewModel.updateQuery()
                                },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = it,
                                color = MaterialTheme.colorScheme.onPrimary,
                            )
                            Spacer(modifier = Modifier.size(18.dp))
                            Icon(
                                painter = painterResource(id = R.drawable.ic_cancel),
                                modifier = Modifier
                                    .size(24.dp)
                                    .background(
                                        color = Color.Black.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(8.dp)
                                    ),
                                contentDescription = "Cancel",
                                tint = MaterialTheme.colorScheme.onPrimary,
                            )
                        }
                    }
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            Button(onClick = {
                viewModel.resetQuery()
            }) {
                Text("Reset")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectTags(query: RecordingManager.Query, viewModel: ViewRecordingsModel) {
    ModalBottomSheet(
        onDismissRequest = { viewModel.showBottomSheet = false },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Text(
                text = "Select Tags",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(8.dp)
            )
            LazyColumn(modifier = Modifier) {
                items(app.recordingManager.histo(query)) { (tag, count) ->
                    Column(
                        Modifier.fillMaxWidth()) {
                        Row(Modifier.fillMaxWidth()) {
                            HorizontalDivider()
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = viewModel.queryTags.contains(tag),
                                    onCheckedChange = {
                                        viewModel.showBottomSheet = false
                                        if (viewModel.queryTags.contains(tag)) {
                                            viewModel.queryTags -= tag
                                        } else {
                                            viewModel.queryTags += tag
                                        }
                                        viewModel.updateQuery()
                                    }
                                )
                                Text(text = tag, fontSize = 16.sp)
                            }
                            Text(text = "$count", fontSize = 16.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchAction(viewModel: ViewRecordingsModel) {
    IconButton(
        onClick = { viewModel.showSearchBox = ! viewModel.showSearchBox }
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_search),
            contentDescription = "Search",
            modifier = Modifier.size(24.dp),
        )
    }
}

@Composable
fun ViewRecordingsScreen(viewModel: ViewRecordingsModel = viewModel()) {
    val state by viewModel.resultFlow.collectAsState()
    val (query, rides) = state

    val appViewModel = LocalAppViewModel.current
    appViewModel.updateApplicationState {
        it.copy(
            title = "Your ${rides.size} rides",
            actions = listOf(object : AppAction {
                @Composable
                override fun Action() {
                    SearchAction(viewModel)
                }
            })
        )
    }.setShowOnLockScreen(false)

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        if ( viewModel.showSearchBox ) {
            SearchBox(viewModel)
        }
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            items(rides) { ride -> DisplayRecordingItem(ride) }
        }
        if ( viewModel.showBottomSheet ) {
            SelectTags(query, viewModel)
        }
    }
}