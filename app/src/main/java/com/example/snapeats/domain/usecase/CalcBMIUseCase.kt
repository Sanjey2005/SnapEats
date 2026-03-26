package com.example.snapeats.domain.usecase

import androidx.compose.ui.graphics.Color
import com.example.snapeats.domain.model.BMIResult

class CalcBMIUseCase {

    /**
     * Computes BMI from weight and height, returning a [BMIResult] with the
     * rounded value, WHO category label, and the colour used throughout the app.
     *
     * @param weightKg  Body weight in kilograms (valid range: 20–300 kg).
     * @param heightCm  Standing height in centimetres (valid range: 50–250 cm).
     * @return [BMIResult] containing the computed BMI, category string, and display colour.
     */
    operator fun invoke(weightKg: Float, heightCm: Float): BMIResult {
        val heightM = heightCm / 100f
        val bmi = weightKg / (heightM * heightM)
        val rounded = (bmi * 100).toInt() / 100f  // truncate to 2 decimal places

        val (category, color) = when {
            rounded < 18.5f  -> "Underweight" to Color(0xFFFFA500)  // Orange
            rounded < 25.0f  -> "Normal"      to Color(0xFF4CAF50)  // Green
            rounded < 30.0f  -> "Overweight"  to Color(0xFFFFC107)  // Amber
            else             -> "Obese"       to Color(0xFFF44336)  // Red
        }

        return BMIResult(bmi = rounded, category = category, color = color)
    }
}
