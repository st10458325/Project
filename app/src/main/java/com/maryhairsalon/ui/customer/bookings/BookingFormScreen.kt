package com.maryhairsalon.ui.customer.bookings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.maryhairsalon.data.models.Category
import com.maryhairsalon.data.models.User
import com.maryhairsalon.ui.theme.AccentAmberLight
import com.maryhairsalon.ui.theme.AccentDeep
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingFormScreen(
    prefillCategoryId: String? = null,
    prefillStyle: String? = null,
    prefillPrice: String? = null,
    prefillBookingId: String? = null,
    onDone: () -> Unit,
    onCancel: () -> Unit,
    viewModel: BookingFormViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showSavedDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        when {
            prefillBookingId != null -> viewModel.loadForReschedule(prefillBookingId)
            prefillCategoryId != null -> viewModel.prefillFromService(prefillCategoryId, prefillStyle, prefillPrice)
        }
    }
    LaunchedEffect(uiState.submitted) {
        if (uiState.submitted) showSavedDialog = true
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (uiState.editingBookingId != null) "Reschedule Appointment" else "Book Your Appointment") },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Cancel")
                    }
                }
            )
        }
    ) { padding ->
        if (uiState.isLoadingCatalog) {
            Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) { CircularProgressIndicator() }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            SectionTitle("1. Personal Information")
            OutlinedTextField(uiState.name, viewModel::onNameChange, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(uiState.surname, viewModel::onSurnameChange, label = { Text("Surname") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(
                uiState.contact, viewModel::onContactChange,
                label = { Text("Contact (Cell No. / WhatsApp)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth()
            )

            SectionTitle("2. Service Details")
            CategoryDropdown(
                categories = uiState.categories,
                selectedId = uiState.selectedCategoryId,
                onSelect = viewModel::onCategorySelected
            )
            StyleDropdown(
                styles = uiState.stylesForCategory,
                selected = uiState.selectedStyle,
                enabled = uiState.stylesForCategory.isNotEmpty(),
                onSelect = viewModel::onStyleSelected
            )
            if (uiState.selectedStyle != null) {
                Text(
                    "Cost: R%.2f".format(uiState.cost) +
                            if (uiState.discount > 0) "  •  ${uiState.discount.toInt()}% discount applied" else "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            SectionTitle("2.1 Choose Your Stylist")
            EmployeeRow(
                employees = uiState.employees,
                selectedId = uiState.selectedEmployeeId,
                onSelect = viewModel::onEmployeeSelected
            )

            SectionTitle("3. Booking Type")
            BookingTypeRow(selected = uiState.bookingType, onSelect = viewModel::onBookingTypeSelected)
            if (uiState.bookingType == "Outcall") {
                OutlinedTextField(
                    uiState.location, viewModel::onLocationChange,
                    label = { Text("Location (for Outcall)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            SectionTitle("4. Select Date")
            BookingCalendar(
                viewMonth = uiState.viewMonth,
                selectedDate = uiState.selectedDate,
                publicHolidays = uiState.publicHolidays,
                onPrev = viewModel::goToPreviousMonth,
                onNext = viewModel::goToNextMonth,
                onSelectDate = viewModel::onDateSelected
            )

            SectionTitle("5. Select Time")
            TimeSlotGrid(selected = uiState.selectedTime, onSelect = viewModel::onTimeSelected)

            SectionTitle("6. Additional Information")
            OutlinedTextField(
                uiState.notes, viewModel::onNotesChange,
                label = { Text("Notes / Special Requests") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth()
            )


            SectionTitle("7. Booking Summary")
            BookingSummaryCard(uiState = uiState, total = viewModel.total)

            uiState.errorMessage?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
            }

            Button(
                onClick = viewModel::submit,
                enabled = !uiState.isSubmitting,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .background(
                        brush = Brush.horizontalGradient(listOf(AccentDeep, AccentAmberLight)),
                        shape = RoundedCornerShape(12.dp)
                    ),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                elevation = ButtonDefaults.buttonElevation(0.dp)
            ) {
                if (uiState.isSubmitting) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Text(
                        if (uiState.editingBookingId != null) "Save Changes" else "Confirm Booking",
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Text(
                "Your information is safe and will never be shared.",
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }

    if (showSavedDialog) {
        AlertDialog(
            onDismissRequest = { showSavedDialog = false; onDone() },
            title = {
                Text(if (uiState.editingBookingId != null) "Changes saved" else "Booking confirmed")
            },
            text = {
                Text(
                    if (uiState.editingBookingId != null)
                        "Your appointment has been updated and is waiting for your stylist to confirm."
                    else
                        "Your booking has been sent to your stylist for confirmation."
                )
            },
            confirmButton = {
                TextButton(onClick = { showSavedDialog = false; onDone() }) { Text("OK") }
            }
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryDropdown(categories: List<Category>, selectedId: String?, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = categories.find { it.id == selectedId }?.label ?: "Select a service"

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selectedLabel, onValueChange = {}, readOnly = true,
            label = { Text("Service") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            categories.forEach { cat ->
                DropdownMenuItem(text = { Text(cat.label) }, onClick = { onSelect(cat.id); expanded = false })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StyleDropdown(styles: List<String>, selected: String?, enabled: Boolean, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(expanded = expanded && enabled, onExpandedChange = { if (enabled) expanded = it }) {
        OutlinedTextField(
            value = selected ?: "Select a style", onValueChange = {}, readOnly = true, enabled = enabled,
            label = { Text("Style") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor()
        )
        ExposedDropdownMenu(expanded = expanded && enabled, onDismissRequest = { expanded = false }) {
            styles.forEach { style ->
                DropdownMenuItem(text = { Text(style) }, onClick = { onSelect(style); expanded = false })
            }
        }
    }
}

@Composable
private fun EmployeeRow(employees: List<User>, selectedId: String?, onSelect: (String) -> Unit) {
    if (employees.isEmpty()) {
        Text(
            "No stylists available yet — the salon will assign one.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        return
    }
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        employees.forEach { emp ->
            val isSelected = emp.id == selectedId
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .border(
                        1.dp,
                        if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                        RoundedCornerShape(12.dp)
                    )
                    .clickable { onSelect(emp.id) }
                    .padding(vertical = 12.dp, horizontal = 6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(emp.name.split(" ").mapNotNull { it.firstOrNull() }.joinToString(""))
                }
                Spacer(Modifier.height(6.dp))
                Text(emp.name, style = MaterialTheme.typography.labelMedium, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                Text("Stylist", style = MaterialTheme.typography.labelSmall, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
        }
    }
}

@Composable
private fun BookingTypeRow(selected: String, onSelect: (String) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        listOf("Incall" to "You come to me", "Outcall" to "I come to you").forEach { (type, subtitle) ->
            val isSelected = type == selected
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .border(
                        1.dp,
                        if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                        RoundedCornerShape(12.dp)
                    )
                    .clickable { onSelect(type) }
                    .padding(12.dp)
            ) {
                RadioButton(selected = isSelected, onClick = { onSelect(type) })
                Column {
                    Text(type, fontWeight = FontWeight.SemiBold)
                    Text(subtitle, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
private fun BookingCalendar(
    viewMonth: YearMonth,
    selectedDate: LocalDate?,
    publicHolidays: Map<LocalDate, String>,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onSelectDate: (LocalDate) -> Unit
) {
    val today = LocalDate.now()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onPrev) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous month") }
            Text(
                "${viewMonth.month.getDisplayName(TextStyle.FULL, Locale.ENGLISH)} ${viewMonth.year}",
                fontWeight = FontWeight.SemiBold
            )
            IconButton(onClick = onNext) { Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next month") }
        }

        Spacer(Modifier.height(8.dp))

        val firstOfMonth = viewMonth.atDay(1)
        val startOffset = (firstOfMonth.dayOfWeek.value + 6) % 7 // Monday-first, matches web
        val daysInMonth = viewMonth.lengthOfMonth()
        val cells = buildList {
            repeat(startOffset) { add(null) }
            for (d in 1..daysInMonth) add(viewMonth.atDay(d))
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            modifier = Modifier.height(((cells.size / 7 + 1) * 44).dp)
        ) {
            items(DayOfWeek.entries.toTypedArray()) { dow ->
                Text(
                    dow.getDisplayName(TextStyle.SHORT, Locale.ENGLISH),
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            items(cells) { date ->
                if (date == null) {
                    Box(Modifier.size(40.dp))
                } else {
                    val isPast = date.isBefore(today)
                    val isUnavailable = publicHolidays.containsKey(date)
                    val isSelected = date == selectedDate
                    val isAvailable = !isPast && !isUnavailable

                    val bgColor = when {
                        isSelected -> MaterialTheme.colorScheme.primary
                        isAvailable -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        else -> Color.Transparent
                    }
                    val textColor = when {
                        isSelected -> MaterialTheme.colorScheme.onPrimary
                        isPast -> MaterialTheme.colorScheme.outline
                        isUnavailable -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.onSurface
                    }

                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .padding(2.dp)
                            .clip(CircleShape)
                            .background(bgColor)
                            .then(if (isAvailable) Modifier.clickable { onSelectDate(date) } else Modifier),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(date.dayOfMonth.toString(), color = textColor)
                    }
                }
            }
        }

        val holidaysThisMonth = publicHolidays.filterKeys { YearMonth.from(it) == viewMonth }
        if (holidaysThisMonth.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text(
                "Closed for: " + holidaysThisMonth.entries.sortedBy { it.key }
                    .joinToString(", ") { "${it.value} (${it.key.dayOfMonth})" },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun TimeSlotGrid(selected: String?, onSelect: (String) -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(4),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.height(((TIME_SLOTS.size / 4 + 1) * 48).dp)
    ) {
        items(TIME_SLOTS) { time ->
            val isSelected = time == selected
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { onSelect(time) }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    time,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun BookingSummaryCard(uiState: BookingFormUiState, total: Double) {
    val categoryLabel = uiState.categories.find { it.id == uiState.selectedCategoryId }?.label ?: "-"
    ElevatedCard {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            SummaryRow("Service", categoryLabel)
            SummaryRow("Style", uiState.selectedStyle ?: "-")
            SummaryRow("Booking Type", uiState.bookingType)
            SummaryRow("Date", uiState.selectedDate?.toString() ?: "-")
            SummaryRow("Time", uiState.selectedTime ?: "-")
            if (uiState.isLoadingWeather) {
                SummaryRow("Weather", "Checking forecast…")
            } else uiState.weatherForecast?.let { forecast ->
                SummaryRow("Weather", "${forecast.description}, ${forecast.minTemp.toInt()}–${forecast.maxTemp.toInt()}°C")
            }
            SummaryRow("Location", if (uiState.bookingType == "Outcall") uiState.location.ifBlank { "-" } else "N/A")
            HorizontalDivider(Modifier.padding(vertical = 6.dp))
            SummaryRow("Cost (ZAR)", "R%.2f".format(uiState.cost))
            SummaryRow("Discount", "${uiState.discount.toInt()}%")
            SummaryRow("Amount After Discount", "R%.2f".format(total), emphasized = true)
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String, emphasized: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            value,
            fontWeight = if (emphasized) FontWeight.Bold else FontWeight.Normal,
            color = if (emphasized) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
    }
}