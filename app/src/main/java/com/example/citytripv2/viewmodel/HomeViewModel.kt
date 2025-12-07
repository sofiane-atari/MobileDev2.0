package com.example.citytripv2.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.citytripv2.model.CityTripLocation
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

class HomeViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _locations = MutableStateFlow<List<CityTripLocation>>(emptyList())
    val locations: StateFlow<List<CityTripLocation>> = _locations.asStateFlow()

    init {
        fetchLocations()
    }

    private fun fetchLocations() {
        val collectionName = "locations"
        Log.d("HomeViewModel", "Luisteren naar collectie: $collectionName")

        db.collection(collectionName)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("HomeViewModel", "Fout bij luisteren:", e)
                    return@addSnapshotListener
                }

                if (snapshot != null && !snapshot.isEmpty) {
                    try {
                        val list = snapshot.toObjects(CityTripLocation::class.java)
                        _locations.value = list
                    } catch (err: Exception) {
                        Log.e("HomeViewModel", "Fout bij omzetten data:", err)
                    }
                } else {
                    _locations.value = emptyList()
                }
            }
    }

    // NIEUW: Haal 1 specifieke locatie op (voor het detailscherm)
    fun getLocationById(id: String, onResult: (CityTripLocation?) -> Unit) {
        // We zoeken eerst in de lokale lijst (sneller)
        val local = _locations.value.find { it.id == id }
        if (local != null) {
            onResult(local)
            return
        }
        // Anders halen we hem op uit DB
        db.collection("locations").document(id).get()
            .addOnSuccessListener { doc ->
                onResult(doc.toObject(CityTripLocation::class.java))
            }
            .addOnFailureListener {
                onResult(null)
            }
    }

    // NIEUW: Review toevoegen
    fun addReview(locationId: String, rating: Int, comment: String, onSuccess: () -> Unit) {
        val user = auth.currentUser ?: return

        val reviewData = hashMapOf(
            "userId" to user.uid,
            "userEmail" to user.email,
            "rating" to rating,
            "comment" to comment,
            "timestamp" to System.currentTimeMillis()
        )

        // 1. Voeg review toe aan subcollectie
        db.collection("locations").document(locationId)
            .collection("reviews")
            .add(reviewData)
            .addOnSuccessListener {
                // 2. Update de gemiddelde rating van de hoofdlocatie (Simpele update: overschrijf met nieuwe)
                db.collection("locations").document(locationId)
                    .update("rating", rating.toFloat())
                    .addOnSuccessListener { onSuccess() }
            }
    }

    fun addLocation(location: CityTripLocation, imageUri: Uri?, context: Context, onSuccess: () -> Unit) {
        val userId = auth.currentUser?.uid ?: return

        viewModelScope.launch {
            // 1. Converteer afbeelding naar Base64 String (indien aanwezig)
            val base64Image = if (imageUri != null) {
                encodeImageToBase64(context, imageUri)
            } else {
                null
            }

            // 2. Update het locatie object
            val locationToSave = location.copy(
                userId = userId,
                imageUrl = base64Image
            )

            // 3. Opslaan
            db.collection("locations")
                .document(locationToSave.id)
                .set(locationToSave)
                .addOnSuccessListener {
                    Log.d("HomeViewModel", "Opgeslagen!")
                    onSuccess()
                }
                .addOnFailureListener { e ->
                    Log.e("HomeViewModel", "Fout bij opslaan (Waarschijnlijk te groot):", e)
                }
        }
    }

    fun deleteLocation(locationId: String, onSuccess: () -> Unit) {
        db.collection("locations").document(locationId).delete()
            .addOnSuccessListener { onSuccess() }
    }

    // --- VERBETERDE COMPRESSIE LOGICA ---
    fun encodeImageToBase64(context: Context, imageUri: Uri): String? {
        return try {
            val inputStream = context.contentResolver.openInputStream(imageUri)
            val originalBitmap = BitmapFactory.decodeStream(inputStream) ?: return null

            // Resize naar max 300px breed/hoog
            val resizedBitmap = resizeBitmap(originalBitmap, 300)

            val outputStream = ByteArrayOutputStream()
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 40, outputStream)

            val byteArray = outputStream.toByteArray()

            // Als het nog groter is dan 900 KB: geen foto opslaan
            if (byteArray.size > 900 * 1024) {
                Log.e("encodeImage", "Foto alsnog te groot (${byteArray.size} bytes)")
                return null
            }

            Base64.encodeToString(byteArray, Base64.NO_WRAP)

        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun resizeBitmap(bitmap: Bitmap, maxSize: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        val ratio = width.toFloat() / height
        val newWidth: Int
        val newHeight: Int

        if (ratio > 1) {
            newWidth = maxSize
            newHeight = (maxSize / ratio).toInt()
        } else {
            newHeight = maxSize
            newWidth = (maxSize * ratio).toInt()
        }

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

}