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
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
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
import com.maryhairsalon.R
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
    var confirmCancelBookingId by remember { mutableStateOf<String?>(null) }
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
            if (uiState.isOffline) {
                OfflineBanner()
            }

            HeaderSection(
                height = with(LocalDensity.current) { currentHeaderHeight.value.toDp() },
                progress = progress.value
            )

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
                        stringResource(R.string.bookings_empty),
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
                            isPendingSync = booking.id in uiState.pendingCancelIds,
                            onRescheduleClick = { onReschedule(booking) },
                            onCancelClick = { confirmCancelBookingId = booking.id }
                        )
                    }
                }
            }
        }
    }

    confirmCancelBookingId?.let { id ->
        AlertDialog(
            onDismissRequest = { confirmCancelBookingId = null },
            title = { Text(stringResource(R.string.cancel_dialog_title)) },
            text = {
                Text(
                    if (uiState.isOffline) stringResource(R.string.cancel_dialog_body_offline)
                    else stringResource(R.string.cancel_dialog_body_online)
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.cancelBooking(id)
                    confirmCancelBookingId = null
                }) { Text(stringResource(R.string.action_confirm_cancel)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmCancelBookingId = null }) { Text(stringResource(R.string.action_keep_booking)) }
            }
        )
    }
}

@Composable
private fun OfflineBanner() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF3E2F1F))
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.WifiOff, contentDescription = null, tint = AccentAmber, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Text(
            stringResource(R.string.bookings_offline_banner),
            style = MaterialTheme.typography.labelSmall,
            color = Color.White
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
                        Color(0xFF2D1B14),
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
                    stringResource(R.string.bookings_account_label),
                    style = MaterialTheme.typography.labelMedium,
                    color = AccentAmber,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                stringResource(R.string.bookings_title),
                style = lerp(MaterialTheme.typography.titleLarge, MaterialTheme.typography.displayLarge, progress),
                color = Color.White
            )
            if (progress > 0.8f) {
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.bookings_subtitle),
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
                    label = filterLabel(status),
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
                    label = filterLabel(status),
                    count = counts[status] ?: 0,
                    selected = activeFilter == status,
                    onClick = { onFilterSelected(status) }
                )
            }
        }
    }
}

@Composable
private fun filterLabel(status: String): String = when (status) {
    "all" -> stringResource(R.string.status_all)
    "pending" -> stringResource(R.string.status_pending)
    "upcoming" -> stringResource(R.string.status_upcoming)
    "completed" -> stringResource(R.string.status_completed)
    "cancelled" -> stringResource(R.string.status_cancelled)
    else -> status.replaceFirstChar { it.uppercase() }
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
private fun BookingCard(
    booking: Booking,
    isPendingSync: Boolean,
    onRescheduleClick: () -> Unit,
    onCancelClick: () -> Unit
) {
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

                val badgeColor = when {
                    isPendingSync -> Color.White.copy(alpha = 0.1f)
                    booking.status.lowercase() == "cancelled" -> Color(0xFF3E1F1F)
                    booking.status.lowercase() == "completed" -> Color(0xFF1F3E2E)
                    else -> Color.White.copy(alpha = 0.1f)
                }
                val badgeTextColor = when {
                    isPendingSync -> AccentAmber
                    booking.status.lowercase() == "cancelled" -> Color(0xFFEF4444)
                    booking.status.lowercase() == "completed" -> Color(0xFF10B981)
                    else -> TextMuted
                }

                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(badgeColor)
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        if (isPendingSync) stringResource(R.string.status_cancelling) else statusLabel(booking.status),
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

                when {
                    isPendingSync -> Text(
                        stringResource(R.string.status_pending_sync),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                    booking.status.equals("pending", ignoreCase = true) ||
                            booking.status.equals("upcoming", ignoreCase = true) -> {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = onRescheduleClick) {
                                Text(stringResource(R.string.action_reschedule), color = AccentAmber)
                            }
                            TextButton(onClick = onCancelClick) {
                                Text(stringResource(R.string.action_cancel), color = Color.Red.copy(alpha = 0.7f))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun statusLabel(status: String): String = when (status) {
    "pending" -> stringResource(R.string.status_pending)
    "upcoming" -> stringResource(R.string.status_upcoming)
    "completed" -> stringResource(R.string.status_completed)
    "cancelled" -> stringResource(R.string.status_cancelled)
    else -> status.replaceFirstChar { it.uppercase() }
}