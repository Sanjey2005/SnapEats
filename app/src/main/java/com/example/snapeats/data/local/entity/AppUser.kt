package com.example.snapeats.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "app_user",
    indices = [Index(value = ["email"], unique = true)]
)
data class AppUser(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val username: String,
    val email: String,
    val passwordHash: String,
    val createdAt: Long = System.currentTimeMillis()
)
