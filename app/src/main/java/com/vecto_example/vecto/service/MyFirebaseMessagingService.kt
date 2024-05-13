package com.vecto_example.vecto.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.vecto_example.vecto.ui.main.MainActivity
import com.vecto_example.vecto.R

class MyFirebaseMessagingService:  FirebaseMessagingService(){
    private val CHANNEL_ID = "소셜 알림"
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

            if(feedId != null){
                showNotification("댓글 알림", body, feedId.toInt())
            }
            else{
                showNotification("팔로우 알림", body, -1)
            }

            LocalBroadcastManager.getInstance(this).sendBroadcast(Intent("NEW_NOTIFICATION"))

        } else {
            // 알림 제목이나 내용이 없을 경우, 로그 출력
            Log.d("MyFirebaseMessagingService", "Empty notification received")
        }
    }

    private fun showNotification(title: String?, body: String?, feedId: Int) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("feedId", feedId)
        }

        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        /*알림 구성*/
        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(body)
            .setSmallIcon(R.drawable.notification_icon)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channel = NotificationChannel(CHANNEL_ID, "소셜 알림", NotificationManager.IMPORTANCE_HIGH)
        notificationManager.createNotificationChannel(channel)

        notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
    }
}