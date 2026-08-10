package com.maryhairsalon.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
fun SignupScreen(
    onSignedUp: () -> Unit,
    onGoToLogin: () -> Unit,
    viewModel: AuthViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var name by remember { mutableStateOf("") }
    var surname by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var contact by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    LaunchedEffect(uiState.isLoggedIn) {
        if (uiState.isLoggedIn) onSignedUp()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDeep)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(40.dp))
        Text("♕", fontSize = 64.sp, color = AccentAmber)
        Spacer(Modifier.height(16.dp))

        Text("Create Account", style = MaterialTheme.typography.displayMedium, color = Color.White)
        Text("Join Mary Hair Salon today.", style = MaterialTheme.typography.bodyLarge, color = TextMuted)
        Spacer(Modifier.height(32.dp))

        AuthField(value = name, onValueChange = { name = it }, label = "First Name")
        Spacer(Modifier.height(12.dp))
        AuthField(value = surname, onValueChange = { surname = it }, label = "Surname")
        Spacer(Modifier.height(12.dp))
        AuthField(value = email, onValueChange = { email = it }, label = "Email Address", keyboardType = KeyboardType.Email)
        Spacer(Modifier.height(12.dp))
        AuthField(value = contact, onValueChange = { contact = it }, label = "Contact Number", keyboardType = KeyboardType.Phone)
        Spacer(Modifier.height(12.dp))
        AuthField(value = password, onValueChange = { password = it }, label = "Password", isPassword = true)
        Spacer(Modifier.height(12.dp))
        AuthField(value = confirmPassword, onValueChange = { confirmPassword = it }, label = "Confirm Password", isPassword = true)

        uiState.errorMessage?.let {
            Spacer(Modifier.height(12.dp))
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }

        Spacer(Modifier.height(32.dp))
        Button(
            onClick = { viewModel.signup(name, surname, email, contact, password, confirmPassword) },
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
                Text("Sign Up", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(Modifier.height(24.dp))
        TextButton(onClick = onGoToLogin) {
            Text("Already have an account? Log In", color = AccentAmber)
        }
        Spacer(Modifier.height(40.dp))
    }
}

@Composable
private fun AuthField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    isPassword: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        visualTransformation = if (isPassword) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
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
}
