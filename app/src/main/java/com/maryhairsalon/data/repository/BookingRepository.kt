package com.maryhairsalon.data.repository

import android.util.Log
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.maryhairsalon.data.models.Booking
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/** Thrown when a transaction finds the requested employee/date/time slot already taken. */
class SlotUnavailableException(message: String) : Exception(message)

/**
 * Handles all Firestore operations related to salon bookings.
 * Uses transactions to ensure that two customers cannot book the same stylist at the same time.
 */
class BookingRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    // Unique ID for a booking slot to prevent double-booking at the database level
    private fun slotId(employeeId: String, date: String, time: String) = "${employeeId}_${date}_${time}"

    /**
     * Real-time stream of bookings for a specific customer.
     * Sorts by status (pending first) and then by date descending.
     */
    fun observeBookings(uid: String): Flow<Result<List<Booking>>> = callbackFlow {
        Log.d("BookingRepo", "Starting listener for uid: $uid")
        val statusOrder = mapOf("pending" to 0, "upcoming" to 1, "completed" to 2, "cancelled" to 3)

        val registration = db.collection("bookings")
            .whereEqualTo("uid", uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("BookingRepo", "Snapshot error: ${error.message}", error)
                    trySend(Result.failure(error))
                    return@addSnapshotListener
                }
                val bookings = snapshot?.toObjects(Booking::class.java)
                    ?.sortedWith(compareBy<Booking> { statusOrder[it.status] ?: 3 }
                        .thenByDescending { it.date }
                    ) ?: emptyList()
                trySend(Result.success(bookings))
            }

        awaitClose { registration.remove() }
    }

    /** Uses the stylist's stable user ID rather than their display name, so this keeps working even if they rename themselves in Settings. */
    /** Uses the stylist's display name to match the security rules, which check `employee == myName()`. Safe because Settings locks staff names — see EmployeeScheduleViewModel. */
    fun observeBookingsForEmployee(employeeName: String): Flow<Result<List<Booking>>> = callbackFlow {
        val statusOrder = mapOf("pending" to 0, "upcoming" to 1, "completed" to 2, "cancelled" to 3)

        val registration = db.collection("bookings")
            .whereEqualTo("employee", employeeName)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.failure(error))
                    return@addSnapshotListener
                }
                val bookings = snapshot?.toObjects(Booking::class.java)
                    ?.sortedWith(compareBy<Booking> { statusOrder[it.status] ?: 3 }.thenBy { it.date })
                    ?: emptyList()
                trySend(Result.success(bookings))
            }

        awaitClose { registration.remove() }
    }

    suspend fun getBooking(id: String): Booking? {
        val snapshot = db.collection("bookings").document(id).get().await()
        return snapshot.toObject(Booking::class.java)
    }

    /**
     * Atomically creates a new booking.
     * 1. Checks if the specific stylist/date/time slot is already taken in 'bookingSlots' collection.
     * 2. If free, creates the booking document and the slot document in a single transaction.
     * Throws [SlotUnavailableException] if the slot is already occupied.
     */
    suspend fun addBooking(uid: String, employeeId: String, data: Map<String, Any?>): String {
        val date = data["date"] as? String ?: ""
        val time = data["time"] as? String ?: ""
        val bookingRef = db.collection("bookings").document()
        val slotRef = db.collection("bookingSlots").document(slotId(employeeId, date, time))

        db.runTransaction { txn ->
            val slotSnap = txn.get(slotRef)
            if (slotSnap.exists()) {
                throw SlotUnavailableException("That stylist is already booked for this date and time.")
            }
            val payload = data + mapOf(
                "uid" to uid,
                "employeeId" to employeeId,
                "status" to "pending",
                "employeeNotified" to false,
                "customerNotified" to true,
                "createdAt" to FieldValue.serverTimestamp()
            )
            txn.set(bookingRef, payload)
            txn.set(slotRef, mapOf(
                "bookingId" to bookingRef.id,
                "employeeId" to employeeId,
                "date" to date,
                "time" to time,
                "createdAt" to FieldValue.serverTimestamp()
            ))
            null
        }.await()

        return bookingRef.id
    }

    /**
     * Updates an existing booking (e.g., rescheduling).
     * If the date, time, or stylist has changed, it releases the old slot and claims the new one.
     */
    suspend fun updateBooking(bookingId: String, employeeId: String, data: Map<String, Any?>) {
        val newDate = data["date"] as? String ?: ""
        val newTime = data["time"] as? String ?: ""
        val bookingRef = db.collection("bookings").document(bookingId)
        val newSlotRef = db.collection("bookingSlots").document(slotId(employeeId, newDate, newTime))

        db.runTransaction { txn ->
            val current = txn.get(bookingRef)
            val oldEmployeeId = current.getString("employeeId") ?: ""
            val oldDate = current.getString("date") ?: ""
            val oldTime = current.getString("time") ?: ""
            val slotChanged = oldEmployeeId != employeeId || oldDate != newDate || oldTime != newTime

            if (slotChanged) {
                val newSlotSnap = txn.get(newSlotRef)
                if (newSlotSnap.exists()) {
                    throw SlotUnavailableException("That stylist is already booked for this date and time.")
                }
                if (oldEmployeeId.isNotBlank() && oldDate.isNotBlank() && oldTime.isNotBlank()) {
                    txn.delete(db.collection("bookingSlots").document(slotId(oldEmployeeId, oldDate, oldTime)))
                }
                txn.set(newSlotRef, mapOf(
                    "bookingId" to bookingId,
                    "employeeId" to employeeId,
                    "date" to newDate,
                    "time" to newTime,
                    "createdAt" to FieldValue.serverTimestamp()
                ))
            }

            val payload = data + mapOf("employeeId" to employeeId, "updatedAt" to FieldValue.serverTimestamp())
            txn.update(bookingRef, payload)
            null
        }.await()
    }

    /** Releases the booking's slot (if any) and marks it cancelled — used by both customer cancel and employee decline. */
    private suspend fun releaseSlotAndSetStatus(bookingId: String, status: String) {
        val bookingRef = db.collection("bookings").document(bookingId)
        db.runTransaction { txn ->
            val current = txn.get(bookingRef)
            val employeeId = current.getString("employeeId") ?: ""
            val date = current.getString("date") ?: ""
            val time = current.getString("time") ?: ""
            if (employeeId.isNotBlank() && date.isNotBlank() && time.isNotBlank()) {
                txn.delete(db.collection("bookingSlots").document(slotId(employeeId, date, time)))
            }
            txn.update(bookingRef, "status", status, "updatedAt", FieldValue.serverTimestamp())
            null
        }.await()
    }

    suspend fun cancelBooking(bookingId: String) = releaseSlotAndSetStatus(bookingId, "cancelled")

    suspend fun markCompleted(bookingId: String) {
        db.collection("bookings").document(bookingId)
            .update("status", "completed", "updatedAt", FieldValue.serverTimestamp())
            .await()
    }

    suspend fun acceptBooking(bookingId: String) {
        db.collection("bookings").document(bookingId)
            .update("status", "upcoming", "customerNotified", false, "updatedAt", FieldValue.serverTimestamp())
            .await()
    }

    /** Declining also releases the slot, same as a customer cancellation. */
    suspend fun declineBooking(bookingId: String) {
        val bookingRef = db.collection("bookings").document(bookingId)
        db.runTransaction { txn ->
            val current = txn.get(bookingRef)
            val employeeId = current.getString("employeeId") ?: ""
            val date = current.getString("date") ?: ""
            val time = current.getString("time") ?: ""
            if (employeeId.isNotBlank() && date.isNotBlank() && time.isNotBlank()) {
                txn.delete(db.collection("bookingSlots").document(slotId(employeeId, date, time)))
            }
            txn.update(bookingRef, "status", "cancelled", "customerNotified", false, "updatedAt", FieldValue.serverTimestamp())
            null
        }.await()
    }
}