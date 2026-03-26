package com.example.snapeats.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.snapeats.data.local.entity.AppUser

@Dao
interface AppUserDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertUser(user: AppUser): Long

    @Query("SELECT * FROM app_user WHERE email = :email AND passwordHash = :passwordHash LIMIT 1")
    suspend fun login(email: String, passwordHash: String): AppUser?

    @Query("SELECT * FROM app_user WHERE email = :email LIMIT 1")
    suspend fun findByEmail(email: String): AppUser?

    @Query("SELECT * FROM app_user WHERE id = :id LIMIT 1")
    suspend fun findById(id: Int): AppUser?
}
