package com.play4xw1n.msging.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class GroupRepository {

    private val db = FirebaseFirestore.getInstance()
    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()

    fun createGroup(
        name: String,
        memberIds: List<String>,
        creatorName: String,
        onResult: (success: Boolean, groupId: String) -> Unit
    ) {
        if (currentUserId.isEmpty() || name.isBlank()) {
            onResult(false, "")
            return
        }
        val members = (memberIds + currentUserId).distinct()
        val groupRef = db.collection("groups").document()
        val groupId = groupRef.id
        val ts = System.currentTimeMillis()

        groupRef.set(
            mapOf(
                "name" to name.trim(),
                "members" to members,
                "createdBy" to currentUserId,
                "createdAt" to FieldValue.serverTimestamp(),
                "lastMessageTime" to ts
            )
        ).addOnSuccessListener {
            members.forEach { uid ->
                UserCache.put(groupId, name.trim())
                db.collection("users").document(uid)
                    .collection("conversations").document(groupId).set(
                        mapOf(
                            "chatRoomId" to groupId,
                            "type" to "group",
                            "name" to name.trim(),
                            "lastMessage" to ("Group created by $creatorName"),
                            "lastMessageTime" to ts,
                            "unreadCount" to 0
                        )
                    )
            }
            onResult(true, groupId)
        }.addOnFailureListener { onResult(false, "") }
    }
}
