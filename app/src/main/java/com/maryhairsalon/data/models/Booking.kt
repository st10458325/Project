/**
 * Booking.kt
 *
 * Data model representing a salon appointment reservation in Mary Hair Salon.
 * Maps directly to Firestore documents in the "bookings" collection.
 * Holds customer identity, service category, date/time schedule, pricing breakdowns, and notification status flags.
 */
package com.maryhairsalon.data.models

import com.google.firebase.firestore.DocumentId

/**
 * Entity representing an appointment booking.
 */
data class Booking(
    @DocumentId val id: String = "",
    val uid: String = "",
    val name: String = "",
    val surname: String = "",
    val contact: String = "",
    val employee: String = "",
    val employeeId: String = "",
    val category: String = "",
    val categoryLabel: String = "",
    val service: String = "",
    val type: String = "Outcall",
    val location: String = "",
    val date: String = "",
    val dateFormatted: String = "",
    val time: String = "",
    val notes: String = "",
    val cost: Double = 0.0,
    val discount: Double = 0.0,
    val total: Double = 0.0,
    val price: String = "",
    val status: String = "pending",
    val employeeNotified: Boolean = true,
    val customerNotified: Boolean = true
)
