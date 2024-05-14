package com.anselm.location

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

enum class CardFace(val angle: Float) {
    Front(0f) {
        override val next: CardFace
            get() = Back
    },
    Back(180f) {
        override val next: CardFace
            get() = Front
    };

    abstract val next: CardFace
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlipCard(
    cardFace: CardFace,
    onClick: (CardFace) -> Unit,
    modifier: Modifier = Modifier,
    back: @Composable () -> Unit = { },
    front: @Composable () -> Unit = { },
    ) {
    val rotation = cardFace.angle
    Card(
        onClick = { onClick(cardFace) },
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            if ( rotation <= 90f) {
                front()
            } else {
                back()
            }
        }
    }
}