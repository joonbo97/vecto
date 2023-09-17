package com.example.vecto


import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.IBinder
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.vecto.Data.LocationData
import com.example.vecto.Data.LocationDatabase
import com.google.android.gms.location.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class LocationService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationDatabase: LocationDatabase

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult?) {
            locationResult ?: return
            for (location in locationResult.locations) {

                // 현재 날짜와 시간
                val currentDateTime = LocalDateTime.now()
                val currentDate = currentDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                val currentTime = currentDateTime.format(DateTimeFormatter.ofPattern("HH:mm:ss"))

                // 위치 데이터 추가
                val locationData = LocationData(
                    date = currentDate,
                    time = currentTime,
                    lat = location.latitude,
                    lng = location.longitude
                )
                Log.d("LocationService", "Save Done = Date : $currentDate Time : $currentTime Lat: ${location.latitude}, Long: ${location.longitude}")
                locationDatabase.addLocationData(locationData)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        locationDatabase = LocationDatabase(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            Actions.START_FOREGROUND -> {
                startForegroundService()
                requestLocationUpdates()
                Log.d("NotificationDebug", "Notification ID: $NOTIFICATION_ID")

            }
            Actions.STOP_FOREGROUND -> {
                stopForegroundService()
                stopLocationUpdates()
            }
        }
        return START_STICKY
    }

    private fun startForegroundService() {
        val channelId = "location_service_channel"
        val channel = NotificationChannel(
            channelId,
            "Location Service Channel",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)

        val notification = MapNotification.createNotification(this)

        startForeground(NOTIFICATION_ID, notification)
    }

    private fun stopForegroundService() {
        stopSelf()
    }

    private fun requestLocationUpdates() {
        val locationRequest = LocationRequest.create().apply {
            interval = 10000
            fastestInterval = 5000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                null
            )
        }
    }

    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        stopLocationUpdates()
    }

    companion object {
        const val NOTIFICATION_ID = 12345
    }
}
