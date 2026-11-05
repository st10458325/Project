/**
 * VerifyEmailBanner.kt
 *
 * Warning banner composable displaying email verification prompts and resend buttons.
 */
package com.maryhairsalon.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.maryhairsalon.ui.theme.AccentAmber

@Composable
fun VerifyEmailBanner(
    isSending: Boolean,
    message: String?,
    onResend: () -> Unit,
    onRefresh: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF3E2F1F))
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Text(
            "Please verify your email to make a booking.",
            style = MaterialTheme.typography.labelMedium,
            color = Color.White
        )
        message?.let {
            Spacer(Modifier.height(4.dp))
            Text(it, style = MaterialTheme.typography.labelSmall, color = AccentAmber)
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onResend, enabled = !isSending) {
                Text(if (isSending) "Sending…" else "Resend email", color = AccentAmber)
            }
            TextButton(onClick = onRefresh) {
                Text("I've verified", color = AccentAmber)
            }
        }
    }
}
