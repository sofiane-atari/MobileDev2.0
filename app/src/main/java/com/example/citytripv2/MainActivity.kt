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
import androidx.lifecycle.viewmodel.compose.viewModel // <--- BELANGRIJK: Import voor ViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.citytripv2.ui.screens.MainScreen
import com.example.citytripv2.ui.auth.LoginScreen
import com.example.citytripv2.ui.auth.RegisterScreen
import com.example.citytripv2.ui.screens.AddCityScreen
import com.example.citytripv2.ui.theme.CityTripV2Theme
import com.example.citytripv2.viewmodel.HomeViewModel // <--- BELANGRIJK: Import je ViewModel
import com.google.firebase.auth.FirebaseAuth

// push
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
                }
            )
        }

        // --- ROUTE 4: Stad Toevoegen (MET FIREBASE OPSLAG) ---
        composable("add_city") {
            // 1. Haal de HomeViewModel op. Deze regelt de communicatie met Firebase.
            val homeViewModel: HomeViewModel = viewModel()

            AddCityScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onCityAdded = { newLocation ->
                    // 2. Stuur de nieuwe locatie naar Firebase via de ViewModel
                    homeViewModel.addLocation(newLocation)

                    Toast.makeText(context, "Opgeslagen! Check je Home scherm.", Toast.LENGTH_SHORT).show()

                    // 3. Ga terug naar het overzicht (de lijst update automatisch)
                    navController.popBackStack()
                }
            )
        }
    }
}