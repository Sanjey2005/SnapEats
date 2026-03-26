package com.example.snapeats

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
                        // Use first() to get the current user state from the Flow
                        val user = app.userDao.getUser().first()
                        startDestination = if (user == null) {
                            Screen.Profile.route
                        } else {
                            Screen.Home.route
                        }
                    } catch (e: Exception) {
                        // Fallback to Profile if DB fails
                        startDestination = Screen.Profile.route
                    }
                }

                // Only compose the navigation graph once we know the correct start destination.
                startDestination?.let { destination ->
                    SnapEatsNavGraph(startDestination = destination)
                }
            }
        }
    }
}
