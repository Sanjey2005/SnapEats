package com.example.snapeats.data.repository

import android.util.Log
import com.example.snapeats.BuildConfig
import com.example.snapeats.data.offline.LocalFoodDatabase
import com.example.snapeats.data.remote.api.FatSecretApi
import com.example.snapeats.data.remote.api.OAuthInterceptor
import com.example.snapeats.domain.model.Food
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class FoodRepository {

    private val api: FatSecretApi by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(
                OAuthInterceptor(
                    consumerKey = BuildConfig.FATSECRET_CONSUMER_KEY,
                    consumerSecret = BuildConfig.FATSECRET_CONSUMER_SECRET
                )
            )
            .addInterceptor(logging)
            .build()

        Retrofit.Builder()
            .baseUrl("https://platform.fatsecret.com/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(FatSecretApi::class.java)
    }

    suspend fun searchFood(query: String): Food {
        return try {
            val response = api.searchFoods(query = query)

            val allItems = response.foods?.food ?: return offlineFallback(query)
            if (allItems.isEmpty()) return offlineFallback(query)

            val lowerQuery = query.lowercase()
            val firstItem = allItems.minByOrNull { item ->
                val name = item.food_name.lowercase()
                when {
                    name == lowerQuery -> 0
                    name.contains(lowerQuery) -> 1
                    lowerQuery.contains(name) -> 2
                    else -> 3
                }
            } ?: return offlineFallback(query)

            val parsed = parseDescription(firstItem.food_description)
                ?: return offlineFallback(query)

            // Reject unrealistic calorie values — bulk/commercial DB entries
            // No real food exceeds 900 kcal per 100g (pure butter is ~717)
            if (parsed.calories > 900) return offlineFallback(query)

            Food(
                name = firstItem.food_name,
                calories = parsed.calories,
                carbs = parsed.carbs,
                fat = parsed.fat,
                protein = parsed.protein,
                thumbnailUrl = "",
                isOffline = false
            )
        } catch (e: Exception) {
            Log.w(TAG, "FatSecret API call failed for \"$query\", using offline fallback. ${e.message}")
            offlineFallback(query)
        }
    }

    private fun parseDescription(description: String): ParsedNutrition? {
        return try {
            val calories = REGEX_CALORIES.find(description)
                ?.groupValues?.get(1)?.trim()?.toFloatOrNull()?.toInt()
                ?: return null

            val fat = REGEX_FAT.find(description)
                ?.groupValues?.get(1)?.trim()?.toFloatOrNull()
                ?: 0f

            val carbs = REGEX_CARBS.find(description)
                ?.groupValues?.get(1)?.trim()?.toFloatOrNull()
                ?: 0f

            val protein = REGEX_PROTEIN.find(description)
                ?.groupValues?.get(1)?.trim()?.toFloatOrNull()
                ?: 0f

            ParsedNutrition(calories = calories, fat = fat, carbs = carbs, protein = protein)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse description: \"$description\". ${e.message}")
            null
        }
    }

    private fun offlineFallback(query: String): Food {
        val lowerQuery = query.lowercase()

        val directMatch = LocalFoodDatabase.foods.entries
            .firstOrNull { (key, _) -> key.contains(lowerQuery) }

        val reverseMatch = if (directMatch == null) {
            LocalFoodDatabase.foods.entries
                .firstOrNull { (key, _) -> lowerQuery.contains(key) }
        } else null

        val match = directMatch ?: reverseMatch

        return if (match != null) {
            Food(
                name = match.key.replaceFirstChar { it.uppercase() },
                calories = match.value,
                carbs = estimateCarbs(match.value),
                fat = estimateFat(match.value),
                protein = estimateProtein(match.value),
                thumbnailUrl = "",
                isOffline = true
            )
        } else {
            Food(
                name = query.replaceFirstChar { it.uppercase() },
                calories = 100,
                carbs = 10f,
                fat = 5f,
                protein = 5f,
                thumbnailUrl = "",
                isOffline = true
            )
        }
    }

    private fun estimateCarbs(calories: Int): Float = (calories * 0.50f / 4f)
    private fun estimateFat(calories: Int): Float = (calories * 0.30f / 9f)
    private fun estimateProtein(calories: Int): Float = (calories * 0.20f / 4f)

    private data class ParsedNutrition(
        val calories: Int,
        val fat: Float,
        val carbs: Float,
        val protein: Float
    )

    companion object {
        private const val TAG = "FoodRepository"

        private val REGEX_CALORIES = Regex("""[Cc]alories\s*:\s*([\d.]+)\s*kcal""")
        private val REGEX_FAT      = Regex("""[Ff]at\s*:\s*([\d.]+)\s*g""")
        private val REGEX_CARBS    = Regex("""[Cc]arbs?\s*:\s*([\d.]+)\s*g""")
        private val REGEX_PROTEIN  = Regex("""[Pp]rotein\s*:\s*([\d.]+)\s*g""")
    }
}