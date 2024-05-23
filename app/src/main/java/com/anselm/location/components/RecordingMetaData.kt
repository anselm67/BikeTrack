package com.anselm.location.components

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.anselm.location.LocalNavController
import com.anselm.location.R
import com.anselm.location.TAG
import com.anselm.location.models.RecordingDetailsViewModel
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
private fun Back(viewModel: RecordingDetailsViewModel) {
    val recording by viewModel.recordingState.collectAsState()

    var title by remember { mutableStateOf(recording.title) }
    var description by remember { mutableStateOf(recording.description) }
    val flip = LocalFaceController.current

    Column(
        modifier = Modifier.fillMaxWidth()
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
                  Log.d(TAG, "cancel")
                  flip()
                },
            ) {
                Icon(
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
                    flip()
                },
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_save),
                    contentDescription = "Save",
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun Front(viewModel: RecordingDetailsViewModel) {
    val recording by viewModel.recordingState.collectAsState()
    val navController = LocalNavController.current

    val dateFormat = SimpleDateFormat("EEEE, MMMM d, yyyy 'at' HH:mm", Locale.US)
    Column (
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = dateFormat.format(recording.time),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom=4.dp)
        )
        Text(
            text = recording.description,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding()
        )
        IconButton (
            onClick = {
                recording.delete()
                navController.popBackStack()
            }
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_trash),
                contentDescription = "Delete",
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}
@Composable
fun RecordingMetaData(viewModel: RecordingDetailsViewModel) {
    val recording by viewModel.recordingState.collectAsState()
    val updatedState = rememberUpdatedState(recording)

    Log.d("com.anselm.location", "RecordingMetaData renders $recording $updatedState")
    FlipCard(
        key = "RecordingMetaData",
        title = recording.title,
        drawableId = R.drawable.ic_edit,
        front = { Front(viewModel) },
        back = { Back(viewModel) },
    )
}

