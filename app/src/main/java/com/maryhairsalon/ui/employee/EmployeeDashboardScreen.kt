/**
 * EmployeeDashboardScreen.kt
 *
 * Dashboard screen for salon stylists displaying assigned schedules, appointment accept/decline tools, and profile settings.
 */
package com.maryhairsalon.ui.employee

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.maryhairsalon.data.models.Booking
import com.maryhairsalon.ui.theme.AccentAmber
import com.maryhairsalon.ui.theme.TextMuted
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.maryhairsalon.ui.theme.BackgroundDeep
import androidx.compose.ui.res.stringResource
import com.maryhairsalon.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployeeDashboardScreen(
    onLogout: () -> Unit,
    onMenuClick: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        containerColor = BackgroundDeep,
        topBar = {
            CenterAlignedTopAppBar(
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = BackgroundDeep,
                    titleContentColor = Color.White
                ),
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(32.dp).padding(4.dp), contentAlignment = Alignment.Center) {
                            Image(
                                painter = painterResource(R.drawable.icon),
                                contentDescription = "Mary Hair Salon",
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text("Mary", style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.Bold)
                            Text(
                                if (selectedTab == 0) "My Schedule" else "Profile",
                                style = MaterialTheme.typography.labelSmall, color = TextMuted, fontSize = 10.sp
                            )
                        }
                    }
                },
                navigationIcon = {},
                actions = {
                    IconButton(onClick = onMenuClick) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu", tint = Color.White)
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar(containerColor = BackgroundDeep, contentColor = TextMuted, tonalElevation = 0.dp) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Assignment, contentDescription = null) },
                    label = { Text(stringResource(R.string.nav_schedule)) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = AccentAmber, selectedTextColor = AccentAmber,
                        unselectedIconColor = TextMuted, unselectedTextColor = TextMuted,
                        indicatorColor = Color.Transparent
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.Person, contentDescription = null) },
                    label = { Text(stringResource(R.string.nav_profile)) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = AccentAmber, selectedTextColor = AccentAmber,
                        unselectedIconColor = TextMuted, unselectedTextColor = TextMuted,
                        indicatorColor = Color.Transparent
                    )
                )
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp).fillMaxSize()) {
            when (selectedTab) {
                0 -> ScheduleView()
                1 -> EmployeeProfileView()
            }
        }
    }
}

@Composable
private fun ScheduleView(viewModel: EmployeeScheduleViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var pendingDeclineId by remember { mutableStateOf<String?>(null) }

    Column(Modifier.fillMaxSize()) {
        Text("My Schedule", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(16.dp))

        when {
            uiState.isLoading -> Box(Modifier.fillMaxWidth().weight(1f), Alignment.Center) {
                CircularProgressIndicator()
            }
            uiState.errorMessage != null -> Text(uiState.errorMessage!!, color = MaterialTheme.colorScheme.error)
            uiState.bookings.isEmpty() -> Text("No appointments assigned to you yet.")
            else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(uiState.bookings, key = { it.id }) { booking ->
                    ScheduleCard(
                        booking = booking,
                        onAccept = { viewModel.acceptBooking(booking.id) },
                        onDeclineClick = { pendingDeclineId = booking.id },
                        onMarkCompleted = { viewModel.markCompleted(booking.id) }
                    )
                }
            }
        }
    }

    pendingDeclineId?.let { id ->
        AlertDialog(
            onDismissRequest = { pendingDeclineId = null },
            title = { Text("Decline this booking?") },
            text = { Text("The customer will see this appointment as cancelled.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.declineBooking(id)
                    pendingDeclineId = null
                }) { Text("Decline booking") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeclineId = null }) { Text("Keep pending") }
            }
        )
    }
}

@Composable
private fun ScheduleCard(
    booking: Booking,
    onAccept: () -> Unit,
    onDeclineClick: () -> Unit,
    onMarkCompleted: () -> Unit
) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(booking.service.ifBlank { "Service" }, style = MaterialTheme.typography.titleMedium)
                AssistChip(onClick = {}, label = { Text(statusLabel(booking.status)) })
            }
            Spacer(Modifier.height(6.dp))
            Text("${booking.dateFormatted.ifBlank { booking.date }} · ${booking.time}")
            Text("${booking.name} ${booking.surname}".trim(), style = MaterialTheme.typography.bodySmall)
            Text("${booking.type}${if (booking.type == "Outcall" && booking.location.isNotBlank()) " — ${booking.location}" else ""}")
            if (booking.notes.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text("Notes: ${booking.notes}", style = MaterialTheme.typography.bodySmall)
            }

            when (booking.status) {
                "pending" -> {
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                        TextButton(onClick = onDeclineClick) { Text("Decline") }
                        Spacer(Modifier.width(4.dp))
                        Button(onClick = onAccept) { Text("Accept") }
                    }
                }
                "upcoming" -> {
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                        Button(onClick = onMarkCompleted) { Text("Mark Completed") }
                    }
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

@Composable
private fun EmployeeProfileView() {
    Column {
        Text("My Performance", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(16.dp))
        Text("Statistics and availability settings will appear here.")
    }
}
