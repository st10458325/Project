package com.maryhairsalon.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.maryhairsalon.ui.theme.AccentAmber
import com.maryhairsalon.ui.theme.BackgroundDeep
import com.maryhairsalon.ui.theme.SurfaceLighter
import com.maryhairsalon.ui.theme.TextMuted

@Composable
fun LoginScreen(
    onLoggedIn: () -> Unit,
    onGoToSignup: () -> Unit,
    viewModel: AuthViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    LaunchedEffect(uiState.isLoggedIn) {
        if (uiState.isLoggedIn) onLoggedIn()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDeep)
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Logo
        Text("♕", fontSize = 64.sp, color = AccentAmber)
        Spacer(Modifier.height(16.dp))
        
        Text(
            "Welcome Back",
            style = MaterialTheme.typography.displayMedium,
            color = Color.White
        )
        Text(
            "Log in to manage your bookings.",
            style = MaterialTheme.typography.bodyLarge,
            color = TextMuted
        )
        Spacer(Modifier.height(48.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AccentAmber,
                unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                focusedLabelColor = AccentAmber,
                unfocusedLabelColor = TextMuted,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedContainerColor = SurfaceLighter,
                unfocusedContainerColor = SurfaceLighter
            )
        )
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AccentAmber,
                unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                focusedLabelColor = AccentAmber,
                unfocusedLabelColor = TextMuted,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedContainerColor = SurfaceLighter,
                unfocusedContainerColor = SurfaceLighter
            )
        )

        uiState.errorMessage?.let {
            Spacer(Modifier.height(12.dp))
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }

        Spacer(Modifier.height(32.dp))
        Button(
            onClick = { viewModel.login(email, password) },
            enabled = !uiState.isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = AccentAmber,
                contentColor = BackgroundDeep
            )
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp, color = BackgroundDeep)
            } else {
                Text("Log In", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(Modifier.height(24.dp))
        TextButton(onClick = onGoToSignup) {
            Text(
                "New to Mary Hair salon? Create an account",
                color = AccentAmber
            )
        }
    }
}
