package com.soundwire

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {
    override fun onMessageReceived(message: RemoteMessage) {
        val title = message.notification?.title ?: "SoundWire"
        val body = message.notification?.body ?: "You have a message"
        val nm = getSystemService(NotificationManager::class.java)
        val channelId = "soundwire_fcm"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            nm.createNotificationChannel(NotificationChannel(channelId, "SoundWire", NotificationManager.IMPORTANCE_DEFAULT))
        }
        val intent = Intent(this, MainActivity::class.java)
        val pi = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        val notif = NotificationCompat.Builder(this, channelId)
            .setContentTitle(title)
            .setContentText(body)
            .setContentIntent(pi)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .build()
        nm.notify(1001, notif)
    }
    override fun onNewToken(token: String) {
        // send token to server or store
    }
}
