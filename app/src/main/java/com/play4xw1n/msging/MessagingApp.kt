package com.play4xw1n.msging

import android.app.Application
import com.play4xw1n.msging.data.UserCache

class MessagingApp : Application() {
    override fun onCreate() {
        super.onCreate()
        UserCache.init(this)
    }
}
