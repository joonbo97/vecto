package com.example.vecto

import android.Manifest
import android.app.DatePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.vecto.data.Auth
import com.example.vecto.data.LocationData
import com.example.vecto.data.LocationDatabase
import com.example.vecto.data.VisitData
import com.example.vecto.data.VisitDatabase
import com.example.vecto.databinding.ActivityEditLocationBinding
import com.naver.maps.geometry.LatLng
import com.naver.maps.map.CameraUpdate
import com.naver.maps.map.LocationTrackingMode
import com.naver.maps.map.MapFragment
import com.naver.maps.map.NaverMap
import com.naver.maps.map.OnMapReadyCallback
import com.naver.maps.map.overlay.CircleOverlay
import com.naver.maps.map.overlay.Marker
import com.naver.maps.map.overlay.PathOverlay
import com.naver.maps.map.util.FusedLocationSource
import java.util.Calendar

class EditLocationActivity : AppCompatActivity(), OnMapReadyCallback {
    private lateinit var binding: ActivityEditLocationBinding
    private lateinit var mapView: MapFragment
    private lateinit var locationSource: FusedLocationSource // 위치를 반환하는 구현체
    private lateinit var naverMap: NaverMap

    private val visitMarkers = mutableListOf<Marker>()
    lateinit var pathOverlay: PathOverlay
    //private val pathOverlays = mutableListOf<PathOverlay>()
    private val circleOverlays = mutableListOf<CircleOverlay>()

    private lateinit var locationDataList: List<LocationData>
    private lateinit var visitDataList: List<VisitData>

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1000
        private const val BACKGROUND_PERMISSION_REQUEST_CODE = 2000
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityEditLocationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val permission: Array<String> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.POST_NOTIFICATIONS)
        } else {
            TODO("VERSION.SDK_INT < Q")
        }
        //필요한 permission array

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
            == PackageManager.PERMISSION_DENIED
            || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_DENIED
            || ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
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

        binding.DeleteButton.setOnClickListener {
            deleteOverlay()
        }

        binding.SearchButton.setOnClickListener {
            //Toast.makeText(this, "공유하고싶은 방문날짜를 선택해주세요.", Toast.LENGTH_SHORT).show()

            val calendar = Calendar.getInstance()
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            DatePickerDialog(this, { _, selectedYear, selectedMonth, selectedDay ->

                val selectedDate = if(selectedMonth > 8) {
                    "$selectedYear-${selectedMonth + 1}-$selectedDay"
                } else {
                    "$selectedYear-0${selectedMonth + 1}-$selectedDay"
                }

                visitDataList = VisitDatabase(this).getAllVisitData().filter {
                    it.datetime.startsWith(selectedDate)
                }

                if (visitDataList.isNotEmpty()) {
                    Toast.makeText(this, "$selectedDate 방문한 장소가 있습니다.", Toast.LENGTH_SHORT).show()


                    Auth.pathPoints.clear()
                    deleteOverlay()


                    locationDataList = LocationDatabase(this).getAllLocationData().filter {
                        it.datetime.startsWith(selectedDate)
                    }//경로 검색

                    locationDataList.forEach { locationData ->
                        Auth.pathPoints.add(LatLng(locationData.lat, locationData.lng))
                    }//경로를 Auth에 저장

                    if(Auth.pathPoints.size > 1)
                    {
                        pathOverlay.coords = Auth.pathPoints
                        pathOverlay.width = 20
                        pathOverlay.color = Color.YELLOW
                        pathOverlay.map = naverMap
                    }//경로 그리기

                    visitDataList.forEach { visitData ->

                        addCircleOverlay(visitData)
                        addVisitMarker(visitData)

                    }

                } else {
                    Toast.makeText(this, "$selectedDate 방문한 장소가 없습니다.", Toast.LENGTH_SHORT).show()
                    deleteOverlay()
                }

                binding.TitleDate.visibility = View.VISIBLE
                binding.TitleDate.text = selectedDate
            }, year, month, day).show()
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

        naverMap.setOnSymbolClickListener { symbol ->
            run {
                Toast.makeText(this, symbol.caption, Toast.LENGTH_SHORT).show()
                // 이벤트 소비, OnMapClick 이벤트는 발생하지 않음
                symbol.position
                true
            }
        }

        initOverlay()
    }

    private fun deleteOverlay() {
        pathOverlay.map = null

        visitMarkers.forEach { it.map = null }
        visitMarkers.clear()

        circleOverlays.forEach{ it.map = null }
        circleOverlays.clear()
    }

    private fun initOverlay() {
        // SQLite에서 모든 위치 데이터 가져오기
        locationDataList = LocationDatabase(this).getAllLocationData()

        Auth.pathPoints.clear()
        locationDataList.forEach { locationData ->
            Auth.pathPoints.add(LatLng(locationData.lat, locationData.lng))
        }

        pathOverlay = PathOverlay()

        if(Auth.pathPoints.size > 1) {
            pathOverlay.coords = Auth.pathPoints
            pathOverlay.width = 20
            pathOverlay.color = Color.YELLOW
            pathOverlay.map = naverMap
        }

        visitDataList = VisitDatabase(this).getAllVisitData()

        visitDataList.forEach { visitData ->
            // 원 그리기
            addCircleOverlay(visitData)
            addVisitMarker(visitData)
        }
    }

    private fun addVisitMarker(visitData: VisitData){
        val visitMarker = Marker()
        visitMarker.position = LatLng(visitData.lat, visitData.lng)
        visitMarker.map = naverMap

        visitMarkers.add(visitMarker)
    }

    private fun addCircleOverlay(visitData: VisitData){
        val circleOverlay = CircleOverlay()
        circleOverlay.center = LatLng(visitData.lat, visitData.lng)
        circleOverlay.radius = 50.0 // 반지름을 50m로 설정
        circleOverlay.color = Color.argb(20, 255, 0, 0) // 원의 색상 설정
        circleOverlay.map = naverMap

        circleOverlays.add(circleOverlay)
    }


}