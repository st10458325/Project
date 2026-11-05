/**
 * ManagerRepository.kt
 *
 * Repository for salon management operations in Firestore.
 * Provides all-appointment monitoring, appointment status changes with slot lock updates,
 * stylist re-assignment, and user role management.
 */
package com.maryhairsalon.data.repository

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.maryhairsalon.data.models.Booking
import com.maryhairsalon.data.models.User
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class ManagerRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    /** Every booking, across every customer — unlike BookingRepository, no uid filter. */
    fun observeAllBookings(): Flow<Result<List<Booking>>> = callbackFlow {
        val statusOrder = mapOf("pending" to 0, "upcoming" to 1, "completed" to 2, "cancelled" to 3)

        val registration = db.collection("bookings")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.failure(error))
                    return@addSnapshotListener
                }
                val bookings = snapshot?.toObjects(Booking::class.java)
                    ?.sortedWith(compareBy<Booking> { statusOrder[it.status] ?: 3 }.thenByDescending { it.date })
                    ?: emptyList()
                trySend(Result.success(bookings))
            }

        awaitClose { registration.remove() }
    }

    /**
     * Changing status now respects the slot lock:
     * - moving TO cancelled releases the slot
     * - moving AWAY FROM cancelled re-claims it, failing if someone else took it since
     * - any other transition (pending/upcoming/completed) leaves the slot untouched
     */
    suspend fun updateBookingStatus(bookingId: String, newStatus: String) {
        val bookingRef = db.collection("bookings").document(bookingId)
        db.runTransaction { txn ->
            val current = txn.get(bookingRef)
            val employeeId = current.getString("employeeId") ?: ""
            val date = current.getString("date") ?: ""
            val time = current.getString("time") ?: ""
            val oldStatus = current.getString("status") ?: ""
            val hasSlotInfo = employeeId.isNotBlank() && date.isNotBlank() && time.isNotBlank()
            val slotRef = if (hasSlotInfo) db.collection("bookingSlots").document("${employeeId}_${date}_${time}") else null

            if (slotRef != null && newStatus == "cancelled" && oldStatus != "cancelled") {
                txn.delete(slotRef)
            } else if (slotRef != null && oldStatus == "cancelled" && newStatus != "cancelled") {
                val slotSnap = txn.get(slotRef)
                if (slotSnap.exists() && slotSnap.getString("bookingId") != bookingId) {
                    throw SlotUnavailableException("That time slot has since been booked by someone else.")
                }
                txn.set(slotRef, mapOf(
                    "bookingId" to bookingId, "employeeId" to employeeId,
                    "date" to date, "time" to time, "createdAt" to FieldValue.serverTimestamp()
                ))
            }

            txn.update(bookingRef, "status", newStatus, "customerNotified", false, "updatedAt", FieldValue.serverTimestamp())
            null
        }.await()
    }

    /** Reassigning releases the old employee's slot and claims the same date/time under the new one. */
    suspend fun reassignEmployee(bookingId: String, employeeId: String, employeeName: String) {
        val bookingRef = db.collection("bookings").document(bookingId)
        db.runTransaction { txn ->
            val current = txn.get(bookingRef)
            val oldEmployeeId = current.getString("employeeId") ?: ""
            val date = current.getString("date") ?: ""
            val time = current.getString("time") ?: ""
            val status = current.getString("status") ?: ""

            if (oldEmployeeId != employeeId && status != "cancelled" && date.isNotBlank() && time.isNotBlank()) {
                val newSlotRef = db.collection("bookingSlots").document("${employeeId}_${date}_${time}")
                val newSlotSnap = txn.get(newSlotRef)
                if (newSlotSnap.exists()) {
                    throw SlotUnavailableException("That stylist already has a booking at this date and time.")
                }
                if (oldEmployeeId.isNotBlank()) {
                    txn.delete(db.collection("bookingSlots").document("${oldEmployeeId}_${date}_${time}"))
                }
                txn.set(newSlotRef, mapOf(
                    "bookingId" to bookingId, "employeeId" to employeeId,
                    "date" to date, "time" to time, "createdAt" to FieldValue.serverTimestamp()
                ))
            }

            txn.update(bookingRef, "employee", employeeName, "employeeId", employeeId, "employeeNotified", false, "updatedAt", FieldValue.serverTimestamp())
            null
        }.await()
    }

    /** Every customer and employee account — managers are filtered out client-side too. */
    fun observeStaffAndCustomers(): Flow<Result<List<User>>> = callbackFlow {
        val registration = db.collection("users")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.failure(error))
                    return@addSnapshotListener
                }
                val users = snapshot?.toObjects(User::class.java)
                    ?.filter { it.role != "manager" }
                    ?.sortedBy { it.name }
                    ?: emptyList()
                trySend(Result.success(users))
            }

        awaitClose { registration.remove() }
    }

    suspend fun setUserRole(uid: String, role: String) {
        require(role == "customer" || role == "employee") {
            "Managers can only assign the customer or employee role from the app."
        }
        db.collection("users").document(uid).update("role", role).await()
    }
}
