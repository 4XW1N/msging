package com.play4xw1n.msging.service

import android.app.ActivityManager
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.play4xw1n.msging.MainActivity
import com.play4xw1n.msging.MessagingApp
import com.play4xw1n.msging.R
import com.play4xw1n.msging.data.UserCache

class MessageListenerService : Service() {

    private val db = FirebaseFirestore.getInstance()
    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
    private val listeners = mutableListOf<com.google.firebase.firestore.ListenerRegistration>()
    private val seenMessages = mutableSetOf<String>()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (currentUserId.isEmpty()) {
            stopSelf()
            return START_NOT_STICKY
        }

        startForeground(9999, buildNotification("Listening for messages..."))
        startListening()
        return START_STICKY
    }

    private fun isAppInForeground(): Boolean {
        val am = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        @Suppress("DEPRECATION")
        val proc = am.runningAppProcesses?.firstOrNull { it.processName == packageName }
        return proc?.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
    }

    private fun startListening() {
        db.collection("users")
            .document(currentUserId)
            .collection("conversations")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener

                for (doc in snapshot.documents) {
                    val roomId = doc.getString("chatRoomId") ?: continue
                    val roomName = doc.getString("name") ?: "Chat"
                    val type = doc.getString("type") ?: "dm"
                    listenToRoom(roomId, roomName, type)
                }
            }
    }

    private fun listenToRoom(roomId: String, roomName: String, type: String) {
        val existing = listeners.find { it.hashCode() == roomId.hashCode() }
        existing?.remove()
        listeners.removeAll { it.hashCode() == roomId.hashCode() }

        val reg = db.collection("rooms")
            .document(roomId)
            .collection("messages")
            .orderBy("timestamp")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener

                for (doc in snapshot.documents) {
                    val sender = doc.getString("sender") ?: continue
                    val text = doc.getString("text") ?: continue
                    val msgId = doc.id

                    if (sender == currentUserId) continue
                    if (seenMessages.contains(msgId)) continue
                    seenMessages.add(msgId)

                    val timestampRaw = doc.get("timestamp")
                    val timestamp = when (timestampRaw) {
                        is Number -> timestampRaw.toLong()
                        is com.google.firebase.Timestamp -> timestampRaw.toDate().time
                        else -> System.currentTimeMillis()
                    }
                    val lastSeen = getLastSeenTimestamp(roomId)
                    if (timestamp <= lastSeen) continue

                    if (isAppInForeground()) {
                        updateLastSeenTimestamp(roomId, timestamp)
                        continue
                    }

                    val senderName = resolveSenderName(sender, type, roomName)
                    val title = if (type == "group") "$roomName - $senderName" else senderName
                    val truncated = if (text.length > 80) text.substring(0, 80) + "..." else text

                    showChatNotification(roomId, roomName, title, truncated, type)
                    updateLastSeenTimestamp(roomId, timestamp)
                }
            }
        listeners.add(reg)
    }

    private fun resolveSenderName(senderUid: String, type: String, roomName: String): String {
        val cached = UserCache.getName(senderUid)
        if (cached != null) return cached

        if (type == "group") {
            val nameFromGroup = UserCache.getName(roomName)
            if (nameFromGroup != null) return nameFromGroup
        }

        var resolved = if (type == "group") roomName else "Someone"
        db.collection("users").document(senderUid).get()
            .addOnSuccessListener { doc ->
                val name = doc.getString("name")
                if (!name.isNullOrBlank()) {
                    UserCache.put(senderUid, name)
                }
            }
        return resolved
    }

    private fun showChatNotification(roomId: String, roomName: String, title: String, body: String, type: String) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigateToChat", true)
            putExtra("chatRoomId", roomId)
            putExtra("chatName", roomName)
            putExtra("isGroup", type == "group")
        }

        val pending = PendingIntent.getActivity(
            this, roomId.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, MessagingApp.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pending)
            .build()

        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(roomId.hashCode(), notification)
    }

    private fun getLastSeenTimestamp(roomId: String): Long {
        val prefs = getSharedPreferences("msging_notification_prefs", MODE_PRIVATE)
        return prefs.getLong("seen_$roomId", 0L)
    }

    private fun updateLastSeenTimestamp(roomId: String, timestamp: Long) {
        getSharedPreferences("msging_notification_prefs", MODE_PRIVATE)
            .edit().putLong("seen_$roomId", timestamp).apply()
    }

    private fun buildNotification(text: String): Notification {
        return NotificationCompat.Builder(this, MessagingApp.SERVICE_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("msging")
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        listeners.forEach { it.remove() }
        listeners.clear()
        super.onDestroy()
    }
}
