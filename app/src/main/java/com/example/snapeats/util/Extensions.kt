package com.example.snapeats.util

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Rounds a [Float] to one decimal place.
 *
 * Example: 22.857f.roundTo1Decimal() → 22.9f
 */
fun Float.roundTo1Decimal(): Float {
    return (this * 10f).roundToInt() / 10f
}

/**
 * Converts a Unix timestamp in milliseconds to a human-readable date string.
 *
 * Format: "dd MMM yyyy" (e.g. "25 Mar 2026").
 * Uses [Locale.getDefault] so the month abbreviation respects the device locale.
 *
 * Example: 1742860800000L.toFormattedDate() → "25 Mar 2026"
 */
fun Long.toFormattedDate(): String {
    val formatter = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    return formatter.format(Date(this))
}

/**
 * Formats a calorie count with thousands separators and a "kcal" suffix.
 *
 * Uses [NumberFormat.getIntegerInstance] with [Locale.getDefault] so
 * separators match the device's regional setting (comma in en-US, period
 * in many European locales, etc.).
 *
 * Example: 1250.toFormattedCalories() → "1,250 kcal"
 * Example:  980.toFormattedCalories() → "980 kcal"
 */
fun Int.toFormattedCalories(): String {
    val numberFormat = NumberFormat.getIntegerInstance(Locale.getDefault())
    return "${numberFormat.format(this)} kcal"
}
