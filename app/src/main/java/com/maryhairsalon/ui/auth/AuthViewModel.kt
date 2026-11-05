/**
 * AuthViewModel.kt
 *
 * ViewModel handling application authentication state flow.
 * Coordinates user sign-in, account creation, Google Sign-In, profile loading, and email verification status.
 */
package com.maryhairsalon.ui.auth

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maryhairsalon.data.repository.AuthRepository
import com.maryhairsalon.data.repository.AuthResult
import com.maryhairsalon.data.repository.SignupData
import com.maryhairsalon.data.models.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AuthUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isLoggedIn: Boolean = false,
    val userRole: String? = null,
    val userProfile: User? = null,
    val isEmailVerified: Boolean = false,
    val isSendingVerification: Boolean = false,
    val verificationMessage: String? = null
)

class AuthViewModel(
    private val repository: AuthRepository = AuthRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        val currentUser = repository.currentUser
        if (currentUser != null) {
            _uiState.value = _uiState.value.copy(isEmailVerified = currentUser.isEmailVerified)
            fetchUserProfile(currentUser.uid)
            refreshVerificationStatus()
        }
    }

    private fun fetchUserProfile(uid: String) {
        viewModelScope.launch {
            val user = repository.getUser(uid)
            _uiState.value = _uiState.value.copy(userProfile = user)
        }
    }

    fun refreshVerificationStatus() {
        viewModelScope.launch {
            val verified = repository.refreshEmailVerifiedStatus()
            _uiState.value = _uiState.value.copy(isEmailVerified = verified)
        }
    }

    fun resendVerificationEmail() {
        _uiState.value = _uiState.value.copy(isSendingVerification = true, verificationMessage = null)
        viewModelScope.launch {
            _uiState.value = when (val result = repository.sendEmailVerification()) {
                is AuthResult.Success -> _uiState.value.copy(
                    isSendingVerification = false,
                    verificationMessage = "Verification email sent — check your inbox (and spam folder)."
                )
                is AuthResult.Error -> _uiState.value.copy(isSendingVerification = false, verificationMessage = result.message)
            }
        }
    }

    fun login(email: String, password: String) {
        Log.d("AuthVM", "Login requested for $email")
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        viewModelScope.launch {
            when (val result = repository.login(email.trim(), password)) {
                is AuthResult.Success -> {
                    Log.d("AuthVM", "Login success for ${result.user.uid}")
                    val role = repository.getUserRole(result.user.uid)
                    Log.d("AuthVM", "Role fetched: $role")
                    fetchUserProfile(result.user.uid)
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isLoggedIn = true,
                        userRole = role,
                        isEmailVerified = result.user.isEmailVerified
                    )
                    refreshVerificationStatus()
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
                is AuthResult.Success -> {
                    fetchUserProfile(result.user.uid)
                    _uiState.value.copy(isLoading = false, isLoggedIn = true, isEmailVerified = result.user.isEmailVerified)
                }
                is AuthResult.Error -> _uiState.value.copy(isLoading = false, errorMessage = result.message)
            }
        }
    }

    fun signInWithGoogle(idToken: String) {
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        viewModelScope.launch {
            when (val result = repository.signInWithGoogle(idToken)) {
                is AuthResult.Success -> {
                    val role = repository.getUserRole(result.user.uid)
                    fetchUserProfile(result.user.uid)
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isLoggedIn = true,
                        userRole = role,
                        isEmailVerified = result.user.isEmailVerified
                    )
                }
                is AuthResult.Error -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = result.message)
                }
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
    fun setError(message: String) {
        _uiState.value = _uiState.value.copy(errorMessage = message)
    }
}
