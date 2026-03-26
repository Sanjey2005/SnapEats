package com.example.snapeats

import android.app.Application
import android.content.Context
import androidx.room.Room
import com.example.snapeats.data.local.db.AppDatabase
import com.example.snapeats.data.remote.api.FatSecretApi
import com.example.snapeats.data.remote.api.OAuthInterceptor
import com.example.snapeats.data.repository.FoodRepository
import com.example.snapeats.domain.usecase.CalcBMIUseCase
import com.example.snapeats.domain.usecase.CalcDailyCalUseCase
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class SnapEatsApplication : Application() {

    // ─── Database ────────────────────────────────────────────────────────────

    val database: AppDatabase by lazy {
        Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "snapeats.db"
        )
            .addMigrations(AppDatabase.MIGRATION_1_2)
            .build()
    }

    // ─── DAOs ─────────────────────────────────────────────────────────────────

    val userDao by lazy { database.userDao() }
    val mealLogDao by lazy { database.mealLogDao() }
    val bmiRecordDao by lazy { database.bmiRecordDao() }
    val appUserDao by lazy { database.appUserDao() }

    // ─── Auth ─────────────────────────────────────────────────────────────────

    private val prefs by lazy {
        getSharedPreferences("snapeats_prefs", Context.MODE_PRIVATE)
    }

    var currentUserId: Int
        get() = prefs.getInt("current_user_id", -1)
        set(value) { prefs.edit().putInt("current_user_id", value).apply() }

    // ─── Network ──────────────────────────────────────────────────────────────

    private val loggingInterceptor: HttpLoggingInterceptor by lazy {
        HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
    }

    private val oAuthInterceptor: OAuthInterceptor by lazy {
        OAuthInterceptor(
            consumerKey = BuildConfig.FATSECRET_CONSUMER_KEY,
            consumerSecret = BuildConfig.FATSECRET_CONSUMER_SECRET
        )
    }

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(oAuthInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl("https://platform.fatsecret.com/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val fatSecretApi: FatSecretApi by lazy {
        retrofit.create(FatSecretApi::class.java)
    }

    // ─── Repositories ─────────────────────────────────────────────────────────

    val foodRepository: FoodRepository by lazy {
        FoodRepository()
    }

    // ─── Use Cases ────────────────────────────────────────────────────────────

    val calcBMIUseCase: CalcBMIUseCase by lazy {
        CalcBMIUseCase()
    }

    val calcDailyCalUseCase: CalcDailyCalUseCase by lazy {
        CalcDailyCalUseCase()
    }
}
