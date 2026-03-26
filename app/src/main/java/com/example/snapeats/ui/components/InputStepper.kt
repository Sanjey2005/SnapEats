package com.example.snapeats.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * A three-part stepper row for incrementing/decrementing a numeric value.
 *
 * @param value         The current numeric value.
 * @param onValueChange Callback invoked with the new value after each tap.
 * @param min           Minimum allowed value — minus button disables at this bound.
 * @param max           Maximum allowed value — plus button disables at this bound.
 * @param step          Increment/decrement size per tap.
 * @param label         Descriptive label shown above the stepper row.
 * @param unit          Optional unit suffix shown after the value (e.g. "cm", "kg").
 * @param modifier      Optional [Modifier] applied to the outer [Column].
 */
@Composable
fun InputStepper(
    value: Float,
    onValueChange: (Float) -> Unit,
    min: Float,
    max: Float,
    step: Float,
    label: String,
    unit: String = "",
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    // Round to avoid floating-point display drift (e.g. 70.00000001)
    val displayValue = (value * 10).roundToInt() / 10f
    val displayText = if (displayValue == displayValue.toInt().toFloat()) {
        "${displayValue.toInt()}$unit"
    } else {
        "$displayValue$unit"
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            FilledTonalIconButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    val next = (value - step).coerceAtLeast(min)
                    // Round to avoid floating-point drift accumulation
                    onValueChange((next * 10).roundToInt() / 10f)
                },
                enabled = value > min,
                shape = CircleShape,
                modifier = Modifier.size(40.dp),
                colors = IconButtonDefaults.filledTonalIconButtonColors()
            ) {
                Icon(
                    imageVector = Icons.Filled.Remove,
                    contentDescription = "Decrease $label"
                )
            }

            Spacer(modifier = Modifier.width(24.dp))

            Text(
                text = displayText,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.width(88.dp)
            )

            Spacer(modifier = Modifier.width(24.dp))

            FilledTonalIconButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    val next = (value + step).coerceAtMost(max)
                    onValueChange((next * 10).roundToInt() / 10f)
                },
                enabled = value < max,
                shape = CircleShape,
                modifier = Modifier.size(40.dp),
                colors = IconButtonDefaults.filledTonalIconButtonColors()
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "Increase $label"
                )
            }
        }
    }
}
