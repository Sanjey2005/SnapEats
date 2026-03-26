package com.example.snapeats.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user")
data class User(
    @PrimaryKey
    val id: Int = 1,
    val height: Float,
    val weight: Float,
    val age: Int,
    val isMale: Boolean,
    val activityFactor: Float,
    val bmi: Float,
    val dailyCalTarget: Int
)
