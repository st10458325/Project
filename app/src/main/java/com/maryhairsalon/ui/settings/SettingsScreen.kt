/**
 * SettingsScreen.kt
 *
 * User settings screen for managing profile contact details, changing passwords, selecting theme preferences, and switching app locales.
 */
package com.maryhairsalon.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.maryhairsalon.R
import com.maryhairsalon.data.local.LocaleManager
import com.maryhairsalon.ui.theme.AccentAmber
import com.maryhairsalon.ui.theme.BackgroundDeep
import com.maryhairsalon.ui.theme.SurfaceLighter
import com.maryhairsalon.ui.theme.TextMuted
import com.maryhairsalon.data.local.ThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = BackgroundDeep,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title), color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back), tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundDeep)
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(Modifier.padding(padding).fillMaxSize(), Alignment.Center) {
                CircularProgressIndicator(color = AccentAmber)
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SectionCard(title = stringResource(R.string.settings_profile_section)) {
                SettingsField(uiState.name, viewModel::onNameChange, stringResource(R.string.label_first_name), enabled = uiState.canEditName)
                Spacer(Modifier.height(10.dp))
                SettingsField(uiState.surname, viewModel::onSurnameChange, stringResource(R.string.label_surname), enabled = uiState.canEditName)

                if (!uiState.canEditName) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        stringResource(R.string.settings_name_locked_note),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                }

                Spacer(Modifier.height(10.dp))
                SettingsField(uiState.contact, viewModel::onContactChange, stringResource(R.string.label_contact_number), keyboardType = KeyboardType.Phone)
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = uiState.email, onValueChange = {}, readOnly = true, enabled = false,
                    label = { Text(stringResource(R.string.label_email)) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledBorderColor = Color.White.copy(alpha = 0.05f),
                        disabledLabelColor = TextMuted,
                        disabledTextColor = TextMuted,
                        disabledContainerColor = BackgroundDeep
                    )
                )

                uiState.profileError?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                if (uiState.profileMessage == "saved") {
                    Spacer(Modifier.height(8.dp))
                    Text(stringResource(R.string.profile_updated), color = AccentAmber, style = MaterialTheme.typography.bodySmall)
                }

                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = viewModel::saveProfile,
                    enabled = !uiState.isSavingProfile,
                    colors = ButtonDefaults.buttonColors(containerColor = AccentAmber, contentColor = BackgroundDeep)
                ) {
                    if (uiState.isSavingProfile) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = BackgroundDeep)
                    } else {
                        Text(stringResource(R.string.action_save_profile), fontWeight = FontWeight.Bold)
                    }
                }
            }

            SectionCard(title = stringResource(R.string.settings_password_section)) {
                SettingsField(uiState.currentPassword, viewModel::onCurrentPasswordChange, stringResource(R.string.label_current_password), isPassword = true)
                Spacer(Modifier.height(10.dp))
                SettingsField(uiState.newPassword, viewModel::onNewPasswordChange, stringResource(R.string.label_new_password), isPassword = true)
                Spacer(Modifier.height(10.dp))
                SettingsField(uiState.confirmNewPassword, viewModel::onConfirmNewPasswordChange, stringResource(R.string.label_confirm_new_password), isPassword = true)

                uiState.passwordError?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                if (uiState.passwordMessage == "changed") {
                    Spacer(Modifier.height(8.dp))
                    Text(stringResource(R.string.password_changed), color = AccentAmber, style = MaterialTheme.typography.bodySmall)
                }

                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = viewModel::changePassword,
                    enabled = !uiState.isChangingPassword,
                    colors = ButtonDefaults.buttonColors(containerColor = AccentAmber, contentColor = BackgroundDeep)
                ) {
                    if (uiState.isChangingPassword) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = BackgroundDeep)
                    } else {
                        Text(stringResource(R.string.action_change_password), fontWeight = FontWeight.Bold)
                    }
                }
            }

            SectionCard(title = "Appearance") {
                Text(
                    "Choose how Mary Hair Salon looks on this device.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
                Spacer(Modifier.height(12.dp))

                val themeOptions = listOf(
                    ThemeMode.SYSTEM to "Match system",
                    ThemeMode.LIGHT to "Light",
                    ThemeMode.DARK to "Dark"
                )

                themeOptions.forEach { (mode, label) ->
                    val selected = uiState.themeMode == mode
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(selected = selected, onClick = { viewModel.setThemeMode(mode) })
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selected,
                            onClick = { viewModel.setThemeMode(mode) },
                            colors = RadioButtonDefaults.colors(selectedColor = AccentAmber, unselectedColor = TextMuted)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(label, color = Color.White)
                    }
                }
            }

            SectionCard(title = stringResource(R.string.settings_language_section)) {
                Text(
                    stringResource(R.string.settings_language_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
                Spacer(Modifier.height(12.dp))

                val options = listOf(
                    LocaleManager.LANGUAGE_ENGLISH to stringResource(R.string.language_english),
                    LocaleManager.LANGUAGE_AFRIKAANS to stringResource(R.string.language_afrikaans),
                    LocaleManager.LANGUAGE_ZULU to stringResource(R.string.language_zulu)
                )

                options.forEach { (code, label) ->
                    val selected = uiState.selectedLanguage == code
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(selected = selected, onClick = { viewModel.setLanguage(code) })
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selected,
                            onClick = { viewModel.setLanguage(code) },
                            colors = RadioButtonDefaults.colors(selectedColor = AccentAmber, unselectedColor = TextMuted)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(label, color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceLighter, shape = RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        content()
    }
}

@Composable
private fun SettingsField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    isPassword: Boolean = false,
    enabled: Boolean = true
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
        singleLine = true,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = AccentAmber,
            unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
            focusedLabelColor = AccentAmber,
            unfocusedLabelColor = TextMuted,
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            focusedContainerColor = BackgroundDeep,
            unfocusedContainerColor = BackgroundDeep,
            disabledBorderColor = Color.White.copy(alpha = 0.05f),
            disabledLabelColor = TextMuted,
            disabledTextColor = TextMuted,
            disabledContainerColor = BackgroundDeep
        )
    )
}
