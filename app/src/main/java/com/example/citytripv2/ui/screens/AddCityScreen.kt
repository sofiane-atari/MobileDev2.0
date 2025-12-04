package com.example.citytripv2.ui.screens

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.citytripv2.model.CityTripLocation
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCityScreen(
    onNavigateBack: () -> Unit,
    onCityAdded: (CityTripLocation) -> Unit
) {
    val context = LocalContext.current

    // --- STATE VARIABELEN ---
    var cityName by remember { mutableStateOf("") }
    var country by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    // Categorie Selectie (ZONDER 'Stad')
    val categories = listOf("Restaurant", "Cafe", "Nature", "Housing", "Bezienswaardigheid")
    var selectedCategory by remember { mutableStateOf(categories[0]) }
    var isCategoryExpanded by remember { mutableStateOf(false) }

    // Locatie (Default: Amsterdam)
    var selectedLatitude by remember { mutableStateOf(52.3676) }
    var selectedLongitude by remember { mutableStateOf(4.9041) }

    var showSuccessDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Locatie Toevoegen") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Terug")
                    }
                }
            )
        }
    ) { padding ->
        // We gebruiken een Column zonder scroll voor de hoofdindeling
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // DEEL 1: De Kaart (Vaste hoogte of gewicht, scrollt NIET mee)
            // Dit lost het probleem op van de springende kaart
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.4f) // Neemt 40% van het scherm in
            ) {
                MapViewComposable(
                    modifier = Modifier.fillMaxSize(),
                    context = context,
                    initialLatitude = selectedLatitude,
                    initialLongitude = selectedLongitude,
                    onLocationSelected = { lat, lon ->
                        selectedLatitude = lat
                        selectedLongitude = lon
                    }
                )
            }

            // DEEL 2: Het Formulier (Scrollbaar gedeelte)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.6f) // Neemt de rest (60%) in
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()), // Alleen dit deel scrollt
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Details invullen",
                    style = MaterialTheme.typography.titleMedium
                )

                Text(
                    text = "Geselecteerd: ${String.format("%.4f", selectedLatitude)}, ${String.format("%.4f", selectedLongitude)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )

                // 1. Categorie Dropdown
                ExposedDropdownMenuBox(
                    expanded = isCategoryExpanded,
                    onExpandedChange = { isCategoryExpanded = !isCategoryExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = selectedCategory,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Categorie *") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isCategoryExpanded) },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = isCategoryExpanded,
                        onDismissRequest = { isCategoryExpanded = false }
                    ) {
                        categories.forEach { category ->
                            DropdownMenuItem(
                                text = { Text(category) },
                                onClick = {
                                    selectedCategory = category
                                    isCategoryExpanded = false
                                }
                            )
                        }
                    }
                }

                // 2. Naam (Stad)
                OutlinedTextField(
                    value = cityName,
                    onValueChange = { cityName = it },
                    label = { Text("Stad (bv. Parijs) *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // 3. Land
                OutlinedTextField(
                    value = country,
                    onValueChange = { country = it },
                    label = { Text("Land (bv. Frankrijk) *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // 4. Beschrijving
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Beschrijving / Naam van plek") },
                    placeholder = { Text("bv. Lekker gegeten bij Chez Pierre...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    maxLines = 5
                )

                // --- OPSLAAN KNOP ---
                Button(
                    onClick = {
                        if (cityName.isNotBlank() && country.isNotBlank()) {
                            val newLocation = CityTripLocation(
                                id = System.currentTimeMillis().toString(),
                                name = if (description.isNotBlank()) description.take(20) else cityName, // Naam is korte beschrijving of stad
                                city = cityName,
                                country = country,
                                description = description.ifBlank { null },
                                latitude = selectedLatitude,
                                longitude = selectedLongitude,
                                category = selectedCategory,
                                imageUrl = null,
                                rating = 0f
                            )
                            onCityAdded(newLocation)
                            showSuccessDialog = true
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    enabled = cityName.isNotBlank() && country.isNotBlank()
                ) {
                    Text("Opslaan")
                }

                // Ruimte onderaan voor toetsenbord
                Spacer(modifier = Modifier.height(300.dp))
            }
        }
    }

    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = {
                showSuccessDialog = false
                onNavigateBack()
            },
            title = { Text("Succes!") },
            text = { Text("Locatie in $cityName is opgeslagen.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSuccessDialog = false
                        onNavigateBack()
                    }
                ) { Text("OK") }
            }
        )
    }
}

// --- DE KAART COMPONENT ---
// Omdat de kaart nu in een eigen Box staat (en niet in een ScrollView),
// hoeven we die ingewikkelde TouchListener hack niet meer te gebruiken!
@Composable
fun MapViewComposable(
    modifier: Modifier = Modifier,
    context: Context,
    initialLatitude: Double,
    initialLongitude: Double,
    onLocationSelected: (Double, Double) -> Unit
) {
    val mapView = remember {
        Configuration.getInstance().apply {
            userAgentValue = context.packageName
            osmdroidBasePath = context.filesDir
            osmdroidTileCache = context.cacheDir
        }

        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            setBuiltInZoomControls(true)
            controller.setZoom(10.0)
            controller.setCenter(GeoPoint(initialLatitude, initialLongitude))
            minZoomLevel = 3.0
            maxZoomLevel = 19.0
        }
    }

    var marker by remember { mutableStateOf<Marker?>(null) }

    DisposableEffect(Unit) {
        val tapOverlay = object : org.osmdroid.views.overlay.Overlay() {
            override fun onSingleTapConfirmed(e: android.view.MotionEvent?, mapView: MapView?): Boolean {
                if (e != null && mapView != null) {
                    val projection = mapView.projection
                    val geoPoint = projection.fromPixels(e.x.toInt(), e.y.toInt()) as GeoPoint

                    onLocationSelected(geoPoint.latitude, geoPoint.longitude)

                    marker?.let { mapView.overlays.remove(it) }

                    val newMarker = Marker(mapView).apply {
                        position = geoPoint
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        title = "Geselecteerde locatie"
                    }
                    mapView.overlays.add(newMarker)
                    marker = newMarker

                    mapView.invalidate()
                    return true
                }
                return false
            }
        }

        mapView.overlays.add(tapOverlay)

        val startMarker = Marker(mapView).apply {
            position = GeoPoint(initialLatitude, initialLongitude)
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            title = "Start"
        }
        mapView.overlays.add(startMarker)
        marker = startMarker

        onDispose {
            mapView.onDetach()
        }
    }

    AndroidView(
        factory = { mapView },
        modifier = modifier
    )
}