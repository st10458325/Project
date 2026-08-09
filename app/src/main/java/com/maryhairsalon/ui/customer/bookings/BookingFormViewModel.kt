package com.maryhairsalon.ui.customer.bookings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.maryhairsalon.data.models.*
import com.maryhairsalon.data.repository.BookingRepository
import com.maryhairsalon.data.repository.CatalogRepository
import com.maryhairsalon.data.repository.EmployeeRepository
import com.maryhairsalon.data.repository.SlotUnavailableException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

val TIME_SLOTS = listOf(
    "08:00","09:00","10:00","11:00","12:00","13:00",
    "14:00","15:00","16:00","17:00","18:00","19:00"
)

val DEMO_UNAVAILABLE_DAYS = setOf(10, 11, 17, 18, 24, 25)

data class BookingFormUiState(
    val isLoadingCatalog: Boolean = true,
    val categories: List<Category> = emptyList(),
    val stylesForCategory: List<String> = emptyList(),
    val employees: List<User> = emptyList(),

    val name: String = "",
    val surname: String = "",
    val contact: String = "",

    val selectedCategoryId: String? = null,
    val selectedStyle: String? = null,
    val cost: Double = 0.0,
    val discount: Double = 0.0,

    val selectedEmployeeId: String? = null,

    val bookingType: String = "Outcall",
    val location: String = "",

    val viewMonth: YearMonth = YearMonth.now(),
    val selectedDate: LocalDate? = null,
    val selectedTime: String? = null,

    val notes: String = "",

    val editingBookingId: String? = null,
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
    val submitted: Boolean = false
)

class BookingFormViewModel(
    private val catalogRepository: CatalogRepository = CatalogRepository(),
    private val employeeRepository: EmployeeRepository = EmployeeRepository(),
    private val bookingRepository: BookingRepository = BookingRepository(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : ViewModel() {

    private val _uiState = MutableStateFlow(BookingFormUiState())
    val uiState: StateFlow<BookingFormUiState> = _uiState.asStateFlow()

    private var servicesByCategory: Map<String, List<Service>> = emptyMap()
    private val catalogJob: Job

    init {
        catalogJob = viewModelScope.launch { loadCatalogInternal() }
    }

    private suspend fun loadCatalogInternal() {
        try {
            val categories = catalogRepository.getCategories()
            val services = catalogRepository.getServices()
            val employees = employeeRepository.getEmployees()
            servicesByCategory = services.groupBy { it.category }
            _uiState.value = _uiState.value.copy(
                isLoadingCatalog = false,
                categories = categories,
                employees = employees,
                selectedEmployeeId = employees.firstOrNull()?.id
            )
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                isLoadingCatalog = false,
                errorMessage = "Couldn't load services. Please try again."
            )
        }
    }

    /** Looks up the live cost/discount for a category+style from the catalog — never trusts client-entered values. */
    private fun resolvePricing(categoryId: String?, style: String?): Pair<Double, Double> {
        val service = servicesByCategory[categoryId]?.find { it.title == style } ?: return 0.0 to 0.0
        val numericCost = Regex("\\d+(\\.\\d+)?").find(service.price)?.value?.toDoubleOrNull() ?: 0.0
        return numericCost to service.discount
    }

    fun prefillFromService(categoryId: String?, style: String?, price: String?) {
        viewModelScope.launch {
            catalogJob.join()
            val styles = servicesByCategory[categoryId]?.map { it.title } ?: emptyList()
            val (cost, discount) = resolvePricing(categoryId, style)
            _uiState.value = _uiState.value.copy(
                selectedCategoryId = categoryId,
                stylesForCategory = styles,
                selectedStyle = style,
                cost = cost,
                discount = discount
            )
        }
    }

    fun loadForReschedule(bookingId: String) {
        viewModelScope.launch {
            catalogJob.join()
            try {
                val booking = bookingRepository.getBooking(bookingId)
                if (booking == null) {
                    _uiState.value = _uiState.value.copy(errorMessage = "Could not find that booking.")
                    return@launch
                }
                val styles = servicesByCategory[booking.category]?.map { it.title } ?: emptyList()
                val employeeId = _uiState.value.employees.find { it.name == booking.employee }?.id
                val parsedDate = runCatching { LocalDate.parse(booking.date) }.getOrNull()
                val (cost, discount) = resolvePricing(booking.category, booking.service)

                _uiState.value = _uiState.value.copy(
                    editingBookingId = booking.id,
                    name = booking.name,
                    surname = booking.surname,
                    contact = booking.contact,
                    selectedCategoryId = booking.category,
                    stylesForCategory = styles,
                    selectedStyle = booking.service,
                    cost = cost,
                    discount = discount,
                    selectedEmployeeId = employeeId ?: _uiState.value.selectedEmployeeId,
                    bookingType = booking.type.ifBlank { "Outcall" },
                    location = booking.location,
                    selectedDate = parsedDate,
                    viewMonth = parsedDate?.let { YearMonth.from(it) } ?: _uiState.value.viewMonth,
                    selectedTime = booking.time,
                    notes = booking.notes
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = "Could not load that booking for editing.")
            }
        }
    }

    fun onCategorySelected(categoryId: String) {
        val styles = servicesByCategory[categoryId]?.map { it.title } ?: emptyList()
        _uiState.value = _uiState.value.copy(
            selectedCategoryId = categoryId,
            stylesForCategory = styles,
            selectedStyle = null,
            cost = 0.0,
            discount = 0.0
        )
    }

    fun onStyleSelected(style: String) {
        val (cost, discount) = resolvePricing(_uiState.value.selectedCategoryId, style)
        _uiState.value = _uiState.value.copy(selectedStyle = style, cost = cost, discount = discount)
    }

    fun onNameChange(v: String) { _uiState.value = _uiState.value.copy(name = v) }
    fun onSurnameChange(v: String) { _uiState.value = _uiState.value.copy(surname = v) }
    fun onContactChange(v: String) { _uiState.value = _uiState.value.copy(contact = v) }
    fun onNotesChange(v: String) { _uiState.value = _uiState.value.copy(notes = v) }
    fun onLocationChange(v: String) { _uiState.value = _uiState.value.copy(location = v) }
    fun onEmployeeSelected(id: String) { _uiState.value = _uiState.value.copy(selectedEmployeeId = id) }
    fun onBookingTypeSelected(type: String) { _uiState.value = _uiState.value.copy(bookingType = type) }
    fun onDateSelected(date: LocalDate) { _uiState.value = _uiState.value.copy(selectedDate = date) }
    fun onTimeSelected(time: String) { _uiState.value = _uiState.value.copy(selectedTime = time) }
    fun goToPreviousMonth() { _uiState.value = _uiState.value.copy(viewMonth = _uiState.value.viewMonth.minusMonths(1)) }
    fun goToNextMonth() { _uiState.value = _uiState.value.copy(viewMonth = _uiState.value.viewMonth.plusMonths(1)) }

    val total: Double
        get() {
            val state = _uiState.value
            return state.cost - (state.cost * state.discount / 100)
        }

    fun submit() {
        val state = _uiState.value
        val uid = auth.currentUser?.uid

        when {
            uid == null -> { _uiState.value = state.copy(errorMessage = "Please log in to book an appointment."); return }
            state.selectedCategoryId == null -> { _uiState.value = state.copy(errorMessage = "Please select a service category."); return }
            state.selectedStyle == null -> { _uiState.value = state.copy(errorMessage = "Please select a style."); return }
            state.selectedEmployeeId == null -> { _uiState.value = state.copy(errorMessage = "Please select a stylist."); return }
            state.selectedDate == null -> { _uiState.value = state.copy(errorMessage = "Please select a date."); return }
            state.selectedTime == null -> { _uiState.value = state.copy(errorMessage = "Please select a time."); return }
        }

        _uiState.value = state.copy(isSubmitting = true, errorMessage = null)

        val categoryLabel = _uiState.value.categories.find { it.id == state.selectedCategoryId }?.label ?: ""
        val employeeName = state.employees.find { it.id == state.selectedEmployeeId }?.name ?: ""
        val totalAmount = state.cost - (state.cost * state.discount / 100)
        val dateFormatted = state.selectedDate!!.format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH))

        val data = mapOf(
            "name" to state.name,
            "surname" to state.surname,
            "contact" to state.contact,
            "employee" to employeeName,
            "category" to state.selectedCategoryId,
            "categoryLabel" to categoryLabel,
            "service" to state.selectedStyle,
            "type" to state.bookingType,
            "location" to if (state.bookingType == "Outcall") state.location else "",
            "date" to state.selectedDate.toString(),
            "dateFormatted" to dateFormatted,
            "time" to state.selectedTime,
            "notes" to state.notes,
            "cost" to state.cost,
            "discount" to state.discount,
            "total" to totalAmount,
            "price" to "%.2f".format(totalAmount)
        )

        viewModelScope.launch {
            try {
                if (state.editingBookingId != null) {
                    bookingRepository.updateBooking(state.editingBookingId, state.selectedEmployeeId!!, data + ("status" to "pending"))
                } else {
                    bookingRepository.addBooking(uid!!, state.selectedEmployeeId!!, data)
                }
                _uiState.value = _uiState.value.copy(isSubmitting = false, submitted = true)
            } catch (e: SlotUnavailableException) {
                _uiState.value = _uiState.value.copy(
                    isSubmitting = false,
                    errorMessage = "That time slot was just taken — please choose a different time."
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSubmitting = false,
                    errorMessage = "Something went wrong saving your booking. Please try again."
                )
            }
        }
    }
}