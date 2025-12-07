package com.example.citytripv2.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.citytripv2.model.CityTripLocation
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HomeViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // De lijst met locaties die we in de UI tonen
    private val _locations = MutableStateFlow<List<CityTripLocation>>(emptyList())
    val locations: StateFlow<List<CityTripLocation>> = _locations.asStateFlow()

    init {
        fetchLocations()
    }

    private fun fetchLocations() {
        val userId = auth.currentUser?.uid
        if (userId == null) return


        db.collection("locations")
            .orderBy("id", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w("HomeViewModel", "Listen failed.", e)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        // Zet de Firestore JSON om naar jouw CityTripLocation object
                        doc.toObject(CityTripLocation::class.java)
                    }
                    _locations.value = list
                }
            }
    }

    fun addLocation(location: CityTripLocation) {
        val userId = auth.currentUser?.uid ?: return

        // Voeg de userId toe aan het object voor veiligheid
        val locationWithUser = location.copy(userId = userId)

        db.collection("locations")
            .document(location.id) // We gebruiken de ID die je in AddCityScreen hebt gemaakt
            .set(locationWithUser)
            .addOnSuccessListener {
                Log.d("HomeViewModel", "Locatie succesvol opgeslagen!")
            }
            .addOnFailureListener { e ->
                Log.w("HomeViewModel", "Fout bij opslaan", e)
            }
    }

    fun deleteLocation(locationId: String, onSuccess: () -> Unit) {
        db.collection("locations")
            .document(locationId)
            .delete()
            .addOnSuccessListener {
                Log.d("HomeViewModel", "Locatie verwijderd!")
                onSuccess()
            }
            .addOnFailureListener { e ->
                Log.e("HomeViewModel", "Fout bij verwijderen", e)
            }
    }
}