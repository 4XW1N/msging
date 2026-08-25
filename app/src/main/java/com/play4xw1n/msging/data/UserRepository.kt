package com.play4xw1n.msging.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class User(
    val id: String,
    val name: String,
    val email: String,
    val isOnline: Boolean = false
)

class UserRepository {
    private val db = FirebaseFirestore.getInstance()
    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()

    private val _users = MutableStateFlow<List<User>>(emptyList())
    val users: StateFlow<List<User>> = _users

    private var registration: com.google.firebase.firestore.ListenerRegistration? = null

    fun start() {
        if (registration != null) return

        registration = db.collection("users")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    _users.value = emptyList()
                    return@addSnapshotListener
                }
                _users.value = snapshot.documents
                    .filter { it.id != currentUserId }
                    .mapNotNull { doc ->
                        runCatching {
                            val name = doc.getString("name") ?: "Unknown"
                            UserCache.put(doc.id, name)
                            User(
                                id = doc.id,
                                name = name,
                                email = doc.getString("email") ?: "",
                                isOnline = doc.getBoolean("isOnline") ?: false
                            )
                        }.getOrNull()
                    }
            }
    }

    fun stop() {
        registration?.remove()
        registration = null
    }

    fun registerCurrentUser(name: String, email: String) {
        val uid = currentUserId
        if (uid.isEmpty()) return
        db.collection("users").document(uid).set(
            mapOf(
                "name" to name,
                "email" to email,
                "isOnline" to true
            )
        )
    }
}
