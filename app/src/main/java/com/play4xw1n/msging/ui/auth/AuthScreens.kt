package com.play4xw1n.msging.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import java.util.regex.Pattern

private val Bg = Color(0xFF0E1013)
private val Card = Color(0xFF151A21)
private val FieldBg = Color(0xFF1D232E)
private val AccentGradient = Brush.linearGradient(listOf(Color(0xFF9B8CFF), Color(0xFF6C5CE7)))
private val SubtleText = Color(0xFF8A93A6)

@Composable
fun AuthRoot() {
    var showSignUp by rememberSaveable { mutableStateOf(false) }
    if (showSignUp) {
        SignUpScreen(onSwitchToLogin = { showSignUp = false })
    } else {
        LoginScreen(onSwitchToSignUp = { showSignUp = true })
    }
}

@Composable
fun LoginScreen(onSwitchToSignUp: () -> Unit) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    fun submit() {
        if (busy) return
        if (!Pattern.compile("^[^@\\s]+@[^@\\s]+$").matcher(email).matches()) {
            error = "Enter a valid email"
            return
        }
        if (password.isEmpty()) {
            error = "Enter your password"
            return
        }
        busy = true
        error = null
        FirebaseAuth.getInstance()
            .signInWithEmailAndPassword(email.trim(), password)
            .addOnCompleteListener { result ->
                busy = false
                if (!result.isSuccessful) {
                    error = result.exception?.localizedMessage?.replace("com.google.firebase.", "")
                        ?: "Sign in failed"
                }
            }
    }

    AuthScaffold(title = "Welcome back", subtitle = "Sign in to continue chatting") {
        AuthField(value = email, onValueChange = { email = it }, label = "Email", isPassword = false)
        Spacer(Modifier.height(12.dp))
        AuthField(value = password, onValueChange = { password = it }, label = "Password", isPassword = true)
        Spacer(Modifier.height(20.dp))

        GradientButton(text = if (busy) "Signing in…" else "Sign in", enabled = !busy) { submit() }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Text("New here?", color = SubtleText, fontSize = 13.sp)
            TextButton(onClick = onSwitchToSignUp) {
                Text("Create account", color = MaterialTheme.colorScheme.primary, fontSize = 13.sp)
            }
        }
    }

    ErrorBanner(error)
}

@Composable
fun SignUpScreen(onSwitchToLogin: () -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    fun submit() {
        if (busy) return
        if (name.isBlank()) {
            error = "Enter a display name"
            return
        }
        if (!Pattern.compile("^[^@\\s]+@[^@\\s]+$").matcher(email).matches()) {
            error = "Enter a valid email"
            return
        }
        if (password.length < 6) {
            error = "Password must be at least 6 characters"
            return
        }
        busy = true
        error = null
        val auth = FirebaseAuth.getInstance()
        auth.createUserWithEmailAndPassword(email.trim(), password)
            .addOnCompleteListener { result ->
                if (result.isSuccessful) {
                    auth.currentUser?.updateProfile(
                        UserProfileChangeRequest.Builder().setDisplayName(name.trim()).build()
                    )?.addOnCompleteListener {
                        auth.currentUser?.sendEmailVerification()
                    }
                } else {
                    busy = false
                    error = result.exception?.localizedMessage?.replace("com.google.firebase.", "")
                        ?: "Sign up failed"
                }
            }
    }

    AuthScaffold(title = "Create account", subtitle = "Join msging in seconds") {
        AuthField(value = name, onValueChange = { name = it }, label = "Display name", isPassword = false)
        Spacer(Modifier.height(12.dp))
        AuthField(value = email, onValueChange = { email = it }, label = "Email", isPassword = false)
        Spacer(Modifier.height(12.dp))
        AuthField(value = password, onValueChange = { password = it }, label = "Password (6+ characters)", isPassword = true)
        Spacer(Modifier.height(20.dp))

        GradientButton(text = if (busy) "Creating…" else "Sign up", enabled = !busy) { submit() }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Text("Already registered?", color = SubtleText, fontSize = 13.sp)
            TextButton(onClick = onSwitchToLogin) {
                Text("Sign in", color = MaterialTheme.colorScheme.primary, fontSize = 13.sp)
            }
        }
    }

    ErrorBanner(error)
}

@Composable
private fun AuthScaffold(title: String, subtitle: String, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(76.dp)
                .clip(CircleShape)
                .background(AccentGradient),
            contentAlignment = Alignment.Center
        ) {
                        Text("M", color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(14.dp))
        Text("msging", color = Color.White, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text(subtitle, color = SubtleText, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(32.dp))

        Surface(color = Card, shape = RoundedCornerShape(22.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp)) {
                content()
            }
        }
    }
}

@Composable
private fun AuthField(value: String, onValueChange: (String) -> Unit, label: String, isPassword: Boolean) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = SubtleText) },
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        visualTransformation = if (isPassword) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        colors = fieldColors(),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun GradientButton(text: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (enabled) AccentGradient else Brush.linearGradient(listOf(Color(0xFF2A3040), Color(0xFF2A3040))))
            .clickable(enabled = enabled) { onClick() }
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
    }
}

@Composable
private fun ErrorBanner(error: String?) {
    if (error != null) {
        Spacer(Modifier.height(14.dp))
        Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = Color(0xFFE9ECF2),
    unfocusedTextColor = Color(0xFFE9ECF2),
    cursorColor = Color(0xFF8B7CFF),
    focusedBorderColor = Color(0xFF8B7CFF).copy(alpha = 0.6f),
    unfocusedBorderColor = Color.Transparent,
    focusedContainerColor = FieldBg,
    unfocusedContainerColor = FieldBg
)
