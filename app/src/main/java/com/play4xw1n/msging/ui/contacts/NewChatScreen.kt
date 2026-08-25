package com.play4xw1n.msging.ui.contacts

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.play4xw1n.msging.data.User
import com.play4xw1n.msging.data.UserRepository

private val Bg = Color(0xFF0E1013)
private val Card = Color(0xFF151A21)
private val HeaderGradient = Brush.horizontalGradient(listOf(Color(0xFF6C5CE7), Color(0xFF9B8CFF)))
private val SubtleText = Color(0xFF8A93A6)
private val AvatarPalette = listOf(
    Color(0xFFFF7043), Color(0xFF42A5F5), Color(0xFF66BB6A),
    Color(0xFFEC407A), Color(0xFFFFCA28), Color(0xFF26C6DA)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewChatScreen(
    onBack: () -> Unit,
    onUserClick: (userId: String, userName: String) -> Unit,
    onNewGroup: () -> Unit = {}
) {
    val viewModel: NewChatViewModel = viewModel(factory = NewChatViewModel.factory())
    val users by viewModel.users.collectAsState()
    var searchQuery by remember { mutableStateOf("") }

    val filteredUsers = remember(users, searchQuery) {
        if (searchQuery.isBlank()) emptyList()
        else users.filter { it.name.equals(searchQuery, ignoreCase = true) }
    }

    fun startConversation(user: User) {
        val currentUserId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
        val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
        
        db.collection("users").document(currentUserId)
            .collection("conversations").document(user.id).set(
                mapOf(
                    "name" to user.name,
                    "lastMessage" to "",
                    "lastMessageTime" to System.currentTimeMillis(),
                    "unreadCount" to 0
                )
            )
    }

    Column(modifier = Modifier.fillMaxSize().background(Bg)) {
        TopAppBar(
            title = { Text("New chat", color = Color.White) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onNewGroup() }
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(42.dp).clip(androidx.compose.foundation.shape.CircleShape).background(
                    androidx.compose.ui.graphics.Brush.linearGradient(listOf(Color(0xFF9B8CFF), Color(0xFF6C5CE7)))
                ),
                contentAlignment = Alignment.Center
            ) {
                Text("G", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
            Spacer(Modifier.size(12.dp))
            Text("New group", color = Color(0xFF9B8CFF), fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
        }

        TextField(
 
           value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            placeholder = { Text("Search users…", color = SubtleText) },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search", tint = SubtleText) },
            shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
            singleLine = true,
            colors = TextFieldDefaults.colors(
                focusedTextColor = Color(0xFFE9ECF2),
                unfocusedTextColor = Color(0xFFE9ECF2),
                cursorColor = Color(0xFF8B7CFF),
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                focusedContainerColor = Card,
                unfocusedContainerColor = Card
            )
        )

        if (searchQuery.isBlank()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(bottom = 80.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Search for a user", color = Color.White, style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(6.dp))
                    Text("Type a name to find someone to chat with", color = SubtleText, style = androidx.compose.material3.MaterialTheme.typography.bodySmall)
                }
            }
        } else if (filteredUsers.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(bottom = 80.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("No users found", color = Color.White, style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(6.dp))
                    Text("Try a different name", color = SubtleText, style = androidx.compose.material3.MaterialTheme.typography.bodySmall)
                }
            }
        } else {
            LazyColumn {
                items(filteredUsers, key = { it.id }) { user ->
                    UserItem(user = user, onClick = {
                        startConversation(user)
                        onUserClick(user.id, user.name)
                    })
                }
            }
        }
    }
}

@Composable
private fun UserItem(user: User, onClick: () -> Unit) {
    val avatarColor = AvatarPalette[Math.abs(user.name.hashCode()) % AvatarPalette.size]

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(CircleShape)
                .background(avatarColor),
            contentAlignment = Alignment.Center
        ) {
            Text(
                user.name.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp
            )
        }
        Spacer(Modifier.width(14.dp))
        Column {
            Text(user.name, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Spacer(Modifier.height(2.dp))
            Text(user.email, color = SubtleText, fontSize = 13.sp)
        }
    }
}
