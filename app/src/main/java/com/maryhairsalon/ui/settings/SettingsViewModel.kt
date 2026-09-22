package com.maryhairsalon.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maryhairsalon.data.repository.AuthRepository
import com.maryhairsalon.data.repository.AuthResult
import com.maryhairsalon.locale.LocaleManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SettingsUiState(
    val isLoading: Boolean = true,
    val role: String = "customer",
    val name: String = "",
    val surname: String = "",
    val contact: String = "",
    val email: String = "",
    val isSavingProfile: Boolean = false,
    val profileMessage: String? = null,
    val profileError: String? = null,

    val currentPassword: String = "",
    val newPassword: String = "",
    val confirmNewPassword: String = "",
    val isChangingPassword: Boolean = false,
    val passwordMessage: String? = null,
    val passwordError: String? = null,

    val selectedLanguage: String = LocaleManager.getCurrentLanguage()
) {
    val canEditName: Boolean get() = role == "customer"
}

class SettingsViewModel(
    private val repository: AuthRepository = AuthRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        val uid = repository.currentUser?.uid
        if (uid != null) {
            viewModelScope.launch {
                val user = repository.getUser(uid)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    role = user?.role ?: "customer",
                    name = user?.name ?: "",
                    surname = user?.surname ?: "",
                    contact = user?.contact ?: "",
                    email = user?.email ?: repository.currentUser?.email.orEmpty()
                )
            }
        } else {
            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }

    fun onNameChange(v: String) {
        if (!_uiState.value.canEditName) return
        _uiState.value = _uiState.value.copy(name = v, profileMessage = null, profileError = null)
    }

    fun onSurnameChange(v: String) {
        if (!_uiState.value.canEditName) return
        _uiState.value = _uiState.value.copy(surname = v, profileMessage = null, profileError = null)
    }

    fun onContactChange(v: String) { _uiState.value = _uiState.value.copy(contact = v, profileMessage = null, profileError = null) }

    fun saveProfile() {
        val uid = repository.currentUser?.uid ?: return
        val state = _uiState.value
        if (state.name.isBlank() || state.surname.isBlank()) {
            _uiState.value = state.copy(profileError = "Name and surname can't be empty.")
            return
        }
        _uiState.value = state.copy(isSavingProfile = true, profileError = null, profileMessage = null)
        viewModelScope.launch {
            val nameToSave = if (state.canEditName) state.name.trim() else state.name
            val surnameToSave = if (state.canEditName) state.surname.trim() else state.surname
            when (val result = repository.updateProfile(uid, nameToSave, surnameToSave, state.contact.trim())) {
                is AuthResult.Success -> _uiState.value = _uiState.value.copy(isSavingProfile = false, profileMessage = "saved")
                is AuthResult.Error -> _uiState.value = _uiState.value.copy(isSavingProfile = false, profileError = result.message)
            }
        }
    }

    fun onCurrentPasswordChange(v: String) { _uiState.value = _uiState.value.copy(currentPassword = v, passwordMessage = null, passwordError = null) }
    fun onNewPasswordChange(v: String) { _uiState.value = _uiState.value.copy(newPassword = v, passwordMessage = null, passwordError = null) }
    fun onConfirmNewPasswordChange(v: String) { _uiState.value = _uiState.value.copy(confirmNewPassword = v, passwordMessage = null, passwordError = null) }

    fun changePassword() {
        val state = _uiState.value
        when {
            state.currentPassword.isBlank() -> { _uiState.value = state.copy(passwordError = "Enter your current password."); return }
            state.newPassword.length < 8 -> { _uiState.value = state.copy(passwordError = "New password should be at least 8 characters."); return }
            state.newPassword != state.confirmNewPassword -> { _uiState.value = state.copy(passwordError = "New passwords don't match."); return }
        }
        _uiState.value = state.copy(isChangingPassword = true, passwordError = null, passwordMessage = null)
        viewModelScope.launch {
            when (val result = repository.changePassword(state.currentPassword, state.newPassword)) {
                is AuthResult.Success -> _uiState.value = _uiState.value.copy(
                    isChangingPassword = false,
                    passwordMessage = "changed",
                    currentPassword = "",
                    newPassword = "",
                    confirmNewPassword = ""
                )
                is AuthResult.Error -> _uiState.value = _uiState.value.copy(isChangingPassword = false, passwordError = result.message)
            }
        }
    }

    fun setLanguage(languageCode: String) {
        LocaleManager.setAppLocale(languageCode)
        _uiState.value = _uiState.value.copy(selectedLanguage = languageCode)
    }
}