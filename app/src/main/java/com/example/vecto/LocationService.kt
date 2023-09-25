package com.example.vecto


import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.vecto.Data.LocationData
import com.example.vecto.Data.LocationDatabase
import com.example.vecto.Data.VisitDatabase
import com.google.android.gms.location.*
import com.naver.maps.geometry.LatLng
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class LocationService : Service() {
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationDatabase: LocationDatabase
    private lateinit var visitDatabase: VisitDatabase

    private var lastUpdateTime: LocalDateTime? = LocalDateTime.now()
    private var lastUpdateLocation: LatLng = LatLng(0.0, 0.0)//center Lat, Lng
    var cnt: Int = 1

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            for (location in locationResult.locations) {

                //accuracy 70M 이내인 정보만 수집할 것임.
                if(location.accuracy <= 70) {

                    //현재 시간
                    val currentDateTime = LocalDateTime.now()
                    /* val currentSecond =
                         currentDateTime.format(DateTimeFormatter.ofPattern("ss")).toInt()*/

                    //CHECK-DISTANCE 내에 위치
                    if(checkDistance(lastUpdateLocation, LatLng(location.latitude, location.longitude)))
                    {
                        //5분이 안되었으면
                        if(Duration.between(lastUpdateTime, currentDateTime).toMinutes() <= 5) {
                            //중심 좌표 조정
                            cnt++
                            lastUpdateLocation = LatLng(((lastUpdateLocation.latitude * (cnt-1) + location.latitude)/cnt), ((lastUpdateLocation.longitude * (cnt-1) + location.longitude)/cnt))

                            //위치 데이터 추가
                            val locationData = LocationData(currentDateTime.toString(), location.latitude, location.longitude)
                            Log.d("LocationService", "Save Done = DateTime : $currentDateTime Lat: ${location.latitude}, Lng: ${location.longitude}\n " +
                                    "accurancy : ${location.accuracy}")
                            locationDatabase.addLocationData(locationData)
                        }
                        else//5분이 경과했으면 (방문)
                        {
                            if(cnt > 1)//이번이 처음 방문으로 판단하는 시점이라면
                            {
                                //평균 값과 처음 업데이트 시간을 visit db에 저장함.
                                Log.d("LocationService", "방문으로 판단 되었습니다. DateTime : $lastUpdateTime Lat: ${lastUpdateLocation.latitude}, Lng: ${lastUpdateLocation.longitude}\n " +
                                        "accurancy : ${location.accuracy}")
                                val locationData = LocationData(lastUpdateTime.toString(), lastUpdateLocation.latitude, lastUpdateLocation.longitude)
                                visitDatabase.addVisitData(locationData)

                                locationDatabase.deleteLocationDataAfter(lastUpdateTime!!)
                                locationDatabase.updateLocationData(lastUpdateTime.toString(), lastUpdateLocation.latitude, lastUpdateLocation.longitude)

                                cnt = 1
                            }
                            else//계속 방문중인 상태라면
                            {

                            }

                        }
                    }
                    else//CHECK-DISTANCE 외부에 위치
                    {
                        //중심 좌표와 갱신 시간을 업데이트함.
                        lastUpdateLocation = LatLng(location.latitude, location.longitude)
                        lastUpdateTime =  LocalDateTime.now()
                        cnt = 1

                        //위치 데이터 추가
                        val locationData = LocationData(currentDateTime.toString(), location.latitude, location.longitude)
                        Log.d("LocationService", "Save Done = DateTime : $currentDateTime Lat: ${location.latitude}, Lng: ${location.longitude}\n " +
                                "accurancy : ${location.accuracy}")
                        locationDatabase.addLocationData(locationData)
                    }

                }
                else//under 70M accuracy is ignored
                {
                    Log.d("LocationService", "Ignoring ${location.accuracy}")
                }
            }
        }
    }

    //CHECK-DISTANCE 거리 만큼 떨어진 점까지 방문으로 간주함. 방문이면 true, 아니면 else
    private fun checkDistance(centerLatLng: LatLng, currendLatLng: LatLng): Boolean{
        val centerLocation = Location("centerLatLng")
        centerLocation.latitude = centerLatLng.latitude
        centerLocation.longitude = centerLatLng.longitude

        val currentLocation = Location("currendLatLng")
        currentLocation.latitude = currendLatLng.latitude
        currentLocation.longitude = currendLatLng.longitude

        return centerLocation.distanceTo(currentLocation) <= CHECKDISTANCE
    }

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        locationDatabase = LocationDatabase(this)
        visitDatabase = VisitDatabase(this)
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

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
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
        const val CHECKDISTANCE = 50 //몇 M떨어진 점까지 방문으로 간주할 것인지
    }
}
