package com.example.vecto.editlocation

import android.app.DatePickerDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.PointF
import android.location.Location
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
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
import com.example.vecto.retrofit.TMapAPIService
import com.naver.maps.geometry.LatLng
import com.naver.maps.geometry.LatLngBounds
import com.naver.maps.map.CameraPosition
import com.naver.maps.map.CameraUpdate
import com.naver.maps.map.LocationTrackingMode
import com.naver.maps.map.MapFragment
import com.naver.maps.map.NaverMap
import com.naver.maps.map.OnMapReadyCallback
import com.naver.maps.map.overlay.CircleOverlay
import com.naver.maps.map.overlay.Marker
import com.naver.maps.map.overlay.PathOverlay
import com.naver.maps.map.util.FusedLocationSource
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Calendar

class EditLocationActivity : AppCompatActivity(), OnMapReadyCallback, MyLocationAdapter.OnItemClickListener {
    private lateinit var binding: ActivityEditLocationBinding
    private lateinit var mapView: MapFragment
    private lateinit var locationSource: FusedLocationSource // 위치를 반환하는 구현체
    private lateinit var naverMap: NaverMap

    private val visitMarkers = mutableListOf<Marker>()
    lateinit var pathOverlay: PathOverlay
    //private val pathOverlays = mutableListOf<PathOverlay>()
    private val circleOverlays = mutableListOf<CircleOverlay>()

    private lateinit var locationDataList: MutableList<LocationData>
    private lateinit var visitDataList: MutableList<VisitData>

    private lateinit var myLocationAdapter: MyLocationAdapter

    lateinit var originalVisitData: VisitData
    var typeNumber = 0




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

        binding.RecommendCourseButton.setOnClickListener{
            if(typeNumber == 2) {

                //TMap API를 통한 경로
                val tMapAPIService = TMapAPIService.create()
                val call = tMapAPIService.getRecommendedRoute(
                    1,
                    TMapAPIService.key(),
                    Auth.pathPoints.first().latitude,
                    Auth.pathPoints.first().longitude,
                    Auth.pathPoints.last().latitude,
                    Auth.pathPoints.last().longitude,
                    "WGS84GEO",
                    "WGS84GEO",
                    "출발지_이름",
                    "도착지_이름",
                    0
                )


                call.enqueue(object : Callback<TMapAPIService.RouteResponse> {
                    override fun onResponse(
                        call: Call<TMapAPIService.RouteResponse>,
                        response: Response<TMapAPIService.RouteResponse>
                    ) {
                        if (response.isSuccessful) {
                            // 응답 성공
                            response.body()?.features?.forEach { feature ->
                                if (feature.geometry.type == "Point") {
                                    val coordinates = feature.geometry.coordinates
                                    val latitude = coordinates[1]
                                    val longitude = coordinates[0]

                                    val pathPoint = LatLng(latitude, longitude)
                                    Auth.pathPoints.add(pathPoint)
                                }
                            }

                        } else {
                            // 응답 실패
                        }
                    }

                    override fun onFailure(call: Call<TMapAPIService.RouteResponse>, t: Throwable) {
                        TODO("Not yet implemented")
                    }
                })

                binding.SetRecommendCourseButton.visibility = View.VISIBLE
                binding.RecommendCourseButton.visibility = View.GONE
                typeNumber = 3
            }
        }

        binding.SetRecommendCourseButton.setOnClickListener {
            if(typeNumber == 3){
                //시작과 끝을 제외한 기존 경로 삭제
                LocationDatabase(this).deleteLocationDataBetween(locationDataList.first().datetime, locationDataList.last().datetime)

                //시작 시간은 시작 지점의 시간.
                val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
                val startTime = LocalDateTime.parse(locationDataList.first().datetime, formatter)

                Auth.pathPoints.forEachIndexed { index, point ->

                    LocationDatabase(this).addLocationData(LocationData(startTime.plusSeconds(index.toLong() + 1).toString(), point.latitude, point.longitude))
                }

                binding.SetRecommendCourseButton.visibility = View.GONE
                Toast.makeText(this, "경로 변경 완료.", Toast.LENGTH_SHORT).show()
                typeNumber = 0
            }
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
                }.toMutableList()

                if (visitDataList.isNotEmpty()) {
                    Toast.makeText(this, "$selectedDate 방문한 장소가 있습니다.", Toast.LENGTH_SHORT).show()




                    Auth.pathPoints.clear()
                    deleteOverlay()




                    myLocationAdapter = MyLocationAdapter(this, this)
                    val locationRecyclerView = binding.LocationRecyclerView
                    locationRecyclerView.adapter = myLocationAdapter
                    locationRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)




                    locationDataList = LocationDatabase(this).getAllLocationData().filter {
                        it.datetime.startsWith(selectedDate)
                    }.toMutableList()//경로 검색



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


                        Log.d("location", "vistdata size : ${visitDataList.size}")
                        if(visitDataList.size > 1) { //저장된 시각이 같으면 방문지점 도착경로 1 cycle 완료
                            if (locationData.datetime == visitDataList[cnt].datetime) {
                                locationDataforPath.add(locationData)
                                myLocationAdapter.pathdata.add(PathData(locationDataforPath))

                                locationDataforPath.clear()
                                locationDataforPath.add(locationData)

                                cnt++

                                if (cnt == visitDataList.size)
                                    cnt--
                            } else {
                                locationDataforPath.add(locationData)
                            }
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

        initOverlay()


        //Symbol Click Event
        naverMap.setOnSymbolClickListener { symbol ->
            run {


                when(typeNumber){
                    1 -> {//visit data 일 경우
                        if(checkDistance(LatLng(originalVisitData.lat, originalVisitData.lng), symbol.position)) {

                            val newVisitData = VisitData(
                                originalVisitData.datetime,
                                originalVisitData.endtime,
                                originalVisitData.lat,
                                originalVisitData.lng,
                                symbol.position.latitude,
                                symbol.position.longitude,
                                originalVisitData.staytime,
                                symbol.caption
                            )

                            updateVisitData(originalVisitData, newVisitData)

                            deleteOverlay()
                            addVisitMarker(newVisitData)//선택한 newVisitData를 마커에 추가
                            addCircleOverlay(newVisitData)

                            VisitDatabase(this).updateVisitData(originalVisitData, newVisitData)


                            Toast.makeText(this, "변경완료", Toast.LENGTH_SHORT).show()
                            typeNumber = 0
                        }
                        else
                        {
                            Toast.makeText(this, "허용범위 외부의 장소입니다.", Toast.LENGTH_SHORT).show()
                        }
                    }

                    2 -> {//location data 일 경우

                    }
                }
                true
            }
        }
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
        if(visitData.name.isNotEmpty()) {
            visitMarker.position = LatLng(visitData.lat_set, visitData.lng_set)
        }
        else {
            visitMarker.position = LatLng(visitData.lat, visitData.lng)
        }
        visitMarker.map = naverMap

        visitMarkers.add(visitMarker)
    }

    private fun addCircleOverlay(visitData: VisitData){
        val circleOverlay = CircleOverlay()
        circleOverlay.center = LatLng(visitData.lat, visitData.lng)
        circleOverlay.radius = 50.0 // 반지름을 50m로 설정

        if(visitData.name.isEmpty()) {
            circleOverlay.color = Color.argb(20, 255, 0, 0) // 원의 색상 설정
        }
        else {
            circleOverlay.color = Color.argb(20, 0, 255, 0) // 원의 색상 설정
        }

        circleOverlay.map = naverMap

        circleOverlays.add(circleOverlay)
    }

    override fun onItemClick(data: Any) {
        deleteOverlay() //지도 clear

        if (data is VisitData) {
            addVisitMarker(data)//선택한 visitdata를 마커에 추가
            addCircleOverlay(data)

            val targetLatLng = LatLng(data.lat_set, data.lng_set)
            val Offset = PointF(0.0f, (-350).toFloat())

            naverMap.moveCamera(CameraUpdate.scrollTo(targetLatLng))
            naverMap.moveCamera(CameraUpdate.scrollBy(Offset))


            //symbol click listener를 위해 data와 type을 설정
            originalVisitData = data
            typeNumber = 1


            Toast.makeText(this, "방문했던 곳을 선택하세요.", Toast.LENGTH_SHORT).show()
        } else if (data is PathData) {
            //방문이 1곳일 경우는 생각할 필요 X.
            //1곳이면 경로가 X


            locationDataList.clear()
            locationDataList = data.coordinates

            //지도에 알맞게 dp하기 위한 min/max
            var minLat = 180.0
            var maxLat = 0.0
            var minLng = 180.0
            var maxLng = 0.0

            Auth.pathPoints.clear()
            data.coordinates.forEach { coordinates ->
                if(minLat > coordinates.lat)
                    minLat = coordinates.lat
                if(maxLat < coordinates.lat)
                    maxLat = coordinates.lat
                if(minLng > coordinates.lng)
                    minLng = coordinates.lng
                if(minLng < coordinates.lng)
                    minLng = coordinates.lng

                Auth.pathPoints.add(LatLng(coordinates.lat, coordinates.lng))
            }

            val bounds = LatLngBounds(LatLng(minLat, minLng), LatLng(maxLat, maxLng))
            naverMap.moveCamera(CameraUpdate.fitBounds(bounds, 50))
            val Offset = PointF(0.0f, (-350).toFloat())
            naverMap.moveCamera(CameraUpdate.scrollBy(Offset))


            if(Auth.pathPoints.size > 1) {
                pathOverlay.coords = Auth.pathPoints
                pathOverlay.width = 20
                pathOverlay.color = Color.YELLOW
                pathOverlay.map = naverMap
            }

            typeNumber = 2
            binding.RecommendCourseButton.visibility = View.VISIBLE


            Toast.makeText(this, "원하는 경로를 선택 하세요.", Toast.LENGTH_SHORT).show()
        }
    }


    //visitdata를 갱신하는 함수
    private fun updateVisitData(oldVisitData: VisitData, newVisitData: VisitData){

        for(i in myLocationAdapter.visitdata.indices){

            if(myLocationAdapter.visitdata[i] == oldVisitData) {
                myLocationAdapter.visitdata[i] = newVisitData
                myLocationAdapter.notifyItemChanged(i * 2)
                break
            }
        }

    }

    private fun checkDistance(centerLatLng: LatLng, currendLatLng: LatLng): Boolean{
        val centerLocation = Location("centerLatLng")
        centerLocation.latitude = centerLatLng.latitude
        centerLocation.longitude = centerLatLng.longitude

        val currentLocation = Location("currendLatLng")
        currentLocation.latitude = currendLatLng.latitude
        currentLocation.longitude = currendLatLng.longitude

        return centerLocation.distanceTo(currentLocation) <= LocationService.CHECKDISTANCE
    }

}