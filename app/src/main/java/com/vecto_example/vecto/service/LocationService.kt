package com.vecto_example.vecto.service


import android.Manifest
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import com.vecto_example.vecto.data.model.LocationData
import com.vecto_example.vecto.data.model.LocationDatabase
import com.vecto_example.vecto.data.model.LogData
import com.vecto_example.vecto.data.model.LogDatabase
import com.vecto_example.vecto.data.model.VisitData
import com.vecto_example.vecto.data.model.VisitDatabase
import com.google.android.gms.location.*
import com.naver.maps.geometry.LatLng
import com.vecto_example.vecto.ui.notification.MapNotification
import com.vecto_example.vecto.utils.ServerResponse
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class LocationService : Service() {
    /*   Foreground Service   */

    /*   위치 관련   */
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    /*   DB 관련   */
    private lateinit var locationDatabase: LocationDatabase
    private lateinit var visitDatabase: VisitDatabase
    private lateinit var logDatabase: LogDatabase

    /*   시간, 위치 관련   */
    private var lastUpdateTime: LocalDateTime? = LocalDateTime.now().withNano(0)
    private var lastUpdateLocation: LatLng = LatLng(0.0, 0.0)//초기 방문 중심 좌표
    val FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
    var cnt: Int = 1

    var visitFlag: Boolean = false

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            Log.d("LocationService", "Location Result")

            for (location in locationResult.locations) {

                //정확도가 CHECK DISTANCE 이내
                if(location.accuracy <= CHECKDISTANCE) {

                    //현재 시간
                    val currentDateTime = LocalDateTime.now().withNano(0)

                    //CHECK-DISTANCE 내에 위치
                    if(checkDistance(lastUpdateLocation, LatLng(location.latitude, location.longitude)))
                    {
                        //5분이 안되었으면
                        if(Duration.between(lastUpdateTime, currentDateTime).toMinutes() <= 5) {
                            //중심 좌표 조정
                            cnt++

                            lastUpdateLocation =
                                LatLng(((lastUpdateLocation.latitude * (cnt - 1) + location.latitude) / cnt),
                                    ((lastUpdateLocation.longitude * (cnt - 1) + location.longitude) / cnt))

                            //위치 데이터 추가
                            locationDatabase.addLocationData(LocationData(currentDateTime.format(FORMAT), location.latitude, location.longitude))
                            saveLog(0)
                        }
                        else//5분이 경과했으면 (방문)
                        {
                            if(!visitFlag)//이번이 처음 방문으로 판단하는 시점이라면
                            {
                                fun saveNewVisit() {
                                    saveLog(1)

                                    //평균 값과 처음 업데이트 시간을 visit db에 저장함.
                                    visitDatabase.addVisitData(VisitData(lastUpdateTime!!.format(FORMAT), lastUpdateTime!!.format(FORMAT), lastUpdateLocation.latitude, lastUpdateLocation.longitude, lastUpdateLocation.latitude, lastUpdateLocation.longitude, 0, "", "",0, ServerResponse.VISIT_TYPE_WALK.code))

                                    locationDatabase.deleteLocationDataAfter(lastUpdateTime!!)  //방문 중 경로 데이터 모두 삭제 후, 방문지에 해당하는 좌표 넣어줌
                                    locationDatabase.addLocationData(LocationData(lastUpdateTime!!.format(FORMAT),lastUpdateLocation.latitude, lastUpdateLocation.longitude))

                                    cnt = 1
                                    visitFlag = true

                                    sendVisitNotification()
                                }

                                if(visitDatabase.isVisitDatabaseEmpty())//경로 측정 이후 처음 방문
                                {
                                    saveNewVisit()
                                }
                                else//다른방문 장소가 있는 경우
                                {
                                    val lastVisitLocation: VisitData = visitDatabase.getLastVisitData() //가장 최근 방문이 종료된 곳
                                    val lastVisitTime = LocalDateTime.parse(lastVisitLocation.endtime, FORMAT) //가장 최근 방문이 종료된 시간

                                    //이전 방문이 종료된 시각과, 현재 방문이 확인된 시각을 비교
                                    //10분이 지났거나, 유효거리 외부인경우, 새로운 방문지로 기록
                                    //lastUpdateTime: 처음 방문으로 찍힌 좌표의 시간
                                    if(Duration.between(lastVisitTime, lastUpdateTime).toMinutes() > 10 || !checkDistance(LatLng(lastUpdateLocation.latitude, lastUpdateLocation.longitude), LatLng(lastVisitLocation.lat, lastVisitLocation.lng)))
                                        saveNewVisit()
                                    else
                                    {
                                        //유효거리 내부에 위치한 방문지이면, 노이즈로 인해 방문이 해제된 것으로 판단하여 기존 visit에 합친다.
                                        locationDatabase.deleteLocationDataAfter(lastVisitTime!!)
                                        lastUpdateLocation = LatLng(lastVisitLocation.lat, lastVisitLocation.lng)

                                        cnt = 1
                                        visitFlag = true
                                        saveLog(2)
                                    }
                                }
                            }
                            else  //계속 방문중인 상태라면
                            {
                                saveLog(3)
                            }
                        }
                    }
                    else//CHECK-DISTANCE 외부에 위치
                    {
                        if(visitFlag)//방문이 깨진 첫 좌표
                        {
                            val lastVisitLocation: VisitData = visitDatabase.getLastVisitData()
                            val lastVisitTime =
                                LocalDateTime.parse(lastVisitLocation.datetime, FORMAT)
                            val minutesPassed = Duration.between(lastVisitTime, currentDateTime).toMinutes().toInt()

                            visitDatabase.updateVisitDataEndTime(
                                lastVisitTime.format(FORMAT),
                                currentDateTime.format(FORMAT),
                                minutesPassed
                            )

                            visitFlag = false
                        }

                        // 중심 좌표와 갱신 시간을 업데이트함.
                        lastUpdateLocation = LatLng(location.latitude, location.longitude)
                        lastUpdateTime = currentDateTime
                        cnt = 1

                        //위치 데이터 추가
                        locationDatabase.addLocationData(LocationData(currentDateTime.format(FORMAT), location.latitude, location.longitude))
                        saveLog(4)
                    }

                }
                else
                {
                    Log.d("LocationService", "Ignoring ${location.accuracy}")
                    saveLog(5)
                }

            }
        }
    }

    //CHECK-DISTANCE 거리 만큼 떨어진 점까지 방문으로 간주함. 방문이면 true, 아니면 else
    private fun checkDistance(centerLatLng: LatLng, currentLatLng: LatLng): Boolean{
        return centerLatLng.distanceTo(currentLatLng) <= CHECKDISTANCE.toDouble()
    }

    override fun onCreate() {
        super.onCreate()

        try {
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        } catch (e: Exception) {
            val datetime = LocalDateTime.now().withNano(0).format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"))
            logDatabase.addLogData(LogData(datetime ,e.message.toString()))
            stopSelf()
        }

        locationDatabase = LocationDatabase(this)
        visitDatabase = VisitDatabase(this)
        logDatabase = LogDatabase(this)

        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            val datetime = LocalDateTime.now().withNano(0).format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"))
            logDatabase.addLogData(LogData(datetime ,throwable.message.toString()))
            defaultHandler?.uncaughtException(thread, throwable)
        }

    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if(intent == null){
            requestLocationUpdates()
        }else {
            when (intent.action) {
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
        }
        return START_STICKY
    }

    private fun startForegroundService() {
        val notification = MapNotification.createNotification(this)
        startForeground(NOTIFICATION_ID, notification)
    }

    private fun sendVisitNotification(){
        val notificationManager = getSystemService(NotificationManager::class.java) as NotificationManager
        val notification = MapNotification.createVisitNotification(this)
        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }

    private fun stopForegroundService() {
        stopSelf()
    }

    private fun requestLocationUpdates() {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000).build()

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

        saveLog(6)
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)

        saveLog(7)
    }

    companion object {
        const val NOTIFICATION_ID = 12345
        const val CHECKDISTANCE = 50 //몇M 떨어진 점까지 방문으로 간주할 것인지
    }
    private fun saveLog(type: Int){
        val datetime = LocalDateTime.now().withNano(0).format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"))

        when(type)
        {
            0 ->{
                //logDatabase.addLogData(LogData(datetime, "CHECK 내부에 위치했으나, 5분이 되지 않았습니다."))
            }
            1 ->{
                logDatabase.addLogData(LogData(datetime, "방문으로 판단되었습니다."))
            }
            2 ->{
                logDatabase.addLogData(LogData(datetime, "직전 방문지와 합병되었습니다."))
            }
            3 ->{
                //logDatabase.addLogData(LogData(datetime, "계속 방문중입니다."))
            }
            4 ->{
                //logDatabase.addLogData(LogData(datetime, "CHECK 외부에 위치하여 이동중으로 판단합니다."))
            }
            5 ->{
                //logDatabase.addLogData(LogData(datetime, "정확도가 떨어져 판단하지 않습니다."))
            }
            6 ->{
                logDatabase.addLogData(LogData(datetime, "서비스가 종료되었습니다."))
            }
            7 ->{
                logDatabase.addLogData(LogData(datetime, "서비스가 강제종료되었습니다."))
            }
        }
    }

}
