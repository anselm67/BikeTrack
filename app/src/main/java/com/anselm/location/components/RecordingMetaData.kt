package com.anselm.location.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
//            .background(
//                color = MaterialTheme.colorScheme.surfaceVariant,
//                shape = RoundedCornerShape(10.dp)
//            )
//            .border(2.dp, MaterialTheme.colorScheme.primary, shape = RoundedCornerShape(10.dp))
    ) {
        OutlinedTextField(
            value = title,
            onValueChange = { value -> title = value.replaceFirstChar { it.uppercase() } },
            label = { Text("Title") },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Enter a title.") }
        )
        OutlinedTextField(
            value = description,
            onValueChange = { value -> description = value.replaceFirstChar { it.uppercase() } },
            label = { Text("Description") },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Enter a description.") }
        )
        Row (
            modifier = Modifier.fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            Button(
                onClick = {
                  viewModel.isEditing.value = false
                },
            ) {
                Icon(
                    modifier = modifier.size(24.dp),
                    painter = painterResource(id = R.drawable.ic_cancel),
                    contentDescription = "Cancel",
                    tint = MaterialTheme.colorScheme.onPrimary,
                )
                Text(
                    text = "Cancel",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Button(
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
                    tint = MaterialTheme.colorScheme.onPrimary,
                )
                Text(
                    text = "Save",
                    style = MaterialTheme.typography.bodySmall,
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
            .padding(vertical = 12.dp, horizontal = 5.dp)
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
            style = MaterialTheme.typography.bodyLarge,
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

