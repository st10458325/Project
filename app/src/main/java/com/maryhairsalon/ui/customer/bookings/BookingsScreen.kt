package com.maryhairsalon.ui.customer.bookings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.maryhairsalon.data.models.Booking

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingsScreen(
    onLogout: () -> Unit,
    onReschedule: (Booking) -> Unit,
    viewModel: BookingsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var pendingCancelId by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Bookings") },
                actions = {
                    TextButton(onClick = onLogout) { Text("Log Out") }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {

            // Filter chips — mirrors .bookings-filter__pill in bookings.php
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
                uiState.isLoading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    CircularProgressIndicator()
                }
                uiState.errorMessage != null -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Text(uiState.errorMessage!!)
                }
                uiState.bookings.isEmpty() -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Text("No bookings yet — once you book a service, it'll show up here.")
                }
                else -> LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.bookings, key = { it.id }) { booking ->
                        BookingCard(
                            booking = booking,
                            onRescheduleClick = { onReschedule(booking) },
                            onCancelClick = { pendingCancelId = booking.id }
                        )
                    }
                }
            }
        }
    }

    pendingCancelId?.let { id ->
        AlertDialog(
            onDismissRequest = { pendingCancelId = null },
            title = { Text("Cancel this booking?") },
            text = { Text("This will mark the appointment as cancelled.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.cancelBooking(id)
                    pendingCancelId = null
                }) { Text("Cancel booking") }
            },
            dismissButton = {
                TextButton(onClick = { pendingCancelId = null }) { Text("No, keep it") }
            }
        )
    }
}

@Composable
private fun BookingCard(booking: Booking, onRescheduleClick: () -> Unit, onCancelClick: () -> Unit) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(booking.service.ifBlank { "Service" }, style = MaterialTheme.typography.titleMedium)
                AssistChip(onClick = {}, label = { Text(statusLabel(booking.status)) })
            }
            Spacer(Modifier.height(6.dp))
            Text("${booking.dateFormatted.ifBlank { booking.date }} · ${booking.time}")
            Text("${booking.type}${if (booking.type == "Outcall" && booking.location.isNotBlank()) " — ${booking.location}" else ""}")
            if (booking.price.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text("R${booking.price}", style = MaterialTheme.typography.titleSmall)
            }

            if (booking.status.equals("pending", ignoreCase = true) || 
                booking.status.equals("upcoming", ignoreCase = true)) {
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = onRescheduleClick) { Text("Reschedule") }
                    TextButton(onClick = onCancelClick) { Text("Cancel") }
                }
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
