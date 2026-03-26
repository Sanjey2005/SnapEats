package com.example.snapeats.domain.model

data class Meal(
    val id: Int,
    val timestamp: Long,
    val foods: List<Food>,
    val totalCal: Int
)
