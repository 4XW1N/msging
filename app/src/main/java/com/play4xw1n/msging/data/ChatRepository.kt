package com.play4xw1n.msging.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.play4xw1n.msging.data.model.Message
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class ChatRepository(
    private val conversationId: String,
    fallbackOtherName: String,
    private val isGroup: Boolean = false
) {

    private val db = FirebaseFirestore.getInstance()
    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
    private val otherUserId: String =
        conversationId.split("_").firstOrNull { it != currentUserId } ?: conversationId
    private var otherName: String =
        UserCache.getName(conversationId.split("_").firstOrNull { it != currentUserId } ?: "")
            ?: fallbackOtherName
    private var memberIds: List<String> = emptyList()
    private val messagesRef = db.collection("rooms")
        .document(conversationId)
        .collection("messages")

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages

    private val _connected = MutableStateFlow(false)
    val connected: StateFlow<Boolean> = _connected

    private var registration: com.google.firebase.firestore.ListenerRegistration? = null

    fun start() {
        if (registration != null) return
        if (isGroup) {
            db.collection("groups").document(conversationId).get().addOnSuccessListener { doc ->
                @Suppress("UNCHECKED_CAST")
                memberIds = (doc.get("members") as? List<String>) ?: emptyList()
                doc.getString("name")?.takeIf { it.isNotBlank() }?.let {
                    otherName = it
                    UserCache.put(conversationId, it)
                }
            }
        } else {
            db.collection("users").document(otherUserId).get().addOnSuccessListener { doc ->
                doc.getString("name")?.takeIf { it.isNotBlank() }?.let {
                    otherName = it
                    UserCache.put(otherUserId, it)
                }
            }
        }
        registration = messagesRef
            .orderBy("timestamp")
            .limitToLast(MAX_HISTORY)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    _connected.value = false
                    return@addSnapshotListener
                }
                _connected.value = true
                _messages.value = snapshot.documents.mapNotNull { doc ->
                    runCatching {
                        Message(
                            id = doc.id,
                            sender = doc.getString("sender") ?: "",
                            text = doc.getString("text") ?: "",
                            timestamp = doc.getTimestamp("timestamp")?.toDate()?.time
                                ?: System.currentTimeMillis(),
                            isSystem = false
                        )
                    }.getOrNull()
                }
            }
    }

    fun sendMessage(sender: String, text: String) {
        val message = mapOf(
            "sender" to sender,
            "text" to text,
            "timestamp" to FieldValue.serverTimestamp()
        )
        messagesRef.add(message)
        updateConversationLists(sender, text)
    }

    private fun updateConversationLists(senderName: String, lastMessage: String) {
        val timestamp = System.currentTimeMillis()

        if (isGroup) {
            memberIds.forEach { uid ->
                db.collection("users").document(uid)
                    .collection("conversations").document(conversationId).set(
                        mapOf(
                            "chatRoomId" to conversationId,
                            "type" to "group",
                            "name" to otherName,
                            "lastMessage" to "$senderName: $lastMessage",
                            "lastMessageTime" to timestamp,
                            "unreadCount" to if (uid == currentUserId) 0 else FieldValue.increment(1)
                        )
                    )
            }
            return
        }

        db.collection("users").document(currentUserId)
            .collection("conversations").document(otherUserId).set(
                mapOf(
                    "chatRoomId" to conversationId,
                    "type" to "dm",
                    "name" to otherName,
                    "lastMessage" to lastMessage,
                    "lastMessageTime" to timestamp,
                    "unreadCount" to 0
                )
            )

        db.collection("users").document(otherUserId)
            .collection("conversations").document(currentUserId).set(
                mapOf(
                    "chatRoomId" to conversationId,
                    "type" to "dm",
                    "name" to senderName,
                    "lastMessage" to lastMessage,
                    "lastMessageTime" to timestamp,
                    "unreadCount" to FieldValue.increment(1)
                )
            )
    }

    fun stop() {
        registration?.remove()
        registration = null
        _connected.value = false
    }

    private companion object {
        const val MAX_HISTORY = 200L
    }
}
