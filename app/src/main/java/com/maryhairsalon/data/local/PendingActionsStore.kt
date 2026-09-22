package com.maryhairsalon.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.pendingActionsDataStore by preferencesDataStore(name = "pending_actions")

/**
 * Local queue of booking cancellations made while offline. Cancelling a
 * booking normally goes through a Firestore transaction (to release the
 * slot atomically), and transactions require a live connection — they
 * don't queue themselves the way plain writes do. This store is what lets
 * "Cancel" work offline at all: we remember the intent here, then replay
 * it for real once we're back online.
 */
class PendingActionsStore(private val context: Context) {
    private val pendingCancelIdsKey = stringSetPreferencesKey("pending_cancel_booking_ids")

    val pendingCancelBookingIds: Flow<Set<String>> =
        context.pendingActionsDataStore.data.map { it[pendingCancelIdsKey] ?: emptySet() }

    suspend fun getPendingCancelIds(): Set<String> = pendingCancelBookingIds.first()

    suspend fun enqueueCancel(bookingId: String) {
        context.pendingActionsDataStore.edit { prefs ->
            val current = prefs[pendingCancelIdsKey] ?: emptySet()
            prefs[pendingCancelIdsKey] = current + bookingId
        }
    }

    suspend fun removeCancel(bookingId: String) {
        context.pendingActionsDataStore.edit { prefs ->
            val current = prefs[pendingCancelIdsKey] ?: emptySet()
            prefs[pendingCancelIdsKey] = current - bookingId
        }
    }
}