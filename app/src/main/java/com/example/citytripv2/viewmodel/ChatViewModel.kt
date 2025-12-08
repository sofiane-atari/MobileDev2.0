package com.example.citytripv2.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import com.example.citytripv2.model.Chat
import com.example.citytripv2.model.ChatMessage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ChatViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // Lijst van gesprekken (voor het Chats tabblad)
    private val _myChats = MutableStateFlow<List<Chat>>(emptyList())
    val myChats: StateFlow<List<Chat>> = _myChats.asStateFlow()

    // Berichten van het actieve gesprek
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    init {
        fetchMyChats()
    }

    // 1. Haal alle chats op waar IK aan meedoe
    private fun fetchMyChats() {
        val myId = auth.currentUser?.uid ?: return

        db.collection("chats")
            .whereArrayContains("participants", myId)
            .orderBy("lastTimestamp", Query.Direction.DESCENDING) // Nieuwste gesprekken bovenaan
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("ChatViewModel", "Fout bij ophalen chats", e)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    _myChats.value = snapshot.toObjects(Chat::class.java)
                }
            }
    }

    // 2. Haal berichten op van een specifiek gesprek (Real-time!)
    fun fetchMessages(chatId: String) {
        db.collection("chats").document(chatId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING) // Oudste eerst (zoals WhatsApp)
            .addSnapshotListener { snapshot, e ->
                if (snapshot != null) {
                    _messages.value = snapshot.toObjects(ChatMessage::class.java)
                }
            }
    }

    // 3. Start een nieuw gesprek (of open bestaand)
    fun startChat(otherUserId: String, otherUserEmail: String, onChatReady: (String) -> Unit) {
        val myId = auth.currentUser?.uid ?: return
        val myEmail = auth.currentUser?.email ?: "Ik"

        // Check eerst of er al een chat is tussen deze 2
        // (Dit is een simpele check, in productie kan dit beter)
        db.collection("chats")
            .whereArrayContains("participants", myId)
            .get()
            .addOnSuccessListener { snapshot ->
                // Zoek lokaal of de andere user er ook in zit
                val existingChat = snapshot.documents.mapNotNull { it.toObject(Chat::class.java) }
                    .find { it.participants.contains(otherUserId) }

                if (existingChat != null) {
                    // Chat bestaat al -> Open die
                    onChatReady(existingChat.id)
                } else {
                    // Chat bestaat niet -> Maak nieuwe
                    val newChatRef = db.collection("chats").document()
                    val newChat = Chat(
                        id = newChatRef.id,
                        participants = listOf(myId, otherUserId),
                        participantEmails = mapOf(myId to myEmail, otherUserId to otherUserEmail),
                        lastMessage = "Nieuw gesprek",
                        lastTimestamp = System.currentTimeMillis()
                    )
                    newChatRef.set(newChat).addOnSuccessListener {
                        onChatReady(newChat.id)
                    }
                }
            }
    }

    // 4. Verstuur een berichtje
    fun sendMessage(chatId: String, text: String) {
        val myId = auth.currentUser?.uid ?: return
        if (text.isBlank()) return

        val message = ChatMessage(
            id = System.currentTimeMillis().toString(), // Simpel ID
            senderId = myId,
            text = text,
            timestamp = System.currentTimeMillis()
        )

        // Voeg bericht toe aan subcollectie
        db.collection("chats").document(chatId)
            .collection("messages")
            .add(message)

        // Update het hoofdgesprek (zodat het bovenaan de lijst komt)
        db.collection("chats").document(chatId)
            .update(
                mapOf(
                    "lastMessage" to text,
                    "lastTimestamp" to System.currentTimeMillis()
                )
            )
    }
}