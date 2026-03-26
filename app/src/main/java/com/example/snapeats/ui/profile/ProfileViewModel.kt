package com.example.snapeats.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.snapeats.data.local.dao.BMIRecordDao
import com.example.snapeats.data.local.dao.UserDao
import com.example.snapeats.data.local.entity.BMIRecord
import com.example.snapeats.data.local.entity.User
import com.example.snapeats.domain.model.BMIResult
import com.example.snapeats.domain.usecase.CalcBMIUseCase
import com.example.snapeats.domain.usecase.CalcDailyCalUseCase
import com.example.snapeats.domain.usecase.GoalAdjustment
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfileUiState(
    val user: User? = null,
    val bmiResult: BMIResult? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val isSaveSuccess: Boolean = false
)

class ProfileViewModel(
    private val userDao: UserDao,
    private val bmiRecordDao: BMIRecordDao,
    private val calcBMIUseCase: CalcBMIUseCase,
    private val calcDailyCalUseCase: CalcDailyCalUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            userDao.getUser().collect { user ->
                val bmiResult = if (user != null) {
                    calcBMIUseCase(weightKg = user.weight, heightCm = user.height)
                } else null

                _uiState.update { current ->
                    current.copy(
                        user = user,
                        bmiResult = bmiResult,
                        isLoading = false
                    )
                }
            }
        }
    }

    /**
     * Validates the supplied profile values, computes BMI and daily calorie target,
     * persists a [User] record, and appends a [BMIRecord] to the history log.
     *
     * Validation ranges (inclusive):
     *   - height : 50–250 cm
     *   - weight : 20–300 kg
     *   - age    : 5–120 years
     *
     * Any validation failure sets [ProfileUiState.errorMessage] and returns early
     * without touching the database.
     */
    fun saveProfile(
        heightCm: Float,
        weightKg: Float,
        age: Int,
        isMale: Boolean,
        activityFactor: Float,
        goal: GoalAdjustment = GoalAdjustment.MAINTAIN
    ) {
        // ── Validation ──────────────────────────────────────────────────────────
        val error = when {
            heightCm < 50f || heightCm > 250f ->
                "Height must be between 50 and 250 cm."
            weightKg < 20f || weightKg > 300f ->
                "Weight must be between 20 and 300 kg."
            age < 5 || age > 120 ->
                "Age must be between 5 and 120 years."
            activityFactor !in VALID_ACTIVITY_FACTORS ->
                "Please select a valid activity level."
            else -> null
        }

        if (error != null) {
            _uiState.update { it.copy(errorMessage = error) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            try {
                val bmiResult = calcBMIUseCase(weightKg = weightKg, heightCm = heightCm)
                val dailyTarget = calcDailyCalUseCase(
                    weightKg = weightKg,
                    heightCm = heightCm,
                    age = age,
                    isMale = isMale,
                    activityFactor = activityFactor,
                    goal = goal
                )

                val user = User(
                    id = 1,
                    height = heightCm,
                    weight = weightKg,
                    age = age,
                    isMale = isMale,
                    activityFactor = activityFactor,
                    bmi = bmiResult.bmi,
                    dailyCalTarget = dailyTarget
                )

                userDao.insertOrUpdateUser(user)

                bmiRecordDao.insertRecord(
                    BMIRecord(
                        timestamp = System.currentTimeMillis(),
                        bmi = bmiResult.bmi,
                        weight = weightKg
                    )
                )

                _uiState.update { current ->
                    current.copy(
                        user = user,
                        bmiResult = bmiResult,
                        isLoading = false,
                        isSaveSuccess = true
                    )
                }
            } catch (e: Exception) {
                _uiState.update { current ->
                    current.copy(
                        isLoading = false,
                        errorMessage = "Failed to save profile: ${e.localizedMessage}"
                    )
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun clearSaveSuccess() {
        _uiState.update { it.copy(isSaveSuccess = false) }
    }

    companion object {
        /** The five valid Harris-Benedict activity multipliers. */
        val VALID_ACTIVITY_FACTORS = setOf(1.2f, 1.375f, 1.55f, 1.725f, 1.9f)
    }
}
