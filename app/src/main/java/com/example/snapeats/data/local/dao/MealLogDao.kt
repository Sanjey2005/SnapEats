package com.example.snapeats.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.snapeats.data.local.entity.MealLog
import kotlinx.coroutines.flow.Flow

@Dao
interface MealLogDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMealLog(log: MealLog)

    /**
     * Returns all meal logs whose timestamp falls within [startOfDayMs, endOfDayMs).
     * Callers should compute these bounds from the desired date using Calendar or LocalDate.
     */
    @Query(
        "SELECT * FROM meal_log WHERE timestamp >= :startOfDayMs AND timestamp < :endOfDayMs ORDER BY timestamp DESC"
    )
    fun getMealLogsByDate(startOfDayMs: Long, endOfDayMs: Long): Flow<List<MealLog>>

    @Query("SELECT * FROM meal_log ORDER BY timestamp DESC")
    fun getAllMealLogs(): Flow<List<MealLog>>

    @Delete
    suspend fun deleteMealLog(log: MealLog)

    @Query("DELETE FROM meal_log WHERE id = :id")
    suspend fun deleteMealLogById(id: Int)
}
