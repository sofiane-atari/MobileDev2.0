package com.example.citytripv2.model

data class Chat(
    val id: String = "",
    val participants: List<String> = emptyList(),
    val participantEmails: Map<String, String> = emptyMap(),
    val lastMessage: String = "",
    val lastTimestamp: Long = 0
) {
    constructor() : this("", emptyList(), emptyMap(), "", 0)
}