package com.example.citytripv2.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.view.MotionEvent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.citytripv2.model.CityTripLocation
import com.example.citytripv2.viewmodel.HomeViewModel
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCityScreen(
    onNavigateBack: () -> Unit,
    onNavigateToRegisterCity: () -> Unit,
    viewModel: HomeViewModel = viewModel(),
    onCityAdded: (CityTripLocation, Uri?) -> Unit
) {
    val context = LocalContext.current
    val currentUser = FirebaseAuth.getInstance().currentUser

    val allLocations by viewModel.locations.collectAsState()
    val myCities = allLocations.filter {
        it.category == "Stad" && it.userId == currentUser?.uid
    }

    // --- STATE ---
    var locationName by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

    // Photo Picker
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri -> selectedImageUri = uri }
    )

    var selectedCityObject by remember { mutableStateOf<CityTripLocation?>(null) }
    var isCityExpanded by remember { mutableStateOf(false) }

    val categories = listOf("Restaurant", "Cafe", "Natuur", "Hotel/Huis", "Bezienswaardigheid")
    var selectedCategory by remember { mutableStateOf(categories[0]) }
    var isCategoryExpanded by remember { mutableStateOf(false) }

    // Locatie
    var selectedLatitude by remember { mutableStateOf(52.3676) }
    var selectedLongitude by remember { mutableStateOf(4.9041) }

    var isUploading by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }

    val headerBlue = Color(0xFF0089D0)
    val headerGreen = Color(0xFF00A88F)
    val bgGradient = Brush.horizontalGradient(listOf(headerBlue, headerGreen))

    // --- NIEUW: Locatie Service Setup ---
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true) {
            // Als permissie gegeven is, haal locatie op
            try {
                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                    if (location != null) {
                        selectedLatitude = location.latitude
                        selectedLongitude = location.longitude
                        Toast.makeText(context, "Locatie gevonden!", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: SecurityException) { e.printStackTrace() }
        }
    }

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
                        Icon(Icons.Default.ArrowBack, "Terug", tint = Color.White)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Nieuwe Spot", style = MaterialTheme.typography.headlineSmall, color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFF5F5F5))
        ) {
            // DEEL 1: Kaart
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.35f)
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
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
            }

            // DEEL 2: Formulier
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.65f)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Coordinaten Tekst
                Text(
                    text = "Coördinaten: ${String.format("%.4f", selectedLatitude)}, ${String.format("%.4f", selectedLongitude)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = headerBlue
                )

                // --- NIEUW: KNOP "GEBRUIK EIGEN LOCATIE" ---
                OutlinedButton(
                    onClick = {
                        // Check permissies
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                                if (location != null) {
                                    selectedLatitude = location.latitude
                                    selectedLongitude = location.longitude
                                } else {
                                    Toast.makeText(context, "Kan locatie niet bepalen (zet GPS aan)", Toast.LENGTH_SHORT).show()
                                }
                            }
                        } else {
                            // Vraag permissie
                            locationPermissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = headerGreen)
                ) {
                    Icon(Icons.Default.MyLocation, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Gebruik mijn huidige locatie")
                }

                // --- FOTO KIEZER ---
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White)
                        .clickable {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (selectedImageUri != null) {
                        AsyncImage(
                            model = selectedImageUri,
                            contentDescription = "Gekozen foto",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)))
                        Text("Klik om te wijzigen", color = Color.White)
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.AddAPhoto, null, tint = headerBlue, modifier = Modifier.size(40.dp))
                            Text("Voeg een foto toe", color = headerBlue)
                        }
                    }
                }

                // 1. STAD KIEZEN
                if (myCities.isEmpty()) {
                    OutlinedButton(
                        onClick = onNavigateToRegisterCity,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = headerBlue)
                    ) {
                        Text("Maak je eerste stad aan")
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ExposedDropdownMenuBox(
                            expanded = isCityExpanded,
                            onExpandedChange = { isCityExpanded = !isCityExpanded },
                            modifier = Modifier.weight(1f)
                        ) {
                            OutlinedTextField(
                                value = selectedCityObject?.name ?: "Kies een stad",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Stad *") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isCityExpanded) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = Color.White,
                                    focusedBorderColor = headerBlue,
                                    focusedLabelColor = headerBlue
                                ),
                                modifier = Modifier.fillMaxWidth().menuAnchor()
                            )
                            ExposedDropdownMenu(
                                expanded = isCityExpanded,
                                onDismissRequest = { isCityExpanded = false },
                                modifier = Modifier.background(Color.White)
                            ) {
                                myCities.forEach { city ->
                                    DropdownMenuItem(
                                        text = { Text("${city.name} (${city.country})") },
                                        onClick = {
                                            selectedCityObject = city
                                            isCityExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = onNavigateToRegisterCity,
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = headerBlue,
                                contentColor = Color.White
                            )
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Nieuwe stad toevoegen")
                        }
                    }
                }

                // 2. CATEGORIE
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
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedBorderColor = headerBlue,
                            focusedLabelColor = headerBlue
                        ),
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = isCategoryExpanded,
                        onDismissRequest = { isCategoryExpanded = false },
                        modifier = Modifier.background(Color.White)
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

                // 3. NAAM
                OutlinedTextField(
                    value = locationName,
                    onValueChange = { locationName = it },
                    label = { Text("Naam van plek *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedBorderColor = headerBlue,
                        focusedLabelColor = headerBlue
                    )
                )

                // 4. BESCHRIJVING
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Beschrijving") },
                    modifier = Modifier.fillMaxWidth().height(100.dp),
                    maxLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedBorderColor = headerBlue,
                        focusedLabelColor = headerBlue
                    )
                )

                // 5. OPSLAAN
                Button(
                    onClick = {
                        if (locationName.isNotBlank() && selectedCityObject != null && selectedImageUri != null) {
                            isUploading = true
                            val newLocation = CityTripLocation(
                                id = System.currentTimeMillis().toString(),
                                name = locationName,
                                city = selectedCityObject!!.name,
                                country = selectedCityObject!!.country,
                                description = description.ifBlank { null },
                                latitude = selectedLatitude,
                                longitude = selectedLongitude,
                                category = selectedCategory,
                                imageUrl = null,
                                rating = 0f
                            )
                            onCityAdded(newLocation, selectedImageUri)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = headerBlue),
                    shape = RoundedCornerShape(8.dp),
                    enabled = locationName.isNotBlank() && selectedCityObject != null && !isUploading
                ) {
                    if (isUploading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Text("Opslaan", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
                Spacer(modifier = Modifier.height(100.dp))
            }
        }
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

            setOnTouchListener { v, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> v.parent.requestDisallowInterceptTouchEvent(true)
                    MotionEvent.ACTION_UP -> {
                        v.parent.requestDisallowInterceptTouchEvent(false)
                        v.performClick()
                    }
                }
                false
            }
        }
    }

    LaunchedEffect(initialLatitude, initialLongitude) {
        val newPoint = GeoPoint(initialLatitude, initialLongitude)
        mapView.controller.animateTo(newPoint)
    }

    var marker by remember { mutableStateOf<Marker?>(null) }

    DisposableEffect(Unit) {
        val tapOverlay = object : org.osmdroid.views.overlay.Overlay() {
            override fun onSingleTapConfirmed(e: android.view.MotionEvent?, mapView: MapView?): Boolean {
                if (e != null && mapView != null) {
                    val projection = mapView.projection
                    val geoPoint = projection.fromPixels(e.x.toInt(), e.y.toInt()) as GeoPoint
                    onLocationSelected(geoPoint.latitude, geoPoint.longitude)

                    // Marker logic
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

        onDispose { mapView.onDetach() }
    }

    LaunchedEffect(initialLatitude, initialLongitude) {
        if (marker != null) {
            marker!!.position = GeoPoint(initialLatitude, initialLongitude)
            mapView.invalidate()
        }
    }

    AndroidView(factory = { mapView }, modifier = modifier)
}