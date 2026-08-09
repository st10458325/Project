package com.maryhairsalon.ui.manager

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maryhairsalon.data.models.Booking
import com.maryhairsalon.data.models.User
import com.maryhairsalon.data.repository.ManagerRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ManagerBookingsUiState(
    val isLoading: Boolean = true,
    val bookings: List<Booking> = emptyList(),
    val errorMessage: String? = null,
    val activeFilter: String = "all"
)

data class ManagerStaffUiState(
    val isLoading: Boolean = true,
    val users: List<User> = emptyList(),
    val errorMessage: String? = null
)

class ManagerViewModel(
    private val repository: ManagerRepository = ManagerRepository()
) : ViewModel() {

    // ---- All Bookings tab ----
    private val _activeFilter = MutableStateFlow("all")

    val bookingsUiState: StateFlow<ManagerBookingsUiState> = combine(
        _activeFilter,
        repository.observeAllBookings()
    ) { filter, result ->
        result.fold(
            onSuccess = { all ->
                val filtered = if (filter == "all") all
                else all.filter { it.status.equals(filter, ignoreCase = true) }
                ManagerBookingsUiState(isLoading = false, bookings = filtered, activeFilter = filter)
            },
            onFailure = {
                ManagerBookingsUiState(
                    isLoading = false,
                    activeFilter = filter,
                    errorMessage = "Couldn't load bookings. Please check your connection."
                )
            }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ManagerBookingsUiState())

    fun setFilter(filter: String) { _activeFilter.value = filter }

    fun updateStatus(bookingId: String, status: String) {
        viewModelScope.launch { runCatching { repository.updateBookingStatus(bookingId, status) } }
    }

    fun reassignEmployee(bookingId: String, employeeName: String) {
        viewModelScope.launch { runCatching { repository.reassignEmployee(bookingId, employeeName) } }
    }

    // ---- Staff tab ----
    val staffUiState: StateFlow<ManagerStaffUiState> = repository.observeStaffAndCustomers()
        .map { result ->
            result.fold(
                onSuccess = { users -> ManagerStaffUiState(isLoading = false, users = users) },
                onFailure = { ManagerStaffUiState(isLoading = false, errorMessage = "Couldn't load staff. Please check your connection.") }
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ManagerStaffUiState())

    fun setUserRole(uid: String, role: String) {
        viewModelScope.launch { runCatching { repository.setUserRole(uid, role) } }
    }
}