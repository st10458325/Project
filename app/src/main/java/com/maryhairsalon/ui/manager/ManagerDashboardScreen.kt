package com.maryhairsalon.ui.manager

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.maryhairsalon.data.models.Booking
import com.maryhairsalon.data.models.User

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManagerDashboardScreen(
    onLogout: () -> Unit,
    viewModel: ManagerViewModel = viewModel()
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (selectedTab == 0) "All Bookings" else "Staff") },
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(Icons.Default.Logout, contentDescription = "Logout")
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.CalendarMonth, contentDescription = null) },
                    label = { Text("Bookings") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.Group, contentDescription = null) },
                    label = { Text("Staff") }
                )
            }
        }
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when (selectedTab) {
                0 -> ManagerBookingsTab(viewModel)
                1 -> ManagerStaffTab(viewModel)
            }
        }
    }
}

@Composable
private fun ManagerBookingsTab(viewModel: ManagerViewModel) {
    val uiState by viewModel.bookingsUiState.collectAsStateWithLifecycle()
    val staffState by viewModel.staffUiState.collectAsStateWithLifecycle()
    val employeeNames = staffState.users.filter { it.role == "employee" }.map { it.name }
    var reassignTarget by remember { mutableStateOf<Booking?>(null) }

    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("all", "pending", "upcoming", "completed", "cancelled").forEach { status ->
                FilterChip(
                    selected = uiState.activeFilter.equals(status, ignoreCase = true),
                    onClick = { viewModel.setFilter(status) },
                    label = { Text(status.replaceFirstChar { it.uppercase() }) }
                )
            }
        }

        when {
            uiState.isLoading -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
            uiState.errorMessage != null -> Box(Modifier.fillMaxSize(), Alignment.Center) { Text(uiState.errorMessage!!) }
            uiState.bookings.isEmpty() -> Box(Modifier.fillMaxSize(), Alignment.Center) { Text("No bookings match this filter.") }
            else -> LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(uiState.bookings, key = { it.id }) { booking ->
                    ManagerBookingCard(
                        booking = booking,
                        onStatusChange = { status -> viewModel.updateStatus(booking.id, status) },
                        onReassignClick = { reassignTarget = booking }
                    )
                }
            }
        }
    }

    reassignTarget?.let { booking ->
        ReassignEmployeeDialog(
            options = employeeNames,
            current = booking.employee,
            onDismiss = { reassignTarget = null },
            onConfirm = { name ->
                viewModel.reassignEmployee(booking.id, name)
                reassignTarget = null
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ManagerBookingCard(
    booking: Booking,
    onStatusChange: (String) -> Unit,
    onReassignClick: () -> Unit
) {
    var statusMenuOpen by remember { mutableStateOf(false) }

    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(booking.service.ifBlank { "Service" }, style = MaterialTheme.typography.titleMedium)
                    Text("${booking.name} ${booking.surname}".trim(), style = MaterialTheme.typography.bodySmall)
                }
                Box {
                    AssistChip(onClick = { statusMenuOpen = true }, label = { Text(statusLabel(booking.status)) })
                    DropdownMenu(expanded = statusMenuOpen, onDismissRequest = { statusMenuOpen = false }) {
                        listOf("pending", "upcoming", "completed", "cancelled").forEach { status ->
                            DropdownMenuItem(
                                text = { Text(statusLabel(status)) },
                                onClick = { onStatusChange(status); statusMenuOpen = false }
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            Text("${booking.dateFormatted.ifBlank { booking.date }} · ${booking.time}")
            Text("${booking.type}${if (booking.type == "Outcall" && booking.location.isNotBlank()) " — ${booking.location}" else ""}")
            Text("Contact: ${booking.contact}", style = MaterialTheme.typography.bodySmall)
            if (booking.price.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text("R${booking.price}", style = MaterialTheme.typography.titleSmall)
            }
            Spacer(Modifier.height(8.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Stylist: ${booking.employee.ifBlank { "Unassigned" }}", style = MaterialTheme.typography.bodySmall)
                TextButton(onClick = onReassignClick) { Text("Reassign") }
            }
        }
    }
}

@Composable
private fun ReassignEmployeeDialog(
    options: List<String>,
    current: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Reassign stylist") },
        text = {
            if (options.isEmpty()) {
                Text("No employee accounts yet — promote a customer to Employee on the Staff tab first.")
            } else {
                Column {
                    options.forEach { name ->
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = name == current, onClick = { onConfirm(name) })
                            Text(name)
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
@Composable
private fun ManagerStaffTab(viewModel: ManagerViewModel) {
    val uiState by viewModel.staffUiState.collectAsStateWithLifecycle()

    when {
        uiState.isLoading -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
        uiState.errorMessage != null -> Box(Modifier.fillMaxSize(), Alignment.Center) { Text(uiState.errorMessage!!) }
        uiState.users.isEmpty() -> Box(Modifier.fillMaxSize(), Alignment.Center) { Text("No staff or customers found.") }
        else -> LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(uiState.users, key = { it.id }) { user ->
                StaffRow(
                    user = user,
                    onToggleRole = {
                        val newRole = if (user.role == "employee") "customer" else "employee"
                        viewModel.setUserRole(user.id, newRole)
                    }
                )
            }
        }
    }
}

@Composable
private fun StaffRow(user: User, onToggleRole: () -> Unit) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(user.name, style = MaterialTheme.typography.titleMedium)
                Text(user.email, style = MaterialTheme.typography.bodySmall)
                Text(
                    if (user.role == "employee") "Employee" else "Customer",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            OutlinedButton(onClick = onToggleRole) {
                Text(if (user.role == "employee") "Make Customer" else "Make Employee")
            }
        }
    }
}

private fun statusLabel(status: String) = when (status) {
    "pending" -> "Pending"
    "upcoming" -> "Upcoming"
    "completed" -> "Completed"
    "cancelled" -> "Cancelled"
    else -> status.replaceFirstChar { it.uppercase() }
}