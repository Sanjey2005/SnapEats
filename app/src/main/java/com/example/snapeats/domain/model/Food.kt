package com.example.snapeats.domain.model

data class Food(
    val name: String,
    val calories: Int,
    val carbs: Float,
    val fat: Float,
    val protein: Float,
    val thumbnailUrl: String = "",
    val isOffline: Boolean = false
)
