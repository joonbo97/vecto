package com.example.vecto.editlocation

import android.app.DatePickerDialog
import android.content.Intent
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.vecto.Actions
import com.example.vecto.LocationService
import com.example.vecto.R
import com.example.vecto.data.Auth
import com.example.vecto.data.LocationData
import com.example.vecto.data.LocationDatabase
import com.example.vecto.data.PathData
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

    private lateinit var myLocationAdapter: MyLocationAdapter



    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1000
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityEditLocationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initMap()

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

                val selectedDate: String = if((selectedMonth > 8) && (selectedDay > 9)) {
                    "$selectedYear-${selectedMonth + 1}-$selectedDay"
                } else if(selectedMonth > 8) {
                    "$selectedYear-${selectedMonth + 1}-0$selectedDay"
                } else if(selectedDay > 9) {
                    "$selectedYear-0${selectedMonth + 1}-$selectedDay"
                } else{
                    "$selectedYear-0${selectedMonth + 1}-0$selectedDay"
                }

                visitDataList = VisitDatabase(this).getAllVisitData().filter {
                    it.datetime.startsWith(selectedDate)
                }

                if (visitDataList.isNotEmpty()) {
                    Toast.makeText(this, "$selectedDate 방문한 장소가 있습니다.", Toast.LENGTH_SHORT).show()




                    Auth.pathPoints.clear()
                    deleteOverlay()




                    myLocationAdapter = MyLocationAdapter(this)
                    val locationRecyclerView = binding.LocationRecyclerView
                    locationRecyclerView.adapter = myLocationAdapter
                    locationRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)




                    locationDataList = LocationDatabase(this).getAllLocationData().filter {
                        it.datetime.startsWith(selectedDate)
                    }//경로 검색



                    val locationDataforPath = mutableListOf<LocationData>()
                    var cnt = 1
                    locationDataforPath.add(LocationData(visitDataList[0].datetime, visitDataList[0].lat, visitDataList[0].lng))

                    visitDataList.forEach { visitData ->
                        addCircleOverlay(visitData)
                        addVisitMarker(visitData)

                        myLocationAdapter.visitdata.add(visitData)
                    }

                    locationDataList.forEach { locationData ->
                        Auth.pathPoints.add(LatLng(locationData.lat, locationData.lng))
                        //경로를 Auth에 저장


                        //저장된 시각이 같으면 방문지점 도착경로 1 cycle 완료
                        if(locationData.datetime == visitDataList[cnt].datetime)
                        {
                            locationDataforPath.add(locationData)
                            myLocationAdapter.pathdata.add(PathData(locationDataforPath))

                            locationDataforPath.clear()
                            locationDataforPath.add(locationData)

                            cnt++

                            if(cnt == visitDataList.size)
                                cnt--
                        }
                        else
                        {
                            locationDataforPath.add(locationData)
                        }


                    }

                    myLocationAdapter.notifyDataSetChanged()

                    if(Auth.pathPoints.size > 1)
                    {
                        pathOverlay.coords = Auth.pathPoints
                        pathOverlay.width = 20
                        pathOverlay.color = Color.YELLOW
                        pathOverlay.map = naverMap
                    }//경로 그리기



                } else {
                    Toast.makeText(this, "$selectedDate 방문한 장소가 없습니다.", Toast.LENGTH_SHORT).show()
                    deleteOverlay()
                }

                binding.TitleDate.visibility = View.VISIBLE
                binding.TitleDate.text = selectedDate
                binding.EditLayout.visibility = View.VISIBLE
                binding.StartServiceButton.visibility = View.GONE
                binding.StopServiceButton.visibility = View.GONE
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