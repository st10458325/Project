package com.maryhairsalon.sync

import android.content.Context
import androidx.work.*
import com.maryhairsalon.data.local.PendingActionsStore

/**
 * Guarantees queued cancellations still sync even if the app was closed
 * while offline. WorkManager only runs this once the OS confirms a network
 * connection exists, and retries with backoff if a sync attempt still fails.
 */
class BookingSyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val store = PendingActionsStore(applicationContext)
        val syncManager = SyncManager(store)
        return try {
            syncManager.syncPendingCancellations()
            if (store.getPendingCancelIds().isEmpty()) Result.success() else Result.retry()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}

object BookingSyncScheduler {
    private const val WORK_NAME = "booking_pending_cancel_sync"

    fun scheduleSync(context: Context) {
        val request = OneTimeWorkRequestBuilder<BookingSyncWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        WorkManager.getInstance(context)
            .enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.REPLACE, request)
    }
}