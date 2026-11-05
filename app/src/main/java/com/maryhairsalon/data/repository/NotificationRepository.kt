/**
 * NotificationRepository.kt
 *
 * Repository for managing Firebase Cloud Messaging (FCM) push tokens in Firestore.
 * Associates device tokens with authenticated user UIDs for notification delivery.
 */
package com.maryhairsalon.data.repository

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await

class NotificationRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    suspend fun registerCurrentDevice(uid: String) {
        val token = FirebaseMessaging.getInstance().token.await()
        saveToken(uid, token)
    }

    suspend fun saveToken(uid: String, token: String) {
        db.collection("fcmTokens").document(uid)
            .set(mapOf("token" to token, "updatedAt" to FieldValue.serverTimestamp()))
            .await()
    }
}
