package com.anselm.location.components

import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.anselm.location.R

@Composable
fun BasicCard(
    key : String,
    title: String? = null,
    @SuppressLint("ModifierParameter") modifier: Modifier = Modifier,
    foldable: Boolean? = false,
    content: @Composable () -> Unit
) {
    val sharedPreferences = LocalContext.current.getSharedPreferences(
            "LocationPreferences",
        Context.MODE_PRIVATE)
    var folded by remember { mutableStateOf(sharedPreferences.getBoolean("${key}.folded", false)) }

    Box (
        modifier = modifier,
    ) {
        Box (
            modifier = Modifier.fillMaxWidth()
        ) {
            if (foldable == true) {
                IconButton(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset((-10).dp, 0.dp)
                        .size(24.dp)
                        .zIndex(2f),
                    onClick = {
                        folded = !folded
                        sharedPreferences.edit().putBoolean("${key}.folded", folded).apply()
                    },
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
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                        text = title,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge,
                    )
                }
                if (!folded) {
                    Spacer(modifier = Modifier.padding(4.dp))
                    content()
                    Spacer(modifier = Modifier.padding(4.dp))
                }
                HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
            }
        }
    }
}