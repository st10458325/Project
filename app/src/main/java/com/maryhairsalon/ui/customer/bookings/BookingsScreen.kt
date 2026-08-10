package com.maryhairsalon.ui.customer.bookings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.lerp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.lerp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.maryhairsalon.data.models.Booking
import com.maryhairsalon.ui.theme.AccentAmber
import com.maryhairsalon.ui.theme.BackgroundDeep
import com.maryhairsalon.ui.theme.SurfaceLighter
import com.maryhairsalon.ui.theme.TextMuted

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingsScreen(
    onLogout: () -> Unit,
    onReschedule: (Booking) -> Unit,
    onMenuClick: () -> Unit,
    viewModel: BookingsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var pendingCancelId by remember { mutableStateOf<String?>(null) }
    val scrollState = rememberLazyListState()
    
    val headerHeight = 200.dp
    val minHeaderHeight = 80.dp
    val headerHeightPx = with(LocalDensity.current) { headerHeight.toPx() }
    val minHeaderHeightPx = with(LocalDensity.current) { minHeaderHeight.toPx() }

    val scrollOffset = remember { derivedStateOf { scrollState.firstVisibleItemScrollOffset } }
    val isFirstItemVisible = remember { derivedStateOf { scrollState.firstVisibleItemIndex == 0 } }

    val currentHeaderHeight = remember {
        derivedStateOf {
            if (isFirstItemVisible.value) {
                val offset = scrollOffset.value.toFloat()
                val newHeight = (headerHeightPx - offset).coerceAtLeast(minHeaderHeightPx)
                newHeight
            } else {
                minHeaderHeightPx
            }
        }
    }

    val progress = remember {
        derivedStateOf {
            ((currentHeaderHeight.value - minHeaderHeightPx) / (headerHeightPx - minHeaderHeightPx)).coerceIn(0f, 1f)
        }
    }

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
                        // Crown placeholder
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .padding(4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("♕", fontSize = 24.sp, color = AccentAmber)
                        }
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(
                                "Mary",
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Hair salon",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted,
                                fontSize = 10.sp
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
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            // Header Section
            HeaderSection(
                height = with(LocalDensity.current) { currentHeaderHeight.value.toDp() },
                progress = progress.value
            )

            // Filter chips
            FilterSection(
                activeFilter = uiState.activeFilter,
                counts = uiState.counts,
                onFilterSelected = { viewModel.setFilter(it) }
            )

            when {
                uiState.isLoading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    CircularProgressIndicator(color = AccentAmber)
                }
                uiState.errorMessage != null -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Text(uiState.errorMessage!!, color = Color.White)
                }
                uiState.bookings.isEmpty() -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Text(
                        "No bookings yet — once you book a service, it'll show up here.",
                        color = TextMuted,
                        modifier = Modifier.padding(32.dp)
                    )
                }
                else -> LazyColumn(
                    state = scrollState,
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
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
private fun HeaderSection(height: androidx.compose.ui.unit.Dp, progress: Float) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF2D1B14), // Dark brown gradient start
                        BackgroundDeep
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.graphicsLayer {
                alpha = progress.coerceAtLeast(0.3f)
                scaleX = 0.8f + (progress * 0.2f)
                scaleY = 0.8f + (progress * 0.2f)
            }
        ) {
            if (progress > 0.5f) {
                Text(
                    "Your Account",
                    style = MaterialTheme.typography.labelMedium,
                    color = AccentAmber,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                "My Bookings",
                style = lerp(MaterialTheme.typography.titleLarge, MaterialTheme.typography.displayLarge, progress),
                color = Color.White
            )
            if (progress > 0.8f) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "All your appointments, in one place.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted
                )
            }
        }
    }
}

@Composable
private fun FilterSection(
    activeFilter: String,
    counts: Map<String, Int>,
    onFilterSelected: (String) -> Unit
) {
    val statuses = listOf("all", "pending", "upcoming", "completed", "cancelled")
    
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            statuses.take(3).forEach { status ->
                FilterPill(
                    label = status.replaceFirstChar { it.uppercase() },
                    count = counts[status] ?: 0,
                    selected = activeFilter == status,
                    onClick = { onFilterSelected(status) }
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            statuses.drop(3).forEach { status ->
                FilterPill(
                    label = status.replaceFirstChar { it.uppercase() },
                    count = counts[status] ?: 0,
                    selected = activeFilter == status,
                    onClick = { onFilterSelected(status) }
                )
            }
        }
    }
}

@Composable
private fun FilterPill(
    label: String,
    count: Int,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = Color.Transparent,
        border = BorderStroke(
            width = 1.dp,
            color = if (selected) AccentAmber else Color.White.copy(alpha = 0.1f)
        ),
        modifier = Modifier.height(36.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) Color.White else TextMuted
            )
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(if (selected) AccentAmber else Color.White.copy(alpha = 0.1f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = count.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (selected) BackgroundDeep else Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun BookingCard(booking: Booking, onRescheduleClick: () -> Unit, onCancelClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceLighter)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    booking.service.ifBlank { "Service" },
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White
                )
                
                // Status badge
                val badgeColor = when (booking.status.lowercase()) {
                    "cancelled" -> Color(0xFF3E1F1F)
                    "completed" -> Color(0xFF1F3E2E)
                    else -> Color.White.copy(alpha = 0.1f)
                }
                val badgeTextColor = when (booking.status.lowercase()) {
                    "cancelled" -> Color(0xFFEF4444)
                    "completed" -> Color(0xFF10B981)
                    else -> TextMuted
                }

                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(badgeColor)
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        statusLabel(booking.status),
                        style = MaterialTheme.typography.labelSmall,
                        color = badgeTextColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Spacer(Modifier.height(16.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.CalendarMonth,
                    contentDescription = null,
                    tint = AccentAmber,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    booking.dateFormatted.ifBlank { booking.date },
                    color = TextMuted,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.width(16.dp))
                Icon(
                    Icons.Default.AccessTime,
                    contentDescription = null,
                    tint = AccentAmber,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    booking.time,
                    color = TextMuted,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            
            Spacer(Modifier.height(8.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = AccentAmber,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "${booking.type}${if (booking.type == "Outcall" && booking.location.isNotBlank()) " — ${booking.location}" else ""}",
                    color = TextMuted,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(Modifier.height(20.dp))
            
            HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
            
            Spacer(Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "R${booking.price}",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                
                if (booking.status.equals("pending", ignoreCase = true) || 
                    booking.status.equals("upcoming", ignoreCase = true)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = onRescheduleClick) { 
                            Text("Reschedule", color = AccentAmber) 
                        }
                        TextButton(onClick = onCancelClick) { 
                            Text("Cancel", color = Color.Red.copy(alpha = 0.7f)) 
                        }
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
