package com.example.snapeats.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Sealed class representing every top-level destination in the SnapEats navigation graph.
 */
sealed class Screen(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector? = null,
    val unselectedIcon: ImageVector? = null
) {
    object Home : Screen(
        route = "home",
        title = "Home",
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home
    )

    object Scan : Screen(
        route = "scan",
        title = "Scan Food"
    )

    object Profile : Screen(
        route = "profile",
        title = "Profile",
        selectedIcon = Icons.Filled.Person,
        unselectedIcon = Icons.Outlined.Person
    )

    object Recs : Screen(
        route = "recs",
        title = "Recommendations",
        selectedIcon = Icons.Filled.Star,
        unselectedIcon = Icons.Outlined.StarBorder
    )

    object History : Screen(
        route = "history",
        title = "History",
        selectedIcon = Icons.Filled.Favorite,
        unselectedIcon = Icons.Outlined.FavoriteBorder
    )

    object Auth : Screen(
        route = "auth",
        title = "Authentication"
    )
}

/**
 * Returns the ordered list of bottom-navigation destinations.
 * Using a function to ensure Screen objects are initialized before being added to the list.
 */
fun getBottomNavScreens(): List<Screen> = listOf(
    Screen.Home,
    Screen.Recs,
    Screen.History,
    Screen.Profile
)

/**
 * Returns the [Screen] whose [route] matches [route], or null if not found.
 */
fun getScreenFromRoute(route: String?): Screen? {
    if (route == null) return null
    return listOf(Screen.Home, Screen.Scan, Screen.Profile, Screen.Recs, Screen.History, Screen.Auth)
        .firstOrNull { it.route == route }
}
