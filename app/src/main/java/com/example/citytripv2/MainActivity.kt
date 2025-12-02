package com.example.citytripv2

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.citytripv2.ui.screens.MainScreen
import com.example.citytripv2.ui.auth.LoginScreen
import com.example.citytripv2.ui.auth.RegisterScreen
import com.example.citytripv2.ui.theme.CityTripV2Theme
import com.google.firebase.auth.FirebaseAuth

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge() // Zorgt dat de app het hele scherm gebruikt (status bar transparant)

        val auth = FirebaseAuth.getInstance()
        // Als er al een gebruiker is ingelogd -> ga direct naar "main" (de tabs)
        // Anders -> ga naar "login"
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

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        // --- ROUTE 1: Login ---
        composable("login") {
            LoginScreen(
                onLoginSuccess = {
                    // Als inloggen lukt, ga naar het hoofdscherm
                    navController.navigate("main") {
                        // Verwijder het login scherm uit de 'terug'-geschiedenis
                        // zodat de gebruiker niet terug kan swipen naar het inlogscherm
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
                    // Als registreren lukt, ga naar het hoofdscherm
                    navController.navigate("main") {
                        popUpTo("login") { inclusive = true }
                    }
                },
                onNavigateToLogin = {
                    navController.popBackStack() // Ga terug naar login
                }
            )
        }

        // --- ROUTE 3: Hoofd App (Met Tabs: Home, Chats, Profile) ---
        composable("main") {
            MainScreen(
                onLogout = {
                    auth.signOut() // Firebase uitloggen
                    // Ga terug naar login en wis de hele navigatiegeschiedenis
                    navController.navigate("login") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
    }
}