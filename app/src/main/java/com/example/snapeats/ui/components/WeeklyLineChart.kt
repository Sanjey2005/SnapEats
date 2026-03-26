package com.example.snapeats.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

// ---------------------------------------------------------------------------
// WeeklyLineChart
// ---------------------------------------------------------------------------

/**
 * Draws a 7-day calorie intake line chart using the Compose [Canvas] API.
 *
 * @param weeklyTotals  Calorie totals for the past 7 days.
 *                      Index 0 = six days ago, index 6 = today.
 * @param dailyTarget   The user's daily calorie target — drawn as a dashed
 *                      horizontal reference line.
 */
@Composable
fun WeeklyLineChart(
    weeklyTotals: List<Int>,
    dailyTarget: Int,
    modifier: Modifier = Modifier
) {
    // Ensure we always have exactly 7 entries
    val totals = remember(weeklyTotals) {
        when {
            weeklyTotals.size >= 7 -> weeklyTotals.takeLast(7)
            else -> List(7 - weeklyTotals.size) { 0 } + weeklyTotals
        }
    }

    // Day-of-week labels for the x-axis (Mon, Tue, …)
    val dayLabels = remember {
        val today = LocalDate.now()
        (6 downTo 0).map { daysAgo ->
            today.minusDays(daysAgo.toLong())
                .dayOfWeek
                .getDisplayName(TextStyle.SHORT, Locale.getDefault())
        }
    }

    // Resolve Material You colours outside the Canvas lambda (no @Composable inside Canvas)
    val gridColor       = MaterialTheme.colorScheme.outlineVariant
    val lineColor       = MaterialTheme.colorScheme.primary
    val targetColor     = MaterialTheme.colorScheme.tertiary
    val aboveColor      = MaterialTheme.colorScheme.error
    val belowColor      = MaterialTheme.colorScheme.primary
    val labelColor      = MaterialTheme.colorScheme.onSurfaceVariant

    val density = LocalDensity.current
    val labelTextSizePx = with(density) { 11.sp.toPx() }

    // Reserve bottom space for x-axis labels
    val bottomPaddingDp = 28.dp
    val horizontalPaddingDp = 16.dp

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
            .padding(horizontal = horizontalPaddingDp)
    ) {
        Canvas(modifier = Modifier.fillMaxWidth().height(200.dp)) {

            val bottomPadding = with(density) { bottomPaddingDp.toPx() }
            val topPadding    = 16f   // small top breathing room
            val chartHeight   = size.height - bottomPadding - topPadding
            val chartWidth    = size.width

            // The y-axis domain — at least cover the target, add 20% headroom
            val maxVal = maxOf(totals.maxOrNull() ?: 0, dailyTarget) * 1.20f
            val minVal = 0f

            fun yForValue(value: Int): Float {
                val ratio = (value - minVal) / (maxVal - minVal)
                // Invert: high values → small y (top of canvas)
                return topPadding + chartHeight * (1f - ratio)
            }

            fun xForIndex(index: Int): Float {
                return if (totals.size <= 1) chartWidth / 2f
                else chartWidth * index / (totals.size - 1).toFloat()
            }

            // ----------------------------------------------------------------
            // 1. Vertical grid lines (7 lines, one per day)
            // ----------------------------------------------------------------
            val gridStroke = Stroke(width = 1.dp.toPx())
            for (i in totals.indices) {
                val x = xForIndex(i)
                drawLine(
                    color = gridColor,
                    start = Offset(x, topPadding),
                    end   = Offset(x, topPadding + chartHeight),
                    strokeWidth = 1.dp.toPx()
                )
            }

            // Horizontal baseline
            drawLine(
                color = gridColor,
                start = Offset(0f, topPadding + chartHeight),
                end   = Offset(chartWidth, topPadding + chartHeight),
                strokeWidth = 1.dp.toPx()
            )

            // ----------------------------------------------------------------
            // 2. Dashed target reference line
            // ----------------------------------------------------------------
            val targetY = yForValue(dailyTarget)
            val dashEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
            drawLine(
                color       = targetColor,
                start       = Offset(0f, targetY),
                end         = Offset(chartWidth, targetY),
                strokeWidth = 2.dp.toPx(),
                pathEffect  = dashEffect
            )

            // ----------------------------------------------------------------
            // 3. Solid polyline connecting the 7 data points
            // ----------------------------------------------------------------
            for (i in 0 until totals.size - 1) {
                drawLine(
                    color       = lineColor,
                    start       = Offset(xForIndex(i), yForValue(totals[i])),
                    end         = Offset(xForIndex(i + 1), yForValue(totals[i + 1])),
                    strokeWidth = 2.5.dp.toPx(),
                    cap         = StrokeCap.Round
                )
            }

            // ----------------------------------------------------------------
            // 4. Circles at each data point — red if above target, primary otherwise
            // ----------------------------------------------------------------
            for (i in totals.indices) {
                val cx = xForIndex(i)
                val cy = yForValue(totals[i])
                val dotColor = if (totals[i] > dailyTarget) aboveColor else belowColor

                // White fill under the dot so it stands out against the line
                drawCircle(
                    color  = Color.White,
                    radius = 5.dp.toPx(),
                    center = Offset(cx, cy)
                )
                drawCircle(
                    color  = dotColor,
                    radius = 4.dp.toPx(),
                    center = Offset(cx, cy)
                )
            }

            // ----------------------------------------------------------------
            // 5. X-axis day labels
            // ----------------------------------------------------------------
            val paint = android.graphics.Paint().apply {
                color     = labelColor.toArgb()
                textSize  = labelTextSizePx
                textAlign = android.graphics.Paint.Align.CENTER
                isAntiAlias = true
            }
            for (i in dayLabels.indices) {
                drawContext.canvas.nativeCanvas.drawText(
                    dayLabels[i],
                    xForIndex(i),
                    size.height - 6f,
                    paint
                )
            }

            // ----------------------------------------------------------------
            // 6. Target label at the right end of the dashed line
            // ----------------------------------------------------------------
            val targetLabelPaint = android.graphics.Paint().apply {
                color     = targetColor.toArgb()
                textSize  = labelTextSizePx
                textAlign = android.graphics.Paint.Align.RIGHT
                isAntiAlias = true
            }
            drawContext.canvas.nativeCanvas.drawText(
                "Target",
                chartWidth - 4f,
                targetY - 6f,
                targetLabelPaint
            )
        }
    }
}
