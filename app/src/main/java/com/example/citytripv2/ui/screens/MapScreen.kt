package com.example.citytripv2.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.Path
import android.location.Location
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
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

    // Kleuren (Consistent met de rest van de app)
    val headerBlue = Color(0xFF0089D0)
    val headerGreen = Color(0xFF00A88F)
    val bgGradient = Brush.horizontalGradient(listOf(headerBlue, headerGreen))

    // Filter states
    var selectedCities by remember { mutableStateOf<Set<String>>(emptySet()) }
    var selectedCategories by remember { mutableStateOf<Set<String>>(emptySet()) }
    var showFilterDialog by remember { mutableStateOf(false) }

    // MapView state om te zoomen en centreren
    var mapViewState by remember { mutableStateOf<MapView?>(null) }
    var initialCenterSet by remember { mutableStateOf(false) } // Zodat we niet blijven springen

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

    // Selected location UI state
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

    // Auto-center op user location (maar slechts 1 keer)
    LaunchedEffect(userLocation, mapViewState) {
        if (userLocation != null && !initialCenterSet && mapViewState != null) {
            val geoPoint = GeoPoint(userLocation!!.latitude, userLocation!!.longitude)
            mapViewState?.controller?.animateTo(geoPoint)
            mapViewState?.controller?.setZoom(14.0) // Lekker inzoomen
            initialCenterSet = true
        }
    }

    // Filter logic
    val filteredLocations = remember(locations, selectedCities, selectedCategories) {
        locations.filter { location ->
            val cityMatch = selectedCities.isEmpty() || location.city in selectedCities
            val categoryMatch = selectedCategories.isEmpty() || location.category in selectedCategories
            val isNotCityObject = location.category != "Stad"
            cityMatch && categoryMatch && isNotCityObject
        }
    }

    val cities = remember(locations) {
        locations.map { it.city }.filter { it.isNotBlank() }.distinct().sorted()
    }
    val categories = remember(locations) {
        locations.map { it.category }.filter { it != "Stad" }.distinct().sorted()
    }

    // Hoofdstructuur: Column zorgt dat Header en Map onder elkaar staan
    Column(modifier = Modifier.fillMaxSize()) {

        // --- 1. HEADER (Vast bovenaan met schaduw) ---
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .zIndex(1f), // Zorg dat de header altijd BOVEN op ligt
            shadowElevation = 8.dp,
            color = Color.Transparent
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(bgGradient)
                    .statusBarsPadding()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    // Titel Rij
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Kaart Verkenner",
                            style = MaterialTheme.typography.headlineMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )

                        // Filter Knop
                        val hasFilters = selectedCities.isNotEmpty() || selectedCategories.isNotEmpty()
                        IconButton(
                            onClick = { showFilterDialog = true },
                            modifier = Modifier
                                .background(Color.White.copy(alpha = 0.2f), CircleShape)
                        ) {
                            BadgedBox(badge = { if (hasFilters) Badge() }) {
                                Icon(
                                    Icons.Default.FilterAlt,
                                    contentDescription = "Filters",
                                    tint = Color.White
                                )
                            }
                        }
                    }

                    // Actieve Filters (Horizontaal Scrollbaar)
                    if (selectedCities.isNotEmpty() || selectedCategories.isNotEmpty()) {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(selectedCities.toList()) { city ->
                                FilterChip(
                                    selected = true,
                                    onClick = { selectedCities = selectedCities - city },
                                    label = { Text(city, color = headerBlue, fontWeight = FontWeight.Bold) },
                                    leadingIcon = { Icon(Icons.Default.Close, null, tint = headerBlue) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color.White,
                                        selectedLabelColor = headerBlue
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        borderColor = Color.Transparent,
                                        selectedBorderColor = Color.Transparent,
                                        enabled = true,
                                        selected = true
                                    ),
                                    shape = RoundedCornerShape(16.dp)
                                )
                            }
                            items(selectedCategories.toList()) { category ->
                                FilterChip(
                                    selected = true,
                                    onClick = { selectedCategories = selectedCategories - category },
                                    label = { Text(category, color = headerGreen, fontWeight = FontWeight.Bold) },
                                    leadingIcon = { Icon(Icons.Default.Close, null, tint = headerGreen) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color.White,
                                        selectedLabelColor = headerGreen
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        borderColor = Color.Transparent,
                                        selectedBorderColor = Color.Transparent,
                                        enabled = true,
                                        selected = true
                                    ),
                                    shape = RoundedCornerShape(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- 2. DE KAART (Vult de rest) ---
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clipToBounds()
        ) {
            AndroidView(
                factory = { ctx ->
                    Configuration.getInstance().load(
                        ctx,
                        ctx.getSharedPreferences("osmdroid", Context.MODE_PRIVATE)
                    )

                    MapView(ctx).apply {
                        setTileSource(TileSourceFactory.MAPNIK)
                        setMultiTouchControls(true)
                        setBuiltInZoomControls(false)
                        controller.setZoom(7.5)
                        controller.setCenter(GeoPoint(50.8503, 4.3517))
                        mapViewState = this // Sla referentie op
                    }
                },
                modifier = Modifier.fillMaxSize(),
                update = { mapView ->
                    mapView.overlays.clear()

                    // User Location Marker
                    if (userLocation != null) {
                        val userMarker = Marker(mapView).apply {
                            position = GeoPoint(userLocation!!.latitude, userLocation!!.longitude)
                            title = "Jij bent hier"
                            icon = createCustomMarker(context, "user")
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                        }
                        mapView.overlays.add(userMarker)
                    }

                    // Location Markers
                    filteredLocations.forEach { location ->
                        val marker = Marker(mapView).apply {
                            position = GeoPoint(location.latitude, location.longitude)
                            title = location.name
                            icon = createCustomMarker(context, location.category)
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)

                            setOnMarkerClickListener { _, _ ->
                                selectedLocation = location

                                if (userLocation != null) {
                                    val distance = calculateDistance(
                                        userLocation!!.latitude,
                                        userLocation!!.longitude,
                                        location.latitude,
                                        location.longitude
                                    )
                                    distanceToUser = distance
                                }
                                true
                            }
                        }
                        mapView.overlays.add(marker)
                    }
                    mapView.invalidate()
                }
            )

            // --- ZOOM & LOCATION BUTTONS ---
            Column(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Knop: Ga naar mijn locatie
                if (userLocation != null) {
                    IconButton(
                        onClick = {
                            mapViewState?.controller?.animateTo(
                                GeoPoint(userLocation!!.latitude, userLocation!!.longitude)
                            )
                            mapViewState?.controller?.setZoom(15.0)
                        },
                        modifier = Modifier
                            .shadow(4.dp, CircleShape)
                            .background(Color.White, CircleShape)
                            .size(48.dp)
                    ) {
                        Icon(Icons.Default.MyLocation, contentDescription = "Mijn Locatie", tint = headerBlue)
                    }
                }

                IconButton(
                    onClick = { mapViewState?.controller?.zoomIn() },
                    modifier = Modifier
                        .shadow(4.dp, CircleShape)
                        .background(Color.White, CircleShape)
                        .size(48.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Zoom In", tint = headerBlue)
                }

                IconButton(
                    onClick = { mapViewState?.controller?.zoomOut() },
                    modifier = Modifier
                        .shadow(4.dp, CircleShape)
                        .background(Color.White, CircleShape)
                        .size(48.dp)
                ) {
                    Icon(Icons.Default.Remove, contentDescription = "Zoom Out", tint = headerBlue)
                }
            }

            // --- 3. BOTTOM INFO CARD ---
            if (selectedLocation != null) {
                Card(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(16.dp)
                        .shadow(8.dp, RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = selectedLocation!!.name,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black
                                )
                                Text(
                                    text = "${selectedLocation!!.category} in ${selectedLocation!!.city}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.Gray
                                )
                            }

                            if (distanceToUser != null) {
                                Surface(
                                    color = headerBlue.copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Navigation, null, tint = headerBlue, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "%.1f km".format(distanceToUser!! / 1000),
                                            style = MaterialTheme.typography.labelMedium,
                                            color = headerBlue,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedButton(
                                onClick = { selectedLocation = null; distanceToUser = null },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Gray)
                            ) {
                                Text("Sluiten")
                            }

                            Button(
                                onClick = { onNavigateToDetail(selectedLocation!!.id) },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = headerBlue)
                            ) {
                                Text("Bekijk Details")
                            }
                        }
                    }
                }
            }
        }
    }

    // --- 4. FILTER DIALOG ---
    if (showFilterDialog) {
        AlertDialog(
            onDismissRequest = { showFilterDialog = false },
            containerColor = Color.White,
            title = { Text("Filter Kaart", color = headerBlue, fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text("Steden", style = MaterialTheme.typography.titleSmall, color = Color.Black)
                    LazyRow(
                        contentPadding = PaddingValues(vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(cities) { city ->
                            FilterChip(
                                selected = city in selectedCities,
                                onClick = {
                                    selectedCities = if (city in selectedCities) selectedCities - city else selectedCities + city
                                },
                                label = { Text(city) },
                                modifier = Modifier.padding(end = 8.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = headerBlue,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }

                    Divider(modifier = Modifier.padding(vertical = 8.dp))

                    Text("Categorieën", style = MaterialTheme.typography.titleSmall, color = Color.Black)
                    LazyRow(
                        contentPadding = PaddingValues(vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(categories) { cat ->
                            FilterChip(
                                selected = cat in selectedCategories,
                                onClick = {
                                    selectedCategories = if (cat in selectedCategories) selectedCategories - cat else selectedCategories + cat
                                },
                                label = { Text(cat) },
                                modifier = Modifier.padding(end = 8.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = headerGreen,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showFilterDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = headerBlue)
                ) { Text("Klaar") }
            },
            dismissButton = {
                TextButton(
                    onClick = { selectedCities = emptySet(); selectedCategories = emptySet() },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
                ) { Text("Wis alles") }
            }
        )
    }
}

// --- HELPER FUNCTIES ---

@Composable
fun FlowRow(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(modifier) { content() }
}

private fun getUserLocation(context: Context, onLocationReceived: (Location) -> Unit) {
    try {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let { onLocationReceived(it) }
            }
        }
    } catch (e: Exception) { e.printStackTrace() }
}

private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
    val results = FloatArray(1)
    Location.distanceBetween(lat1, lon1, lat2, lon2, results)
    return results[0]
}

private fun createCustomMarker(context: Context, category: String): android.graphics.drawable.Drawable {
    val size = 100
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    // Kleuren per categorie
    val (bgColor, iconChar) = when (category.lowercase()) {
        "user" -> AndroidColor.parseColor("#2979FF") to ""
        "stad" -> AndroidColor.parseColor("#AA00FF") to "S"
        "restaurant" -> AndroidColor.parseColor("#FF1744") to "R"
        "cafe" -> AndroidColor.parseColor("#FF9100") to "C"
        "nature" -> AndroidColor.parseColor("#00E676") to "N"
        else -> AndroidColor.parseColor("#00BFA5") to "?"
    }

    if (category.lowercase() == "user") {
        paint.color = bgColor
        paint.style = Paint.Style.FILL
        canvas.drawCircle(size / 2f, size / 2f, 25f, paint)
        paint.color = AndroidColor.WHITE
        paint.strokeWidth = 5f
        paint.style = Paint.Style.STROKE
        canvas.drawCircle(size / 2f, size / 2f, 25f, paint)
    } else {
        paint.color = bgColor
        paint.style = Paint.Style.FILL
        val path = Path().apply {
            moveTo(size / 2f, size.toFloat())
            lineTo(size / 2f - 30, size / 2f)
            arcTo(size / 2f - 30, size / 2f - 60, size / 2f + 30, size / 2f, 180f, 180f, false)
            close()
        }
        canvas.drawPath(path, paint)
        paint.color = AndroidColor.WHITE
        canvas.drawCircle(size / 2f, size / 2f - 30, 20f, paint)
    }

    return android.graphics.drawable.BitmapDrawable(context.resources, bitmap)
}

// Helper function to draw a star (re-added as it was used in custom marker)
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