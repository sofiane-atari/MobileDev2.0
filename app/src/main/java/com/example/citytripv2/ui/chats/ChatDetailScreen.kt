package com.example.citytripv2.ui.chats

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ArrowBack
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
import com.example.citytripv2.model.ChatMessage
import com.example.citytripv2.viewmodel.ChatViewModel
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDetailScreen(
    chatId: String,
    otherUserEmail: String,
    onNavigateBack: () -> Unit,
    viewModel: ChatViewModel = viewModel()
) {
    var text by remember { mutableStateOf("") }
    val messages by viewModel.messages.collectAsState()
    val listState = rememberLazyListState()
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

    // Naam mooi maken
    val displayName = otherUserEmail.split("@")[0].replaceFirstChar { it.uppercase() }

    // Kleuren
    val headerBlue = Color(0xFF0089D0)
    val headerGreen = Color(0xFF00A88F)
    val bgGradient = Brush.horizontalGradient(listOf(headerBlue, headerGreen))

    LaunchedEffect(chatId) {
        viewModel.fetchMessages(chatId)
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(displayName, color = Color.White, style = MaterialTheme.typography.titleMedium)
                        Text("Chat", color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.bodySmall)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Terug", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                modifier = Modifier.background(bgGradient)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFF5F5F5))
        ) {
            // Berichten
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(messages) { msg ->
                    val isMe = msg.senderId == currentUserId
                    MessageBubble(message = msg, isMe = isMe, headerBlue = headerBlue)
                }
            }

            // Input Balk
            Surface(
                shadowElevation = 8.dp,
                color = Color.White
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = text,
                        onValueChange = { text = it },
                        placeholder = { Text("Typ een bericht...") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = Color.LightGray,
                            focusedBorderColor = headerBlue,
                            focusedContainerColor = Color(0xFFF8F8F8),
                            unfocusedContainerColor = Color(0xFFF8F8F8)
                        ),
                        maxLines = 3
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = {
                            if (text.isNotBlank()) {
                                viewModel.sendMessage(chatId, text)
                                text = ""
                            }
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .background(headerBlue, CircleShape)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, null, tint = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun MessageBubble(message: ChatMessage, isMe: Boolean, headerBlue: Color) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
    ) {
        Card(
            shape = RoundedCornerShape(
                topStart = 16.dp, topEnd = 16.dp,
                bottomStart = if (isMe) 16.dp else 4.dp,
                bottomEnd = if (isMe) 4.dp else 16.dp
            ),
            colors = CardDefaults.cardColors(
                containerColor = if (isMe) headerBlue else Color.White
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier.widthIn(max = 280.dp) // Max breedte zodat het niet hele scherm vult
        ) {
            Text(
                text = message.text,
                modifier = Modifier.padding(12.dp),
                color = if (isMe) Color.White else Color.Black
            )
        }

        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        Text(
            text = timeFormat.format(Date(message.timestamp)),
            style = MaterialTheme.typography.labelSmall,
            color = Color.Gray,
            modifier = Modifier.padding(top = 4.dp, start = 4.dp, end = 4.dp)
        )
    }
}