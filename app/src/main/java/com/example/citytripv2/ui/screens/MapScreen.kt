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
import androidx.compose.ui.graphics.toArgb
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

    // Filter states - multiple selections
    var selectedCities by remember { mutableStateOf<Set<String>>(emptySet()) }
    var selectedCategories by remember { mutableStateOf<Set<String>>(emptySet()) }
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

    // Filter locations based on selected cities and categories
    val filteredLocations = remember(locations, selectedCities, selectedCategories) {
        locations.filter { location ->
            val cityMatch = selectedCities.isEmpty() || location.city in selectedCities
            val categoryMatch = selectedCategories.isEmpty() || location.category in selectedCategories
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

    Box(modifier = Modifier.fillMaxSize()) {
        // Map view (fullscreen)
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

        // Compact header at the top (covers status bar area)
        Surface(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 4.dp
        ) {
            Row(
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Map",
                    style = MaterialTheme.typography.titleMedium
                )
                IconButton(onClick = { showFilterDialog = true }) {
                    Icon(Icons.Default.FilterList, contentDescription = "Filters")
                }
            }
        }

        // Active filters display (below header)
        if (selectedCities.isNotEmpty() || selectedCategories.isNotEmpty()) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(top = 60.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
                tonalElevation = 2.dp
            ) {
                LazyRow(
                    modifier = Modifier.padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(selectedCities.toList()) { city ->
                        FilterChip(
                            selected = true,
                            onClick = { selectedCities = selectedCities - city },
                            label = { Text(city, style = MaterialTheme.typography.bodySmall) }
                        )
                    }
                    items(selectedCategories.toList()) { category ->
                        FilterChip(
                            selected = true,
                            onClick = { selectedCategories = selectedCategories - category },
                            label = { Text(category, style = MaterialTheme.typography.bodySmall) }
                        )
                    }
                }
            }
        }

        // Distance info card (bottom)
        if (selectedLocation != null && distanceToUser != null) {
            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = selectedLocation!!.name,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Afstand: %.2f km".format(distanceToUser!! / 1000),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TextButton(onClick = { 
                            selectedLocation = null
                            distanceToUser = null
                        }) {
                            Text("Sluiten")
                        }
                        TextButton(onClick = { 
                            onNavigateToDetail(selectedLocation!!.id) 
                        }) {
                            Text("Details")
                        }
                    }
                }
            }
        }

        // Filter dialog
        if (showFilterDialog) {
            AlertDialog(
                onDismissRequest = { showFilterDialog = false },
                title = { Text("Filters - Selecteer meerdere") },
                text = {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Steden", style = MaterialTheme.typography.titleSmall)
                            if (selectedCities.isNotEmpty()) {
                                TextButton(onClick = { selectedCities = emptySet() }) {
                                    Text("Wis alles", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(vertical = 8.dp)
                        ) {
                            items(cities) { city ->
                                FilterChip(
                                    selected = city in selectedCities,
                                    onClick = { 
                                        selectedCities = if (city in selectedCities) {
                                            selectedCities - city
                                        } else {
                                            selectedCities + city
                                        }
                                    },
                                    label = { Text(city) }
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Categorieën", style = MaterialTheme.typography.titleSmall)
                            if (selectedCategories.isNotEmpty()) {
                                TextButton(onClick = { selectedCategories = emptySet() }) {
                                    Text("Wis alles", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(vertical = 8.dp)
                        ) {
                            items(categories) { category ->
                                FilterChip(
                                    selected = category in selectedCategories,
                                    onClick = { 
                                        selectedCategories = if (category in selectedCategories) {
                                            selectedCategories - category
                                        } else {
                                            selectedCategories + category
                                        }
                                    },
                                    label = { Text(category) }
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showFilterDialog = false }) {
                        Text("Toepassen")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { 
                        selectedCities = emptySet()
                        selectedCategories = emptySet()
                    }) {
                        Text("Reset")
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
    val size = 120  // Increased from 80 to 120
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    
    // Define colors and shapes for each category
    when (category.lowercase()) {
        "user" -> {
            // Bright blue circle with gradient effect for user location
            // Outer glow
            paint.color = Color.parseColor("#40C4FF") // Lighter blue
            paint.style = Paint.Style.FILL
            canvas.drawCircle(size / 2f, size / 2f, 42f, paint)
            
            // Main circle - vibrant blue
            paint.color = Color.parseColor("#0091EA") // Vivid blue
            canvas.drawCircle(size / 2f, size / 2f, 30f, paint)
            
            // Inner white dot
            paint.color = Color.WHITE
            canvas.drawCircle(size / 2f, size / 2f, 12f, paint)
            
            // Pulsing ring effect
            paint.color = Color.parseColor("#00B0FF") // Cyan blue
            paint.strokeWidth = 6f
            paint.style = Paint.Style.STROKE
            canvas.drawCircle(size / 2f, size / 2f, 42f, paint)
        }
        "stad" -> {
            // Vibrant purple/magenta star for cities
            // Shadow
            paint.color = Color.parseColor("#4A148C") // Dark purple shadow
            paint.style = Paint.Style.FILL
            drawStar(canvas, paint, size / 2f + 2, size / 2f + 2, 38f, 18f)
            
            // Main star - bright magenta
            paint.color = Color.parseColor("#E040FB") // Bright magenta
            drawStar(canvas, paint, size / 2f, size / 2f, 38f, 18f)
            
            // Yellow/gold border for contrast
            paint.color = Color.parseColor("#FFD600") // Gold
            paint.strokeWidth = 4f
            paint.style = Paint.Style.STROKE
            drawStar(canvas, paint, size / 2f, size / 2f, 38f, 18f)
        }
        "hotel" -> {
            // Bright orange/amber house for hotels
            // Shadow
            paint.color = Color.parseColor("#E65100") // Dark orange shadow
            paint.style = Paint.Style.FILL
            canvas.drawRect(size / 2f - 28, size / 2f - 13, size / 2f + 32, size / 2f + 32, paint)
            
            // Main building - vibrant orange
            paint.color = Color.parseColor("#FF6D00") // Vivid orange
            canvas.drawRect(size / 2f - 30, size / 2f - 15, size / 2f + 30, size / 2f + 30, paint)
            
            // Roof - red gradient
            val roofPath = Path()
            roofPath.moveTo(size / 2f, size / 2f - 38)
            roofPath.lineTo(size / 2f - 38, size / 2f - 15)
            roofPath.lineTo(size / 2f + 38, size / 2f - 15)
            roofPath.close()
            paint.color = Color.parseColor("#DD2C00") // Bright red roof
            canvas.drawPath(roofPath, paint)
            
            // Windows - yellow
            paint.color = Color.parseColor("#FFFF00") // Bright yellow
            canvas.drawRect(size / 2f - 20, size / 2f, size / 2f - 10, size / 2f + 10, paint)
            canvas.drawRect(size / 2f + 10, size / 2f, size / 2f + 20, size / 2f + 10, paint)
            
            // White border
            paint.color = Color.WHITE
            paint.strokeWidth = 4f
            paint.style = Paint.Style.STROKE
            canvas.drawRect(size / 2f - 30, size / 2f - 15, size / 2f + 30, size / 2f + 30, paint)
            canvas.drawPath(roofPath, paint)
        }
        "horeca" -> {
            // Vibrant red circle for restaurants
            // Shadow/glow
            paint.color = Color.parseColor("#FF5252") // Light red glow
            paint.style = Paint.Style.FILL
            canvas.drawCircle(size / 2f, size / 2f, 38f, paint)
            
            // Main circle - bright red
            paint.color = Color.parseColor("#FF1744") // Vivid red
            canvas.drawCircle(size / 2f, size / 2f, 33f, paint)
            
            // Golden/yellow cutlery for contrast
            paint.color = Color.parseColor("#FFD740") // Golden yellow
            paint.strokeWidth = 6f
            paint.style = Paint.Style.STROKE
            
            // Fork
            canvas.drawLine(size / 2f - 12, size / 2f - 15, size / 2f - 12, size / 2f + 15, paint)
            canvas.drawLine(size / 2f - 18, size / 2f - 15, size / 2f - 18, size / 2f, paint)
            canvas.drawLine(size / 2f - 6, size / 2f - 15, size / 2f - 6, size / 2f, paint)
            
            // Knife
            canvas.drawLine(size / 2f + 12, size / 2f - 15, size / 2f + 12, size / 2f + 15, paint)
            paint.style = Paint.Style.FILL
            canvas.drawCircle(size / 2f + 12, size / 2f - 15, 5f, paint)
        }
        "bezienswaardigheid" -> {
            // Vibrant green camera for attractions
            // Shadow
            paint.color = Color.parseColor("#2E7D32") // Dark green shadow
            paint.style = Paint.Style.FILL
            canvas.drawRoundRect(size / 2f - 31, size / 2f - 21, size / 2f + 35, size / 2f + 25, 8f, 8f, paint)
            
            // Main camera body - bright green
            paint.color = Color.parseColor("#00E676") // Neon green
            canvas.drawRoundRect(size / 2f - 33, size / 2f - 23, size / 2f + 33, size / 2f + 23, 8f, 8f, paint)
            
            // Lens - cyan/blue
            paint.color = Color.parseColor("#00B8D4") // Bright cyan
            canvas.drawCircle(size / 2f + 8, size / 2f, 15f, paint)
            
            // Lens reflection
            paint.color = Color.parseColor("#E0F7FA") // Light cyan
            canvas.drawCircle(size / 2f + 10, size / 2f - 3, 6f, paint)
            
            // Flash - bright yellow
            paint.color = Color.parseColor("#FFFF00") // Bright yellow flash
            canvas.drawRect(size / 2f - 27, size / 2f - 30, size / 2f - 15, size / 2f - 23, paint)
            
            // Border - white
            paint.color = Color.WHITE
            paint.strokeWidth = 4f
            paint.style = Paint.Style.STROKE
            canvas.drawRoundRect(size / 2f - 33, size / 2f - 23, size / 2f + 33, size / 2f + 23, 8f, 8f, paint)
        }
        else -> {
            // Colorful teal/turquoise pin for other locations
            // Shadow
            paint.color = Color.parseColor("#00695C") // Dark teal shadow
            paint.style = Paint.Style.FILL
            val shadowPath = Path()
            shadowPath.moveTo(size / 2f + 2, size / 2f + 40)
            shadowPath.lineTo(size / 2f - 28, size / 2f - 13)
            shadowPath.quadTo(size / 2f - 28, size / 2f - 36, size / 2f + 2, size / 2f - 36)
            shadowPath.quadTo(size / 2f + 32, size / 2f - 36, size / 2f + 32, size / 2f - 13)
            shadowPath.close()
            canvas.drawPath(shadowPath, paint)
            
            // Main pin - bright teal
            val pinPath = Path()
            pinPath.moveTo(size / 2f, size / 2f + 38)
            pinPath.lineTo(size / 2f - 30, size / 2f - 15)
            pinPath.quadTo(size / 2f - 30, size / 2f - 38, size / 2f, size / 2f - 38)
            pinPath.quadTo(size / 2f + 30, size / 2f - 38, size / 2f + 30, size / 2f - 15)
            pinPath.close()
            paint.color = Color.parseColor("#00BFA5") // Bright teal
            canvas.drawPath(pinPath, paint)
            
            // Yellow center dot
            paint.color = Color.parseColor("#FFEB3B") // Bright yellow
            canvas.drawCircle(size / 2f, size / 2f - 15, 12f, paint)
            
            // White outline on center
            paint.color = Color.WHITE
            paint.strokeWidth = 3f
            paint.style = Paint.Style.STROKE
            canvas.drawCircle(size / 2f, size / 2f - 15, 12f, paint)
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
