package com.example.vecto


import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder

class LocationService : Service() {
    private val binder = LocationServiceBinder()
    private var callback: LocationServiceCallback? = null

    inner class LocationServiceBinder : Binder() {
        fun getService(): LocationService {
            return this@LocationService
        }

        fun setCallback(callback: LocationServiceCallback) {
            this@LocationService.callback = callback
        }
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }



    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action){
            Actions.START_FOREGROUND -> {
                startForegroundService()
            }
            Actions.STOP_FOREGROUND -> {
                stopForegroundService()
            }
        }
        return START_STICKY
    }

    private fun stopForegroundService() {
        stopSelf()
    }

    private fun startForegroundService() {
        val notification = MapNotification.createNotification(this)
        startForeground(NOTIFICATION_ID, notification)
    }

    /*override fun onBind(p0: Intent?): IBinder? {
        return null
    }*/

    override fun onCreate() {
        super.onCreate()
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    companion object {
        const val NOTIFICATION_ID = 20
    }
}
