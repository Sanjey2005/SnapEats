package com.example.snapeats.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.snapeats.data.local.dao.AppUserDao
import com.example.snapeats.data.local.dao.BMIRecordDao
import com.example.snapeats.data.local.dao.MealLogDao
import com.example.snapeats.data.local.dao.UserDao
import com.example.snapeats.data.local.entity.AppUser
import com.example.snapeats.data.local.entity.BMIRecord
import com.example.snapeats.data.local.entity.MealLog
import com.example.snapeats.data.local.entity.User

@Database(
    entities = [User::class, MealLog::class, BMIRecord::class, AppUser::class],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun mealLogDao(): MealLogDao
    abstract fun bmiRecordDao(): BMIRecordDao
    abstract fun appUserDao(): AppUserDao

    companion object {
        private const val DATABASE_NAME = "snapeats.db"

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Create app_user table
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS app_user (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        username TEXT NOT NULL,
                        email TEXT NOT NULL,
                        passwordHash TEXT NOT NULL,
                        createdAt INTEGER NOT NULL DEFAULT 0
                    )"""
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_app_user_email ON app_user(email)")
                
                // Update meal_log table
                db.execSQL("ALTER TABLE meal_log ADD COLUMN userId INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE meal_log ADD COLUMN mealType TEXT NOT NULL DEFAULT 'Others'")
                
                // Update user table
                db.execSQL("ALTER TABLE user ADD COLUMN userId INTEGER NOT NULL DEFAULT 1")

                // Update bmi_record table
                db.execSQL("ALTER TABLE bmi_record ADD COLUMN userId INTEGER NOT NULL DEFAULT 1")
            }
        }

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
