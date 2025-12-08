package com.example.citytripv2.ui.chats

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.citytripv2.model.Chat
import com.example.citytripv2.viewmodel.ChatViewModel
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ChatScreen(
    onNavigateToChat: (String, String) -> Unit, // (chatId, otherEmail)
    viewModel: ChatViewModel = viewModel()
) {
    val chats by viewModel.myChats.collectAsState()
    val myId = FirebaseAuth.getInstance().currentUser?.uid

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
    ) {
        // Simpele witte header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(16.dp)
        ) {
            Text("Berichten", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }

        if (chats.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Nog geen gesprekken.\nGa naar een locatie en stuur een bericht!",
                    style = MaterialTheme.typography.bodyMedium, color = Color.Gray,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(chats) { chat ->
                    // Zoek de naam van de 'ander'
                    // We pakken de email uit de map die NIET van mij is
                    val otherEmail = chat.participantEmails.filterKeys { it != myId }.values.firstOrNull() ?: "Onbekend"

                    ChatItem(chat, otherEmail) {
                        onNavigateToChat(chat.id, otherEmail)
                    }
                }
            }
        }
    }
}

@Composable
fun ChatItem(chat: Chat, otherEmail: String, onClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE0E0E0)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Person, null, tint = Color.White, modifier = Modifier.size(30.dp))
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(otherEmail.split("@")[0], style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    val date = Date(chat.lastTimestamp)
                    val format = SimpleDateFormat("HH:mm", Locale.getDefault())
                    Text(format.format(date), style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
                Text(
                    text = chat.lastMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray,
                    maxLines = 1
                )
            }
        }
    }
}