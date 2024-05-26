package com.anselm.location.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.anselm.location.R
import com.anselm.location.dateFormat
import com.anselm.location.models.RecordingDetailsViewModel


@Composable
private fun Back(
    viewModel: RecordingDetailsViewModel,
    modifier: Modifier
) {
    val recordingWrapper by viewModel.recordingState.collectAsState()
    val recording = recordingWrapper.value

    var title by remember { mutableStateOf(recording.title) }
    var description by remember { mutableStateOf(recording.description) }

    Column(
        modifier =modifier.fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 5.dp)
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(10.dp)
            )
            .border(2.dp, MaterialTheme.colorScheme.primary, shape = RoundedCornerShape(10.dp))
    ) {
        TextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Title") },
            modifier = Modifier.fillMaxWidth()
        )
        TextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Description") },
            modifier = Modifier.fillMaxWidth()
        )
        Row (
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            IconButton(
                onClick = {
                  viewModel.isEditing.value = false
                },
            ) {
                Icon(
                    modifier = modifier.size(24.dp),
                    painter = painterResource(id = R.drawable.ic_cancel),
                    contentDescription = "Save",
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
            IconButton(
                onClick = {
                    recording.title = title
                    recording.description = description
                    viewModel.updateRecording(recording)
                    viewModel.isEditing.value = false
                },
            ) {
                Icon(
                    modifier = modifier.size(24.dp),
                    painter = painterResource(id = R.drawable.ic_check),
                    contentDescription = "Save",
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun Front(
    viewModel: RecordingDetailsViewModel,
    modifier: Modifier
) {
    val recordingWrapper by viewModel.recordingState.collectAsState()
    val recording = recordingWrapper.value

    Column (
        modifier = modifier.fillMaxWidth()
    ) {
        Row (
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(
                    text = recording.title,
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    text = dateFormat.format(recording.time),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
            IconButton(
                onClick = {
                    viewModel.isEditing.value = true
                }
            ) {
                Icon(
                    modifier = Modifier.size(24.dp),
                    painter = painterResource(id = R.drawable.ic_edit),
                    contentDescription = "Save",
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
        Text(
            text = recording.description,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding()
        )
    }
}
@Composable
fun RecordingMetaData(
    viewModel: RecordingDetailsViewModel,
    modifier : Modifier = Modifier,
) {
    if ( viewModel.isEditing.value ) {
        Back(viewModel, modifier = modifier)
    } else {
        Front(viewModel, modifier = modifier)
    }
}

