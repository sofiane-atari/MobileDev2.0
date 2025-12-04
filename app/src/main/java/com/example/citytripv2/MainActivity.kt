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
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.citytripv2.ui.screens.MainScreen
import com.example.citytripv2.ui.auth.LoginScreen
import com.example.citytripv2.ui.auth.RegisterScreen
import com.example.citytripv2.ui.screens.AddCityScreen
import com.example.citytripv2.ui.theme.CityTripV2Theme
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
    val context = LocalContext.current // Nodig voor de Toast later

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

        // --- ROUTE 3: Hoofd App (Tabs) ---
        composable("main") {
            MainScreen(
                onLogout = {
                    auth.signOut()
                    navController.navigate("login") {
                        popUpTo(0) { inclusive = true }
                    }
                },
                // NIEUW: De koppeling voor de + knop op HomeScreen
                onNavigateToAddCity = {
                    navController.navigate("add_city")
                }
            )
        }

        // --- ROUTE 4: Stad Toevoegen (NIEUW) ---
        composable("add_city") {
            AddCityScreen(
                onNavigateBack = {
                    navController.popBackStack() // Ga terug naar de lijst
                },
                onCityAdded = { city ->
                    // TODO: Hier komt later de Firestore opslag code
                    Toast.makeText(context, "Stad ${city.name} opgeslagen (lokaal)", Toast.LENGTH_SHORT).show()
                    navController.popBackStack() // Ga terug na opslaan
                }
            )
        }
    }
}