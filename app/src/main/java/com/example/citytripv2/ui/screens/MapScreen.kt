package com.example.citytripv2.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.citytripv2.model.CityTripLocation
import com.example.citytripv2.viewmodel.HomeViewModel
import com.google.android.gms.location.LocationServices
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    onNavigateToDetail: (String) -> Unit,
    viewModel: HomeViewModel = viewModel()
) {
    val context = LocalContext.current
    val locations by viewModel.locations.collectAsState()

    // Filter states
    var selectedCity by remember { mutableStateOf<String?>(null) }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var showFilterDialog by remember { mutableStateOf(false) }

    // User location state
    var userLocation by remember { mutableStateOf<Location?>(null) }
    var hasLocationPermission by remember { 
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    // Selected location for distance display
    var selectedLocation by remember { mutableStateOf<CityTripLocation?>(null) }
    var distanceToUser by remember { mutableStateOf<Float?>(null) }

    // Permission launcher
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasLocationPermission = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        
        if (hasLocationPermission) {
            getUserLocation(context) { location ->
                userLocation = location
            }
        }
    }

    // Request location permission on launch
    LaunchedEffect(Unit) {
        if (!hasLocationPermission) {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        } else {
            getUserLocation(context) { location ->
                userLocation = location
            }
        }
    }

    // Filter locations based on selected city and category
    val filteredLocations = remember(locations, selectedCity, selectedCategory) {
        locations.filter { location ->
            val cityMatch = selectedCity == null || location.city == selectedCity
            val categoryMatch = selectedCategory == null || location.category == selectedCategory
            cityMatch && categoryMatch
        }
    }

    // Get unique cities and categories
    val cities = remember(locations) { 
        locations.map { it.city }.distinct().sorted() 
    }
    val categories = remember(locations) { 
        locations.map { it.category }.distinct().sorted() 
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Map") },
                actions = {
                    IconButton(onClick = { showFilterDialog = true }) {
                        Icon(Icons.Default.FilterList, contentDescription = "Filters")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Active filters display
            if (selectedCity != null || selectedCategory != null) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    tonalElevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier
                            .padding(8.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        selectedCity?.let { city ->
                            FilterChip(
                                selected = true,
                                onClick = { selectedCity = null },
                                label = { Text("Stad: $city") }
                            )
                        }
                        selectedCategory?.let { category ->
                            FilterChip(
                                selected = true,
                                onClick = { selectedCategory = null },
                                label = { Text("Categorie: $category") }
                            )
                        }
                    }
                }
            }

            // Distance info card
            if (selectedLocation != null && distanceToUser != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = selectedLocation!!.name,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Afstand: %.2f km".format(distanceToUser!! / 1000),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        TextButton(onClick = { 
                            onNavigateToDetail(selectedLocation!!.id) 
                        }) {
                            Text("Bekijk details")
                        }
                    }
                }
            }

            // Map view
            AndroidView(
                factory = { ctx ->
                    Configuration.getInstance().load(
                        ctx,
                        ctx.getSharedPreferences("osmdroid", Context.MODE_PRIVATE)
                    )
                    
                    MapView(ctx).apply {
                        setTileSource(TileSourceFactory.MAPNIK)
                        setMultiTouchControls(true)
                        
                        // Set initial position (Belgium center)
                        controller.setZoom(8.0)
                        controller.setCenter(GeoPoint(50.8503, 4.3517)) // Brussels
                    }
                },
                modifier = Modifier.fillMaxSize(),
                update = { mapView ->
                    mapView.overlays.clear()
                    
                    // Add markers for filtered locations
                    filteredLocations.forEach { location ->
                        val marker = Marker(mapView).apply {
                            position = GeoPoint(location.latitude, location.longitude)
                            title = location.name
                            snippet = "${location.city} - ${location.category}"
                            
                            setOnMarkerClickListener { clickedMarker, _ ->
                                selectedLocation = location
                                
                                // Calculate distance if user location is available
                                if (userLocation != null) {
                                    val distance = calculateDistance(
                                        userLocation!!.latitude,
                                        userLocation!!.longitude,
                                        location.latitude,
                                        location.longitude
                                    )
                                    distanceToUser = distance
                                } else {
                                    Toast.makeText(
                                        context,
                                        "Locatie permissie nodig voor afstandsberekening",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                                
                                true
                            }
                        }
                        mapView.overlays.add(marker)
                    }
                    
                    mapView.invalidate()
                }
            )
        }

        // Filter dialog
        if (showFilterDialog) {
            AlertDialog(
                onDismissRequest = { showFilterDialog = false },
                title = { Text("Filters") },
                text = {
                    Column {
                        Text("Stad", style = MaterialTheme.typography.titleSmall)
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(vertical = 8.dp)
                        ) {
                            item {
                                FilterChip(
                                    selected = selectedCity == null,
                                    onClick = { selectedCity = null },
                                    label = { Text("Alle") }
                                )
                            }
                            items(cities) { city ->
                                FilterChip(
                                    selected = selectedCity == city,
                                    onClick = { selectedCity = city },
                                    label = { Text(city) }
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text("Categorie", style = MaterialTheme.typography.titleSmall)
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(vertical = 8.dp)
                        ) {
                            item {
                                FilterChip(
                                    selected = selectedCategory == null,
                                    onClick = { selectedCategory = null },
                                    label = { Text("Alle") }
                                )
                            }
                            items(categories) { category ->
                                FilterChip(
                                    selected = selectedCategory == category,
                                    onClick = { selectedCategory = category },
                                    label = { Text(category) }
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showFilterDialog = false }) {
                        Text("Sluiten")
                    }
                }
            )
        }
    }
}

// Get user's current location
private fun getUserLocation(context: Context, onLocationReceived: (Location) -> Unit) {
    try {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let { onLocationReceived(it) }
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

// Calculate distance between two points using Haversine formula
private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
    val earthRadius = 6371000.0 // meters
    
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    
    val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(dLon / 2) * sin(dLon / 2)
    
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    
    return (earthRadius * c).toFloat()
}
