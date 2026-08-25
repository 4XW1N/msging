package com.play4xw1n.msging

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.play4xw1n.msging.data.UserCache

class MessagingApp : Application() {
    override fun onCreate() {
        super.onCreate()
        UserCache.init(this)
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        val msgChannel = NotificationChannel(
            CHANNEL_ID,
            "Messages",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "New message notifications"
            enableVibration(true)
        }
        val serviceChannel = NotificationChannel(
            SERVICE_CHANNEL_ID,
            "Background Service",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Keeps message notifications running"
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(msgChannel)
        manager.createNotificationChannel(serviceChannel)
    }

    companion object {
        const val CHANNEL_ID = "messages"
        const val SERVICE_CHANNEL_ID = "service"
    }
}
