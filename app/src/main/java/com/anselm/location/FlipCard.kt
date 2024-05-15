// https://fvilarino.medium.com/creating-a-rotating-card-in-jetpack-compose-ba94c7dd76fb

package com.anselm.location

import android.util.Log
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.motionEventSpy
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize

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
    var frontCardSize by remember { mutableStateOf(IntSize.Zero) }
    val rotation = animateFloatAsState(
        targetValue = cardFace.angle,
        animationSpec = tween(
            durationMillis = 400,
            easing = FastOutSlowInEasing,
        ), label = "FlipCard rotation."
    )
    Card(
        onClick = { onClick(cardFace) },
        modifier = modifier
            .onGloballyPositioned {
                Log.d("com.anselm.location", "TOP FlipCard: ${it.size} ${it.parentCoordinates?.size}")
            }
            .graphicsLayer {
                rotationY = rotation.value
                cameraDistance = 12f * density
            },
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
        ) {
            if ( rotation.value <= 90f) {
                Box(modifier = Modifier.onGloballyPositioned {
                    coordinates -> frontCardSize = coordinates.size
                    Log.d(TAG, "FlipCard: ${coordinates.size} ${coordinates.parentCoordinates?.size}")
                }) {
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