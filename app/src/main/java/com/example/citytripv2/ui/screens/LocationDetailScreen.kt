package com.example.citytripv2.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.citytripv2.model.CityTripLocation
import com.example.citytripv2.viewmodel.HomeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationDetailScreen(
    locationId: String,
    onNavigateBack: () -> Unit,
    viewModel: HomeViewModel = viewModel()
) {
    var location by remember { mutableStateOf<CityTripLocation?>(null) }

    // Haal locatie op bij start
    LaunchedEffect(locationId) {
        viewModel.getLocationById(locationId) { loc ->
            location = loc
        }
    }

    val headerBlue = Color(0xFF0089D0)
    val headerGreen = Color(0xFF00A88F)
    val bgGradient = Brush.horizontalGradient(listOf(headerBlue, headerGreen))

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(location?.name ?: "Details", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Terug", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                modifier = Modifier.background(bgGradient)
            )
        }
    ) { padding ->
        if (location == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            val loc = location!!
            val imageBitmap: ImageBitmap? = remember(loc.imageUrl) {
                if (loc.imageUrl != null) base64ToBitmap(loc.imageUrl) else null
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
            ) {
                // 1. Grote Foto
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp)
                        .background(Color.LightGray)
                ) {
                    if (imageBitmap != null) {
                        Image(
                            bitmap = imageBitmap,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        // Placeholder
                        Box(modifier = Modifier.fillMaxSize().background(Color.Gray))
                    }
                }

                // 2. Info
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(loc.name, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    }
                    Text("${loc.category} in ${loc.city}, ${loc.country}", color = Color.Gray)

                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Over deze plek:", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(loc.description ?: "Geen beschrijving.", style = MaterialTheme.typography.bodyLarge)

                    Divider(modifier = Modifier.padding(vertical = 24.dp))

                    // 3. Review Sectie
                    ReviewSection(
                        onSubmitReview = { rating, comment ->
                            viewModel.addReview(loc.id, rating, comment) {
                                // Refresh of toon melding
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ReviewSection(onSubmitReview: (Int, String) -> Unit) {
    var rating by remember { mutableStateOf(0) }
    var comment by remember { mutableStateOf("") }
    var isSubmitted by remember { mutableStateOf(false) }

    Column {
        Text("Schrijf een review", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))

        // Sterren Selectie
        Row {
            for (i in 1..5) {
                IconButton(onClick = { rating = i }) {
                    Icon(
                        imageVector = if (i <= rating) Icons.Default.Star else Icons.Outlined.StarOutline,
                        contentDescription = "$i sterren",
                        tint = Color(0xFFFFC107),
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }

        OutlinedTextField(
            value = comment,
            onValueChange = { comment = it },
            label = { Text("Jouw ervaring...") },
            modifier = Modifier.fillMaxWidth().height(100.dp),
            maxLines = 4
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                if (rating > 0) {
                    onSubmitReview(rating, comment)
                    isSubmitted = true
                }
            },
            enabled = rating > 0 && !isSubmitted,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0089D0)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (isSubmitted) "Bedankt voor je review!" else "Plaats Review")
        }
    }
}