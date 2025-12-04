package com.example.citytripv2.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToAddCity: () -> Unit
) {
    val user = FirebaseAuth.getInstance().currentUser

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mijn Steden") }
            )
        },
        // 2. De grote + knop rechtsonder toegevoegd
        floatingActionButton = {
            FloatingActionButton(onClick = onNavigateToAddCity) {
                Icon(Icons.Default.Add, contentDescription = "Stad Toevoegen")
            }
        }
    ) { padding ->
        // 3. Inhoud aangepast van Kaart naar Lijst (Placeholder)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                contentPadding = PaddingValues(16.dp)
            ) {
                // Hier komt straks de lijst: items(cities) { city -> ... }
                item {
                    Text(
                        text = "Welkom ${user?.email}!\n\nJe hebt nog geen steden.\nKlik op + om er een toe te voegen.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.secondary,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }
    }
}