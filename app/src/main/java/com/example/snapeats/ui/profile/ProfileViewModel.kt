package com.example.snapeats.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
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
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfileUiState(
    val user: User? = null,
    val bmiResult: BMIResult? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val isSaveSuccess: Boolean = false,
    val showChangePassword: Boolean = false,
    val passwordChangeSuccess: Boolean = false,
    val passwordChangeError: String? = null
)

class ProfileViewModel(
    private val userDao: UserDao,
    private val bmiRecordDao: BMIRecordDao,
    private val calcBMIUseCase: CalcBMIUseCase,
    private val calcDailyCalUseCase: CalcDailyCalUseCase,
    private val userId: Int
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            userDao.getUser(userId).collect { user ->
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

    fun saveProfile(
        heightCm: Float,
        weightKg: Float,
        age: Int,
        isMale: Boolean,
        activityFactor: Float,
        goal: GoalAdjustment = GoalAdjustment.MAINTAIN
    ) {
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

                val existingUser = userDao.getUserOnce(userId)
                val user = User(
                    id = existingUser?.id ?: 0,
                    userId = userId,
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
                        userId = userId,
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

    fun changePassword(
        appUserDao: com.example.snapeats.data.local.dao.AppUserDao,
        currentPassword: String,
        newPassword: String,
        confirmPassword: String
    ) {
        if (currentPassword.isBlank() || newPassword.isBlank() || confirmPassword.isBlank()) {
            _uiState.update { it.copy(passwordChangeError = "All fields are required.") }
            return
        }
        if (newPassword.length < 6) {
            _uiState.update { it.copy(passwordChangeError = "New password must be at least 6 characters.") }
            return
        }
        if (newPassword != confirmPassword) {
            _uiState.update { it.copy(passwordChangeError = "New passwords do not match.") }
            return
        }
        viewModelScope.launch {
            val currentHash = sha256(currentPassword)
            val user = appUserDao.findById(userId)
            if (user == null || user.passwordHash != currentHash) {
                _uiState.update { it.copy(passwordChangeError = "Current password is incorrect.") }
                return@launch
            }
            val newHash = sha256(newPassword)
            appUserDao.updatePassword(userId, newHash)
            _uiState.update { it.copy(
                passwordChangeSuccess = true,
                passwordChangeError = null,
                showChangePassword = false
            )}
        }
    }

    fun toggleChangePassword() {
        _uiState.update { it.copy(
            showChangePassword = !it.showChangePassword,
            passwordChangeError = null,
            passwordChangeSuccess = false
        )}
    }

    fun clearPasswordStatus() {
        _uiState.update { it.copy(
            passwordChangeSuccess = false,
            passwordChangeError = null
        )}
    }

    private fun sha256(input: String): String {
        val bytes = java.security.MessageDigest
            .getInstance("SHA-256")
            .digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    companion object {
        val VALID_ACTIVITY_FACTORS = setOf(1.2f, 1.375f, 1.55f, 1.725f, 1.9f)

        fun factory(
            userDao: UserDao,
            bmiRecordDao: BMIRecordDao,
            calcBMIUseCase: CalcBMIUseCase,
            calcDailyCalUseCase: CalcDailyCalUseCase,
            userId: Int
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ProfileViewModel(userDao, bmiRecordDao, calcBMIUseCase, calcDailyCalUseCase, userId) as T
            }
        }
    }
}
