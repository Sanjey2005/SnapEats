package com.example.snapeats.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.snapeats.data.local.entity.User
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateUser(user: User)

    @Query("SELECT * FROM user WHERE userId = :userId LIMIT 1")
    fun getUser(userId: Int): Flow<User?>

    @Query("SELECT * FROM user WHERE userId = :userId LIMIT 1")
    suspend fun getUserOnce(userId: Int): User?
}
