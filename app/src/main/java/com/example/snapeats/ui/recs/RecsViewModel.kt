package com.example.snapeats.ui.recs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.snapeats.data.local.dao.MealLogDao
import com.example.snapeats.data.local.dao.UserDao
import com.example.snapeats.data.repository.FoodRepository
import com.example.snapeats.domain.model.Food
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.time.LocalDate
import java.time.ZoneId

data class RecsUiState(
    val foods: List<Food> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val remainingCal: Int = 0
)

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class RecsViewModel(
    private val userDao: UserDao,
    private val mealLogDao: MealLogDao,
    private val foodRepository: FoodRepository,
    private val userId: Int
) : ViewModel() {

    // Raw search query typed by the user
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Selected meal type filter chip ("All", "Breakfast", "Lunch", "Dinner", "Snack")
    private val _selectedMealType = MutableStateFlow("All")
    val selectedMealType: StateFlow<String> = _selectedMealType.asStateFlow()

    // Toggle states for dietary filters
    private val _isVeg = MutableStateFlow(false)
    val isVeg: StateFlow<Boolean> = _isVeg.asStateFlow()

    private val _isVegan = MutableStateFlow(false)
    val isVegan: StateFlow<Boolean> = _isVegan.asStateFlow()

    // Pre-filled healthy Indian meal recommendations shown when search is blank
    val defaultRecommendations: StateFlow<Map<String, List<Food>>> = _searchQuery
        .map { query ->
            if (query.isBlank()) {
            linkedMapOf(
                "🌅 Breakfast" to listOf(
                    Food("Idli (2 pieces)",           116, 22f, 1f,  4f,  "", true),
                    Food("Masala Dosa",               210, 32f, 7f,  5f,  "", true),
                    Food("Upma",                      145, 25f, 4f,  4f,  "", true),
                    Food("Poha",                      130, 26f, 2f,  3f,  "", true),
                    Food("Oats Porridge",             150, 27f, 3f,  5f,  "", true)
                ),
                "☀️ Lunch" to listOf(
                    Food("Dal Tadka with Rice",       280, 52f, 4f,  12f, "", true),
                    Food("Rajma Chawal",              340, 62f, 4f,  14f, "", true),
                    Food("Chole with Roti",           310, 48f, 8f,  12f, "", true),
                    Food("Palak Paneer with Roti",    290, 34f, 12f, 13f, "", true),
                    Food("Chicken Biryani",           400, 55f, 12f, 22f, "", true)
                ),
                "🌙 Dinner" to listOf(
                    Food("Grilled Chicken with Salad", 250, 8f,  8f,  35f, "", true),
                    Food("Dal Khichdi",               260, 45f, 5f,  10f, "", true),
                    Food("Paneer Bhurji with Roti",   320, 30f, 14f, 18f, "", true),
                    Food("Vegetable Soup",             80, 12f, 2f,  3f,  "", true),
                    Food("Egg Curry with Rice",        350, 45f, 12f, 18f, "", true)
                ),
                "🍎 Snack" to listOf(
                    Food("Fruit Chaat",               120, 28f, 1f,  2f,  "", true),
                    Food("Roasted Chana",             164, 27f, 3f,  9f,  "", true),
                    Food("Sprouts Salad",             100, 18f, 1f,  7f,  "", true),
                    Food("Greek Yogurt",              100, 6f,  0f,  17f, "", true),
                    Food("Mixed Nuts (30g)",          180, 6f,  16f, 5f,  "", true)
                )
            )
        } else {
            emptyMap()
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyMap()
    )

    // Debounced query used for API calls — waits 400 ms after user stops typing
    private val debouncedQuery: StateFlow<String> = _searchQuery
        .debounce(400L)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ""
        )

    // Combined UiState exposed to the screen
    val uiState: StateFlow<RecsUiState> = combine(
        debouncedQuery,
        _selectedMealType,
        _isVeg,
        _isVegan
    ) { query, mealType, veg, vegan ->
        SearchParams(query = query, mealType = mealType, isVeg = veg, isVegan = vegan)
    }
        .flatMapLatest { params -> fetchRecommendations(params) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = RecsUiState(isLoading = true)
        )

    // -------------------------------------------------------------------------
    // Public event handlers called from the screen
    // -------------------------------------------------------------------------

    fun onSearchQueryChange(query: String) {
        _searchQuery.update { query }
    }

    fun onMealTypeSelected(mealType: String) {
        _selectedMealType.update { mealType }
    }

    fun onVegToggle() {
        _isVeg.update { current ->
            val next = !current
            // Veg and Vegan are mutually exclusive: enabling Vegan turns off Veg
            if (!next) _isVegan.update { false }
            next
        }
    }

    fun onVeganToggle() {
        _isVegan.update { current ->
            val next = !current
            // Enabling Vegan implicitly enables Veg as well
            if (next) _isVeg.update { true }
            next
        }
    }

    fun retry() {
        // Retrigger the pipeline by emitting the current query value again
        _searchQuery.update { _searchQuery.value }
    }

    // -------------------------------------------------------------------------
    // Internal data-fetching pipeline
    // -------------------------------------------------------------------------

    private data class SearchParams(
        val query: String,
        val mealType: String,
        val isVeg: Boolean,
        val isVegan: Boolean
    )

    private fun fetchRecommendations(params: SearchParams) = flow<RecsUiState> {
        // When no active search/filter, let defaultRecommendations handle the UI
        if (params.query.isBlank() && params.mealType == "All" && !params.isVeg && !params.isVegan) {
            emit(RecsUiState(isLoading = false, foods = emptyList()))
            return@flow
        }

        emit(RecsUiState(isLoading = true))

        try {
            // 1. Load user profile to get daily calorie target and BMI
            val user = userDao.getUser(userId).first()
                ?: run {
                    emit(RecsUiState(error = "Profile not set up. Please complete onboarding."))
                    return@flow
                }

            // 2. Sum today's logged calories
            val todayStart = LocalDate.now()
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
            val todayEnd = todayStart + 86_400_000L
            val todayLogs = mealLogDao.getMealLogsByDate(todayStart, todayEnd, userId).first()
            val consumed = todayLogs.sumOf { it.totalCal }

            // 3. Compute remaining calorie budget
            var remaining = user.dailyCalTarget - consumed

            // 4. Apply BMI adjustment — reduce ceiling for overweight / obese users
            if (user.bmi >= 25.0f) {
                remaining -= 200
            }

            // Clamp to a sensible minimum so the API call is always meaningful
            val maxCalories = remaining.coerceAtLeast(200)

            // 5. Build the effective search query
            val effectiveQuery = buildSearchQuery(
                rawQuery = params.query,
                mealType = params.mealType,
                isVeg = params.isVeg,
                isVegan = params.isVegan
            )

            // 6. Call FatSecret API via FoodRepository and filter client-side
            val food = foodRepository.searchFood(effectiveQuery)
            val foods = if (food.calories <= maxCalories) listOf(food) else emptyList()

            emit(
                RecsUiState(
                    foods = foods,
                    isLoading = false,
                    error = if (foods.isEmpty()) "No results found. Try a different search." else null,
                    remainingCal = remaining
                )
            )
        } catch (e: Exception) {
            emit(
                RecsUiState(
                    isLoading = false,
                    error = e.message ?: "Failed to load recommendations. Check your connection.",
                    remainingCal = 0
                )
            )
        }
    }

    /**
     * Combines the user-typed query with the active filters into a single
     * search string that FatSecret understands.
     */
    private fun buildSearchQuery(
        rawQuery: String,
        mealType: String,
        isVeg: Boolean,
        isVegan: Boolean
    ): String {
        val parts = mutableListOf<String>()

        when {
            rawQuery.isNotBlank() -> parts.add(rawQuery.trim())
            mealType != "All" -> parts.add(mealType.lowercase())
            else -> parts.add("healthy meal")
        }

        if (isVegan) parts.add("vegan")
        else if (isVeg) parts.add("vegetarian")

        return parts.joinToString(" ")
    }

    companion object {
        fun factory(
            userDao: UserDao,
            mealLogDao: MealLogDao,
            foodRepository: FoodRepository,
            userId: Int
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return RecsViewModel(userDao, mealLogDao, foodRepository, userId) as T
            }
        }
    }
}
