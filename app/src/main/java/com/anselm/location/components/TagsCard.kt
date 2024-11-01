package com.anselm.location.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.anselm.location.data.Recording
import com.anselm.location.data.RecordingTagger

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TagsCard(
    recording: Recording,
    modifier: Modifier = Modifier,
) {
    val isTagged = remember { mutableStateOf(recording.tags.isNotEmpty()) }

    Column(
        modifier = modifier.fillMaxWidth()
            .padding(top = 16.dp),
    ) {
        if ( ! isTagged.value ) {
            Button(
                modifier = Modifier.align(Alignment.CenterHorizontally),
                onClick = {
                    RecordingTagger(recording).tag {
                        isTagged.value = true
                        recording.save()
                    }
                }
            ) {
                Text("Extract Places")
            }
        } else {
            FlowRow(
                modifier = Modifier.fillMaxWidth()
            ) {
                recording.tags.forEach {
                    Box (
                        modifier = Modifier.padding(4.dp)
                    ) {
                        Text(
                            text = it,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier
                                .background(
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = RoundedCornerShape(5.dp)
                                )
                                .padding(8.dp)
                        )
                    }
                }
            }
        }
    }
}