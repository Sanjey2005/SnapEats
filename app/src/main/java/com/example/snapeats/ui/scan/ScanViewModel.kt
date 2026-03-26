package com.example.snapeats.ui.scan

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.snapeats.BuildConfig
import com.example.snapeats.data.local.dao.MealLogDao
import com.example.snapeats.data.local.entity.MealLog
import com.example.snapeats.data.repository.FoodRepository
import com.example.snapeats.domain.model.Food
import com.example.snapeats.util.BitmapUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

sealed class ScanState {
    object Idle : ScanState()
    object Scanning : ScanState()
    data class Results(val foods: List<Food>) : ScanState()
    data class Error(val message: String) : ScanState()
}

class ScanViewModel(
    private val foodRepository: FoodRepository,
    private val mealLogDao: MealLogDao
) : ViewModel() {

    private val _scanState = MutableStateFlow<ScanState>(ScanState.Idle)
    val scanState: StateFlow<ScanState> = _scanState.asStateFlow()

    fun processImage(bitmap: Bitmap) {
        _scanState.value = ScanState.Scanning
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val scaled = BitmapUtils.scaleBitmap(bitmap)
                val foodLabels = analyzeImageWithGroq(scaled)
                
                if (foodLabels.isEmpty()) {
                    _scanState.value = ScanState.Error(
                        "Could not identify food. Please try again with better lighting."
                    )
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

    private suspend fun analyzeImageWithGroq(bitmap: Bitmap): List<String> {
        return try {
            val base64Image = BitmapUtils.toBase64(bitmap)
            val client = OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .build()

            val jsonRequest = JSONObject().apply {
                put("model", "meta-llama/llama-4-scout-17b-16e-instruct")
                val messages = JSONArray().apply {
                    val userMessage = JSONObject().apply {
                        put("role", "user")
                        val content = JSONArray().apply {
                            put(JSONObject().apply {
                                put("type", "text")
                                put("text", """
                                    You are a world-class food recognition AI specialized in Indian and global cuisine. 
                                    Your job is to identify every single food item visible in this image with maximum 
                                    precision. Follow these rules strictly:

                                    IDENTIFICATION RULES:
                                    1. Always identify the MAIN dish first (e.g. Dosa, Idli, Biryani, Roti, Naan, Rice)
                                    2. Then identify every side dish, curry, chutney, sambar, raita, pickle, papad
                                    3. Never skip any item — even small bowls, dips, or garnishes count
                                    4. Never group multiple items into one (e.g. do not say "curry" if you can see 
                                       it is specifically "Palak Paneer" or "Dal Makhani")
                                    5. Never hallucinate items that are not visibly present in the image
                                    6. Never duplicate items — if you already listed Chicken Curry, do not also list Chicken

                                    NAMING RULES:
                                    1. Always use the most specific Indian name possible:
                                       - White crepe = Plain Dosa (NOT pancake, NOT flatbread)
                                       - White coconut-based side = Coconut Chutney (NOT Butter Sauce, NOT dip)
                                       - Orange lentil soup = Sambar (NOT soup, NOT curry)
                                       - Dark green curry = Palak Paneer or Saag (NOT green curry)
                                       - Red tomato-based thin soup = Rasam (NOT soup)
                                       - White yogurt-based side = Raita (NOT yogurt dip)
                                       - Layered rice dish with meat = Biryani (NOT fried rice)
                                    2. If you are not sure between two Indian names, pick the most visually accurate one
                                    3. Never use Western food names for clearly Indian dishes

                                    OUTPUT RULES:
                                    1. Return ONLY a comma-separated list
                                    2. No numbering, no bullet points, no explanations, no extra text
                                    3. No full stops or line breaks
                                    4. Maximum 8 items
                                    5. Example good output: Plain Dosa, Coconut Chutney, Sambar
                                    6. Example bad output: Here are the foods I can see: 1. Dosa 2. Chutney

                                    Now identify all food items in this image.
                                """.trimIndent())
                            })
                            put(JSONObject().apply {
                                put("type", "image_url")
                                put("image_url", JSONObject().put("url", "data:image/jpeg;base64,$base64Image"))
                            })
                        }
                        put("content", content)
                    }
                    put(userMessage)
                }
                put("messages", messages)
                put("max_tokens", 300)
                put("temperature", 0.1)
            }

            val requestBody = jsonRequest.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url("https://api.groq.com/openai/v1/chat/completions")
                .header("Authorization", "Bearer ${BuildConfig.GROQ_API_KEY}")
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return emptyList()
                val responseBody = response.body?.string() ?: return emptyList()
                val jsonResponse = JSONObject(responseBody)
                val content = jsonResponse.getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content")
                
                content.split(",")
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Groq API call failed: ${e.message}", e)
            emptyList()
        }
    }

    private suspend fun processLabels(
        foodLabels: List<String>, 
        original: Bitmap, 
        scaled: Bitmap
    ) {
        val foods: List<Food> = foodLabels.take(6).map { label ->
            foodRepository.searchFood(label)
        }
        if (scaled !== original) scaled.recycle()
        _scanState.value = ScanState.Results(foods)
    }

    fun addFoodsToLog(foods: List<Food>) {
        if (foods.isEmpty()) { _scanState.value = ScanState.Idle; return }
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

    fun resetState() { _scanState.value = ScanState.Idle }

    private fun buildFoodsJson(foods: List<Food>): String {
        val items = foods.joinToString(",") { f ->
            """{"name":"${f.name.escapeJson()}","calories":${f.calories},""" +
            """"carbs":${f.carbs},"fat":${f.fat},"protein":${f.protein},""" +
            """"thumbnailUrl":"${f.thumbnailUrl.escapeJson()}","isOffline":${f.isOffline}}"""
        }
        return "[$items]"
    }

    private fun String.escapeJson(): String =
        replace("\\", "\\\\").replace("\"", "\\\"")

    override fun onCleared() {
        super.onCleared()
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
