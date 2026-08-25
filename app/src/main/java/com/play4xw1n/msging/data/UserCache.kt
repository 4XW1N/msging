package com.play4xw1n.msging.data

import android.content.Context
import android.content.SharedPreferences

object UserCache {
    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        if (!::prefs.isInitialized) {
            prefs = context.getSharedPreferences("msging_user_cache", Context.MODE_PRIVATE)
        }
    }

    fun getName(uid: String): String? =
        if (::prefs.isInitialized && uid.isNotBlank()) prefs.getString("name_$uid", null) else null

    fun put(uid: String, name: String) {
        if (!::prefs.isInitialized || uid.isBlank() || name.isBlank()) return
        prefs.edit().putString("name_$uid", name).apply()
    }

    fun clear() {
        if (::prefs.isInitialized) prefs.edit().clear().apply()
    }
}
