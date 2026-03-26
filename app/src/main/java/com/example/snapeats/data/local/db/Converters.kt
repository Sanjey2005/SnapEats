package com.example.snapeats.data.local.db

import androidx.room.TypeConverter
import com.example.snapeats.domain.model.Food
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class Converters {

    private val gson = Gson()

    @TypeConverter
    fun fromFoodList(foods: List<Food>): String {
        return gson.toJson(foods)
    }

    @TypeConverter
    fun toFoodList(json: String): List<Food> {
        val type = object : TypeToken<List<Food>>() {}.type
        return gson.fromJson(json, type)
    }
}
