package com.example.snapeats.ui.home

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.snapeats.data.local.dao.MealLogDao
import com.example.snapeats.data.local.dao.UserDao
import com.example.snapeats.domain.model.Food
import com.example.snapeats.domain.model.Meal
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar

data class HomeUiState(
    val currentCal: Int = 0,
    val targetCal: Int = 2000,
    val bmiCategory: String = "",
    val bmiColor: Color = Color(0xFF4CAF50),
    val todayMeals: List<Meal> = emptyList(),
    val mealsByType: Map<String, List<Meal>> = emptyMap(),
    val isLoading: Boolean = true
)

class HomeViewModel(
    private val userDao: UserDao,
    private val mealLogDao: MealLogDao,
    private val userId: Int
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            val (startOfDay, endOfDay) = todayRange()

            combine(
                userDao.getUser(userId),
                mealLogDao.getMealLogsByDate(startOfDay, endOfDay, userId)
            ) { user, mealLogs ->
                val currentCal = mealLogs.sumOf { it.totalCal }
                val targetCal = user?.dailyCalTarget ?: 2000
                val bmi = user?.bmi ?: 0f
                val (category, color) = bmiCategoryAndColor(bmi)

                val todayMeals = mealLogs.map { log ->
                    Meal(
                        id = log.id,
                        timestamp = log.timestamp,
                        foods = parseFoodsJson(log.foodsJson),
                        totalCal = log.totalCal,
                        mealType = log.mealType
                    )
                }

                val mealsByType = todayMeals.groupBy { it.mealType }

                HomeUiState(
                    currentCal = currentCal,
                    targetCal = targetCal,
                    bmiCategory = category,
                    bmiColor = color,
                    todayMeals = todayMeals,
                    mealsByType = mealsByType,
                    isLoading = false
                )
            }.collect { state ->
                _uiState.update { state }
            }
        }
    }

    private fun todayRange(): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val startOfDay = cal.timeInMillis

        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        val endOfDay = cal.timeInMillis

        return Pair(startOfDay, endOfDay)
    }

    private fun bmiCategoryAndColor(bmi: Float): Pair<String, Color> = when {
        bmi <= 0f -> Pair("", Color(0xFF4CAF50))
        bmi < 18.5f -> Pair("Underweight", Color(0xFFFF9800))
        bmi < 25.0f -> Pair("Normal", Color(0xFF4CAF50))
        bmi < 30.0f -> Pair("Overweight", Color(0xFFFFC107))
        else -> Pair("Obese", Color(0xFFF44336))
    }

    private fun parseFoodsJson(json: String): List<Food> {
        return try {
            com.google.gson.Gson().fromJson(
                json,
                Array<Food>::class.java
            ).toList()
        } catch (e: Exception) {
            emptyList()
        }
    }
}
