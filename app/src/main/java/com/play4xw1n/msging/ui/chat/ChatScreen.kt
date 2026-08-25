package com.play4xw1n.msging.ui.chat

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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontStyle
import com.google.firebase.auth.FirebaseAuth
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.play4xw1n.msging.data.model.Message
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val Bg = Color(0xFF0E1013)
private val HeaderGradient = Brush.horizontalGradient(listOf(Color(0xFF6C5CE7), Color(0xFF9B8CFF)))
private val SendGradient = Brush.linearGradient(listOf(Color(0xFF9B8CFF), Color(0xFF6C5CE7)))
private val BubbleMe = Color(0xFF6C5CE7)
private val BubbleOther = Color(0xFF1E242E)
private val TextOnMe = Color.White
private val TextOnOther = Color(0xFFE9ECF2)
private val SubtleText = Color(0xFF8A93A6)
private val InputSurface = Color(0xFF151A21)
private val AvatarPalette = listOf(
    Color(0xFFFF7043), Color(0xFF42A5F5), Color(0xFF66BB6A),
    Color(0xFFEC407A), Color(0xFFFFCA28), Color(0xFF26C6DA)
)

@Composable
fun ChatScreen(conversationId: String, contactName: String, userName: String, isGroup: Boolean = false, onBack: () -> Unit) {
    val viewModel: ChatViewModel = viewModel(factory = ChatViewModel.factory(conversationId, contactName, isGroup))
    val messages by viewModel.messages.collectAsState()
    val connected by viewModel.connected.collectAsState()
    var input by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(Unit) { viewModel.start() }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex)
    }

    Column(modifier = Modifier.fillMaxSize().background(Bg)) {
        ChatHeader(contactName = contactName, connected = connected, onBack = onBack)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            Box(modifier = Modifier.weight(1f)) {
                if (messages.isEmpty()) {
                    EmptyState()
                } else {
                    MessageList(messages = messages, myName = userName, state = listState)
                }
            }

            InputBar(
                value = input,
                enabled = connected,
                onValueChange = { input = it },
                onSend = {
                    viewModel.send(userName, input)
                    input = ""
                }
            )
        }
    }
}

@Composable
private fun ChatHeader(contactName: String, connected: Boolean, onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(HeaderGradient)
            .statusBarsPadding()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
        }
        Spacer(Modifier.size(8.dp))
        AvatarCircle(name = contactName, size = 42.dp)
        Spacer(Modifier.size(12.dp))
        Column(Modifier.weight(1f)) {
            Text(contactName, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(if (connected) Color(0xFF69F0AE) else Color(0xFFFF8A80))
                )
                Spacer(Modifier.size(5.dp))
                Text(
                    if (connected) "online" else "offline",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun MessageList(messages: List<Message>, myName: String, state: androidx.compose.foundation.lazy.LazyListState) {
    val rows = remember(messages) { buildRows(messages) }
    LazyColumn(
        state = state,
        modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        itemsIndexed(rows, contentType = { _, row -> row::class.simpleName }) { index, row ->
            when (row) {
                is Row.Day -> DayChip(row.label)
                is Row.Msg -> MessageRow(
                    message = row.message,
                    myName = myName,
                    modifier = if (index == rows.lastIndex) Modifier.padding(top = 6.dp) else Modifier
                )
            }
        }
    }
}

private sealed interface Row {
    data class Day(val label: String) : Row
    data class Msg(val message: Message) : Row
}

private fun buildRows(messages: List<Message>): List<Row> {
    val fmt = SimpleDateFormat("EEEE, d MMMM", Locale.getDefault())
    val cal = Calendar.getInstance()
    var lastDay = Int.MIN_VALUE
    return buildList {
        for (message in messages) {
            cal.timeInMillis = message.timestamp
            val dayKey = cal.get(Calendar.YEAR) * 1000 + cal.get(Calendar.DAY_OF_YEAR)
            if (dayKey != lastDay) {
                add(Row.Day(fmt.format(Date(message.timestamp))))
                lastDay = dayKey
            }
            add(Row.Msg(message))
        }
    }
}

@Composable
private fun DayChip(label: String) {
    Box(Modifier.padding(vertical = 10.dp)) {
        Text(
            label,
            fontSize = 11.sp,
            color = SubtleText,
            fontStyle = FontStyle.Italic,
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(Color(0xFF1A1F28))
                .padding(horizontal = 12.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun MessageRow(message: Message, myName: String, modifier: Modifier = Modifier) {
    val mine = message.sender == FirebaseAuth.getInstance().currentUser?.uid
    val time = remember(message.timestamp) {
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(message.timestamp))
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start
    ) {
        if (mine) {
            MyBubble(text = message.text, time = time)
        } else {
            OtherBubble(message = message, time = time)
        }
    }
}

@Composable
private fun MyBubble(text: String, time: String) {
    Surface(
        color = BubbleMe,
        shape = RoundedCornerShape(18.dp, 18.dp, 4.dp, 18.dp),
        modifier = Modifier.widthIn(max = 300.dp)
    ) {
        Column(Modifier.padding(start = 14.dp, end = 14.dp, top = 8.dp, bottom = 6.dp)) {
            Text(text, color = TextOnMe, fontSize = 15.sp, lineHeight = 20.sp)
            Text(
                time,
                color = TextOnMe.copy(alpha = 0.6f),
                fontSize = 10.sp,
                modifier = Modifier.align(Alignment.End)
            )
        }
    }
}

@Composable
private fun OtherBubble(message: Message, time: String) {
    val avatarColor = avatarColorFor(message.sender)
    Row(verticalAlignment = Alignment.Bottom) {
        AvatarCircle(name = message.sender, size = 30.dp, color = avatarColor)
        Spacer(Modifier.size(6.dp))
        Surface(
            color = BubbleOther,
            shape = RoundedCornerShape(18.dp, 18.dp, 18.dp, 4.dp),
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Column(Modifier.padding(start = 14.dp, end = 14.dp, top = 6.dp, bottom = 6.dp)) {
                Text(message.sender, color = avatarColor, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                Text(message.text, color = TextOnOther, fontSize = 15.sp, lineHeight = 20.sp)
                Text(
                    time,
                    color = SubtleText,
                    fontSize = 10.sp,
                    modifier = Modifier.align(Alignment.End)
                )
            }
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

@Composable
private fun InputBar(value: String, enabled: Boolean, onValueChange: (String) -> Unit, onSend: () -> Unit) {
    Surface(
        color = InputSurface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Type a message…", color = SubtleText) },
                shape = RoundedCornerShape(26.dp),
                maxLines = 4,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextOnOther,
                    unfocusedTextColor = TextOnOther,
                    cursorColor = MaterialTheme.colorScheme.primary,
                    focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                    unfocusedBorderColor = Color.Transparent,
                    focusedContainerColor = Color(0xFF1D232E),
                    unfocusedContainerColor = Color(0xFF1D232E)
                )
            )
            Spacer(Modifier.size(8.dp))
            val canSend = enabled && value.isNotBlank()
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(if (canSend) SendGradient else SolidColor(Color(0xFF242B38)))
                    .clickable(enabled = canSend) { onSend() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.Send,
                    contentDescription = "Send",
                    tint = if (canSend) Color.White else Color(0xFF5A6272),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

private fun avatarColorFor(sender: String): Color =
    AvatarPalette[Math.abs(sender.hashCode()) % AvatarPalette.size]

@Composable
private fun EmptyState() {
    Column(
        modifier = Modifier.fillMaxSize().padding(bottom = 60.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("No messages yet", style = MaterialTheme.typography.titleMedium, color = TextOnOther)
        Spacer(Modifier.height(6.dp))
        Text("Be the first to say hello.", style = MaterialTheme.typography.bodySmall, color = SubtleText)
    }
}
