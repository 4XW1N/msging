package com.play4xw1n.msging.ui.home

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.ExperimentalAnimationApi
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.play4xw1n.msging.data.Conversation
import com.play4xw1n.msging.data.ConversationRepository
import com.play4xw1n.msging.update.UpdateDialog
import com.play4xw1n.msging.update.UpdateManager
import com.play4xw1n.msging.update.UpdateChecker
import kotlinx.coroutines.launch

private val Bg = Color(0xFF0E1013)
private val Card = Color(0xFF151A21)
private val HeaderGradient = Brush.horizontalGradient(listOf(Color(0xFF6C5CE7), Color(0xFF9B8CFF)))
private val AccentGradient = Brush.linearGradient(listOf(Color(0xFF9B8CFF), Color(0xFF6C5CE7)))
private val SubtleText = Color(0xFF8A93A6)
private val AvatarPalette = listOf(
    Color(0xFFFF7043), Color(0xFF42A5F5), Color(0xFF66BB6A),
    Color(0xFFEC407A), Color(0xFFFFCA28), Color(0xFF26C6DA)
)

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun HomeScreen(
    userName: String,
    onConversationClick: (otherUserId: String, contactName: String, isGroup: Boolean) -> Unit,
    onNewChatClick: () -> Unit,
    onSignOut: () -> Unit
) {
    val viewModel: HomeViewModel = viewModel(factory = HomeViewModel.factory())
    val conversations by viewModel.conversations.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    val context = LocalContext.current

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* no-op, permission result handled by system */ }

    var updateChecked by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (!updateChecked) {
            val info = UpdateChecker.checkForUpdate(context)
            if (info != null) {
                UpdateManager.onUpdateChecked(info)
            }
            updateChecked = true
        }
        // Existing permission logic
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    val filteredConversations = remember(conversations, searchQuery) {
        if (searchQuery.isBlank()) conversations
        else conversations.filter { it.name.contains(searchQuery, ignoreCase = true) }
    }

    Column(modifier = Modifier.fillMaxSize().background(Bg)) {
        HomeHeader(userName = userName, onSignOut = onSignOut)

        SearchBar(query = searchQuery, onQueryChange = { searchQuery = it })

        if (filteredConversations.isEmpty()) {
            EmptyState(hasConversations = conversations.isNotEmpty())
        } else {
            ConversationList(
                conversations = filteredConversations,
                onClick = { onConversationClick(if (it.type == "group") it.chatRoomId.ifEmpty { it.id } else it.id, it.name, it.type == "group") }
            )
        }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomEnd) {
        FloatingActionButton(
            onClick = onNewChatClick,
            containerColor = Color(0xFF6C5CE7),
            contentColor = Color.White,
            modifier = Modifier.padding(20.dp)
        ) {
            Icon(Icons.Filled.Add, contentDescription = "New chat", modifier = Modifier.size(28.dp))
        }
    }

    UpdateDialog(
        onDismiss = { UpdateManager.dismiss() },
        onDownload = {
            val updateInfo = (UpdateManager.state.value as? com.play4xw1n.msging.update.UpdateState.UpdateAvailable)?.info
            if (updateInfo != null) {
                UpdateManager.downloadUpdate(context, updateInfo.apkUrl)
            }
        }
    )
}

@Composable
private fun HomeHeader(userName: String, onSignOut: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(HeaderGradient)
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AvatarCircle(name = userName, size = 40.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("msging", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text("Hello, $userName", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
        }
        IconButton(onClick = onSignOut) {
            Icon(Icons.Filled.ExitToApp, contentDescription = "Sign out", tint = Color.White)
        }
    }
}

@Composable
private fun SearchBar(query: String, onQueryChange: (String) -> Unit) {
    TextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        placeholder = { Text("Search conversations…", color = SubtleText) },
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search", tint = SubtleText) },
        shape = RoundedCornerShape(24.dp),
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
}

@Composable
private fun ConversationList(conversations: List<Conversation>, onClick: (Conversation) -> Unit) {
    LazyColumn {
        items(conversations, key = { it.id }) { conversation ->
            ConversationItem(conversation = conversation, onClick = { onClick(conversation) })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConversationItem(conversation: Conversation, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box {
            AvatarCircle(name = conversation.name, size = 50.dp, color = avatarColorFor(conversation.name))
            if (conversation.isOnline) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF69F0AE))
                        .align(Alignment.BottomEnd)
                        .padding(1.dp)
                )
            }
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    conversation.name,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    modifier = Modifier.weight(1f)
                )
                if (conversation.lastMessageTime > 0) {
                    Text(
                        ConversationRepository.formatTime(conversation.lastMessageTime),
                        color = SubtleText,
                        fontSize = 11.sp
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    conversation.lastMessage.ifEmpty { "Start a conversation" },
                    color = SubtleText,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (conversation.unreadCount > 0) {
                    Spacer(Modifier.width(8.dp))
                    Badge(
                        containerColor = Color(0xFF6C5CE7),
                        contentColor = Color.White
                    ) {
                        Text("${conversation.unreadCount}", fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyState(hasConversations: Boolean) {
    Column(
        modifier = Modifier.fillMaxSize().padding(bottom = 80.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (hasConversations) {
            Text("No matches", color = Color.White, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(6.dp))
            Text("Try a different search", color = SubtleText, style = MaterialTheme.typography.bodySmall)
        } else {
            Text("No conversations yet", color = Color.White, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(6.dp))
            Text("Tap + to start a new chat", color = SubtleText, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun AvatarCircle(name: String, size: androidx.compose.ui.unit.Dp, color: Color = Color.Transparent) {
    val resolved = if (color == Color.Transparent) avatarColorFor(name) else color
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(resolved),
        contentAlignment = Alignment.Center
    ) {
        Text(
            name.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = if (size >= 40.dp) 17.sp else 12.sp
        )
    }
}

private fun avatarColorFor(sender: String): Color =
    AvatarPalette[Math.abs(sender.hashCode()) % AvatarPalette.size]
