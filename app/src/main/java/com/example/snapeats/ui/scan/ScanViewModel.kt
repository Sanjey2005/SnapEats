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
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
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

    private val objectDetector by lazy {
        val options = ObjectDetectorOptions.Builder()
            .setDetectorMode(ObjectDetectorOptions.SINGLE_IMAGE_MODE)
            .enableMultipleObjects()
            .enableClassification()
            .build()
        ObjectDetection.getClient(options)
    }

    fun processImage(bitmap: Bitmap) {
        _scanState.value = ScanState.Scanning

        viewModelScope.launch(Dispatchers.Default) {
            try {
                val scaled = BitmapUtils.scaleBitmap(bitmap)
                val inputImage = InputImage.fromBitmap(scaled, 0)
                val detectedObjects = objectDetector.process(inputImage).await()

                Log.d(TAG, "ML Kit returned ${detectedObjects.size} object(s).")

                val foodLabels: List<String> = detectedObjects
                    .flatMap { detectedObject ->
                        detectedObject.labels.filter { label ->
                            label.text.contains("Food", ignoreCase = true) &&
                                    label.confidence > 0.5f
                        }
                    }
                    .map { it.text }
                    .distinct()

                Log.d(TAG, "Food labels detected: $foodLabels")

                if (foodLabels.isEmpty()) {
                    _scanState.value = ScanState.Error(
                        "No food detected. Try moving closer or improving the lighting."
                    )
                    return@launch
                }

                val foods: List<Food> = foodLabels.map { label ->
                    foodRepository.searchFood(label)
                }

                if (scaled !== bitmap) {
                    scaled.recycle()
                }

                _scanState.value = ScanState.Results(foods)

            } catch (e: Exception) {
                Log.e(TAG, "processImage failed: ${e.message}", e)
                _scanState.value = ScanState.Error(
                    "Something went wrong during scanning. Please try again."
                )
            }
        }
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
        objectDetector.close()
    }

    companion object {
        private const val TAG = "ScanViewModel"

        fun factory(
            foodRepository: FoodRepository,
            mealLogDao: MealLogDao
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ScanViewModel(foodRepository, mealLogDao) as T
            }
        }
    }
}
