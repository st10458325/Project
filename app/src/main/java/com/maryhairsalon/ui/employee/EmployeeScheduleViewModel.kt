package com.maryhairsalon.ui.employee

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.maryhairsalon.data.models.Booking
import com.maryhairsalon.data.repository.AuthRepository
import com.maryhairsalon.data.repository.BookingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class EmployeeScheduleUiState(
    val isLoading: Boolean = true,
    val bookings: List<Booking> = emptyList(),
    val errorMessage: String? = null
)

class EmployeeScheduleViewModel(
    private val authRepository: AuthRepository = AuthRepository(),
    private val bookingRepository: BookingRepository = BookingRepository(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : ViewModel() {

    private val _uiState = MutableStateFlow(EmployeeScheduleUiState())
    val uiState: StateFlow<EmployeeScheduleUiState> = _uiState.asStateFlow()

    init {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            _uiState.value = EmployeeScheduleUiState(isLoading = false, errorMessage = "Please log in to see your schedule.")
        } else {
            viewModelScope.launch {
                val me = authRepository.getUser(uid)
                if (me == null || me.name.isBlank()) {
                    _uiState.value = EmployeeScheduleUiState(
                        isLoading = false,
                        errorMessage = "Couldn't load your profile. Please try again."
                    )
                    return@launch
                }
                bookingRepository.observeBookingsForEmployee(me.name).collect { result ->
                    _uiState.value = result.fold(
                        onSuccess = { bookings -> EmployeeScheduleUiState(isLoading = false, bookings = bookings) },
                        onFailure = { EmployeeScheduleUiState(isLoading = false, errorMessage = "Couldn't load your schedule. Please check your connection.") }
                    )
                }
            }
        }
    }

    fun markCompleted(bookingId: String) {
        viewModelScope.launch {
            try {
                bookingRepository.markCompleted(bookingId)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = "Couldn't update that booking. Please try again.")
            }
        }
    }

    fun acceptBooking(bookingId: String) {
        viewModelScope.launch {
            try {
                bookingRepository.acceptBooking(bookingId)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = "Couldn't accept that booking. Please try again.")
            }
        }
    }

    fun declineBooking(bookingId: String) {
        viewModelScope.launch {
            try {
                bookingRepository.declineBooking(bookingId)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = "Couldn't decline that booking. Please try again.")
            }
        }
    }
}