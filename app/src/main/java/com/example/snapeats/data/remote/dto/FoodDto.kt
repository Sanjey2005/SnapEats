package com.example.snapeats.data.remote.dto

import com.google.gson.annotations.SerializedName

data class FoodSearchResponse(
    @SerializedName("foods") val foods: FoodsWrapper?
)

data class FoodsWrapper(
    @SerializedName("food") val food: List<FoodItem>?
)

data class FoodItem(
    @SerializedName("food_id") val food_id: String,
    @SerializedName("food_name") val food_name: String,
    /**
     * Example value:
     * "Per 100g - Calories: 52kcal | Fat: 0.17g | Carbs: 13.81g | Protein: 0.26g"
     */
    @SerializedName("food_description") val food_description: String
)
