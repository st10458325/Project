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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.maryhairsalon.R
import com.maryhairsalon.data.repository.DialCodeOption
import com.maryhairsalon.ui.components.CountryCodePicker
import com.maryhairsalon.ui.theme.AccentAmber
import com.maryhairsalon.ui.theme.BackgroundDeep
import com.maryhairsalon.ui.theme.SurfaceLighter
import com.maryhairsalon.ui.theme.TextMuted

@Composable
fun SignupScreen(
    onSignedUp: () -> Unit,
    onGoToLogin: () -> Unit,
    viewModel: AuthViewModel = viewModel(),
    countryCodeViewModel: CountryCodeViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val countryOptions by countryCodeViewModel.options.collectAsStateWithLifecycle()
    var selectedDialCode by remember { mutableStateOf<DialCodeOption?>(null) }

    LaunchedEffect(countryOptions) {
        if (selectedDialCode == null && countryOptions.isNotEmpty()) {
            selectedDialCode = countryOptions.find { it.countryCode == "ZA" } ?: countryOptions.first()
        }
    }

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

        Text(stringResource(R.string.signup_title), style = MaterialTheme.typography.displayMedium, color = Color.White)
        Text(stringResource(R.string.signup_subtitle), style = MaterialTheme.typography.bodyLarge, color = TextMuted)
        Spacer(Modifier.height(32.dp))

        AuthField(value = name, onValueChange = { name = it }, label = stringResource(R.string.label_first_name))
        Spacer(Modifier.height(12.dp))
        AuthField(value = surname, onValueChange = { surname = it }, label = stringResource(R.string.label_surname))
        Spacer(Modifier.height(12.dp))
        AuthField(value = email, onValueChange = { email = it }, label = stringResource(R.string.label_email_address), keyboardType = KeyboardType.Email)
        Spacer(Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            CountryCodePicker(
                selected = selectedDialCode,
                options = countryOptions,
                onSelect = { selectedDialCode = it }
            )
            AuthField(
                value = contact,
                onValueChange = { contact = it },
                label = stringResource(R.string.label_contact_number),
                keyboardType = KeyboardType.Phone,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(12.dp))
        AuthField(value = password, onValueChange = { password = it }, label = stringResource(R.string.label_password), isPassword = true)
        Spacer(Modifier.height(12.dp))
        AuthField(value = confirmPassword, onValueChange = { confirmPassword = it }, label = stringResource(R.string.label_confirm_password), isPassword = true)

        uiState.errorMessage?.let {
            Spacer(Modifier.height(12.dp))
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }

        Spacer(Modifier.height(32.dp))
        Button(
            onClick = {
                val fullContact = selectedDialCode?.let { "${it.dialCode} $contact" } ?: contact
                viewModel.signup(name, surname, email, fullContact, password, confirmPassword)
            },
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
                Text(stringResource(R.string.action_signup), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(Modifier.height(20.dp))
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            HorizontalDivider(modifier = Modifier.weight(1f), color = Color.White.copy(alpha = 0.1f))
            Text(
                stringResource(R.string.divider_or),
                color = TextMuted,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(horizontal = 12.dp)
            )
            HorizontalDivider(modifier = Modifier.weight(1f), color = Color.White.copy(alpha = 0.1f))
        }
        Spacer(Modifier.height(20.dp))

        GoogleSignInButton(
            enabled = !uiState.isLoading,
            onIdToken = { token -> viewModel.signInWithGoogle(token) },
            onError = { message -> viewModel.setError(message) }
        )

        Spacer(Modifier.height(24.dp))
        TextButton(onClick = onGoToLogin) {
            Text(stringResource(R.string.signup_have_account), color = AccentAmber)
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
    isPassword: Boolean = false,
    modifier: Modifier = Modifier.fillMaxWidth()
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        visualTransformation = if (isPassword) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        singleLine = true,
        modifier = modifier,
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