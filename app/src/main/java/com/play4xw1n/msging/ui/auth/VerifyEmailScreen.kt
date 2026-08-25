package com.play4xw1n.msging.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun VerifyEmailScreen(
    email: String,
    onResend: (onResult: (Boolean) -> Unit) -> Unit,
    onCheck: (onResult: (Boolean) -> Unit) -> Unit,
    onUseDifferentAccount: () -> Unit
) {
    var checking by remember { mutableStateOf(false) }
    var resent by remember { mutableStateOf(false) }
    var note by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(resent) {
        if (resent) {
            delay(4000)
            resent = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0E1013))
            .statusBarsPadding()
            .padding(horizontal = 28.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(76.dp)
                .clip(CircleShape)
                .background(Color(0xFF1D232E)),
            contentAlignment = Alignment.Center
        ) {
            Text("@", color = Color(0xFF9B8CFF), fontSize = 32.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(16.dp))
        Text(
            "Verify your email",
            color = Color.White,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "We sent a verification link to\n$email",
            color = Color(0xFF8A93A6),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = 12.dp)
        )
        Spacer(Modifier.height(10.dp))
        Text(
            "Open the link in your email, then come back and continue.",
            color = Color(0xFF8A93A6),
            style = MaterialTheme.typography.bodySmall
        )

        Spacer(Modifier.height(28.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(if (!checking) Brush.linearGradient(listOf(Color(0xFF9B8CFF), Color(0xFF6C5CE7))) else Brush.linearGradient(listOf(Color(0xFF2A3040), Color(0xFF2A3040))))
                .clickable(enabled = !checking) {
                    checking = true
                    onCheck { verified ->
                        checking = false
                        if (!verified) note = "Not verified yet — check your inbox."
                    }
                }
                .padding(vertical = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                if (checking) "Checking…" else "I've verified my email — Continue",
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp
            )
        }

        Spacer(Modifier.height(10.dp))
        OutlinedButton(
            onClick = {
                onResend { success ->
                    resent = success
                    if (!success) note = "Couldn't send the email. Try again."
                }
            },
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (resent) "Email sent!" else "Resend verification email")
        }

        TextButton(onClick = onUseDifferentAccount) {
            Text("Use a different account", color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
        }

        note?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = Color(0xFFFFCA28), style = MaterialTheme.typography.bodySmall)
        }
    }
}
