package com.example.citytripv2

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

import com.example.citytripv2.ui.auth.LoginScreen
import com.example.citytripv2.ui.auth.RegisterScreen
import com.example.citytripv2.ui.screens.AddCityScreen
import com.example.citytripv2.ui.screens.MainScreen
import com.example.citytripv2.ui.screens.MyLocationsScreen
import com.example.citytripv2.ui.screens.RegisterCityScreen
import com.example.citytripv2.ui.theme.CityTripV2Theme
import com.example.citytripv2.viewmodel.HomeViewModel
import com.google.firebase.auth.FirebaseAuth

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val auth = FirebaseAuth.getInstance()
        val startDestination = if (auth.currentUser != null) "main" else "login"

        setContent {
            CityTripV2Theme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    CityTripNavigation(
                        startDestination = startDestination,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun CityTripNavigation(startDestination: String, modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    val auth = FirebaseAuth.getInstance()
    val context = LocalContext.current

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        // --- ROUTE 1: Login ---
        composable("login") {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate("main") {
                        popUpTo("login") { inclusive = true }
                    }
                },
                onNavigateToRegister = {
                    navController.navigate("register")
                }
            )
        }

        // --- ROUTE 2: Registratie ---
        composable("register") {
            RegisterScreen(
                onRegisterSuccess = {
                    navController.navigate("main") {
                        popUpTo("login") { inclusive = true }
                    }
                },
                onNavigateToLogin = {
                    navController.popBackStack()
                }
            )
        }

        // --- ROUTE 3: Hoofd App ---
        composable("main") {
            MainScreen(
                onLogout = {
                    auth.signOut()
                    navController.navigate("login") {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onNavigateToAddCity = {
                    navController.navigate("add_city")
                },
                onNavigateToMyLocations = {
                    navController.navigate("my_locations")
                }
            )
        }

        // --- ROUTE 4: Stad Toevoegen (Locatie/Spot) ---
        composable("add_city") {
            val homeViewModel: HomeViewModel = viewModel()

            AddCityScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToRegisterCity = {
                    navController.navigate("register_city")
                },
                onCityAdded = { newLocation ->
                    homeViewModel.addLocation(newLocation)
                    Toast.makeText(context, "Opgeslagen!", Toast.LENGTH_SHORT).show()
                    navController.popBackStack()
                }
            )
        }

        // --- ROUTE 5: Nieuwe Stad Registreren ---
        composable("register_city") {
            val homeViewModel: HomeViewModel = viewModel()
            RegisterCityScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                viewModel = homeViewModel
            )
        }

        // --- ROUTE 6: Mijn Locaties ---
        composable("my_locations") {
            val homeViewModel: HomeViewModel = viewModel()

            MyLocationsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                viewModel = homeViewModel
            )
        }
    }
}