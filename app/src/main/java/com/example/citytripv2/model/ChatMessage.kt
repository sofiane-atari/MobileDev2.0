package com.example.citytripv2.model

data class ChatMessage(
    val id: String = "",
    val senderId: String = "",
    val text: String = "",
    val timestamp: Long = 0
) {
    constructor() : this("", "", "", 0)
}