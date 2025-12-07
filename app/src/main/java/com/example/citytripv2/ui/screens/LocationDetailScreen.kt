package com.example.citytripv2.ui.screens

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.citytripv2.model.CityTripLocation
import com.example.citytripv2.model.Review
import com.example.citytripv2.viewmodel.HomeViewModel
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationDetailScreen(
    locationId: String,
    onNavigateBack: () -> Unit,
    viewModel: HomeViewModel = viewModel()
) {
    var location by remember { mutableStateOf<CityTripLocation?>(null) }
    val reviews by viewModel.reviews.collectAsState()
    val currentUser = FirebaseAuth.getInstance().currentUser

    // Haal locatie EN reviews op bij start
    LaunchedEffect(locationId) {
        viewModel.getLocationById(locationId) { loc ->
            location = loc
        }
        viewModel.fetchReviews(locationId)
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

            // Is dit MIJN eigen locatie?
            val isMyOwnPost = loc.userId == currentUser?.uid

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(Color(0xFFF5F5F5)),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                // 1. Grote Foto Header
                item {
                    val imageBitmap: ImageBitmap? = remember(loc.imageUrl) {
                        if (loc.imageUrl != null) base64ToBitmapDetail(loc.imageUrl) else null
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(250.dp)
                            .background(Color.White)
                    ) {
                        if (imageBitmap != null) {
                            Image(
                                bitmap = imageBitmap,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Box(modifier = Modifier.fillMaxSize().background(Color.Gray))
                        }
                    }
                }

                // 2. Info Sectie
                item {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(loc.name, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        Text("${loc.category} in ${loc.city}, ${loc.country}", color = Color.Gray)

                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Over deze plek:", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(loc.description ?: "Geen beschrijving.", style = MaterialTheme.typography.bodyLarge)

                        Divider(modifier = Modifier.padding(vertical = 24.dp))
                    }
                }

                // 3. Review Schrijven Sectie (Met Check!)
                item {
                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        Text("Reviews", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

                        if (isMyOwnPost) {
                            // Als het je eigen post is -> Toon melding
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF4E5)), // Zacht oranje
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                            ) {
                                Text(
                                    text = "Je kunt geen review schrijven voor je eigen locatie.",
                                    modifier = Modifier.padding(16.dp),
                                    color = Color(0xFFD87D4A)
                                )
                            }
                        } else {
                            // Als het NIET je eigen post is -> Toon formulier
                            ReviewSection(
                                onSubmitReview = { rating, comment ->
                                    viewModel.addReview(loc.id, rating, comment) {
                                        // Succes actie (optioneel)
                                    }
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }

                // 4. Lijst met Bestaande Reviews
                if (reviews.isEmpty()) {
                    item {
                        Text(
                            text = "Nog geen reviews. Wees de eerste!",
                            modifier = Modifier.padding(16.dp),
                            color = Color.Gray,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                } else {
                    items(reviews) { review ->
                        ReviewItem(review)
                    }
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

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Schrijf een review", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))

            // Sterren Selectie
            Row {
                for (i in 1..5) {
                    IconButton(onClick = { rating = i }, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = if (i <= rating) Icons.Default.Star else Icons.Outlined.StarOutline,
                            contentDescription = "$i sterren",
                            tint = Color(0xFFFFC107)
                        )
                    }
                }
            }

            OutlinedTextField(
                value = comment,
                onValueChange = { comment = it },
                label = { Text("Jouw ervaring...") },
                modifier = Modifier.fillMaxWidth().height(100.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF0089D0),
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (rating > 0) {
                        onSubmitReview(rating, comment)
                        isSubmitted = true
                        rating = 0
                        comment = ""
                    }
                },
                enabled = rating > 0,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0089D0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Plaats Review")
            }
        }
    }
}

@Composable
fun ReviewItem(review: Review) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Avatar Placeholder
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE0E0E0)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Person, null, tint = Color.White)
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                // Naam en Datum
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = review.userEmail.split("@")[0], // Toon deel voor @
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    val date = Date(review.timestamp)
                    val format = SimpleDateFormat("dd MMM", Locale.getDefault())
                    Text(
                        text = format.format(date),
                        color = Color.Gray,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                // Sterren
                Row {
                    repeat(review.rating) {
                        Icon(
                            Icons.Default.Star, null,
                            tint = Color(0xFFFFC107),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Commentaar
                if (review.comment.isNotBlank()) {
                    Text(
                        text = review.comment,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

// Hulpfunctie (hernoemd om conflicten te voorkomen als hij ook in Home staat)
fun base64ToBitmapDetail(base64String: String): ImageBitmap? {
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