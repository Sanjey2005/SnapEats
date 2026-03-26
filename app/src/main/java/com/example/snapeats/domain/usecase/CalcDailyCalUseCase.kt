package com.example.snapeats.domain.usecase

import kotlin.math.roundToInt

/**
 * Calculates a personalised daily calorie target using the Harris-Benedict
 * revised BMR formula multiplied by an activity factor.
 *
 * An optional [GoalAdjustment] can shift the result by ±500 kcal to support
 * weight-loss or weight-gain goals.
 */
class CalcDailyCalUseCase {

    /**
     * @param weightKg       Body weight in kilograms.
     * @param heightCm       Standing height in centimetres.
     * @param age            Age in whole years.
     * @param isMale         True for male, false for female (used to select the
     *                       correct Harris-Benedict coefficients).
     * @param activityFactor One of the five WHO multipliers:
     *                       1.2 / 1.375 / 1.55 / 1.725 / 1.9.
     * @param goal           [GoalAdjustment] to apply after the base calculation.
     *                       Defaults to [GoalAdjustment.MAINTAIN].
     * @return Daily calorie target as a whole number (kcal).
     */
    operator fun invoke(
        weightKg: Float,
        heightCm: Float,
        age: Int,
        isMale: Boolean,
        activityFactor: Float,
        goal: GoalAdjustment = GoalAdjustment.MAINTAIN
    ): Int {
        val bmr = if (isMale) {
            // Harris-Benedict (revised Roza & Shizgal, 1984) — male
            88.362f +
                    (13.397f * weightKg) +
                    (4.799f  * heightCm) -
                    (5.677f  * age)
        } else {
            // Harris-Benedict (revised Roza & Shizgal, 1984) — female
            447.593f +
                    (9.247f  * weightKg) +
                    (3.098f  * heightCm) -
                    (4.330f  * age)
        }

        val maintenance = bmr * activityFactor
        val adjusted    = maintenance + goal.kcalDelta

        return adjusted.roundToInt().coerceAtLeast(1200) // never below safe minimum
    }
}

/**
 * Represents a user's calorie goal.
 *
 * @property kcalDelta The number of kilocalories added to (positive) or subtracted
 *                     from (negative) the maintenance target.
 */
enum class GoalAdjustment(val kcalDelta: Int) {
    /** Lose ~0.5 kg per week — 500 kcal deficit. */
    LOSE_WEIGHT(kcalDelta = -500),

    /** Maintain current body weight — no adjustment. */
    MAINTAIN(kcalDelta = 0),

    /** Gain ~0.5 kg per week — 500 kcal surplus. */
    GAIN_WEIGHT(kcalDelta = +500)
}
