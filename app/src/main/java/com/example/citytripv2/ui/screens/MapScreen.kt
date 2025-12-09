package com.example.citytripv2.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
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
                    
                    // Add user location marker if available
                    if (userLocation != null) {
                        val userMarker = Marker(mapView).apply {
                            position = GeoPoint(userLocation!!.latitude, userLocation!!.longitude)
                            title = "Jouw Locatie"
                            snippet = "Je bent hier"
                            icon = createCustomMarker(context, "user")
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        }
                        mapView.overlays.add(userMarker)
                    }
                    
                    // Add markers for filtered locations
                    filteredLocations.forEach { location ->
                        val marker = Marker(mapView).apply {
                            position = GeoPoint(location.latitude, location.longitude)
                            title = location.name
                            snippet = "${location.city} - ${location.category}"
                            icon = createCustomMarker(context, location.category)
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            
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

// Create custom marker based on category
private fun createCustomMarker(context: Context, category: String): android.graphics.drawable.Drawable {
    val size = 80
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    
    // Define colors and shapes for each category
    when (category.lowercase()) {
        "user" -> {
            // Blue circle with dot for user location
            paint.color = Color.parseColor("#2196F3")
            paint.style = Paint.Style.FILL
            canvas.drawCircle(size / 2f, size / 2f, 20f, paint)
            
            paint.color = Color.WHITE
            canvas.drawCircle(size / 2f, size / 2f, 8f, paint)
            
            paint.color = Color.parseColor("#2196F3")
            paint.strokeWidth = 4f
            paint.style = Paint.Style.STROKE
            canvas.drawCircle(size / 2f, size / 2f, 28f, paint)
        }
        "stad" -> {
            // Purple star for cities
            paint.color = Color.parseColor("#9C27B0")
            paint.style = Paint.Style.FILL
            drawStar(canvas, paint, size / 2f, size / 2f, 25f, 12f)
            
            paint.color = Color.WHITE
            paint.strokeWidth = 3f
            paint.style = Paint.Style.STROKE
            drawStar(canvas, paint, size / 2f, size / 2f, 25f, 12f)
        }
        "hotel" -> {
            // Orange house/building for hotels
            paint.color = Color.parseColor("#FF9800")
            paint.style = Paint.Style.FILL
            canvas.drawRect(size / 2f - 20, size / 2f - 10, size / 2f + 20, size / 2f + 20, paint)
            
            // Roof
            val roofPath = Path()
            roofPath.moveTo(size / 2f, size / 2f - 25)
            roofPath.lineTo(size / 2f - 25, size / 2f - 10)
            roofPath.lineTo(size / 2f + 25, size / 2f - 10)
            roofPath.close()
            canvas.drawPath(roofPath, paint)
            
            // Border
            paint.color = Color.WHITE
            paint.strokeWidth = 3f
            paint.style = Paint.Style.STROKE
            canvas.drawRect(size / 2f - 20, size / 2f - 10, size / 2f + 20, size / 2f + 20, paint)
            canvas.drawPath(roofPath, paint)
        }
        "horeca" -> {
            // Red fork and knife for restaurants
            paint.color = Color.parseColor("#F44336")
            paint.style = Paint.Style.FILL
            canvas.drawCircle(size / 2f, size / 2f, 22f, paint)
            
            paint.color = Color.WHITE
            paint.strokeWidth = 4f
            paint.style = Paint.Style.STROKE
            
            // Fork
            canvas.drawLine(size / 2f - 8, size / 2f - 10, size / 2f - 8, size / 2f + 10, paint)
            canvas.drawLine(size / 2f - 12, size / 2f - 10, size / 2f - 12, size / 2f, paint)
            canvas.drawLine(size / 2f - 4, size / 2f - 10, size / 2f - 4, size / 2f, paint)
            
            // Knife
            canvas.drawLine(size / 2f + 8, size / 2f - 10, size / 2f + 8, size / 2f + 10, paint)
            canvas.drawCircle(size / 2f + 8, size / 2f - 10, 3f, paint)
        }
        "bezienswaardigheid" -> {
            // Green camera for attractions
            paint.color = Color.parseColor("#4CAF50")
            paint.style = Paint.Style.FILL
            canvas.drawRoundRect(size / 2f - 22, size / 2f - 15, size / 2f + 22, size / 2f + 15, 5f, 5f, paint)
            
            // Lens
            paint.color = Color.parseColor("#81C784")
            canvas.drawCircle(size / 2f + 5, size / 2f, 10f, paint)
            
            // Flash
            paint.color = Color.WHITE
            canvas.drawRect(size / 2f - 18, size / 2f - 20, size / 2f - 10, size / 2f - 15, paint)
            
            // Border
            paint.color = Color.WHITE
            paint.strokeWidth = 3f
            paint.style = Paint.Style.STROKE
            canvas.drawRoundRect(size / 2f - 22, size / 2f - 15, size / 2f + 22, size / 2f + 15, 5f, 5f, paint)
        }
        else -> {
            // Gray pin for other/default
            paint.color = Color.parseColor("#9E9E9E")
            paint.style = Paint.Style.FILL
            
            // Pin shape
            val pinPath = Path()
            pinPath.moveTo(size / 2f, size / 2f + 25)
            pinPath.lineTo(size / 2f - 20, size / 2f - 10)
            pinPath.quadTo(size / 2f - 20, size / 2f - 25, size / 2f, size / 2f - 25)
            pinPath.quadTo(size / 2f + 20, size / 2f - 25, size / 2f + 20, size / 2f - 10)
            pinPath.close()
            canvas.drawPath(pinPath, paint)
            
            paint.color = Color.WHITE
            canvas.drawCircle(size / 2f, size / 2f - 10, 8f, paint)
        }
    }
    
    return android.graphics.drawable.BitmapDrawable(context.resources, bitmap)
}

// Helper function to draw a star
private fun drawStar(canvas: Canvas, paint: Paint, cx: Float, cy: Float, outerRadius: Float, innerRadius: Float) {
    val path = Path()
    val angle = Math.PI / 5.0
    
    for (i in 0..9) {
        val radius = if (i % 2 == 0) outerRadius else innerRadius
        val currentAngle = i * angle - Math.PI / 2
        val x = (cx + radius * cos(currentAngle)).toFloat()
        val y = (cy + radius * sin(currentAngle)).toFloat()
        
        if (i == 0) {
            path.moveTo(x, y)
        } else {
            path.lineTo(x, y)
        }
    }
    path.close()
    canvas.drawPath(path, paint)
}
