package com.anselm.location

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex

@Composable
fun BasicCard(
    title: String? = null,
    @SuppressLint("ModifierParameter") modifier: Modifier = Modifier,
    foldable: Boolean? = false,
    content: @Composable () -> Unit
) {
    var folded by remember { mutableStateOf(false) }
    Card (
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 6.dp
        )
    ) {
        Box (
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
        ) {
            if (foldable == true) {
                IconButton(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset((-10).dp, 10.dp)
                        .size(24.dp)
                        .zIndex(2f),
                    onClick = { folded = !folded },
                ) {
                    Icon(
                        painter = painterResource(
                            id = if (folded) R.drawable.ic_unfold else R.drawable.ic_fold
                        ),
                        contentDescription = if (folded) "Unfold" else "Fold",
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                if (title != null) {
                    Text(
                        text = title,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.displaySmall,
                    )
                }
                if (!folded) {
                    Spacer(modifier = Modifier.padding(8.dp))
                    content()
                }
            }
        }
        Spacer(modifier = Modifier.padding(4.dp))
    }
}