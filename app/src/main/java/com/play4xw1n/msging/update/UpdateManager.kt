package com.play4xw1n.msging.update

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

sealed class UpdateState {
    data object Idle : UpdateState()
    data object Checking : UpdateState()
    data class UpdateAvailable(val info: UpdateInfo) : UpdateState()
    data class Downloading(val progress: Int) : UpdateState()
    data object Downloaded : UpdateState()
    data object Installing : UpdateState()
    data class Error(val message: String) : UpdateState()
}

object UpdateManager {

    private val _state = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val state: StateFlow<UpdateState> = _state

    private var downloadId: Long = -1
    private var downloadReceiver: BroadcastReceiver? = null

    fun checkForUpdate(context: Context) {
        _state.value = UpdateState.Checking
    }

    fun onUpdateChecked(info: UpdateInfo?) {
        _state.value = if (info != null) {
            UpdateState.UpdateAvailable(info)
        } else {
            UpdateState.Idle
        }
    }

    fun downloadUpdate(context: Context, apkUrl: String) {
        _state.value = UpdateState.Downloading(0)

        val request = DownloadManager.Request(Uri.parse(apkUrl))
            .setTitle("Downloading update")
            .setDescription("msging update")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "msging-update.apk")
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)

        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        downloadId = dm.enqueue(request)

        downloadReceiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                if (intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1) != downloadId) return

                val query = DownloadManager.Query().setFilterById(downloadId)
                val cursor = dm.query(query)
                if (cursor.moveToFirst()) {
                    val status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                    if (status == DownloadManager.STATUS_SUCCESSFUL) {
                        _state.value = UpdateState.Downloaded
                        val path = cursor.getString(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_LOCAL_URI))
                        installApk(ctx!!, Uri.parse(path))
                    } else {
                        _state.value = UpdateState.Error("Download failed")
                    }
                }
                cursor.close()
                ctx?.unregisterReceiver(this)
                downloadReceiver = null
            }
        }

        context.registerReceiver(downloadReceiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
    }

    private fun installApk(context: Context, uri: Uri) {
        _state.value = UpdateState.Installing
        try {
            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(installIntent)
        } catch (e: Exception) {
            _state.value = UpdateState.Error("Cannot install: enable unknown apps in settings")
        }
    }

    fun dismiss() {
        _state.value = UpdateState.Idle
    }
}
