/**
 * SignupScreen.kt
 *
 * Compose screen for registering new user accounts with contact number dial-code selector.
 */
package com.maryhairsalon.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.maryhairsalon.R
import com.maryhairsalon.data.repository.DialCodeOption
import com.maryhairsalon.ui.components.AuthTextField
import com.maryhairsalon.ui.components.CountryCodePicker
import com.maryhairsalon.ui.components.OrDivider
import com.maryhairsalon.ui.components.SalonLogo
import com.maryhairsalon.ui.theme.AccentAmber
import com.maryhairsalon.ui.theme.BackgroundDeep
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
            .systemBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SalonLogo(size = 88.dp)
        Spacer(Modifier.height(16.dp))

        Text(
            stringResource(R.string.signup_title),
            style = MaterialTheme.typography.displayLarge,
            color = Color.White,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(4.dp))
        Text(
            stringResource(R.string.signup_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = TextMuted,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(28.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            AuthTextField(name, { name = it }, stringResource(R.string.label_first_name), modifier = Modifier.weight(1f))
            AuthTextField(surname, { surname = it }, stringResource(R.string.label_surname), modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(12.dp))
        AuthTextField(email, { email = it }, stringResource(R.string.label_email_address), keyboardType = KeyboardType.Email)
        Spacer(Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            CountryCodePicker(
                selected = selectedDialCode,
                options = countryOptions,
                onSelect = { selectedDialCode = it }
            )
            AuthTextField(
                contact, { contact = it },
                stringResource(R.string.label_contact_number),
                keyboardType = KeyboardType.Phone,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(12.dp))
        AuthTextField(password, { password = it }, stringResource(R.string.label_password), isPassword = true)
        Spacer(Modifier.height(12.dp))
        AuthTextField(confirmPassword, { confirmPassword = it }, stringResource(R.string.label_confirm_password), isPassword = true)

        uiState.errorMessage?.let {
            Spacer(Modifier.height(12.dp))
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }

        Spacer(Modifier.height(24.dp))
        Button(
            onClick = {
                val fullContact = selectedDialCode?.let { "${it.dialCode} $contact" } ?: contact
                viewModel.signup(name, surname, email, fullContact, password, confirmPassword)
            },
            enabled = !uiState.isLoading,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AccentAmber, contentColor = BackgroundDeep)
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp, color = BackgroundDeep)
            } else {
                Text(stringResource(R.string.action_signup), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(Modifier.height(20.dp))
        OrDivider()
        Spacer(Modifier.height(20.dp))

        GoogleSignInButton(
            enabled = !uiState.isLoading,
            onIdToken = { token -> viewModel.signInWithGoogle(token) },
            onError = { message -> viewModel.setError(message) }
        )

        Spacer(Modifier.height(16.dp))
        TextButton(onClick = onGoToLogin) {
            Text(stringResource(R.string.signup_have_account), color = AccentAmber)
        }
        Spacer(Modifier.height(16.dp))
    }
}
