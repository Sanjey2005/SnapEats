import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

// Read FatSecret API keys from local.properties
val localProperties = Properties().apply {
    val propsFile = rootProject.file("local.properties")
    if (propsFile.exists()) load(propsFile.inputStream())
}

android {
    namespace = "com.example.snapeats"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.snapeats"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        buildConfigField(
            "String",
            "FATSECRET_CONSUMER_KEY",
            "\"${localProperties.getProperty("FATSECRET_CONSUMER_KEY", "")}\""
        )
        buildConfigField(
            "String",
            "FATSECRET_CONSUMER_SECRET",
            "\"${localProperties.getProperty("FATSECRET_CONSUMER_SECRET", "")}\""
        )
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {

    // -------------------------------------------------------------------------
    // Jetpack Compose — BOM manages all individual Compose library versions
    // -------------------------------------------------------------------------
    val composeBom = platform("androidx.compose:compose-bom:2024.09.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    // Material 3 (Material You) design components
    implementation("androidx.compose.material3:material3")

    // Material Icons — extended set (includes CameraAlt, Restaurant, etc.)
    implementation("androidx.compose.material:material-icons-extended")

    // Core Compose UI toolkits
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")

    // Compose debug tooling (layout inspector, preview)
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    // Compose instrumented test rule
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")

    // -------------------------------------------------------------------------
    // Activity + Navigation
    // -------------------------------------------------------------------------
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.navigation:navigation-compose:2.8.4")

    // -------------------------------------------------------------------------
    // Lifecycle — ViewModel + coroutine / Flow integration
    // -------------------------------------------------------------------------
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")

    // -------------------------------------------------------------------------
    // Room — local SQLite database
    // -------------------------------------------------------------------------
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // -------------------------------------------------------------------------
    // CameraX — camera preview, lifecycle binding, and view
    // -------------------------------------------------------------------------
    implementation("androidx.camera:camera-camera2:1.3.4")
    implementation("androidx.camera:camera-lifecycle:1.3.4")
    implementation("androidx.camera:camera-view:1.3.4")

    // -------------------------------------------------------------------------
    // Google ML Kit — on-device object detection (food classification)
    // -------------------------------------------------------------------------
    implementation("com.google.mlkit:object-detection:17.0.2")

    // Coroutines Play Services integration for .await()
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.10.2")

    // -------------------------------------------------------------------------
    // Networking — Retrofit + OkHttp + Gson converter
    // -------------------------------------------------------------------------
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // -------------------------------------------------------------------------
    // Gson — JSON serialisation (also used by Room type converters)
    // -------------------------------------------------------------------------
    implementation("com.google.code.gson:gson:2.11.0")

    // -------------------------------------------------------------------------
    // Coil — Kotlin-first image loading for Compose AsyncImage
    // -------------------------------------------------------------------------
    implementation("io.coil-kt:coil-compose:2.7.0")

    // -------------------------------------------------------------------------
    // DataStore — key-value preferences (unit toggle: metric / imperial)
    // -------------------------------------------------------------------------
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // -------------------------------------------------------------------------
    // Core Android utilities
    // -------------------------------------------------------------------------
    implementation("androidx.core:core-ktx:1.13.1")

    // -------------------------------------------------------------------------
    // Testing
    // -------------------------------------------------------------------------
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
}
