package com.anselm.location.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.TextStyle

@Composable
fun NumberWithUnits(
    value: String,
    units: String,
    style: TextStyle = MaterialTheme.typography.bodyLarge,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = style
        )
        Text(
            text = units,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}