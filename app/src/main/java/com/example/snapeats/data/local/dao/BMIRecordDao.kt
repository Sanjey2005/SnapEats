package com.example.snapeats.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.snapeats.data.local.entity.BMIRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface BMIRecordDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: BMIRecord)

    @Query("SELECT * FROM bmi_record WHERE userId = :userId ORDER BY timestamp DESC")
    fun getAllRecords(userId: Int): Flow<List<BMIRecord>>

    @Query("SELECT * FROM bmi_record WHERE userId = :userId ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentRecords(userId: Int, limit: Int): Flow<List<BMIRecord>>
}
