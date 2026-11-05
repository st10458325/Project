/**
 * BookingsScreen.kt
 *
 * Customer screen displaying current and past appointments with filtering, cancellation, and reschedule controls.
 */
package com.maryhairsalon.ui.customer.bookings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.maryhairsalon.R
import com.maryhairsalon.data.models.Booking
import com.maryhairsalon.ui.components.SalonBrandRow
import com.maryhairsalon.ui.components.SalonHeroHeader
import com.maryhairsalon.ui.components.rememberTopScreenGradient

@Composable
fun BookingsScreen(
    onLogout: () -> Unit,
    onReschedule: (Booking) -> Unit,
    onMenuClick: () -> Unit,
    viewModel: BookingsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var confirmCancelBookingId by remember { mutableStateOf<String?>(null) }
    val colors = MaterialTheme.colorScheme
    val gradient = rememberTopScreenGradient()

    Scaffold(
        containerColor = colors.background,
        contentWindowInsets = WindowInsets.systemBars.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal)
    ) { padding ->
        Box(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .background(gradient)
        ) {
            LazyVerticalGrid(
                modifier = Modifier.statusBarsPadding(),
                columns = GridCells.Adaptive(minSize = 340.dp),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    SalonBrandRow(onMenuClick = onMenuClick, modifier = Modifier.padding(bottom = 12.dp))
                }
                item(span = { GridItemSpan(maxLineSpan) }) {
                    SalonHeroHeader(
                        title = stringResource(R.string.bookings_title),
                        eyebrow = stringResource(R.string.bookings_account_label),
                        subtitle = stringResource(R.string.bookings_subtitle),
                        searchContent = null
                    )
                }

                if (uiState.isOffline) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        OfflineBanner()
                    }
                }

                item(span = { GridItemSpan(maxLineSpan) }) {
                    FilterSection(
                        activeFilter = uiState.activeFilter,
                        counts = uiState.counts,
                        onFilterSelected = { viewModel.setFilter(it) }
                    )
                }

                when {
                    uiState.isLoading -> item(span = { GridItemSpan(maxLineSpan) }) {
                        Box(Modifier.fillMaxWidth().padding(vertical = 48.dp), Alignment.Center) {
                            CircularProgressIndicator(color = colors.primary)
                        }
                    }
                    uiState.errorMessage != null -> item(span = { GridItemSpan(maxLineSpan) }) {
                        Box(Modifier.fillMaxWidth().padding(vertical = 48.dp), Alignment.Center) {
                            Text(uiState.errorMessage!!, color = colors.onBackground, textAlign = TextAlign.Center)
                        }
                    }
                    uiState.bookings.isEmpty() -> item(span = { GridItemSpan(maxLineSpan) }) {
                        Box(Modifier.fillMaxWidth().padding(vertical = 48.dp), Alignment.Center) {
                            Text(
                                stringResource(R.string.bookings_empty),
                                color = colors.tertiary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    else -> items(uiState.bookings, key = { it.id }) { booking ->
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
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.surfaceVariant)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.WifiOff, contentDescription = null, tint = colors.primary, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Text(
            stringResource(R.string.bookings_offline_banner),
            style = MaterialTheme.typography.labelSmall,
            color = colors.onSurfaceVariant
        )
    }
}

@Composable
private fun FilterSection(
    activeFilter: String,
    counts: Map<String, Int>,
    onFilterSelected: (String) -> Unit
) {
    val statuses = listOf("all", "pending", "upcoming", "completed", "cancelled")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        statuses.forEach { status ->
            FilterPill(
                label = filterLabel(status),
                count = counts[status] ?: 0,
                selected = activeFilter == status,
                onClick = { onFilterSelected(status) }
            )
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
    val colors = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = Color.Transparent,
        border = BorderStroke(
            width = 1.dp,
            color = if (selected) colors.primary else colors.onBackground.copy(alpha = 0.1f)
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
                color = if (selected) colors.onBackground else colors.tertiary
            )
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(if (selected) colors.primary else colors.onBackground.copy(alpha = 0.1f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = count.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (selected) colors.background else colors.onBackground,
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
    val colors = MaterialTheme.colorScheme
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface)
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
                    color = colors.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))

                val badgeColor = when {
                    isPendingSync -> colors.onSurface.copy(alpha = 0.1f)
                    booking.status.lowercase() == "cancelled" -> colors.errorContainer
                    booking.status.lowercase() == "completed" -> Color(0xFF1F3E2E)
                    else -> colors.onSurface.copy(alpha = 0.1f)
                }
                val badgeTextColor = when {
                    isPendingSync -> colors.primary
                    booking.status.lowercase() == "cancelled" -> colors.error
                    booking.status.lowercase() == "completed" -> Color(0xFF10B981)
                    else -> colors.tertiary
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
                Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = colors.primary, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    booking.dateFormatted.ifBlank { booking.date },
                    color = colors.tertiary,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.width(16.dp))
                Icon(Icons.Default.AccessTime, contentDescription = null, tint = colors.primary, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text(booking.time, color = colors.tertiary, style = MaterialTheme.typography.bodyMedium)
            }

            Spacer(Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.LocationOn, contentDescription = null, tint = colors.primary, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    "${booking.type}${if (booking.type == "Out-call" && booking.location.isNotBlank()) " — ${booking.location}" else ""}",
                    color = colors.tertiary,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(Modifier.height(20.dp))
            HorizontalDivider(color = colors.onSurface.copy(alpha = 0.05f))
            Spacer(Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "R${booking.price}",
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.onSurface,
                    fontWeight = FontWeight.Bold
                )

                when {
                    isPendingSync -> Text(
                        stringResource(R.string.status_pending_sync),
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.tertiary
                    )
                    booking.status.equals("pending", ignoreCase = true) ||
                            booking.status.equals("upcoming", ignoreCase = true) -> {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = onRescheduleClick) {
                                Text(stringResource(R.string.action_reschedule), color = colors.primary)
                            }
                            TextButton(onClick = onCancelClick) {
                                Text(stringResource(R.string.action_cancel), color = colors.error)
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
