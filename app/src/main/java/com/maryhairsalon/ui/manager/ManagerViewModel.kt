package com.maryhairsalon.ui.manager

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maryhairsalon.data.models.Booking
import com.maryhairsalon.data.models.Category
import com.maryhairsalon.data.models.Service
import com.maryhairsalon.data.models.User
import com.maryhairsalon.data.repository.CatalogRepository
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

data class ManagerCatalogUiState(
    val isLoading: Boolean = true,
    val categories: List<Category> = emptyList(),
    val services: List<Service> = emptyList(),
    val errorMessage: String? = null
)

class ManagerViewModel(
    private val repository: ManagerRepository = ManagerRepository(),
    private val catalogRepository: CatalogRepository = CatalogRepository()
) : ViewModel() {

    // ---- All Bookings tab ----
    private val _activeFilter = MutableStateFlow("all")
    private val _bookingActionMessage = MutableStateFlow<String?>(null)
    val bookingActionMessage: StateFlow<String?> = _bookingActionMessage.asStateFlow()

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
    fun clearBookingActionMessage() { _bookingActionMessage.value = null }

    fun updateStatus(bookingId: String, status: String) {
        viewModelScope.launch {
            runCatching { repository.updateBookingStatus(bookingId, status) }
                .onFailure { _bookingActionMessage.value = it.message ?: "Couldn't update that booking." }
        }
    }

    fun reassignEmployee(bookingId: String, employeeId: String, employeeName: String) {
        viewModelScope.launch {
            runCatching { repository.reassignEmployee(bookingId, employeeId, employeeName) }
                .onFailure { _bookingActionMessage.value = it.message ?: "Couldn't reassign that booking." }
        }
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

    // ---- Catalog tab ----
    private val _catalogActionMessage = MutableStateFlow<String?>(null)
    val catalogActionMessage: StateFlow<String?> = _catalogActionMessage.asStateFlow()
    fun clearCatalogActionMessage() { _catalogActionMessage.value = null }

    val catalogUiState: StateFlow<ManagerCatalogUiState> = combine(
        catalogRepository.observeCategories(),
        catalogRepository.observeServices()
    ) { categoriesResult, servicesResult ->
        if (categoriesResult.isFailure || servicesResult.isFailure) {
            ManagerCatalogUiState(isLoading = false, errorMessage = "Couldn't load the catalog. Please check your connection.")
        } else {
            ManagerCatalogUiState(
                isLoading = false,
                categories = categoriesResult.getOrDefault(emptyList()),
                services = servicesResult.getOrDefault(emptyList())
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ManagerCatalogUiState())

    fun addCategory(label: String, desc: String) {
        viewModelScope.launch {
            runCatching {
                catalogRepository.addCategory(mapOf("label" to label, "desc" to desc, "image" to "", "icon" to "grid"))
                _catalogActionMessage.value = "Category added."
            }.onFailure { _catalogActionMessage.value = "Couldn't add category. Please try again." }
        }
    }

    fun updateCategory(id: String, label: String, desc: String) {
        viewModelScope.launch {
            runCatching {
                catalogRepository.updateCategory(id, mapOf("label" to label, "desc" to desc))
                _catalogActionMessage.value = "Category updated."
            }.onFailure { _catalogActionMessage.value = "Couldn't update category. Please try again." }
        }
    }

    fun deleteCategory(id: String) {
        viewModelScope.launch {
            runCatching {
                catalogRepository.deleteCategory(id)
                _catalogActionMessage.value = "Category deleted."
            }.onFailure { _catalogActionMessage.value = it.message ?: "Couldn't delete category." }
        }
    }

    fun addService(title: String, categoryId: String, price: String, discount: Double, description: String) {
        viewModelScope.launch {
            runCatching {
                catalogRepository.addService(mapOf(
                    "title" to title, "category" to categoryId, "price" to price,
                    "discount" to discount, "description" to description,
                    "tags" to emptyList<String>(), "image" to ""
                ))
                _catalogActionMessage.value = "Service added."
            }.onFailure { _catalogActionMessage.value = "Couldn't add service. Please try again." }
        }
    }

    fun updateService(id: String, title: String, categoryId: String, price: String, discount: Double, description: String) {
        viewModelScope.launch {
            runCatching {
                catalogRepository.updateService(id, mapOf(
                    "title" to title, "category" to categoryId,
                    "price" to price, "discount" to discount, "description" to description
                ))
                _catalogActionMessage.value = "Service updated."
            }.onFailure { _catalogActionMessage.value = "Couldn't update service. Please try again." }
        }
    }

    fun deleteService(id: String) {
        viewModelScope.launch {
            runCatching {
                catalogRepository.deleteService(id)
                _catalogActionMessage.value = "Service deleted."
            }.onFailure { _catalogActionMessage.value = "Couldn't delete service. Please try again." }
        }
    }
}