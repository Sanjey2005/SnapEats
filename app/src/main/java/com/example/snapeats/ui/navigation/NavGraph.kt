package com.example.snapeats.ui.navigation

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.snapeats.ui.history.HistoryScreen
import com.example.snapeats.ui.history.HistoryViewModel
import com.example.snapeats.ui.home.HomeScreen
import com.example.snapeats.ui.home.HomeViewModel
import com.example.snapeats.ui.profile.ProfileScreen
import com.example.snapeats.ui.profile.ProfileViewModel
import com.example.snapeats.ui.recs.RecsScreen
import com.example.snapeats.ui.recs.RecsViewModel
import com.example.snapeats.ui.scan.ScanScreen
import com.example.snapeats.ui.scan.ScanViewModel
import com.example.snapeats.ui.auth.AuthScreen
import com.example.snapeats.ui.auth.AuthViewModel

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

            composable(route = Screen.Auth.route) {
                val app = LocalContext.current.applicationContext
                    as com.example.snapeats.SnapEatsApplication
                val authViewModel: AuthViewModel =
                    androidx.lifecycle.viewmodel.compose.viewModel(
                        factory = AuthViewModel.factory(
                            appUserDao = app.appUserDao
                        )
                    )
                AuthScreen(
                    viewModel = authViewModel,
                    onAuthSuccess = { userId ->
                        app.currentUserId = userId
                        navController.navigate(Screen.Profile.route) {
                            popUpTo(Screen.Auth.route) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                )
            }

            composable(route = Screen.Home.route) {
                val app = LocalContext.current.applicationContext
                    as com.example.snapeats.SnapEatsApplication
                val homeViewModel: HomeViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                    factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                            return HomeViewModel(
                                userDao = app.userDao,
                                mealLogDao = app.mealLogDao,
                                userId = app.currentUserId
                            ) as T
                        }
                    }
                )
                HomeScreen(
                    viewModel           = homeViewModel,
                    onNavigateToScan    = { source -> 
                        navController.navigate("${Screen.Scan.route}?source=$source") 
                    },
                )
            }

            composable(
                route = "${Screen.Scan.route}?source={source}",
                arguments = listOf(
                    navArgument("source") {
                        type = NavType.StringType
                        defaultValue = "camera"
                    }
                )
            ) { backStackEntry ->
                val source = backStackEntry.arguments?.getString("source") ?: "camera"
                val app = LocalContext.current.applicationContext
                    as com.example.snapeats.SnapEatsApplication
                val scanViewModel: ScanViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                    factory = ScanViewModel.factory(
                        foodRepository = app.foodRepository,
                        mealLogDao = app.mealLogDao,
                        userId = app.currentUserId
                    )
                )
                ScanScreen(
                    viewModel      = scanViewModel,
                    initialSource  = source,
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
                val profileViewModel: ProfileViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                    factory = ProfileViewModel.factory(
                        userDao = app.userDao,
                        bmiRecordDao = app.bmiRecordDao,
                        calcBMIUseCase = app.calcBMIUseCase,
                        calcDailyCalUseCase = app.calcDailyCalUseCase,
                        userId = app.currentUserId
                    )
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
                val recsViewModel: RecsViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                    factory = RecsViewModel.factory(
                        userDao = app.userDao,
                        mealLogDao = app.mealLogDao,
                        foodRepository = app.foodRepository,
                        userId = app.currentUserId
                    )
                )
                RecsScreen(
                    viewModel        = recsViewModel,
                    onNavigateBack   = { navController.popBackStack() },
                )
            }

            composable(route = Screen.History.route) {
                val app = LocalContext.current.applicationContext
                    as com.example.snapeats.SnapEatsApplication

                val historyViewModel: HistoryViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                    factory = HistoryViewModel.factory(
                        mealLogDao = app.mealLogDao,
                        userDao = app.userDao,
                        userId = app.currentUserId
                    )
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

    // Hide bottom bar on the Scan and Auth screens
    val route = currentDestination?.route ?: ""
    val showBar = !route.startsWith(Screen.Scan.route) && route != Screen.Auth.route
    if (!showBar) return

    NavigationBar(
        containerColor = Color(0xFF161B22),
        modifier = Modifier.border(
            width = 1.dp,
            color = Color(0xFF30363D),
            shape = RectangleShape
        )
    ) {
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
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor   = Color(0xFF4CAF50),
                    selectedTextColor   = Color(0xFF4CAF50),
                    unselectedIconColor = Color(0xFF8B949E),
                    unselectedTextColor = Color(0xFF8B949E),
                    indicatorColor      = Color(0xFF4CAF50).copy(alpha = 0.15f)
                )
            )
        }
    }
}
