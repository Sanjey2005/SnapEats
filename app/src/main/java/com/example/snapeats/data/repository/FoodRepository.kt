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

/**
 * Repository responsible for searching food nutritional data.
 *
 * Strategy:
 * 1. Call the FatSecret Platform API via [FatSecretApi].
 * 2. Parse the `food_description` field of the first result.
 * 3. On any [Exception] (network unavailable, timeout, HTTP error, parse failure),
 *    fall back to [LocalFoodDatabase] and return a [Food] with [Food.isOffline] = true.
 *
 * Constructed manually (no Hilt/Dagger) — a singleton instance should be created
 * in `SnapEatsApplication` and provided to ViewModels via their factory.
 */
class FoodRepository {

    // -------------------------------------------------------------------------
    // Retrofit / OkHttp setup
    // -------------------------------------------------------------------------

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

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Search for a food by [query] and return a [Food] domain model.
     *
     * The FatSecret `food_description` field looks like:
     * `"Per 100g - Calories: 52kcal | Fat: 0.17g | Carbs: 13.81g | Protein: 0.26g"`
     *
     * This function parses those values and converts them to the types expected
     * by [Food]. If anything goes wrong (network error, missing fields, parse
     * exception), it falls back to [LocalFoodDatabase].
     *
     * @param query The food name / keyword (typically the ML Kit label text).
     * @return      A [Food] populated with nutritional data.
     */
    suspend fun searchFood(query: String): Food {
        return try {
            val response = api.searchFoods(query = query)
            val firstItem = response.foods?.food?.firstOrNull()
                ?: return offlineFallback(query)

            val parsed = parseDescription(firstItem.food_description)
                ?: return offlineFallback(query)

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

    // -------------------------------------------------------------------------
    // Description parser
    // -------------------------------------------------------------------------

    /**
     * Parses the FatSecret `food_description` string.
     *
     * Expected format (may vary slightly):
     * `"Per 100g - Calories: 52kcal | Fat: 0.17g | Carbs: 13.81g | Protein: 0.26g"`
     *
     * @return A [ParsedNutrition] on success, or null if required fields are missing.
     */
    private fun parseDescription(description: String): ParsedNutrition? {
        return try {
            // Extract each value with a tolerant regex that handles integer or
            // decimal amounts and optional spaces around the colon.
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

    // -------------------------------------------------------------------------
    // Offline fallback
    // -------------------------------------------------------------------------

    /**
     * Searches [LocalFoodDatabase] for the best matching food name using
     * [String.contains]. Returns the first match found, or a generic 100 kcal
     * placeholder if nothing matches.
     *
     * The returned [Food] always has [Food.isOffline] = true so the UI can
     * show the offline badge.
     */
    private fun offlineFallback(query: String): Food {
        val lowerQuery = query.lowercase()

        // 1. Try: does an entry key contain the query word?
        val directMatch = LocalFoodDatabase.foods.entries
            .firstOrNull { (key, _) -> key.contains(lowerQuery) }

        // 2. Try: does the query contain an entry key?
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
            // No match at all — return a generic placeholder.
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

    // -------------------------------------------------------------------------
    // Macro estimators for offline data
    // -------------------------------------------------------------------------

    // These are rough typical macro ratios used only when the offline DB
    // has calories but not individual macro data.

    private fun estimateCarbs(calories: Int): Float = (calories * 0.50f / 4f)   // ~50 % of kcal from carbs
    private fun estimateFat(calories: Int): Float = (calories * 0.30f / 9f)     // ~30 % of kcal from fat
    private fun estimateProtein(calories: Int): Float = (calories * 0.20f / 4f) // ~20 % of kcal from protein

    // -------------------------------------------------------------------------
    // Private helpers / companion
    // -------------------------------------------------------------------------

    private data class ParsedNutrition(
        val calories: Int,
        val fat: Float,
        val carbs: Float,
        val protein: Float
    )

    companion object {
        private const val TAG = "FoodRepository"

        // Regex patterns — case-insensitive, tolerant of spacing variations.
        private val REGEX_CALORIES = Regex("""[Cc]alories\s*:\s*([\d.]+)\s*kcal""")
        private val REGEX_FAT      = Regex("""[Ff]at\s*:\s*([\d.]+)\s*g""")
        private val REGEX_CARBS    = Regex("""[Cc]arbs?\s*:\s*([\d.]+)\s*g""")
        private val REGEX_PROTEIN  = Regex("""[Pp]rotein\s*:\s*([\d.]+)\s*g""")
    }
}
