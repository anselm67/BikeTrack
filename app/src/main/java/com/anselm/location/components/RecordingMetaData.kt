package com.anselm.location.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.anselm.location.R
import com.anselm.location.data.Recording
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
private fun Front(recording: Recording) {
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
    }
}
@Composable
fun RecordingMetaData(recording: Recording) {
    FlipCard(
        key = "RecordingMetaData",
        title = recording.title,
        drawableId = R.drawable.ic_edit,
        front = { Front(recording) } ,
        back = { Front(recording) } ,
    )
}