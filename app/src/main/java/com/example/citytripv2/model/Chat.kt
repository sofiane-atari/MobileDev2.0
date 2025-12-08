package com.example.citytripv2.model

data class Chat(
    val id: String = "",
    val participants: List<String> = emptyList(), // De userIds van de 2 mensen: [jouwId, anderId]
    val participantEmails: Map<String, String> = emptyMap(), // Om de naam van de ander te tonen: {id: email}
    val lastMessage: String = "",
    val lastTimestamp: Long = 0
) {
    constructor() : this("", emptyList(), emptyMap(), "", 0)
}