package com.example.citytripv2.ui.screens

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCityScreen(
    onNavigateBack: () -> Unit,
    onCityAdded: (City) -> Unit
) {
    val context = LocalContext.current
    var cityName by remember { mutableStateOf("") }
    var country by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedLatitude by remember { mutableStateOf(52.3676) }
    var selectedLongitude by remember { mutableStateOf(4.9041) }
    var showSuccessDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Stad Toevoegen") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Terug")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Voeg een nieuwe stad toe",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            OutlinedTextField(
                value = cityName,
                onValueChange = { cityName = it },
                label = { Text("Stadsnaam *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = country,
                onValueChange = { country = it },
                label = { Text("Land *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Beschrijving") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                maxLines = 5
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    if (cityName.isNotBlank() && country.isNotBlank()) {
                        val newCity = City(
                            id = System.currentTimeMillis().toString(),
                            name = cityName,
                            country = country,
                            description = description.ifBlank { null },
                            latitude = selectedLatitude,
                            longitude = selectedLongitude,
                            locations = emptyList()
                        )
                        onCityAdded(newCity)
                        showSuccessDialog = true
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = cityName.isNotBlank() && country.isNotBlank()
            ) {
                Text("Stad Opslaan")
            }

            Text(
                text = "* Verplichte velden",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = {
                showSuccessDialog = false
                onNavigateBack()
            },
            title = { Text("Succes!") },
            text = { Text("De stad '$cityName' is succesvol toegevoegd. Je kunt nu locaties toevoegen.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSuccessDialog = false
                        onNavigateBack()
                    }
                ) {
                    Text("OK")
                }
            }
        )
    }
}

@Composable
fun MapViewComposable(
    modifier: Modifier = Modifier,
    context: Context,
    initialLatitude: Double,
    initialLongitude: Double,
    onLocationSelected: (Double, Double) -> Unit
) {
    val mapView = remember {
        // Configureer OSMDroid
        Configuration.getInstance().apply {
            userAgentValue = context.packageName
            osmdroidBasePath = context.filesDir
            osmdroidTileCache = context.cacheDir
        }
        
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            setBuiltInZoomControls(false)
            controller.setZoom(10.0)
            controller.setCenter(GeoPoint(initialLatitude, initialLongitude))
            minZoomLevel = 3.0
            maxZoomLevel = 19.0
        }
    }

    var marker by remember { mutableStateOf<Marker?>(null) }

    DisposableEffect(Unit) {
        mapView.overlays.add(object : org.osmdroid.views.overlay.Overlay() {
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
        })

        val initialMarker = Marker(mapView).apply {
            position = GeoPoint(initialLatitude, initialLongitude)
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            title = "Geselecteerde locatie"
        }
        mapView.overlays.add(initialMarker)
        marker = initialMarker

        onDispose {
            mapView.onDetach()
        }
    }

    AndroidView(
        factory = { mapView },
        modifier = modifier
    )
}

data class City(
    val id: String,
    val name: String,
    val country: String,
    val description: String? = null,
    val latitude: Double,
    val longitude: Double,
    val locations: List<Location> = emptyList()
)

data class Location(
    val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val description: String? = null,
    val cityId: String
)
