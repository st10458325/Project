/**
 * MarySalonApplication.kt
 *
 * Main application class for the Mary Hair Salon app.
 * Responsible for app-level initialization on startup, including configuring
 * Firebase Firestore settings with unlimited local persistent caching to support offline access.
 */
package com.maryhairsalon

import android.app.Application
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.PersistentCacheSettings

/**
 * Custom Application subclass managing global app-level configurations.
 */
class MarySalonApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Firestore already enables offline persistence by default on Android,
        // but we set it explicitly (and give it an unlimited cache) so it's an
        // intentional part of the app rather than an accident of defaults —
        // this is what keeps Services/About/Bookings readable when offline.
        FirebaseFirestore.getInstance().firestoreSettings = FirebaseFirestoreSettings.Builder()
            .setLocalCacheSettings(
                PersistentCacheSettings.newBuilder()
                    .setSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
                    .build()
            )
            .build()
    }
}
