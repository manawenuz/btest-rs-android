package com.btestrs.android.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import com.btestrs.android.BtestResult

@Composable
fun SpeedGraph(
    intervals: List<BtestResult>,
    modifier: Modifier = Modifier
) {
    val txColor = TxBlue
    val rxColor = RxGreen

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
    ) {
        if (intervals.isEmpty()) {
            drawEmptyState()
            return@Canvas
        }

        val txPoints = mutableMapOf<Int, Double>()
        val rxPoints = mutableMapOf<Int, Double>()

        for (result in intervals) {
            when (result.direction) {
                "TX" -> txPoints[result.intervalSec] = result.speedMbps
                "RX" -> rxPoints[result.intervalSec] = result.speedMbps
            }
        }

        val allSpeeds = (txPoints.values + rxPoints.values)
        val maxSpeed = (allSpeeds.maxOrNull() ?: 100.0).coerceAtLeast(10.0)
        val maxTime = (txPoints.keys + rxPoints.keys).maxOrNull() ?: 1

        val padding = 48f
        val graphWidth = size.width - padding * 2
        val graphHeight = size.height - padding * 2

        // Draw grid lines and labels
        drawGridLines(padding, graphWidth, graphHeight, maxSpeed, maxTime)

        // Draw TX line
        if (txPoints.isNotEmpty()) {
            drawSpeedLine(txPoints, maxSpeed, maxTime, padding, graphWidth, graphHeight, txColor)
        }

        // Draw RX line
        if (rxPoints.isNotEmpty()) {
            drawSpeedLine(rxPoints, maxSpeed, maxTime, padding, graphWidth, graphHeight, rxColor)
        }
    }
}

private fun DrawScope.drawEmptyState() {
    val paint = android.graphics.Paint().apply {
        color = android.graphics.Color.GRAY
        textSize = 36f
        textAlign = android.graphics.Paint.Align.CENTER
    }
    drawContext.canvas.nativeCanvas.drawText(
        "Waiting for data...",
        size.width / 2,
        size.height / 2,
        paint
    )
}

private fun DrawScope.drawGridLines(
    padding: Float,
    graphWidth: Float,
    graphHeight: Float,
    maxSpeed: Double,
    maxTime: Int
) {
    val gridColor = Color.Gray.copy(alpha = 0.3f)
    val paint = android.graphics.Paint().apply {
        color = android.graphics.Color.GRAY
        textSize = 24f
        textAlign = android.graphics.Paint.Align.RIGHT
    }
    val timePaint = android.graphics.Paint().apply {
        color = android.graphics.Color.GRAY
        textSize = 24f
        textAlign = android.graphics.Paint.Align.CENTER
    }

    // Horizontal grid lines (speed)
    val speedSteps = 4
    for (i in 0..speedSteps) {
        val y = padding + graphHeight - (graphHeight * i / speedSteps)
        drawLine(gridColor, Offset(padding, y), Offset(padding + graphWidth, y), strokeWidth = 1f)
        val speedLabel = String.format("%.0f", maxSpeed * i / speedSteps)
        drawContext.canvas.nativeCanvas.drawText(speedLabel, padding - 8, y + 8, paint)
    }

    // Vertical grid lines (time)
    val timeSteps = minOf(maxTime, 6)
    if (timeSteps > 0) {
        for (i in 0..timeSteps) {
            val x = padding + graphWidth * i / timeSteps
            drawLine(gridColor, Offset(x, padding), Offset(x, padding + graphHeight), strokeWidth = 1f)
            val timeLabel = "${maxTime * i / timeSteps}s"
            drawContext.canvas.nativeCanvas.drawText(timeLabel, x, padding + graphHeight + 28, timePaint)
        }
    }
}

private fun DrawScope.drawSpeedLine(
    points: Map<Int, Double>,
    maxSpeed: Double,
    maxTime: Int,
    padding: Float,
    graphWidth: Float,
    graphHeight: Float,
    color: Color
) {
    val sortedPoints = points.toSortedMap()
    val path = Path()
    var first = true

    for ((time, speed) in sortedPoints) {
        val x = padding + (time.toFloat() / maxTime) * graphWidth
        val y = padding + graphHeight - ((speed / maxSpeed) * graphHeight).toFloat()

        if (first) {
            path.moveTo(x, y)
            first = false
        } else {
            path.lineTo(x, y)
        }

        // Draw dot at each data point
        drawCircle(color, radius = 4f, center = Offset(x, y))
    }

    drawPath(path, color, style = Stroke(width = 3f))
}
