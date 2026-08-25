package com.play4xw1n.msging.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class Conversation(
    val id: String,
    val name: String,
    val lastMessage: String = "",
    val lastMessageTime: Long = 0,
    val unreadCount: Int = 0,
    val isOnline: Boolean = false,
    val chatRoomId: String = "",
    val type: String = "dm"
)

class ConversationRepository {
    private val db = FirebaseFirestore.getInstance()
    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()

    private val _conversations = MutableStateFlow<List<Conversation>>(emptyList())
    val conversations: StateFlow<List<Conversation>> = _conversations

    private var registration: com.google.firebase.firestore.ListenerRegistration? = null

    fun start() {
        if (registration != null || currentUserId.isEmpty()) return

        registration = db.collection("users")
            .document(currentUserId)
            .collection("conversations")
            .orderBy("lastMessageTime")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    _conversations.value = emptyList()
                    return@addSnapshotListener
                }

                val convos = snapshot.documents.mapNotNull { doc ->
                    runCatching {
                        val type = doc.getString("type") ?: "dm"
                        Conversation(
                            id = doc.id,
                            name = if (type == "group") {
                                UserCache.getName(doc.id)
                                    ?: doc.getString("name")
                                    ?: "Group"
                            } else {
                                UserCache.getName(doc.id)
                                    ?: doc.getString("name")?.takeIf { !it.matches(Regex("^[A-Za-z0-9_-]{20,}$")) }
                                    ?: "Unknown"
                            },
                            lastMessage = doc.getString("lastMessage") ?: "",
                            lastMessageTime = doc.getLong("lastMessageTime") ?: 0,
                            unreadCount = (doc.getLong("unreadCount") ?: 0).toInt(),
                            chatRoomId = doc.getString("chatRoomId") ?: "",
                            type = type
                        )
                    }.getOrNull()
                }

                convos.forEach { convo ->
                    if (convo.type == "group") return@forEach
                    db.collection("users").document(convo.id).get()
                        .addOnSuccessListener { userDoc ->
                            val updatedName = userDoc.getString("name") ?: convo.name
                            UserCache.put(convo.id, updatedName)
                            val isOnline = userDoc.getBoolean("isOnline") ?: false
                            _conversations.value = _conversations.value.map {
                                if (it.id == convo.id) it.copy(name = updatedName, isOnline = isOnline) else it
                            }
                        }
                }

                _conversations.value = convos
            }
    }

    fun stop() {
        registration?.remove()
        registration = null
    }

    companion object {
        fun formatTime(timestamp: Long): String {
            if (timestamp == 0L) return ""
            val now = System.currentTimeMillis()
            val diff = now - timestamp
            return when {
                diff < 60_000 -> "Now"
                diff < 3_600_000 -> "${diff / 60_000}m"
                diff < 86_400_000 -> SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))
                else -> SimpleDateFormat("dd/MM/yy", Locale.getDefault()).format(Date(timestamp))
            }
        }
    }
}
