package com.maryhairsalon.ui.customer.bookings

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.maryhairsalon.data.local.NetworkMonitor
import com.maryhairsalon.data.local.PendingActionsStore
import com.maryhairsalon.data.models.Booking
import com.maryhairsalon.data.repository.BookingRepository
import com.maryhairsalon.sync.BookingSyncScheduler
import com.maryhairsalon.sync.SyncManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class BookingsUiState(
    val isLoading: Boolean = true,
    val bookings: List<Booking> = emptyList(),
    val counts: Map<String, Int> = emptyMap(),
    val errorMessage: String? = null,
    val activeFilter: String = "all",
    val isOffline: Boolean = false,
    val pendingCancelIds: Set<String> = emptySet()
)

class BookingsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = BookingRepository()
    private val auth = FirebaseAuth.getInstance()
    private val pendingActionsStore = PendingActionsStore(application)
    private val networkMonitor = NetworkMonitor(application)
    private val syncManager = SyncManager(pendingActionsStore, repository)

    private val _activeFilter = MutableStateFlow("all")

    private val isOnline: StateFlow<Boolean> = networkMonitor.isOnline
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val uiState: StateFlow<BookingsUiState> = combine(
        _activeFilter,
        auth.currentUser?.let { repository.observeBookings(it.uid) } ?: flowOf(Result.success(emptyList())),
        isOnline,
        pendingActionsStore.pendingCancelBookingIds
    ) { filter, result, online, pendingIds ->
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
                    errorMessage = null,
                    isOffline = !online,
                    pendingCancelIds = pendingIds
                )
            },
            onFailure = { error ->
                Log.e("BookingsVM", "Error observing bookings", error)
                BookingsUiState(
                    isLoading = false,
                    activeFilter = filter,
                    isOffline = !online,
                    pendingCancelIds = pendingIds,
                    // Don't show a scary error just because we're offline — the banner already covers that.
                    errorMessage = if (online) "Something went wrong loading your bookings. Please check your connection." else null
                )
            }
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = BookingsUiState(isLoading = true)
    )

    init {
        // Whenever connectivity comes back — including right at app start, if
        // we already have a connection — flush anything queued from an
        // earlier offline session. BookingSyncScheduler (triggered from
        // cancelBooking below) is the backup path for when the app was
        // fully closed while still offline.
        viewModelScope.launch {
            isOnline.collect { online ->
                if (online) syncManager.syncPendingCancellations()
            }
        }
    }

    fun setFilter(filter: String) {
        _activeFilter.value = filter
    }

    fun cancelBooking(bookingId: String) {
        viewModelScope.launch {
            if (isOnline.value) {
                try {
                    repository.cancelBooking(bookingId)
                } catch (e: Exception) {
                    Log.e("BookingsVM", "Error cancelling booking", e)
                }
            } else {
                pendingActionsStore.enqueueCancel(bookingId)
                BookingSyncScheduler.scheduleSync(getApplication())
            }
        }
    }
}