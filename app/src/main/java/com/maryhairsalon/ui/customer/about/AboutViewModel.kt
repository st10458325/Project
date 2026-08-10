package com.maryhairsalon.ui.customer.about

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maryhairsalon.data.models.User
import com.maryhairsalon.data.repository.EmployeeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AboutUiState(
    val isLoading: Boolean = true,
    val stylists: List<User> = emptyList(),
    val errorMessage: String? = null
)

class AboutViewModel(
    private val repository: EmployeeRepository = EmployeeRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(AboutUiState())
    val uiState: StateFlow<AboutUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                val stylists = repository.getEmployees()
                _uiState.value = AboutUiState(isLoading = false, stylists = stylists)
            } catch (e: Exception) {
                _uiState.value = AboutUiState(isLoading = false, errorMessage = "Couldn't load our team. Please try again.")
            }
        }
    }
}