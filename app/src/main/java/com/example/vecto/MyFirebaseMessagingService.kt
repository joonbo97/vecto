package com.example.vecto

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

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

        //Message received: {feedId=8, body=정아님께서 회원님의 게시글에 댓글을 달았습니다., title=vecto}

        if (title != null && body != null) {
            // 알림 제목과 내용이 있을 경우, 알림 표시
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
            .setSmallIcon(R.drawable.ic_launcher_foreground)
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