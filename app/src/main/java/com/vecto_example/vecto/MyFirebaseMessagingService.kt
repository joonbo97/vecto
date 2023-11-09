package com.vecto_example.vecto

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.util.Log
import androidx.core.app.NotificationCompat
import com.vecto_example.vecto.data.NotificationData
import com.vecto_example.vecto.data.NotificationDatabase
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import java.time.LocalDateTime

class MyFirebaseMessagingService:  FirebaseMessagingService(){
    private val CHANNEL_ID = "vecto"
    private val SHARED_PREF_NAME = "fcm_pref"
    private val TOKEN_KEY = "fcm_token"

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        saveTokenToSharedPref(token)
    }

    private fun saveTokenToSharedPref(token: String) {
        val sharedPreferences = getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE)
        sharedPreferences.edit().putString(TOKEN_KEY, token).apply()
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d("MyFirebaseMessagingService", "Message received: " + remoteMessage)
        Log.d("MyFirebaseMessagingService", "Message received: " + remoteMessage.notification)
        Log.d("MyFirebaseMessagingService", "Message received: " + remoteMessage.data)
        val data = remoteMessage.data
        val title = data["title"]
        val body = data["body"]
        val feedId = data["feedId"]


        if (title != null && body != null) {
            // 알림 제목과 내용이 있을 경우, 알림 표시
            val currentDateTime = LocalDateTime.now().withNano(0).toString()

            val notificationDB = NotificationDatabase(this)
            if(feedId != null)
                notificationDB.addNotificationData(NotificationData(currentDateTime, feedId.toInt(), body, 0))
            else
                notificationDB.addNotificationData(NotificationData(currentDateTime, -1, body, 0))

            showNotification(title, body)
        } else {
            // 알림 제목이나 내용이 없을 경우, 로그 출력
            Log.d("MyFirebaseMessagingService", "Empty notification received")
        }
    }

    private fun showNotification(title: String?, body: String?) {
        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(body)
            .setSmallIcon(R.drawable.notification_icon)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channel = NotificationChannel(
            CHANNEL_ID,
            "FCM Notifications",
            NotificationManager.IMPORTANCE_HIGH
        )
        notificationManager.createNotificationChannel(channel)

        notificationManager.notify(0, notificationBuilder.build())
    }
}