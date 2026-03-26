package com.example.snapeats.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bmi_record")
data class BMIRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val userId: Int,
    val timestamp: Long,
    val bmi: Float,
    val weight: Float
)
