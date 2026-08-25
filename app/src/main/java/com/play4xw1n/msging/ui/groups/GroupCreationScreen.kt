package com.play4xw1n.msging.ui.groups

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
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
import com.play4xw1n.msging.data.GroupRepository
import com.play4xw1n.msging.data.User
import com.play4xw1n.msging.data.UserRepository

private val Bg = Color(0xFF0E1013)
private val Card = Color(0xFF151A21)
private val FieldBg = Color(0xFF1D232E)
private val AccentGradient = Brush.linearGradient(listOf(Color(0xFF9B8CFF), Color(0xFF6C5CE7)))
private val SubtleText = Color(0xFF8A93A6)
private val AvatarPalette = listOf(
    Color(0xFFFF7043), Color(0xFF42A5F5), Color(0xFF66BB6A),
    Color(0xFFEC407A), Color(0xFFFFCA28), Color(0xFF26C6DA)
)

@Composable
fun GroupCreationScreen(
    userName: String,
    onBack: () -> Unit,
    onCreated: (groupId: String, groupName: String) -> Unit
) {
    val viewModel: com.play4xw1n.msging.ui.contacts.NewChatViewModel =
        viewModel(factory = com.play4xw1n.msging.ui.contacts.NewChatViewModel.factory())
    val users by viewModel.users.collectAsState()

    var groupName by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf(setOf<String>()) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier.fillMaxSize().background(Bg).statusBarsPadding()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Text("New group", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
        }

        OutlinedTextField(
            value = groupName,
            onValueChange = { groupName = it },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            placeholder = { Text("Group name", color = SubtleText) },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                cursorColor = Color(0xFF8B7CFF),
                focusedBorderColor = Color(0xFF8B7CFF).copy(alpha = 0.5f),
                unfocusedBorderColor = Color.Transparent,
                focusedContainerColor = FieldBg,
                unfocusedContainerColor = FieldBg
            )
        )

        Text(
            if (selected.isEmpty()) "Add members" else "${selected.size} selected",
            color = SubtleText,
            fontSize = 13.sp,
            modifier = Modifier.padding(start = 24.dp, top = 14.dp, bottom = 6.dp)
        )

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            items(users, key = { it.id }) { user ->
                val isSelected = user.id in selected
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            selected = if (isSelected) selected - user.id else selected + user.id
                        }
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.size(42.dp).clip(CircleShape).background(avatarColor(user.name)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            user.name.firstOrNull()?.uppercase() ?: "?",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                    Spacer(Modifier.size(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(user.name, color = Color.White, fontWeight = FontWeight.Medium)
                        Text(user.email, color = SubtleText, fontSize = 12.sp)
                    }
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) Color(0xFF6C5CE7) else Color.Transparent)
                            .clickable {
                                selected = if (isSelected) selected - user.id else selected + user.id
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }
                    if (!isSelected) {
                        Spacer(Modifier.size(24.dp))
                    }
                }
            }
        }

        error?.let {
            Text(
                it,
                color = Color(0xFFFFCA28),
                fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp)
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(if (!busy && groupName.isNotBlank()) AccentGradient else Brush.linearGradient(listOf(Color(0xFF242B38), Color(0xFF242B38))))
                .clickable(enabled = !busy && groupName.isNotBlank()) {
                    busy = true
                    error = null
                    GroupRepository().createGroup(
                        name = groupName,
                        memberIds = selected.toList(),
                        creatorName = userName
                    ) { success, groupId ->
                        busy = false
                        if (success) onCreated(groupId, groupName.trim())
                        else error = "Couldn't create the group. Try again."
                    }
                }
                .padding(vertical = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                if (busy) "Creating…" else "Create group",
                color = if (groupName.isNotBlank()) Color.White else Color(0xFF5A6272),
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp
            )
        }
    }
}

private fun avatarColor(name: String): Color =
    AvatarPalette[Math.abs(name.hashCode()) % AvatarPalette.size]
