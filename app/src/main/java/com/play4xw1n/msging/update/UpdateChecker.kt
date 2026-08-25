package com.play4xw1n.msging.update

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL

data class UpdateInfo(
    val versionName: String,
    val versionCode: Int,
    val apkUrl: String,
    val releaseNotes: String
)

object UpdateChecker {

    private const val REPO_OWNER = "4XW1N"
    private const val REPO_NAME = "msging"
    private const val API_URL =
        "https://api.github.com/repos/$REPO_OWNER/$REPO_NAME/releases/latest"

    suspend fun checkForUpdate(context: Context): UpdateInfo? = withContext(Dispatchers.IO) {
        try {
            val currentVersion = parseVersionCode(getCurrentVersionName(context))
            val response = URL(API_URL).readText()
            val json = JSONObject(response)

            val tagName = json.getString("tag_name")
            val releaseNotes = json.getString("body")
            val latestVersion = parseVersionCode(tagName.removePrefix("v"))

            val assets = json.getJSONArray("assets")
            var apkUrl: String? = null
            for (i in 0 until assets.length()) {
                val asset = assets.getJSONObject(i)
                if (asset.getString("name").endsWith(".apk")) {
                    apkUrl = asset.getString("browser_download_url")
                    break
                }
            }
            if (apkUrl == null) return@withContext null

            if (latestVersion > currentVersion) {
                UpdateInfo(
                    versionName = tagName,
                    versionCode = latestVersion,
                    apkUrl = apkUrl,
                    releaseNotes = releaseNotes
                )
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun getCurrentVersionName(context: Context): String {
        return try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "0"
        } catch (_: Exception) { "0" }
    }

    private fun parseVersionCode(version: String): Int {
        val parts = version.split(".")
        return try {
            when {
                parts.size >= 3 -> parts[0].toInt() * 10000 + parts[1].toInt() * 100 + parts[2].toInt()
                parts.size == 2 -> parts[0].toInt() * 100 + parts[1].toInt()
                else -> parts[0].toInt()
            }
        } catch (_: Exception) { 0 }
    }
}
