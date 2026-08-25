package com.play4xw1n.msging

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.graphics.Color
import com.play4xw1n.msging.ui.AppViewModel
import com.play4xw1n.msging.ui.AuthStep
import com.play4xw1n.msging.ui.Screen
import com.play4xw1n.msging.ui.auth.AuthRoot
import com.play4xw1n.msging.ui.auth.VerifyEmailScreen
import com.play4xw1n.msging.ui.chat.ChatScreen
import com.play4xw1n.msging.ui.contacts.NewChatScreen
import com.play4xw1n.msging.ui.home.HomeScreen

private val AppColors = darkColorScheme(
    primary = Color(0xFF8B7CFF),
    onPrimary = Color.White,
    background = Color(0xFF0E1013),
    onBackground = Color(0xFFE8EAF0),
    surface = Color(0xFF151A21),
    onSurface = Color(0xFFE8EAF0),
    surfaceVariant = Color(0xFF1D232E),
    onSurfaceVariant = Color(0xFFB9BFCC),
    outline = Color(0xFF39404E),
    error = Color(0xFFFF6B6B)
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = AppColors) {
                Surface(color = AppColors.background) {
                    val appViewModel: AppViewModel = viewModel()
                    val step by appViewModel.step.collectAsState()
                    val currentScreen by appViewModel.currentScreen.collectAsState()

                    when (step) {
                        is AuthStep.SignedOut -> AuthRoot()
                        is AuthStep.NeedsVerification -> VerifyEmailScreen(
                            email = appViewModel.pendingEmail(),
                            onResend = appViewModel::resendVerificationEmail,
                            onCheck = appViewModel::checkVerification,
                            onUseDifferentAccount = appViewModel::signOut
                        )
                        is AuthStep.SignedIn -> {
                            when (currentScreen) {
                                is Screen.Home -> HomeScreen(
                                    userName = appViewModel.displayName(),
                                    onConversationClick = { id, name, isGroup ->
                                        if (isGroup) appViewModel.openGroupChat(id, name)
                                        else appViewModel.navigateToChat(id, name)
                                    },
                                    onNewChatClick = {
                                        appViewModel.navigateTo(Screen.NewChat)
                                    },
                                    onSignOut = appViewModel::signOut
                                )
                                is Screen.Chat -> ChatScreen(
                                    conversationId = (currentScreen as Screen.Chat).conversationId,
                                    contactName = (currentScreen as Screen.Chat).contactName,
                                    userName = appViewModel.displayName(),
                                    isGroup = (currentScreen as Screen.Chat).isGroup,
                                    onBack = appViewModel::navigateBack
                                )
                                is Screen.NewChat -> NewChatScreen(
                                    onBack = appViewModel::navigateBack,
                                    onUserClick = { userId, userName ->
                                        appViewModel.navigateToChat(userId, userName)
                                    },
                                    onNewGroup = {
                                        appViewModel.navigateTo(Screen.GroupCreator)
                                    }
                                )
                                is Screen.GroupCreator -> com.play4xw1n.msging.ui.groups.GroupCreationScreen(
                                    userName = appViewModel.displayName(),
                                    onBack = appViewModel::navigateBack,
                                    onCreated = { groupId, groupName ->
                                        appViewModel.openGroupChat(groupId, groupName)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
