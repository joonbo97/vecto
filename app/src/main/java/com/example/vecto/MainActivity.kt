package com.example.vecto

import android.content.pm.PackageManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.vecto.databinding.ActivityMainBinding
import android.Manifest
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.example.vecto.Data.Auth.pathPoints
import com.example.vecto.Data.LocationDatabase
import com.example.vecto.Data.VisitDatabase
import com.naver.maps.geometry.LatLng
import com.naver.maps.map.*
import com.naver.maps.map.overlay.CircleOverlay
import com.naver.maps.map.overlay.Marker
import com.naver.maps.map.overlay.PathOverlay
import com.naver.maps.map.overlay.PolylineOverlay
import com.naver.maps.map.util.FusedLocationSource

class MainActivity : AppCompatActivity(), OnMapReadyCallback {
    private lateinit var binding: ActivityMainBinding
    private lateinit var mapView: MapFragment
    private lateinit var locationSource: FusedLocationSource // 위치를 반환하는 구현체
    private lateinit var naverMap: NaverMap

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1000
        private const val BACKGROUND_PERMISSION_REQUEST_CODE = 2000
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val permission: Array<String> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.POST_NOTIFICATIONS)
        } else {
            TODO("VERSION.SDK_INT < Q")
        }
        //필요한 permission array

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
            == PackageManager.PERMISSION_DENIED
            ||ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_DENIED
            ||ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            == PackageManager.PERMISSION_DENIED){ //대략적 위치 권한, 정확한 위치 권한, 알림에 대한 권한에 대한 여부 확인
            ActivityCompat.requestPermissions(this, permission, LOCATION_PERMISSION_REQUEST_CODE) //권한이 하나 라도 없으면 권한에 대한 허가 요청


            /*백그라운드 위치 정보 권한은 따로 요청해야 하기에 다이얼로그를 통해 사용자에게 접근하도록 유도*/
            if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_DENIED)
            {
                ActivityCompat.requestPermissions(this,
                    arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION), BACKGROUND_PERMISSION_REQUEST_CODE)

                Log.d("Check", "Background Check")
            }//백그라운드 권한 REJECT
            else
            {
                Log.d("Check", "Background Check 2")
            }

        }
        else
        {
            initMap()
        }

        binding.StartServiceButton.setOnClickListener {
            val serviceIntent = Intent(this, LocationService::class.java)
            serviceIntent.action = Actions.START_FOREGROUND
            startService(serviceIntent)
            Toast.makeText(this, "백그라운드 서비스를 시작합니다", Toast.LENGTH_SHORT).show()
        }

        binding.StopServiceButton.setOnClickListener{
            val serviceIntent = Intent(this, LocationService::class.java)
            serviceIntent.action = Actions.STOP_FOREGROUND
            startService(serviceIntent)
            Toast.makeText(this, "백그라운드 서비스를 종료합니다.", Toast.LENGTH_SHORT).show()
        }

        binding.SearchButton.setOnClickListener {
            Toast.makeText(this, "공유하고싶은 방문날짜를 선택해주세요.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun initMap(){
        mapView = supportFragmentManager.findFragmentById(R.id.naver_map) as MapFragment?
            ?: MapFragment.newInstance().also {
                supportFragmentManager.beginTransaction().add(R.id.naver_map, it).commit()
            }
        mapView.getMapAsync(this)

        locationSource = FusedLocationSource(this, LOCATION_PERMISSION_REQUEST_CODE)
    }

    override fun onMapReady(naverMap: NaverMap) {
        this.naverMap = naverMap
        naverMap.locationSource = locationSource
        naverMap.locationTrackingMode = LocationTrackingMode.Follow
        naverMap.moveCamera(CameraUpdate.zoomTo(18.0))


        // SQLite에서 모든 위치 데이터 가져오기
        val locationDataList = LocationDatabase(this).getAllLocationData()

        val pathOverlay = PathOverlay()

        pathPoints.clear()
        locationDataList.forEach { locationData ->
            pathPoints.add(LatLng(locationData.lat, locationData.lng))
        }

        if(pathPoints.size > 1) {
            pathOverlay.coords = pathPoints
            pathOverlay.width = 20
            pathOverlay.color = Color.YELLOW
            pathOverlay.map = naverMap
        }

        val visitDataList = VisitDatabase(this).getAllVisitData()

        visitDataList.forEach { visitData ->
            // 원 그리기
            val circleOverlay = CircleOverlay()
            circleOverlay.center = LatLng(visitData.lat, visitData.lng)
            circleOverlay.radius = 50.0 // 반지름을 50m로 설정
            circleOverlay.color = Color.argb(20, 255, 0, 0) // 원의 색상 설정
            circleOverlay.map = naverMap

            val marker = Marker()
            marker.position = LatLng(visitData.lat, visitData.lng)
            marker.map = naverMap
        }
    }

}