package com.example.vecto

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat

object MapNotification {
    const val CHANNEL_ID = "foreground_service_channel"

    fun createNotification(context: Context) :Notification
    {
        val notificationIntent = Intent(context, MainActivity::class.java)
        notificationIntent.action = Actions.MAIN
        notificationIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK

        val pendingIntent = PendingIntent.getActivity(context, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE or FLAG_UPDATE_CURRENT)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("위치정보를 수집중입니다.")
            .setContentText("백그라운드에서 작업을 수행중입니다.")
            .setContentInfo("Info 입니다")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)//사라지지 않는 알림으로 만들기
            //.addAction()
            .setContentIntent(pendingIntent)
            .build()

        val serviceChannel = NotificationChannel(CHANNEL_ID, "Channel 입니다", NotificationManager.IMPORTANCE_DEFAULT)

        val manager = context.getSystemService(NotificationManager::class.java)
        manager?.createNotificationChannel(serviceChannel)

        return notification
    }
}