package com.maryhairsalon.ui.auth

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maryhairsalon.data.repository.AuthRepository
import com.maryhairsalon.data.repository.AuthResult
import com.maryhairsalon.data.repository.SignupData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AuthUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isLoggedIn: Boolean = false,
    val userRole: String? = null
)

class AuthViewModel(
    private val repository: AuthRepository = AuthRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun login(email: String, password: String) {
        Log.d("AuthVM", "Login requested for $email")
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        viewModelScope.launch {
            when (val result = repository.login(email.trim(), password)) {
                is AuthResult.Success -> {
                    Log.d("AuthVM", "Login success for ${result.user.uid}")
                    val role = repository.getUserRole(result.user.uid)
                    Log.d("AuthVM", "Role fetched: $role")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isLoggedIn = true,
                        userRole = role
                    )
                }
                is AuthResult.Error -> {
                    Log.e("AuthVM", "Login error: ${result.message}")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = result.message
                    )
                }
            }
        }
    }

    fun signup(
        name: String, surname: String, email: String,
        contact: String, password: String, confirmPassword: String
    ) {
        if (password != confirmPassword) {
            _uiState.value = _uiState.value.copy(errorMessage = "Passwords don't match.")
            return
        }
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        viewModelScope.launch {
            val data = SignupData(name.trim(), surname.trim(), email.trim(), contact.trim(), password)
            _uiState.value = when (val result = repository.signup(data)) {
                is AuthResult.Success -> _uiState.value.copy(isLoading = false, isLoggedIn = true)
                is AuthResult.Error -> _uiState.value.copy(isLoading = false, errorMessage = result.message)
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}
