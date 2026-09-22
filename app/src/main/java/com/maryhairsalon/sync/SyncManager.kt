package com.maryhairsalon.sync

import android.util.Log
import com.maryhairsalon.data.local.PendingActionsStore
import com.maryhairsalon.data.repository.BookingRepository

/** Replays anything queued in [PendingActionsStore] against the real backend. */
class SyncManager(
    private val store: PendingActionsStore,
    private val bookingRepository: BookingRepository = BookingRepository()
) {
    suspend fun syncPendingCancellations() {
        val ids = store.getPendingCancelIds()
        for (id in ids) {
            try {
                bookingRepository.cancelBooking(id)
                store.removeCancel(id)
            } catch (e: Exception) {
                Log.e("SyncManager", "Failed to sync cancellation for booking $id", e)
                // Leave it queued — we'll retry on the next reconnect or WorkManager run.
            }
        }
    }
}