package com.example.citytripv2.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.citytripv2.model.CityTripLocation
import com.example.citytripv2.viewmodel.HomeViewModel
import androidx.compose.foundation.Image
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.clickable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToAddCity: () -> Unit,
    onNavigateToDetail: (String) -> Unit,
    viewModel: HomeViewModel = viewModel()
) {
    val allLocations by viewModel.locations.collectAsState()

    // --- STATES VOOR FILTEREN ---
    var showFilterDialog by remember { mutableStateOf(false) }
    var selectedCityFilter by remember { mutableStateOf<String?>(null) }
    var selectedCategoryFilter by remember { mutableStateOf<String?>(null) }
    var searchText by remember { mutableStateOf("") }

    // 1. UPDATE: Haal steden op uit ALLE items (ook spots van anderen)
    val availableCities = allLocations
        .map { it.city }
        .filter { it.isNotBlank() }
        .distinct()
        .sorted()

    // 2. Vaste lijst categorieën
    val categories = listOf("Restaurant", "Cafe", "Natuur", "Hotel/Huis", "Bezienswaardigheid")

    // 3. Pas de filters toe
    val filteredLocations = allLocations.filter { location ->
        if (location.category == "Stad") return@filter false

        if (searchText.isNotBlank()) {
            val matchName = location.name.contains(searchText, ignoreCase = true)
            val matchDesc = location.description?.contains(searchText, ignoreCase = true) == true
            if (!matchName && !matchDesc) return@filter false
        }

        if (selectedCityFilter != null && location.city != selectedCityFilter) return@filter false
        if (selectedCategoryFilter != null && location.category != selectedCategoryFilter) return@filter false

        true
    }

    // Kleuren
    val headerBlue = Color(0xFF0089D0)
    val headerGreen = Color(0xFF00A88F)
    val bgGradient = Brush.horizontalGradient(listOf(headerBlue, headerGreen))

    Scaffold(
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .background(bgGradient)
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Zoekbalk
                    TextField(
                        value = searchText,
                        onValueChange = { searchText = it },
                        placeholder = { Text("Zoek op naam...", color = Color.Gray) },
                        leadingIcon = { Icon(Icons.Default.Search, null, tint = Color.Gray) },
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(24.dp)),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            disabledIndicatorColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        singleLine = true
                    )

                    // Filter Knop
                    val hasActiveFilters = selectedCityFilter != null || selectedCategoryFilter != null
                    IconButton(
                        onClick = { showFilterDialog = true },
                        modifier = Modifier
                            .size(56.dp)
                            .background(Color.White, CircleShape)
                    ) {
                        BadgedBox(
                            badge = {
                                if (hasActiveFilters) {
                                    Badge { Text("!") }
                                }
                            }
                        ) {
                            Icon(
                                Icons.Default.FilterList,
                                contentDescription = "Filter",
                                tint = headerBlue
                            )
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToAddCity,
                containerColor = Color(0xFF0089D0),
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Toevoegen")
            }
        },
        floatingActionButtonPosition = FabPosition.Center
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFF5F5F5))
        ) {
            if (filteredLocations.isEmpty()) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (allLocations.isEmpty()) "Nog geen locaties.\nKlik op + om te beginnen!"
                        else "Geen resultaten gevonden voor je filters.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.Gray,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )

                    if (selectedCityFilter != null || selectedCategoryFilter != null) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = {
                            selectedCityFilter = null
                            selectedCategoryFilter = null
                            searchText = ""
                        }, colors = ButtonDefaults.buttonColors(containerColor = headerBlue)) {
                            Text("Wis filters")
                        }
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp, start = 16.dp, end = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredLocations) { location ->
                        LocationCard(
                            location = location,
                            // Geef de click door
                            onClick = { onNavigateToDetail(location.id) }
                        )
                    }
                }
            }
        }
    }

    // --- FILTER DIALOOG ---
    if (showFilterDialog) {
        FilterDialog(
            availableCities = availableCities,
            categories = categories,
            currentCity = selectedCityFilter,
            currentCategory = selectedCategoryFilter,
            onApply = { city, category ->
                selectedCityFilter = city
                selectedCategoryFilter = category
                showFilterDialog = false
            },
            onDismiss = { showFilterDialog = false },
            onClear = {
                selectedCityFilter = null
                selectedCategoryFilter = null
                showFilterDialog = false
            }
        )
    }
}

// --- GESTYLEDE FILTER DIALOOG ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterDialog(
    availableCities: List<String>,
    categories: List<String>,
    currentCity: String?,
    currentCategory: String?,
    onApply: (String?, String?) -> Unit,
    onDismiss: () -> Unit,
    onClear: () -> Unit
) {
    var tempCity by remember { mutableStateOf(currentCity) }
    var tempCategory by remember { mutableStateOf(currentCategory) }

    var isCityExpanded by remember { mutableStateOf(false) }
    var isCategoryExpanded by remember { mutableStateOf(false) }

    // De hoofdkleur (Blue)
    val headerBlue = Color(0xFF0089D0)

    AlertDialog(
        containerColor = Color.White, // Witte achtergrond
        titleContentColor = headerBlue, // Blauwe titel
        shape = RoundedCornerShape(16.dp), // Ronde hoeken
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Filter Locaties",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Dropdown 1: Stad
                ExposedDropdownMenuBox(
                    expanded = isCityExpanded,
                    onExpandedChange = { isCityExpanded = it }
                ) {
                    OutlinedTextField(
                        value = tempCity ?: "Alle steden",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Stad") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isCityExpanded) },
                        // Custom kleuren voor de dropdowns
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedBorderColor = headerBlue,
                            unfocusedBorderColor = Color.LightGray,
                            focusedLabelColor = headerBlue,
                            unfocusedLabelColor = Color.Gray
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = isCityExpanded,
                        onDismissRequest = { isCityExpanded = false },
                        modifier = Modifier.background(Color.White)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Alle steden") },
                            onClick = { tempCity = null; isCityExpanded = false }
                        )
                        availableCities.forEach { city ->
                            DropdownMenuItem(
                                text = { Text(city) },
                                onClick = { tempCity = city; isCityExpanded = false }
                            )
                        }
                    }
                }

                // Dropdown 2: Categorie
                ExposedDropdownMenuBox(
                    expanded = isCategoryExpanded,
                    onExpandedChange = { isCategoryExpanded = it }
                ) {
                    OutlinedTextField(
                        value = tempCategory ?: "Alle categorieën",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Categorie") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isCategoryExpanded) },
                        // Custom kleuren
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedBorderColor = headerBlue,
                            unfocusedBorderColor = Color.LightGray,
                            focusedLabelColor = headerBlue,
                            unfocusedLabelColor = Color.Gray
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = isCategoryExpanded,
                        onDismissRequest = { isCategoryExpanded = false },
                        modifier = Modifier.background(Color.White)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Alle categorieën") },
                            onClick = { tempCategory = null; isCategoryExpanded = false }
                        )
                        categories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat) },
                                onClick = { tempCategory = cat; isCategoryExpanded = false }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onApply(tempCity, tempCategory) },
                colors = ButtonDefaults.buttonColors(containerColor = headerBlue),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Toepassen", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            Row {
                if (currentCity != null || currentCategory != null) {
                    TextButton(
                        onClick = onClear,
                        colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFFA5A5A))
                    ) {
                        Text("Wissen")
                    }
                }
                TextButton(
                    onClick = onDismiss,
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.Gray)
                ) {
                    Text("Annuleren")
                }
            }
        }
    )
}

@Composable
fun LocationCard(location: CityTripLocation, onClick: () -> Unit) {
    val imageBitmap: ImageBitmap? = remember(location.imageUrl) {
        if (location.imageUrl != null) {
            base64ToBitmap(location.imageUrl)
        } else {
            null
        }
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .clickable { onClick() }
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.65f)
            ) {
                if (imageBitmap != null) {
                    Image(
                        bitmap = imageBitmap,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
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

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(12.dp)
                ) {
                    Text(
                        text = location.name,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        ),
                        color = Color.White
                    )
                    Text(
                        text = location.country,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.35f)
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = Color(0xFF5A78FA),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = location.category,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (location.rating > 0) String.format("%.1f", location.rating) else "Nieuw",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.DarkGray
                    )
                    Spacer(modifier = Modifier.width(4.dp))

                    val starColor = Color(0xFFFFC107)
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

fun base64ToBitmap(base64String: String): ImageBitmap? {
    return try {
        val pureBase64 = if (base64String.contains(",")) {
            base64String.split(",")[1]
        } else {
            base64String
        }

        val decodedBytes = Base64.decode(pureBase64, Base64.DEFAULT)
        val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
        bitmap?.asImageBitmap()
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}