package com.example.snapeats.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ── Palette constants (used here and in FoodCard macro bar) ──────────────────
val MacroColorCarbs   = Color(0xFF2196F3)   // blue
val MacroColorFat     = Color(0xFFFFC107)   // amber
val MacroColorProtein = Color(0xFF4CAF50)   // green

private data class MacroSlice(
    val label: String,
    val grams: Float,
    val color: Color,
    val animationDelayMs: Int
)

/**
 * Circular pie chart showing the carbohydrate / fat / protein breakdown.
 *
 * Each slice sweeps in from the 12 o'clock position with a staggered delay so
 * they appear one after the other on first composition. A three-item legend row
 * sits below the chart with a colour dot, macro label, and gram value for each.
 *
 * @param carbsG   Carbohydrate grams.
 * @param fatG     Fat grams.
 * @param proteinG Protein grams.
 * @param size     Diameter of the chart canvas. Defaults to 180.dp.
 * @param modifier Optional [Modifier] applied to the outer [Column].
 */
@Composable
fun MacroPieChart(
    carbsG: Float,
    fatG: Float,
    proteinG: Float,
    size: Dp = 180.dp,
    modifier: Modifier = Modifier
) {
    val slices = remember(carbsG, fatG, proteinG) {
        listOf(
            MacroSlice("Carbs",   carbsG,   MacroColorCarbs,   animationDelayMs = 0),
            MacroSlice("Fat",     fatG,     MacroColorFat,     animationDelayMs = 200),
            MacroSlice("Protein", proteinG, MacroColorProtein, animationDelayMs = 400)
        )
    }

    val total = (carbsG + fatG + proteinG).coerceAtLeast(1f)   // guard against zero division

    // One Animatable per slice — each goes from 0f → its proportion of 360°
    val sweepAngles = slices.map { slice ->
        remember(slice.grams, total) { Animatable(0f) }
    }

    // Kick off staggered animations whenever the gram values change
    LaunchedEffect(carbsG, fatG, proteinG) {
        slices.forEachIndexed { index, slice ->
            val targetSweep = (slice.grams / total) * 360f
            launch {
                delay(slice.animationDelayMs.toLong())
                sweepAngles[index].animateTo(
                    targetValue = targetSweep,
                    animationSpec = tween(durationMillis = 600)
                )
            }
        }
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ── Pie chart canvas ─────────────────────────────────────────────────
        Box(contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.size(size)) {
                val canvasSize = this.size
                var startAngle = -90f   // 12 o'clock

                slices.forEachIndexed { index, slice ->
                    val sweep = sweepAngles[index].value
                    if (sweep > 0f) {
                        drawPieSlice(
                            color = slice.color,
                            startAngle = startAngle,
                            sweepAngle = sweep,
                            canvasSize = canvasSize
                        )
                        startAngle += sweep
                    }
                }

                // Draw a small white circle in the centre for a donut effect
                val holeRadius = canvasSize.minDimension * 0.28f
                drawCircle(
                    color = Color.White,
                    radius = holeRadius
                )
            }
        }

        // ── Legend ───────────────────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            slices.forEach { slice ->
                MacroLegendItem(
                    color = slice.color,
                    label = slice.label,
                    grams = slice.grams
                )
            }
        }
    }
}

// ── Private helpers ───────────────────────────────────────────────────────────

private fun DrawScope.drawPieSlice(
    color: Color,
    startAngle: Float,
    sweepAngle: Float,
    canvasSize: Size
) {
    val strokePadding = 0f
    drawArc(
        color = color,
        startAngle = startAngle,
        sweepAngle = sweepAngle,
        useCenter = true,
        topLeft = androidx.compose.ui.geometry.Offset(strokePadding, strokePadding),
        size = Size(
            canvasSize.width - strokePadding * 2,
            canvasSize.height - strokePadding * 2
        )
    )
}

@Composable
private fun MacroLegendItem(
    color: Color,
    label: String,
    grams: Float
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Colour dot
        Canvas(modifier = Modifier.size(10.dp)) {
            drawCircle(color = color)
        }

        Spacer(modifier = Modifier.width(2.dp))

        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "${grams.toInt()}g",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
