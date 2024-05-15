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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.floor

private const val TAG = "com.anselm.Location.Graph"

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

data class Axis(
    val minValue: Float,
    val maxValue: Float,
    val stepValue: Float,
    var stepCount: Int,
)
private fun split(
    minValue: Float,
    maxValue: Float,
    stepRange: Array<Int>
): Axis {
    val range = maxValue - minValue
    Log.d(TAG, "range $range min $minValue max $maxValue")
    var minHate = Float.MAX_VALUE
    var output = Axis(0.0f, 0.0f, 0.0f, 0)
    for (stepCount in stepRange) {
        val stepValue = floor(range / stepCount) + 1
        val roundedMax = (floor(maxValue / stepValue) + 1) * stepValue
        var roundedMin = floor(minValue / stepValue) * stepValue
        if (minValue >= 0 && roundedMin < 0) {
            roundedMin = 0.0f
        }
        Log.d(TAG, "stepValue: $stepValue minValue: $roundedMin maxValue: $roundedMax stepCount: $stepCount ")
        listOf(5, 10).forEach { granularity ->
            val hate = stepValue / granularity - floor(stepValue / granularity)
            if (hate < minHate) {
                output = Axis(roundedMin, roundedMax, stepValue, stepCount)
                minHate = hate
            }
        }
    }
    // One last thing ...
    // Range is compute *before* rounding max and min values, so we can end up
    // short on stepCount.
    if (output.stepCount * output.stepValue < output.maxValue - output.minValue) {
        output.stepCount += 1
    }
    return output
}

private val xValueStepCount = arrayOf(6, 7, 8, 9, 10)
private val yValueStepCount = arrayOf(4, 5, 6)

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

    val xAxis = split(xValues.min(), xValues.max(), xValueStepCount)
    val yAxis = split(yValues.min(), yValues.max(), yValueStepCount)
    Box(
        modifier = modifier
            .background(Color.White)
            .padding(horizontal = 4.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(
            modifier = Modifier.fillMaxSize(),
        ) {
            val leftMargin = 2f * fontSize
            val bottomMargin = 1.5f * fontSize

            val p1 = PointF(leftMargin, size.height - bottomMargin)       // Bottom left
            val p2 = PointF(size.width, 0f)       // Top right

            drawContext.canvas.nativeCanvas.drawLines(
                floatArrayOf(
                    p1.x, p1.y, p2.x, p1.y,
                    p1.x, p1.y, p1.x, p2.y,
                ), textPaint
            )

            val plotSize = Size(
                size.width - leftMargin,
                size.height - bottomMargin,
            )

            fun toPlotPixel(x: Float, y: Float): Pair<Float, Float> {
                return Pair(
                    leftMargin + x * plotSize.width / xAxis.maxValue,
                    plotSize.height - (y - yAxis.minValue) * plotSize.height / (yAxis.maxValue - yAxis.minValue)
                )
            }

            for (i in 1..< xAxis.stepCount) {
                val xValue = i * xAxis.stepValue
                val (ppx, _) = toPlotPixel(xValue, 0f)
                textPaint.textAlign = Paint.Align.CENTER
                drawContext.canvas.nativeCanvas.drawText(
                    "%2.1f".format(xValue),
                    ppx,
                    size.height - 4.dp.toPx() ,
                    textPaint
                )
                drawContext.canvas.nativeCanvas.drawLine(
                    ppx, size.height - bottomMargin,
                    ppx, 0f,
                    textPaint
                )
            }

            for (i in 1..< yAxis.stepCount) {
                val yValue = yAxis.minValue + i * yAxis.stepValue
                val (_, ppy) = toPlotPixel(0f, yValue)
                /** placing y axis points */
                textPaint.textAlign = Paint.Align.RIGHT
                drawContext.canvas.nativeCanvas.drawText(
                    "%2d".format(yValue.toInt()),
                    leftMargin - 4.dp.toPx(),
                    ppy + fontSize / 2,
                    textPaint
                )
                drawContext.canvas.nativeCanvas.drawLine(
                    leftMargin, ppy ,
                    leftMargin + plotSize.width, ppy ,
                    textPaint
                )
            }

            val coordinates = mutableListOf<PointF>()
            for (i in xValues.indices) {
                val (x, y) = Pair(xValues[i], yValues[i])
                val (px, py) = toPlotPixel(x, y)
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

            if (graphAppearance.isColorAreaUnderChart){
                val fillPath = android.graphics.Path(stroke.asAndroidPath())
                    .asComposePath()
                    .apply {
                        lineTo(leftMargin * xValues.last(), size.height - bottomMargin)
                        lineTo(leftMargin, size.height - bottomMargin)
                        close()
                    }
                drawPath(
                    fillPath,
                    brush = Brush.verticalGradient(
                        listOf(
                            graphAppearance.colorAreaUnderChart,
                            Color.Transparent,
                        ),
                        endY = size.height - bottomMargin
                    ),
                )
            }

            drawPath(
                stroke,
                color = graphAppearance.graphColor,
                style = Stroke(
                    width = graphAppearance.graphThickness,
                    cap = StrokeCap.Round
                )
            )
        }
    }
}