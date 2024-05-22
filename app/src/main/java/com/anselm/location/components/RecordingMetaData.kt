package com.anselm.location.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.anselm.location.data.Recording

@Composable
fun RecordingMetaData(recording: Recording) {
    BasicCard(
        key = "RecordingMetaData",
        title = "Lunch Ride",
        modifier = Modifier
            .padding(0.dp, 4.dp)
            .border(2.dp, Color(0xffcccccc), shape = RoundedCornerShape(10.dp))
    ) {
        Column (
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Monday, 23 June 2023 10:00",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom=4.dp)
            )
            Text(
                text = "Some ride description. Rather long and very long.",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding()
            )
        }
    }
}