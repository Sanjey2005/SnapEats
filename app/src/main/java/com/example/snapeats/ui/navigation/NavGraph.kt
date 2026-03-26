package com.example.snapeats.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.snapeats.ui.history.HistoryScreen
import com.example.snapeats.ui.home.HomeScreen
import com.example.snapeats.ui.profile.ProfileScreen
import com.example.snapeats.ui.recs.RecsScreen
import com.example.snapeats.ui.scan.ScanScreen

@Composable
fun SnapEatsNavGraph(
    startDestination: String = Screen.Home.route,
) {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = {
            SnapEatsBottomBar(navController = navController)
        },
    ) { innerPadding ->

        NavHost(
            navController    = navController,
            startDestination = startDestination,
            modifier         = Modifier.padding(innerPadding),
        ) {

            composable(route = Screen.Home.route) {
                val app = LocalContext.current.applicationContext
                    as com.example.snapeats.SnapEatsApplication
                val homeViewModel: com.example.snapeats.ui.home.HomeViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                    factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                            return com.example.snapeats.ui.home.HomeViewModel(
                                userDao = app.userDao,
                                mealLogDao = app.mealLogDao
                            ) as T
                        }
                    }
                )
                HomeScreen(
                    viewModel           = homeViewModel,
                    onNavigateToScan    = { navController.navigate(Screen.Scan.route) },
                )
            }

            composable(route = Screen.Scan.route) {
                val app = LocalContext.current.applicationContext
                    as com.example.snapeats.SnapEatsApplication
                val scanViewModel: com.example.snapeats.ui.scan.ScanViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                    factory = com.example.snapeats.ui.scan.ScanViewModel.factory(
                        foodRepository = app.foodRepository,
                        mealLogDao = app.mealLogDao
                    )
                )
                ScanScreen(
                    viewModel      = scanViewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onMealLogged   = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Home.route) { inclusive = false }
                            launchSingleTop = true
                        }
                    },
                )
            }

            composable(route = Screen.Profile.route) {
                val app = LocalContext.current.applicationContext
                    as com.example.snapeats.SnapEatsApplication
                val profileViewModel: com.example.snapeats.ui.profile.ProfileViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                    factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                            return com.example.snapeats.ui.profile.ProfileViewModel(
                                userDao = app.userDao,
                                bmiRecordDao = app.bmiRecordDao,
                                calcBMIUseCase = app.calcBMIUseCase,
                                calcDailyCalUseCase = app.calcDailyCalUseCase
                            ) as T
                        }
                    }
                )
                ProfileScreen(
                    viewModel    = profileViewModel,
                    isOnboarding = false,
                    onSaveSuccess = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Profile.route) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                )
            }

            composable(route = Screen.Recs.route) {
                val app = LocalContext.current.applicationContext
                    as com.example.snapeats.SnapEatsApplication
                val recsViewModel: com.example.snapeats.ui.recs.RecsViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                    factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                            return com.example.snapeats.ui.recs.RecsViewModel(
                                userDao = app.userDao,
                                mealLogDao = app.mealLogDao,
                                foodRepository = app.foodRepository
                            ) as T
                        }
                    }
                )
                RecsScreen(
                    viewModel        = recsViewModel,
                    onNavigateBack   = { navController.popBackStack() },
                )
            }

            composable(route = Screen.History.route) {
                val app = LocalContext.current.applicationContext
                    as com.example.snapeats.SnapEatsApplication

                val historyViewModel: com.example.snapeats.ui.history.HistoryViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                    factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : androidx.lifecycle.ViewModel> create(
                            modelClass: Class<T>
                        ): T {
                            return com.example.snapeats.ui.history.HistoryViewModel(
                                mealLogDao = app.mealLogDao,
                                userDao = app.userDao
                            ) as T
                        }
                    }
                )

                HistoryScreen(
                    viewModel = historyViewModel,
                    userDao = app.userDao,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}

@Composable
private fun SnapEatsBottomBar(navController: NavController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    // Hide bottom bar on the Scan screen
    val showBar = currentDestination?.route != Screen.Scan.route
    if (!showBar) return

    NavigationBar {
        getBottomNavScreens().forEach { screen ->
            val isSelected = currentDestination?.hierarchy
                ?.any { it.route == screen.route } == true

            NavigationBarItem(
                selected = isSelected,
                onClick  = {
                    navController.navigate(screen.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState    = true
                    }
                },
                icon = {
                    val icon = if (isSelected) screen.selectedIcon else screen.unselectedIcon
                    if (icon != null) {
                        Icon(
                            imageVector        = icon,
                            contentDescription = screen.title,
                        )
                    }
                },
                label = { Text(text = screen.title) },
            )
        }
    }
}
