// https://fvilarino.medium.com/creating-a-rotating-card-in-jetpack-compose-ba94c7dd76fb

package com.anselm.location.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.anselm.location.R

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

@Composable
private fun FlipCardInternal(
    cardFace: CardFace,
    onClick: (CardFace) -> Unit,
    modifier: Modifier = Modifier,
    drawableId: Int,
    back: @Composable () -> Unit = { },
    front: @Composable () -> Unit = { },
) {
    var frontCardSize by remember { mutableStateOf(IntSize.Zero) }
    val rotation = animateFloatAsState(
        targetValue = cardFace.angle,
        animationSpec = tween(
            durationMillis = 400,
            easing = FastOutSlowInEasing,
        ), label = "FlipCard rotation."
    )
    Box (
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer {
                rotationY = rotation.value
                cameraDistance = 12f * density
            }.clickable {
                onClick(cardFace)
            }
    ) {
        Box(
            modifier = Modifier.matchParentSize()
        ) {
            if (cardFace == CardFace.Front) {
                Icon(
                    modifier = Modifier
                        .size(24.dp)
                        .align(Alignment.TopEnd)
                        .offset((-10).dp, (10).dp)
                        .zIndex(2f),
                    painter = painterResource(id = drawableId),
                    contentDescription = "Flip card.",
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
            if ( rotation.value <= 90f ) {
                Box(
                    modifier = Modifier.fillMaxSize()
                        .onGloballyPositioned {
                            coordinates -> frontCardSize = coordinates.size
                        }
                ) {
                    front()
                }
            } else {
                Box(modifier = Modifier
                    .size(
                        with(LocalDensity.current) { frontCardSize.width.toDp() },
                        with(LocalDensity.current) { frontCardSize.height.toDp() },
                    )
                    .background(Color.Red)
                    .graphicsLayer { rotationY = 180f }) {
                    back()
                }
            }
        }
    }
}

val LocalFaceController = compositionLocalOf<() -> Unit> {
    error("LocalFaceController: none provided.")
}

@Composable
fun FlipCard(
    key: String,
    modifier: Modifier = Modifier,
    title: String? = null,
    drawableId: Int = R.drawable.ic_flip_card,
    front: @Composable () -> Unit,
    back: @Composable () -> Unit
) {
    var cardFace by remember { mutableStateOf(CardFace.Front) }
    val flip = { cardFace = cardFace.next }
    CompositionLocalProvider(LocalFaceController provides flip) {
        FlipCardInternal(
            cardFace = cardFace,
            onClick = { cardFace = cardFace.next },
            modifier = modifier,
            drawableId = drawableId,
            front = {
                BasicCard(key, title, modifier = Modifier.fillMaxSize()) {
                    front()
                }
            },
            back = back
        )
    }
}