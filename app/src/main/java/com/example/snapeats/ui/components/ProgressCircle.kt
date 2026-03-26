package com.example.snapeats.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val ColorGreen = Color(0xFF4CAF50)
private val ColorAmber = Color(0xFFFFC107)
private val ColorRed = Color(0xFFF44336)
private val ColorTrack = Color(0xFFE0E0E0)

@Composable
fun ProgressCircle(
    currentCal: Int,
    targetCal: Int,
    modifier: Modifier = Modifier,
    size: Dp = 220.dp
) {
    val ratio = if (targetCal > 0) currentCal.toFloat() / targetCal.toFloat() else 0f
    val targetSweep = (ratio * 360f).coerceAtMost(360f)

    val animatedSweep by animateFloatAsState(
        targetValue = targetSweep,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "progressSweep"
    )

    val arcColor = when {
        ratio < 0.8f -> ColorGreen
        ratio <= 1.0f -> ColorAmber
        else -> ColorRed
    }

    val strokeWidth = size.value * 0.08f

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(size)
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val stroke = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            val inset = strokeWidth / 2f
            val arcSize = Size(this.size.width - strokeWidth, this.size.height - strokeWidth)
            val topLeft = Offset(inset, inset)

            // Background track arc
            drawArc(
                color = ColorTrack,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = stroke
            )

            // Foreground progress arc
            if (animatedSweep > 0f) {
                drawArc(
                    color = arcColor,
                    startAngle = -90f,
                    sweepAngle = animatedSweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = stroke
                )
            }
        }

        // Centre text
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = buildAnnotatedString {
                    withStyle(
                        SpanStyle(
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Bold,
                            color = arcColor
                        )
                    ) {
                        append("$currentCal\n")
                    }
                    withStyle(
                        SpanStyle(
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Normal,
                            color = Color.Gray
                        )
                    ) {
                        append("/ $targetCal kcal")
                    }
                },
                textAlign = TextAlign.Center
            )
        }
    }
}
