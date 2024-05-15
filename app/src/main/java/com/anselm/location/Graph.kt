package com.anselm.location


import android.graphics.Paint
import android.graphics.PointF
import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class GraphAppearance(
    val graphColor: Color,
    val graphAxisColor: Color,
    val graphThickness: Float,
    val isColorAreaUnderChart: Boolean,
    val colorAreaUnderChart: Color,
    val isCircleVisible: Boolean,
    val circleColor: Color,
    val backgroundColor: Color
)
@Composable
fun Graph(
    modifier : Modifier,
    xValues: List<Float>,
    yValues: List<Float>,
    graphAppearance: GraphAppearance,
) {
    val density = LocalDensity.current
    val fontSize = density.run { 8.sp.toPx() }
    val textPaint = remember(density) {
        Paint().apply {
            color = graphAppearance.graphAxisColor.toArgb()
            textAlign = Paint.Align.LEFT
            textSize = fontSize
        }
    }

    val xMax = xValues.max()
    val yMax = yValues.max()
    val yMin = yValues.min()

    Box(
        modifier = modifier
            .background(Color.White)
            .padding(horizontal = 4.dp, vertical = 4.dp)
            .border(1.dp, Color.Red),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(
            modifier = Modifier.fillMaxSize(),
        ) {

            val canvasSize = Size(
                size.width,
                size.height
            )

            val leftMargin = 2f * fontSize
            val bottomMargin = 1.5f * fontSize
            drawRect(Color.Green, size = canvasSize)
            drawRect(Color.Cyan,
                topLeft = Offset(leftMargin, 0f),
                size = Size(canvasSize.width - leftMargin, canvasSize.height - bottomMargin),
            )

            val p1 = PointF(leftMargin, canvasSize.height - bottomMargin)       // Bottom left
            val p2 = PointF(canvasSize.width, 0f)       // Top right

            drawContext.canvas.nativeCanvas.drawLines(
                floatArrayOf(
                    p1.x, p1.y, p2.x, p1.y,
                    p1.x, p1.y, p1.x, p2.y,
                ), textPaint
            )

            fun toLabelPixel(x: Float, y: Float): Pair<Float, Float> {
                return Pair(
                    x * canvasSize.width / xMax,
                    canvasSize.height - (y - yMin) * canvasSize.height / (yMax - yMin)
                )
            }

            val plotSize = Size(
                canvasSize.width - leftMargin,
                canvasSize.height - bottomMargin,
            )

            fun toPlotPixel(x: Float, y: Float): Pair<Float, Float> {
                return Pair(
                    leftMargin + x * plotSize.width / xMax,
                    plotSize.height - (y - yMin) * plotSize.height / (yMax - yMin)
                )
            }

            for (i in 1..9) {
                val xValue = i * xMax / 10.0f
                val yValue = yMin + i * (yMax - yMin) / 10.0f
                val (ppx, ppy) = toPlotPixel(xValue, yValue)
                textPaint.textAlign = Paint.Align.CENTER
                drawContext.canvas.nativeCanvas.drawText(
                    "%2.1f".format(xValue),
                    ppx,
                    canvasSize.height - 4.dp.toPx() ,
                    textPaint
                )
                /** placing y axis points */
                textPaint.textAlign = Paint.Align.RIGHT
                drawContext.canvas.nativeCanvas.drawText(
                    "%2d".format(yValue.toInt()),
                    leftMargin - 4.dp.toPx(),
                    ppy + fontSize / 2,
                    textPaint
                )
                drawContext.canvas.nativeCanvas.drawLine(
                    ppx, canvasSize.height - bottomMargin,
                    ppx, 0f,
                    textPaint
                )
                drawContext.canvas.nativeCanvas.drawLine(
                    leftMargin, ppy ,
                    leftMargin + plotSize.width, ppy ,
                    textPaint
                )
            }

            drawCircle(
                color = Color.Blue,
                radius = 10f,
                center = Offset(0f, canvasSize.height)
            )

            val coordinates = mutableListOf<PointF>()
            for (i in xValues.indices) {
                val (x, y) = Pair(xValues[i], yValues[i])
                val (px, py) = toPlotPixel(x, y)
                /*
                drawCircle(
                    color = Color.Red,
                    radius = 1f,
                    center = Offset(px, py)
                )*/
                coordinates.add(PointF(px, py))
            }

            val controlPoints1 = mutableListOf<PointF>()
            val controlPoints2 = mutableListOf<PointF>()

            for (i in 1 until coordinates.size) {
                controlPoints1.add(PointF((coordinates[i].x + coordinates[i - 1].x) / 2, coordinates[i - 1].y))
                controlPoints2.add(PointF((coordinates[i].x + coordinates[i - 1].x) / 2, coordinates[i].y))
            }

            val stroke = Path().apply {
                reset()
                moveTo(coordinates.first().x, coordinates.first().y)
                for (i in 0 until coordinates.size - 1) {
                    cubicTo(
                        controlPoints1[i].x,controlPoints1[i].y,
                        controlPoints2[i].x,controlPoints2[i].y,
                        coordinates[i + 1].x,coordinates[i + 1].y
                    )
                }
            }
            drawPath(
                stroke,
                color = graphAppearance.graphColor,
                style = Stroke(
                    width = graphAppearance.graphThickness,
                    cap = StrokeCap.Round
                )
            )



/*
            for (i in xValues.indices) {
                val (x, y) = Pair(xValues[i], yValues[i])
                val (px, py) = toPixel(x, y)
                /** drawing circles to indicate all the points */
                drawCircle(
                    color = Color.Red,
                    radius = 1f,
                    center = Offset(px, py)
                )
            }
 */
        }
    }
}