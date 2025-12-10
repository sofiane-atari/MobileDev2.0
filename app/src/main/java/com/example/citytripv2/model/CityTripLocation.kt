package com.example.citytripv2.model

import com.google.firebase.firestore.PropertyName


data class CityTripLocation(
    val id: String = "",
    val name: String = "",
    val city: String = "",
    val country: String = "",
    val description: String? = null,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val category: String = "Overige",
    val imageUrl: String? = null,
    val rating: Float = 0f,
    val userId: String = "",       // Wie heeft dit aangemaakt?
    val userEmail: String = ""
) {

    constructor() : this("", "", "", "", null, 0.0, 0.0, "Overige", null, 0f, "","")
}