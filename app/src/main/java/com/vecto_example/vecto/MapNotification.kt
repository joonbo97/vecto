package com.vecto_example.vecto

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat

object MapNotification {
    const val CHANNEL_ID = "foreground_service_channel"
    const val CHANNEL_ID_VISIT = "foreground_service_visit_channel"

    fun createNotification(context: Context): Notification
    {
        val notificationIntent = Intent(context, MainActivity::class.java)
        notificationIntent.action = Actions.MAIN
        notificationIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK

        val pendingIntent = PendingIntent.getActivity(context, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE or FLAG_UPDATE_CURRENT)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("위치정보를 수집중입니다.")
            .setContentText("백그라운드에서 작업을 수행중입니다.")
            //.setContentInfo("Info 입니다")
            .setSmallIcon(R.drawable.notification_icon)
            .setOngoing(true)//사라지지 않는 알림으로 만들기
            .setContentIntent(pendingIntent)
            .build()

        val serviceChannel = NotificationChannel(CHANNEL_ID, "백그라운드 경로 알림", NotificationManager.IMPORTANCE_DEFAULT)
        serviceChannel.setShowBadge(false)

        val manager = context.getSystemService(NotificationManager::class.java)
        manager?.createNotificationChannel(serviceChannel)

        return notification
    }

    fun  createVisitNotification(context: Context): Notification
    {
        val notificationIntent = Intent(context, MainActivity::class.java)

        val pendingIntent = PendingIntent.getActivity(context, 1, notificationIntent, PendingIntent.FLAG_IMMUTABLE or FLAG_UPDATE_CURRENT)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_VISIT)
            .setContentTitle("방문이 확인되었습니다.")
            .setContentText("추후 방문지 설정을 완료해주세요.")
            .setSmallIcon(R.drawable.notification_icon)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)  // 알림 클릭 시 알림을 자동으로 삭제합니다.
            .build()

        val visitChannel = NotificationChannel(CHANNEL_ID_VISIT, "방문 알림 채널", NotificationManager.IMPORTANCE_DEFAULT)

        val manager = context.getSystemService(NotificationManager::class.java)
        manager?.createNotificationChannel(visitChannel)

        return notification
    }

}