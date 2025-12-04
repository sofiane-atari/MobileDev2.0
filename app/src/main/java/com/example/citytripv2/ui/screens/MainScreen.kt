package com.example.citytripv2.ui.screens

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.citytripv2.ui.screens.ChatScreen
import com.example.citytripv2.ui.screens.ProfileScreen

sealed class BottomNavItem(val route: String, val label: String, val icon: ImageVector) {
    object Home : BottomNavItem("tab_home", "Home", Icons.Default.Home)
    // AddCity is hier verwijderd, want het is geen tabblad meer
    object Chats : BottomNavItem("tab_chats", "Chats", Icons.Default.Chat)
    object Profile : BottomNavItem("tab_profile", "Profiel", Icons.Default.Person)
}

@Composable
fun MainScreen(
    onLogout: () -> Unit,
    onNavigateToAddCity: () -> Unit // <--- NIEUW: We moeten dit doorgeven aan HomeScreen
) {
    val navController = rememberNavController()

    // AddCity ook hier uit de lijst gehaald
    val items = listOf(
        BottomNavItem.Home,
        BottomNavItem.Chats,
        BottomNavItem.Profile
    )

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                items.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = screen.label) },
                        label = { Text(screen.label) },
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = BottomNavItem.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            // Geef de callback door aan HomeScreen zodat de + knop werkt
            composable(BottomNavItem.Home.route) {
                HomeScreen(onNavigateToAddCity = onNavigateToAddCity)
            }

            // De AddCity composable is hier verwijderd.
            // Die zit nu in MainActivity.kt als aparte route "add_city".

            composable(BottomNavItem.Chats.route) { ChatScreen() }
            composable(BottomNavItem.Profile.route) { ProfileScreen(onLogout = onLogout) }
        }
    }
}