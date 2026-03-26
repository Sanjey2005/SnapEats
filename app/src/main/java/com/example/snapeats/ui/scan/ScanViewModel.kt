package com.example.snapeats.ui.scan

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.snapeats.data.local.dao.MealLogDao
import com.example.snapeats.data.local.entity.MealLog
import com.example.snapeats.data.repository.FoodRepository
import com.example.snapeats.domain.model.Food
import com.example.snapeats.util.BitmapUtils
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

// ---------------------------------------------------------------------------
// Sealed UI state
// ---------------------------------------------------------------------------

sealed class ScanState {
    object Idle : ScanState()
    object Scanning : ScanState()
    data class Results(val foods: List<Food>) : ScanState()
    data class Error(val message: String) : ScanState()
}

// ---------------------------------------------------------------------------
// ViewModel
// ---------------------------------------------------------------------------

class ScanViewModel(
    private val foodRepository: FoodRepository,
    private val mealLogDao: MealLogDao
) : ViewModel() {

    private val _scanState = MutableStateFlow<ScanState>(ScanState.Idle)
    val scanState: StateFlow<ScanState> = _scanState.asStateFlow()

    // Use Image Labeling for better descriptive tags (e.g., "Curry", "Bread", "Rice")
    private val imageLabeler by lazy {
        val options = ImageLabelerOptions.Builder()
            .setConfidenceThreshold(0.6f) // Higher threshold for accuracy
            .build()
        ImageLabeling.getClient(options)
    }

    fun processImage(bitmap: Bitmap) {
        _scanState.value = ScanState.Scanning

        viewModelScope.launch(Dispatchers.Default) {
            try {
                val scaled = BitmapUtils.scaleBitmap(bitmap)
                val inputImage = InputImage.fromBitmap(scaled, 0)
                
                // Get labels for the entire image
                val labels = imageLabeler.process(inputImage).await()

                Log.d(TAG, "ML Kit returned ${labels.size} labels.")

                // Filter out generic labels like "Food", "Tableware", "Dishware", etc.
                val filteredLabels = labels.filter { label ->
                    val text = label.text.lowercase()
                    val isGeneric = text in listOf("food", "tableware", "dishware", "plate", "dish", "cuisine", "recipe", "ingredient")
                    !isGeneric && label.confidence > 0.6f
                }

                val foodLabels = filteredLabels.map { it.text }.distinct()

                Log.d(TAG, "Specific food labels detected: $foodLabels")

                if (foodLabels.isEmpty()) {
                    // Fallback: If no specific labels, but we have "Food", use the top label
                    val hasFood = labels.any { it.text.lowercase() == "food" }
                    if (hasFood && labels.isNotEmpty()) {
                        // Use the most confident label that isn't just "Food" if possible
                        val bestLabel = labels.firstOrNull { it.text.lowercase() != "food" }?.text ?: "Meal"
                        processLabels(listOf(bestLabel), bitmap, scaled)
                    } else {
                        _scanState.value = ScanState.Error(
                            "No food detected. Try moving closer or improving the lighting."
                        )
                    }
                    return@launch
                }

                processLabels(foodLabels, bitmap, scaled)

            } catch (e: Exception) {
                Log.e(TAG, "processImage failed: ${e.message}", e)
                _scanState.value = ScanState.Error(
                    "Something went wrong during scanning. Please try again."
                )
            }
        }
    }

    private suspend fun processLabels(foodLabels: List<String>, original: Bitmap, scaled: Bitmap) {
        // Look up nutritional data for every label
        val foods: List<Food> = foodLabels.take(3).map { label ->
            foodRepository.searchFood(label)
        }

        if (scaled !== original) {
            scaled.recycle()
        }

        _scanState.value = ScanState.Results(foods)
    }

    fun addFoodsToLog(foods: List<Food>) {
        if (foods.isEmpty()) {
            _scanState.value = ScanState.Idle
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val totalCal = foods.sumOf { it.calories }
                val foodsJson = buildFoodsJson(foods)

                val mealLog = MealLog(
                    timestamp = System.currentTimeMillis(),
                    foodsJson = foodsJson,
                    totalCal = totalCal
                )

                mealLogDao.insertMealLog(mealLog)
                Log.d(TAG, "MealLog saved — $totalCal kcal, ${foods.size} item(s).")

            } catch (e: Exception) {
                Log.e(TAG, "Failed to save MealLog: ${e.message}", e)
            } finally {
                _scanState.value = ScanState.Idle
            }
        }
    }

    fun resetState() {
        _scanState.value = ScanState.Idle
    }

    private fun buildFoodsJson(foods: List<Food>): String {
        val items = foods.joinToString(",") { f ->
            """{"name":"${f.name.escapeJson()}","calories":${f.calories},"carbs":${f.carbs},"fat":${f.fat},"protein":${f.protein},"thumbnailUrl":"${f.thumbnailUrl.escapeJson()}","isOffline":${f.isOffline}}"""
        }
        return "[$items]"
    }

    private fun String.escapeJson(): String =
        replace("\\", "\\\\").replace("\"", "\\\"")

    override fun onCleared() {
        super.onCleared()
        imageLabeler.close()
    }

    companion object {
        private const val TAG = "ScanViewModel"

        fun factory(
            foodRepository: FoodRepository,
            mealLogDao: MealLogDao
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return ScanViewModel(foodRepository, mealLogDao) as T
            }
        }
    }
}
