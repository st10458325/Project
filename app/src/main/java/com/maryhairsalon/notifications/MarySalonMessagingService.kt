package com.maryhairsalon.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.maryhairsalon.MainActivity
import com.maryhairsalon.R
import com.maryhairsalon.data.repository.NotificationRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Service to handle incoming Firebase Cloud Messaging (FCM) notifications.
 * Automatically registers tokens and displays system notifications for booking updates.
 */
class MarySalonMessagingService : FirebaseMessagingService() {

    private val channelId = "bookings"

    /**
     * Called when a new FCM token is generated (e.g., first app launch or token refresh).
     * The token is linked to the user's UID in Firestore so we can send targeted notifications.
     */
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        CoroutineScope(Dispatchers.IO).launch {
            runCatching { NotificationRepository().saveToken(uid, token) }
        }
    }

    /**
     * Triggered when a message is received while the app is in the foreground or contains a data payload.
     */
    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val title = message.notification?.title ?: message.data["title"] ?: "Mary Hair Salon"
        val body = message.notification?.body ?: message.data["body"] ?: return

        ensureChannel()

        // Create an intent that opens the app when the notification is tapped
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        // Show the notification using a unique ID (current time) to prevent overwriting previous ones
        ContextCompat.getSystemService(this, NotificationManager::class.java)
            ?.notify(System.currentTimeMillis().toInt(), notification)
    }

    /**
     * Creates the required Notification Channel for Android 8.0+.
     */
    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Booking updates", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "New bookings and status changes"
            }
            ContextCompat.getSystemService(this, NotificationManager::class.java)?.createNotificationChannel(channel)
        }
    }
}
