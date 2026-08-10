package com.maryhairsalon.ui.customer.bookings

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.maryhairsalon.data.models.Booking
import com.maryhairsalon.data.repository.BookingRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class BookingsUiState(
    val isLoading: Boolean = true,
    val bookings: List<Booking> = emptyList(),
    val counts: Map<String, Int> = emptyMap(),
    val errorMessage: String? = null,
    val activeFilter: String = "all"
)

class BookingsViewModel(
    private val repository: BookingRepository = BookingRepository(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : ViewModel() {

    private val _activeFilter = MutableStateFlow("all")
    
    val uiState: StateFlow<BookingsUiState> = combine(
        _activeFilter,
        auth.currentUser?.let { repository.observeBookings(it.uid) } ?: flowOf(Result.success(emptyList()))
    ) { filter, result ->
        result.fold(
            onSuccess = { allBookings ->
                val filtered = if (filter == "all") allBookings
                else allBookings.filter { it.status.equals(filter, ignoreCase = true) }
                
                val counts = mutableMapOf<String, Int>()
                counts["all"] = allBookings.size
                counts["pending"] = allBookings.count { it.status.equals("pending", ignoreCase = true) }
                counts["upcoming"] = allBookings.count { it.status.equals("upcoming", ignoreCase = true) }
                counts["completed"] = allBookings.count { it.status.equals("completed", ignoreCase = true) }
                counts["cancelled"] = allBookings.count { it.status.equals("cancelled", ignoreCase = true) }

                BookingsUiState(
                    isLoading = false,
                    bookings = filtered,
                    counts = counts,
                    activeFilter = filter,
                    errorMessage = null
                )
            },
            onFailure = { error ->
                Log.e("BookingsVM", "Error observing bookings", error)
                BookingsUiState(
                    isLoading = false,
                    activeFilter = filter,
                    errorMessage = "Something went wrong loading your bookings. Please check your connection."
                )
            }
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = BookingsUiState(isLoading = true)
    )

    fun setFilter(filter: String) {
        _activeFilter.value = filter
    }

    fun cancelBooking(bookingId: String) {
        viewModelScope.launch {
            try {
                repository.cancelBooking(bookingId)
            } catch (e: Exception) {
                Log.e("BookingsVM", "Error cancelling booking", e)
            }
        }
    }
}
