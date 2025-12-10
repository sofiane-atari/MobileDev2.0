package com.example.citytripv2.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.citytripv2.model.CityTripLocation
import com.example.citytripv2.viewmodel.HomeViewModel
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterCityScreen(
    onNavigateBack: () -> Unit,
    viewModel: HomeViewModel = viewModel()
) {
    var cityName by remember { mutableStateOf("") }
    var country by remember { mutableStateOf("") }
    val userId = FirebaseAuth.getInstance().currentUser?.uid

    val context = LocalContext.current

    // Kleuren
    val headerBlue = Color(0xFF0089D0)
    val headerGreen = Color(0xFF00A88F)
    val bgGradient = Brush.horizontalGradient(listOf(headerBlue, headerGreen))

    Scaffold(
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(bgGradient)
                    .statusBarsPadding()
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Terug",
                            tint = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Nieuwe Stad",
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFF5F5F5))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.LocationCity,
                        contentDescription = null,
                        tint = headerBlue,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "Maak hier een stad aan (bv. Parijs). Daarna kun je spots aan deze stad koppelen.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = cityName,
                onValueChange = { cityName = it },
                label = { Text("Stadsnaam") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedBorderColor = headerBlue,
                    focusedLabelColor = headerBlue
                )
            )

            OutlinedTextField(
                value = country,
                onValueChange = { country = it },
                label = { Text("Land") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedBorderColor = headerBlue,
                    focusedLabelColor = headerBlue
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (cityName.isNotBlank() && country.isNotBlank() && userId != null) {
                        val newCity = CityTripLocation(
                            id = System.currentTimeMillis().toString(),
                            name = cityName,
                            city = cityName,
                            country = country,
                            description = "Hoofdstad/Regio",
                            category = "Stad",
                            latitude = 0.0,
                            longitude = 0.0,
                            userId = userId
                        )

                        viewModel.addLocation(newCity, null, context) {
                            onNavigateBack()
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = headerBlue),
                shape = RoundedCornerShape(8.dp),
                enabled = cityName.isNotBlank() && country.isNotBlank()
            ) {
                Text(
                    text = "Stad Opslaan",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}