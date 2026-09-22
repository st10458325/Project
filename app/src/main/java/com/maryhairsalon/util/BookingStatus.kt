package com.maryhairsalon.util

/** Human-readable label for a booking's raw status string — shared across customer, employee, and manager screens. */
fun bookingStatusLabel(status: String): String = when (status) {
    "pending" -> "Pending"
    "upcoming" -> "Upcoming"
    "completed" -> "Completed"
    "cancelled" -> "Cancelled"
    else -> status.replaceFirstChar { it.uppercase() }
}
