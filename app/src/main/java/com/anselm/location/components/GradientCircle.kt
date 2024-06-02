package com.anselm.location.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import kotlin.math.max

@Composable
fun GradientCircle(
    accuracy: Float,
    modifier: Modifier = Modifier,
) {
    val textMeasurer = rememberTextMeasurer()

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = modifier
        ) {
            Canvas(
                modifier = Modifier.matchParentSize(),
            ) {
                // 5m maps to 0.1, 100m maps to 5.0
                // f = alpha * accuracy + beta
                val alpha = (5.0 - 0.1) / 95.0
                val beta = -3.0 / 19.0
                val f = max(0.01, alpha * accuracy + beta)
                val radius = size.minDimension / 2
                val brush = Brush.radialGradient(
                    colors = listOf(Color(0xffe63900) /* Red */, Color(0xff009933) /* Green */),
                    center = center,
                    radius = f.toFloat() * radius
                )
                drawCircle(
                    radius = radius,
                    brush = brush,
                )
                val textLayoutResult = textMeasurer.measure(
                    text = AnnotatedString("%2.1f".format(accuracy)),
                    style = TextStyle(
                        color = Color.White,
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center
                    )
                )

                val textX = center.x - textLayoutResult.size.width / 2f
                val textY = center.y - textLayoutResult.size.height / 2f

                drawText(
                    textLayoutResult,
                    topLeft = Offset(textX, textY)
                )
            }
        }
        Text(
            text = "Accuracy",
            style = MaterialTheme.typography.bodySmall
        )
    }
}