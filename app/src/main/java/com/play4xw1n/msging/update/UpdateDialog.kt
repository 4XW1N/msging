package com.play4xw1n.msging.update

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Purple = Color(0xFF6C5CE7)

@Composable
fun UpdateDialog(
    onDismiss: () -> Unit,
    onDownload: () -> Unit
) {
    val state by UpdateManager.state.collectAsState()

    when (val s = state) {
        is UpdateState.UpdateAvailable -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                containerColor = Color(0xFF1A1D26),
                titleContentColor = Color.White,
                textContentColor = Color(0xFFB0B8C8),
                shape = RoundedCornerShape(20.dp),
                title = {
                    Text("Update Available", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                },
                text = {
                    Column {
                        Text(
                            "Version ${s.info.versionName}",
                            color = Purple,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            s.info.releaseNotes.take(300),
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = onDownload,
                        colors = ButtonDefaults.buttonColors(containerColor = Purple)
                    ) {
                        Text("Update")
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = onDismiss) {
                        Text("Later", color = Color(0xFFB0B8C8))
                    }
                }
            )
        }
        is UpdateState.Downloading -> {
            AlertDialog(
                onDismissRequest = {},
                containerColor = Color(0xFF1A1D26),
                titleContentColor = Color.White,
                textContentColor = Color(0xFFB0B8C8),
                shape = RoundedCornerShape(20.dp),
                title = {
                    Text("Downloading...", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                },
                text = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        @Suppress("DEPRECATION")
                        LinearProgressIndicator(
                            progress = s.progress / 100f,
                            modifier = Modifier.fillMaxWidth().height(6.dp),
                            color = Purple,
                            trackColor = Color(0xFF2A2D36),
                        )
                        Spacer(Modifier.height(12.dp))
                        Text("${s.progress}%", fontSize = 14.sp, textAlign = TextAlign.Center)
                    }
                },
                confirmButton = {}
            )
        }
        is UpdateState.Downloaded -> {
            AlertDialog(
                onDismissRequest = {},
                containerColor = Color(0xFF1A1D26),
                titleContentColor = Color.White,
                textContentColor = Color(0xFFB0B8C8),
                shape = RoundedCornerShape(20.dp),
                title = {
                    Text("Download Complete", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                },
                text = {
                    Text("Tap Install to update the app.", fontSize = 14.sp)
                },
                confirmButton = {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = Purple)
                    ) {
                        Text("OK")
                    }
                }
            )
        }
        is UpdateState.Error -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                containerColor = Color(0xFF1A1D26),
                titleContentColor = Color.White,
                textContentColor = Color(0xFFB0B8C8),
                shape = RoundedCornerShape(20.dp),
                title = {
                    Text("Update Failed", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                },
                text = {
                    Text(s.message, fontSize = 14.sp)
                },
                confirmButton = {
                    OutlinedButton(onClick = onDismiss) {
                        Text("OK", color = Color(0xFFB0B8C8))
                    }
                }
            )
        }
        is UpdateState.Checking -> {
            AlertDialog(
                onDismissRequest = {},
                containerColor = Color(0xFF1A1D26),
                titleContentColor = Color.White,
                shape = RoundedCornerShape(20.dp),
                title = {
                    Text("Checking for updates...", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(32.dp),
                            color = Purple
                        )
                    }
                },
                confirmButton = {}
            )
        }
        else -> {}
    }
}
