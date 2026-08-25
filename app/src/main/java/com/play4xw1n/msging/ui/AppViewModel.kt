package com.play4xw1n.msging.ui

import android.app.Application
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.play4xw1n.msging.fcm.FcmTokenManager
import com.play4xw1n.msging.service.MessageListenerService
import com.play4xw1n.msging.service.ServiceKeepAliveWorker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

sealed interface AuthStep {
    data object SignedOut : AuthStep
    data object NeedsVerification : AuthStep
    data class SignedIn(val displayName: String) : AuthStep
}

sealed interface Screen {
    data object Home : Screen
    data class Chat(val conversationId: String, val contactName: String, val isGroup: Boolean = false) : Screen
    data object NewChat : Screen
    data object GroupCreator : Screen
}

class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private val _step = MutableStateFlow(computeStep(auth.currentUser))
    val step: StateFlow<AuthStep> = _step

    private val _currentScreen = MutableStateFlow<Screen>(Screen.Home)
    val currentScreen: StateFlow<Screen> = _currentScreen

    init {
        auth.addAuthStateListener { listener ->
            val user = listener.currentUser
            _step.value = computeStep(user)
            if (user != null && user.isEmailVerified) {
                registerUserInFirestore(user)
                startBackgroundService()
            } else {
                stopBackgroundService()
            }
        }
    }

    private fun startBackgroundService() {
        val ctx = getApplication<Application>()
        val pm = ctx.getSystemService(PowerManager::class.java)
        if (!pm.isIgnoringBatteryOptimizations(ctx.packageName)) {
            try {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = android.net.Uri.parse("package:${ctx.packageName}")
                }
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                ctx.startActivity(intent)
            } catch (_: Exception) { }
        }
        val intent = Intent(ctx, MessageListenerService::class.java)
        ctx.startForegroundService(intent)
        ServiceKeepAliveWorker.schedule(ctx)
    }

    private fun stopBackgroundService() {
        val ctx = getApplication<Application>()
        ctx.stopService(Intent(ctx, MessageListenerService::class.java))
        ServiceKeepAliveWorker.cancel(ctx)
    }

    private fun registerUserInFirestore(user: FirebaseUser) {
        val name = user.displayName?.takeIf { it.isNotBlank() }
            ?: user.email?.substringBefore("@")
            ?: "user"
        com.play4xw1n.msging.data.UserCache.put(user.uid, name)
        db.collection("users").document(user.uid).set(
            mapOf(
                "name" to name,
                "email" to user.email.orEmpty(),
                "isOnline" to true
            )
        ).addOnSuccessListener {
            FcmTokenManager.saveTokenOnLogin()
        }
    }

    fun navigateTo(screen: Screen) {
        _currentScreen.value = screen
    }

    fun navigateToChat(otherUserId: String, otherUserName: String) {
        val myId = auth.currentUser?.uid.orEmpty()
        val conversationId = if (myId < otherUserId) "${myId}_${otherUserId}" else "${otherUserId}_${myId}"
        _currentScreen.value = Screen.Chat(conversationId, otherUserName, isGroup = false)
    }

    fun openGroupChat(groupId: String, groupName: String) {
        _currentScreen.value = Screen.Chat(groupId, groupName, isGroup = true)
    }

    fun navigateBack() {
        _currentScreen.value = Screen.Home
    }

    fun handleNotificationIntent(chatRoomId: String, chatName: String, isGroup: Boolean) {
        if (isGroup) {
            _currentScreen.value = Screen.Chat(chatRoomId, chatName, isGroup = true)
        } else {
            _currentScreen.value = Screen.Chat(chatRoomId, chatName, isGroup = false)
        }
    }

    fun pendingEmail(): String = auth.currentUser?.email.orEmpty()

    fun displayName(): String = (_step.value as? AuthStep.SignedIn)?.displayName ?: "user"

    fun checkVerification(onResult: (Boolean) -> Unit) {
        val user = auth.currentUser ?: run { onResult(false); return }
        user.reload().addOnCompleteListener {
            _step.value = computeStep(user)
            onResult(user.isEmailVerified)
        }
    }

    fun resendVerificationEmail(onResult: (Boolean) -> Unit) {
        val user = auth.currentUser ?: run { onResult(false); return }
        user.sendEmailVerification().addOnCompleteListener { onResult(it.isSuccessful) }
    }

    fun signOut() {
        stopBackgroundService()
        auth.currentUser?.let { user ->
            db.collection("users").document(user.uid).update("isOnline", false)
        }
        _currentScreen.value = Screen.Home
        auth.signOut()
    }

    private companion object {
        fun computeStep(user: FirebaseUser?): AuthStep = when {
            user == null -> AuthStep.SignedOut
            !user.isEmailVerified -> AuthStep.NeedsVerification
            else -> AuthStep.SignedIn(
                user.displayName?.takeIf { it.isNotBlank() }
                    ?: user.email?.substringBefore("@")
                    ?: "user"
            )
        }
    }
}
