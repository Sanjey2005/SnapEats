package com.example.snapeats

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.snapeats.ui.navigation.SnapEatsNavGraph
import com.example.snapeats.ui.navigation.Screen
import com.example.snapeats.ui.theme.SnapEatsTheme
import kotlinx.coroutines.flow.first

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as SnapEatsApplication

        setContent {
            SnapEatsTheme {
                // Determine start destination: show profile/onboarding if no User record exists,
                // otherwise go straight to home. Use null as the "loading" sentinel.
                var startDestination by remember { mutableStateOf<String?>(null) }

                LaunchedEffect(Unit) {
                    try {
                        if (app.currentUserId == -1) {
                            // Not logged in — show auth screen
                            startDestination = Screen.Auth.route
                        } else {
                            // Logged in — check if health profile exists
                            val user = app.userDao.getUser(app.currentUserId).first()
                            startDestination = if (user == null) {
                                Screen.Profile.route
                            } else {
                                Screen.Home.route
                            }
                        }
                    } catch (e: Exception) {
                        // Fallback to Auth if DB fails
                        startDestination = Screen.Auth.route
                    }
                }

                // Only compose the navigation graph once we know the correct start destination.
                startDestination?.let { destination ->
                    key(destination) {
                        SnapEatsNavGraph(
                            startDestination = destination,
                            userId = app.currentUserId
                        )
                    }
                }
            }
        }
    }
}
