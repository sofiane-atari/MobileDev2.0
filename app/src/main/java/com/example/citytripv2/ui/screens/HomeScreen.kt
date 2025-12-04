package com.example.citytripv2.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.citytripv2.model.CityTripLocation
import com.example.citytripv2.viewmodel.HomeViewModel
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToAddCity: () -> Unit,
    viewModel: HomeViewModel = viewModel()
) {
    val locations by viewModel.locations.collectAsState()

    // Kleuren uit je voorbeeld afbeelding (Ongeveer)
    val headerBlue = Color(0xFF0089D0)
    val headerGreen = Color(0xFF00A88F)
    val bgGradient = Brush.horizontalGradient(listOf(headerBlue, headerGreen))

    Scaffold(
        // We maken onze eigen header, dus geen standaard TopAppBar
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .background(bgGradient)
                    .padding(16.dp)
            ) {
                // Titel of Search bar
                TextField(
                    value = "",
                    onValueChange = {},
                    placeholder = { Text("Search cities...", color = Color.Gray) },
                    leadingIcon = { Icon(Icons.Default.Search, null, tint = Color.Gray) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .clip(RoundedCornerShape(24.dp)), // Ronde zoekbalk
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        disabledIndicatorColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    )
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToAddCity,
                containerColor = Color(0xFF0089D0), // Blauwe kleur van de FAB
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Toevoegen")
            }
        },
        floatingActionButtonPosition = FabPosition.Center // FAB in het midden zoals op de foto
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFF5F5F5)) // Lichte achtergrond
        ) {
            if (locations.isEmpty()) {
                Text(
                    text = "Nog geen steden.\nKlik op + om te beginnen!",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.Gray,
                    modifier = Modifier.align(Alignment.Center),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp, start = 16.dp, end = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(locations) { location ->
                        LocationCard(location)
                    }
                }
            }
        }
    }
}

@Composable
fun LocationCard(location: CityTripLocation) {
    Card(
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
    ) {
        Column {
            // --- DE AFBEELDING ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.65f) // Afbeelding neemt 65% van kaart in
            ) {
                // Als er een URL is, laad die. Anders een placeholder kleur/gradient.
                if (location.imageUrl != null) {
                    AsyncImage(
                        model = location.imageUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    // Placeholder als er geen foto is (bv. een grijze box)
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.LightGray)
                    ) {
                        Icon(
                            Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.align(Alignment.Center).size(48.dp)
                        )
                    }
                }

                // Gradient Overlay voor leesbare tekst
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f)),
                                startY = 100f
                            )
                        )
                )

                // Tekst op de foto (Naam & Land)
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(12.dp)
                ) {
                    Text(
                        text = location.name, // "Paris"
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        ),
                        color = Color.White
                    )
                    Text(
                        text = location.country, // "France"
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
            }

            // --- DE INFO BALK ONDERAAN ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.35f) // Rest van de ruimte
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Linkerkant: Categorie / Locaties count
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = Color(0xFF5A78FA), // Blauw icoontje
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = location.category, // Bv. "Stad" of "24 locations"
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                }

                // Rechterkant: Rating & Sterren
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Cijfer (bv. 4.8)
                    Text(
                        text = if (location.rating > 0) String.format("%.1f", location.rating) else "Nieuw",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.DarkGray
                    )
                    Spacer(modifier = Modifier.width(4.dp))

                    // Sterren (vast 5 sterren tonen)
                    // (Voor nu hardcoded geel, later dynamisch maken obv rating)
                    val starColor = Color(0xFFFFC107) // Goud/Geel
                    Row {
                        repeat(5) { index ->
                            Icon(
                                imageVector = if (index < location.rating) Icons.Default.Star else Icons.Outlined.StarOutline,
                                contentDescription = null,
                                tint = starColor,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}