package com.example.citytripv2.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onLogout: () -> Unit
) {
    val user = FirebaseAuth.getInstance().currentUser

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Mijn Profiel") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Ingelogd als:", style = MaterialTheme.typography.titleMedium)
            Text(user?.email ?: "Onbekend", style = MaterialTheme.typography.bodyLarge)

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = onLogout,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Uitloggen")
            }
        }
    }
}