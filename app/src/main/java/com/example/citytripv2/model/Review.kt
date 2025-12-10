package com.example.citytripv2.model

import java.util.Date

data class Review(
    val id: String = "",
    val userId: String = "",
    val userEmail: String = "",
    val rating: Int = 0,
    val comment: String = "",
    val timestamp: Long = 0
) {

    constructor() : this("", "", "", 0, "", 0)
}