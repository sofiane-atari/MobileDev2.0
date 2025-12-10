package com.example.citytripv2.ui.chats

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
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

    // Kleuren
    val headerBlue = Color(0xFF0089D0)
    val headerGreen = Color(0xFF00A88F)
    val bgGradient = Brush.horizontalGradient(listOf(headerBlue, headerGreen))

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
    ) {
        // --- HEADER ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp) // Iets compacter dan Home
                .background(bgGradient)
                .padding(16.dp),
            contentAlignment = Alignment.BottomStart
        ) {
            Text(
                text = "Berichten",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }

        if (chats.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "Nog geen gesprekken.\nGa naar een locatie en stuur een bericht!",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.Gray,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(chats) { chat ->
                    // Zoek de naam van de 'ander'
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
    // Haal de naam uit de email (alles voor de @)
    val displayName = otherEmail.split("@")[0].replaceFirstChar { it.uppercase() }

    Card(
        shape = RoundedCornerShape(12.dp),
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
            // Avatar met eerste letter
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFEEF2FF)), // Zacht blauw
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = displayName.take(1),
                    style = MaterialTheme.typography.titleLarge,
                    color = Color(0xFF0089D0),
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    val date = Date(chat.lastTimestamp)
                    val format = SimpleDateFormat("HH:mm", Locale.getDefault())
                    Text(
                        text = format.format(date),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

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