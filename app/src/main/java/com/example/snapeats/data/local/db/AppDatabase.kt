package com.example.snapeats.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.snapeats.data.local.dao.BMIRecordDao
import com.example.snapeats.data.local.dao.MealLogDao
import com.example.snapeats.data.local.dao.UserDao
import com.example.snapeats.data.local.entity.BMIRecord
import com.example.snapeats.data.local.entity.MealLog
import com.example.snapeats.data.local.entity.User

@Database(
    entities = [User::class, MealLog::class, BMIRecord::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun mealLogDao(): MealLogDao
    abstract fun bmiRecordDao(): BMIRecordDao

    companion object {
        private const val DATABASE_NAME = "snapeats.db"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
