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
import com.example.vecto.LocationService.Companion.CHECKDISTANCE
import com.example.vecto.R
import com.example.vecto.data.LocationData
import com.example.vecto.data.LocationDatabase
import com.example.vecto.data.PathData
import com.example.vecto.data.VisitData
import com.example.vecto.data.VisitDatabase
import com.example.vecto.databinding.ActivityEditLocationBinding
import com.example.vecto.retrofit.TMapAPIService
import com.naver.maps.geometry.LatLng
import com.naver.maps.geometry.LatLngBounds
import com.naver.maps.map.CameraUpdate
import com.naver.maps.map.LocationTrackingMode
import com.naver.maps.map.MapFragment
import com.naver.maps.map.NaverMap
import com.naver.maps.map.OnMapReadyCallback
import com.naver.maps.map.overlay.CircleOverlay
import com.naver.maps.map.overlay.Marker
import com.naver.maps.map.overlay.PathOverlay
import com.naver.maps.map.util.FusedLocationSource
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Calendar

class EditLocationActivity : AppCompatActivity(), OnMapReadyCallback, MyLocationAdapter.OnItemClickListener {
    private lateinit var binding: ActivityEditLocationBinding


    //map설정 관련
    private lateinit var mapView: MapFragment
    private lateinit var locationSource: FusedLocationSource // 위치를 반환하는 구현체
    private lateinit var naverMap: NaverMap
    //overlay 관련
    private val visitMarkers = mutableListOf<Marker>()
    private val pathOverlays = mutableListOf<PathOverlay>()
    private val circleOverlays = mutableListOf<CircleOverlay>()
    //내부 함수 관련
    private lateinit var locationDataList: MutableList<LocationData>
    private lateinit var visitDataList: MutableList<VisitData> //search, merge에 사용하는 용도
    private lateinit var originalVisitData: VisitData //recyclerrView 클릭 데이터 전달 용도
    private var typeNumber = 0
    //rectclerview adpater
    private lateinit var myLocationAdapter: MyLocationAdapter
    //API 응답 관련
    private var responsePathData = mutableListOf<LatLng>()



    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1000
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityEditLocationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //Button Binding
        val startServiceButton = binding.StartServiceButton
        val stopServiceButton = binding.StopServiceButton
        val searchButton = binding.SearchButton
        val deleteButton = binding.DeleteButton
        val recommendCourseButton = binding.RecommendCourseButton
        val setRecommendCourseButton = binding.SetRecommendCourseButton
        val mergeButton = binding.MergeVisitButton
        //----------
        val titleDate = binding.TitleDate
        val editLayout = binding.EditLayout

        initMap()


        startServiceButton.setOnClickListener {
            val serviceIntent = Intent(this, LocationService::class.java)
            serviceIntent.action = Actions.START_FOREGROUND
            startService(serviceIntent)
            Toast.makeText(this, "백그라운드 서비스를 시작합니다", Toast.LENGTH_SHORT).show()
        }

        stopServiceButton.setOnClickListener{
            val serviceIntent = Intent(this, LocationService::class.java)
            serviceIntent.action = Actions.STOP_FOREGROUND
            startService(serviceIntent)
            Toast.makeText(this, "백그라운드 서비스를 종료합니다.", Toast.LENGTH_SHORT).show()
        }

        deleteButton.setOnClickListener {
            deleteOverlay()
        }

        recommendCourseButton.setOnClickListener{
            if(typeNumber == 2) {

                deleteOverlay()

                val Start = LatLng(locationDataList.first().lat, locationDataList.first().lng)
                val End = LatLng(locationDataList.last().lat, locationDataList.last().lng)

                //TMap API를 통한 경로
                val tMapAPIService = TMapAPIService.create()
                val call = tMapAPIService.getRecommendedRoute(
                    1, TMapAPIService.key(),
                    Start.latitude, Start.longitude,
                    End.latitude, End.longitude,
                    "WGS84GEO", "WGS84GEO",
                    "출발지_이름", "도착지_이름",
                    0
                )

                responsePathData.clear()

                call.enqueue(object : Callback<TMapAPIService.GeoJsonResponse> {
                    override fun onResponse(
                        call: Call<TMapAPIService.GeoJsonResponse>,
                        response: Response<TMapAPIService.GeoJsonResponse>
                    ) {
                        if (response.isSuccessful) {
                            Log.d("Response", response.body().toString())
                            // 응답 성공
                            responsePathData.add(Start)
                            val geoData = response.body()
                            geoData?.features?.forEach { feature ->
                                when (feature.geometry.type) {
                                    "Point" -> {
                                        val coordinate = feature.geometry.coordinates as List<Double>
                                        val latLng = LatLng(coordinate[1], coordinate[0])
                                        responsePathData.add(latLng)
                                    }
                                    "LineString" -> {
                                        val coordinates = feature.geometry.coordinates as List<List<Double>>
                                        coordinates.forEach { coordinate ->
                                            val latLng = LatLng(coordinate[1], coordinate[0])
                                            responsePathData.add(latLng)
                                        }

                                    }
                                }
                            }
                            responsePathData.add(End)
                            addPathOverlay(responsePathData)

                        } else {
                            // 응답 실패
                        }
                    }

                    override fun onFailure(call: Call<TMapAPIService.GeoJsonResponse>, t: Throwable) {
                        Log.e("Retrofit", t.message.toString())
                    }
                })

                setRecommendCourseButton.visibility = View.VISIBLE
                recommendCourseButton.visibility = View.GONE
                typeNumber = 3
            }
        }

        setRecommendCourseButton.setOnClickListener {
            if(typeNumber == 3){
                //시작과 끝을 제외한 기존 경로 삭제
                LocationDatabase(this).deleteLocationDataBetween(locationDataList.first().datetime, locationDataList.last().datetime)

                //시작 시간은 시작 지점의 시간.
                val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
                val startTime = LocalDateTime.parse(locationDataList.first().datetime, formatter)

                responsePathData.forEachIndexed { index, point ->
                    LocationDatabase(this).addLocationData(LocationData(startTime.plusSeconds(index.toLong() + 1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")), point.latitude, point.longitude))
                }

                setRecommendCourseButton.visibility = View.GONE
                Toast.makeText(this, "경로 변경 완료.", Toast.LENGTH_SHORT).show()
                typeNumber = 0
            }
        }

        searchButton.setOnClickListener {
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

                    deleteOverlay()//Overlay 삭제

                    myLocationAdapter = MyLocationAdapter(this, this)
                    val locationRecyclerView = binding.LocationRecyclerView
                    locationRecyclerView.adapter = myLocationAdapter
                    locationRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

                    //선택한 날짜의 방문지의 처음과 끝까지의 경로
                    locationDataList = LocationDatabase(this).getBetweenLocationData(visitDataList.first().datetime, visitDataList.last().datetime)

                    addPathOverlayForLoacation(locationDataList)


                    val locationDataforPath = mutableListOf<LocationData>()
                    var cnt = 1

                    //location 첫 좌표 넣어줌.
                    locationDataforPath.add(LocationData(visitDataList[0].datetime, visitDataList[0].lat_set, visitDataList[0].lng_set))

                    for (visitdatalist in visitDataList){
                        addCircleOverlay(visitdatalist)
                        addVisitMarker(visitdatalist)

                        myLocationAdapter.visitdata.add(visitdatalist)
                    }

                    for (locationData in locationDataList){
                        if(visitDataList.size > 1) { //저장된 시각이 같으면 방문지점 도착경로 1 cycle 완료
                            if (locationData.datetime == visitDataList[cnt].datetime) {
                                //다음 방문 지점의 경로 좌표에 도달하면, 방문지점 좌표까지 추가해서, adapter에 넘겨주고, 비운후 방문지점 좌표 추가해서 시작
                                locationDataforPath.add(locationData)
                                val pathData = PathData(locationDataforPath.toMutableList())
                                myLocationAdapter.pathdata.add(pathData)

                                locationDataforPath.clear()
                                locationDataforPath.add(locationData)
                                cnt++

                                if (cnt == visitDataList.size) {
                                    Log.d("location", "마지막 항목에 도달하여 종료합니다. 저장된 경로 수: ${myLocationAdapter.pathdata}")

                                    break
                                }
                            } else {
                                locationDataforPath.add(locationData)
                            }
                        }

                    }



                    myLocationAdapter.notifyDataSetChanged()

                } else {
                    Toast.makeText(this, "$selectedDate 방문한 장소가 없습니다.", Toast.LENGTH_SHORT).show()
                    deleteOverlay()
                }

                titleDate.visibility = View.VISIBLE
                titleDate.text = selectedDate
                editLayout.visibility = View.VISIBLE
                startServiceButton.visibility = View.GONE
                stopServiceButton.visibility = View.GONE
            }, year, month, day).show()
        }

        mergeButton.setOnClickListener {
            if(typeNumber == 1)
            {
                Toast.makeText(this, "선택한 장소와 합칠 장소를 선택해주세요. 현재 선택된 장소는 사라집니다.", Toast.LENGTH_SHORT).show()
                visitDataList.clear()
                visitDataList.add(originalVisitData)
                addCircleOverlayForMerge(visitDataList[0])

                typeNumber = 4
            }
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
                        if(checkDistance(LatLng(originalVisitData.lat, originalVisitData.lng), symbol.position, CHECKDISTANCE)) {

                            val newVisitData = VisitData(
                                originalVisitData.datetime, originalVisitData.endtime,
                                originalVisitData.lat, originalVisitData.lng,
                                symbol.position.latitude, symbol.position.longitude,
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

    /*RecyclerView 관련 함수*/
    /*____________________________________________________________________________________________*/

    override fun onItemClick(data: Any) {
        deleteOverlay() //지도 clear

        if (data is VisitData) {
            if(typeNumber == 4)
            {

                if(checkDistance(LatLng(data.lat, data.lng), LatLng(visitDataList[0].lat, visitDataList[0].lng), 100)) {


                    mergeVisitData(data, visitDataList[0])
                    //삭제를 리사이클러뷰에 반영
                    myLocationAdapter.visitdata.remove(visitDataList[0])
                    myLocationAdapter.notifyItemRemoved(myLocationAdapter.visitdata.indexOf(visitDataList[0]))

                    typeNumber = 0
                    binding.MergeVisitButton.visibility = View.GONE
                }
                else
                {
                    Toast.makeText(this, "100M 까지 합치기 가능합니다.", Toast.LENGTH_SHORT).show()
                }
            }else {
                addVisitMarker(data)//선택한 visitdata를 마커에 추가
                addCircleOverlay(data)

                moveCameraForVisit(data)

                //symbol click listener를 위해 data와 type을 설정
                originalVisitData = data
                typeNumber = 1

                binding.RecommendCourseButton.visibility = View.GONE
                binding.SetRecommendCourseButton.visibility = View.GONE
                binding.MergeVisitButton.visibility = View.VISIBLE


                Toast.makeText(this, "방문한 곳을 선택하세요.", Toast.LENGTH_SHORT).show()
            }
        } else if (data is PathData) {//return mutableList<LocationData>
            //방문이 1곳일 경우는 생각할 필요 X.
            //1곳이면 경로가 X
            locationDataList = data.coordinates

            addPathOverlayForLoacation(data.coordinates)
            moveCameraForPath(data.coordinates)


            typeNumber = 2
            binding.RecommendCourseButton.visibility = View.VISIBLE
            binding.SetRecommendCourseButton.visibility = View.GONE
            binding.MergeVisitButton.visibility = View.GONE
        }
    }

    /*Data 관련 함수*/
    /*____________________________________________________________________________________________*/

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

    //merging 기준으로, merged visit 을 합치는 함수
    private fun mergeVisitData(mergingVisitData: VisitData, mergedVisitData: VisitData): String{
        val datetime =
            if (mergingVisitData.datetime > mergedVisitData.datetime)
                mergedVisitData.datetime
            else mergingVisitData.datetime

        val endtime =
            if(mergingVisitData.endtime > mergedVisitData.endtime)
                mergingVisitData.endtime
            else mergedVisitData.endtime

        val FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
        val newVisitData = VisitData(
            datetime, endtime,
            mergingVisitData.lat, mergingVisitData.lng,
            mergingVisitData.lat_set, mergingVisitData.lng_set,
            Duration.between(LocalDateTime.parse(datetime, FORMAT), LocalDateTime.parse(endtime, FORMAT)).toMinutes().toInt(),
            mergingVisitData.name
        )

        VisitDatabase(this).deleteVisitData(mergedVisitData.datetime)
        VisitDatabase(this).updateVisitData(mergingVisitData, newVisitData)
        Log.d("merge", "합치기 완료")

        return mergedVisitData.datetime
    }

    private fun checkDistance(centerLatLng: LatLng, currendLatLng: LatLng, checkDistance: Int): Boolean{
        val centerLocation = Location("centerLatLng")
        centerLocation.latitude = centerLatLng.latitude
        centerLocation.longitude = centerLatLng.longitude

        val currentLocation = Location("currendLatLng")
        currentLocation.latitude = currendLatLng.latitude
        currentLocation.longitude = currendLatLng.longitude

        return centerLocation.distanceTo(currentLocation) <= checkDistance
    }

    /*Overlay 관련 함수*/
    /*____________________________________________________________________________________________*/
    private fun initOverlay() {
        // SQLite에서 모든 위치 데이터 가져오기
        locationDataList = LocationDatabase(this).getAllLocationData()
        addPathOverlayForLoacation(locationDataList)

        visitDataList = VisitDatabase(this).getAllVisitData()
        visitDataList.forEach { visitData ->
            addCircleOverlay(visitData)//원 그리기
            addVisitMarker(visitData)//마커 찍기
        }

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

    private fun addCircleOverlayForMerge(visitData: VisitData){
        val circleOverlay = CircleOverlay()
        circleOverlay.center = LatLng(visitData.lat, visitData.lng)
        circleOverlay.radius = 100.0 // 반지름을 50m로 설정

        if(visitData.name.isEmpty()) {
            circleOverlay.color = Color.argb(20, 255, 255, 0) // 원의 색상 설정
        }
        else {
            circleOverlay.color = Color.argb(20, 255, 255, 0) // 원의 색상 설정
        }

        circleOverlay.map = naverMap

        circleOverlays.add(circleOverlay)
    }

    private fun deleteOverlay() {
        pathOverlays.forEach{ it.map = null}
        pathOverlays.clear()

        visitMarkers.forEach { it.map = null }
        visitMarkers.clear()

        circleOverlays.forEach{ it.map = null }
        circleOverlays.clear()
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
    private fun addPathOverlayForLoacation(pathPoints: MutableList<LocationData>){
        val pathLatLng = mutableListOf<LatLng>()
        //LocationData를 이용하여 PathOverlay를 만들기 위해 mutableList LatLng을 만듬

        for(i in 0 until pathPoints.size - 1) {
            pathLatLng.add(LatLng(pathPoints[i].lat, pathPoints[i].lng))
        }

        val pathOverlay = PathOverlay()

        if(pathLatLng.size > 1) {
            pathOverlay.coords = pathLatLng
            pathOverlay.width = 20
            pathOverlay.color = Color.YELLOW
            pathOverlay.map = naverMap
        }

        pathOverlays.add(pathOverlay)
    }

    private fun addPathOverlay(pathPoints: MutableList<LatLng>){
        val pathOverlay = PathOverlay()

        if(pathPoints.size > 1) {
            pathOverlay.coords = pathPoints
            pathOverlay.width = 20
            pathOverlay.color = Color.YELLOW
            pathOverlay.map = naverMap
        }

        pathOverlays.add(pathOverlay)
    }

    /*Camera 관련 함수*/
    /*____________________________________________________________________________________________*/
    private fun moveCameraForPath(pathPoints: MutableList<LocationData>){
        if(pathPoints.isNotEmpty()) {
            val minLat = pathPoints.minOf { it.lat }
            val maxLat = pathPoints.maxOf { it.lat }
            val minLng = pathPoints.minOf { it.lng }
            val maxLng = pathPoints.maxOf { it.lng }

            val bounds = LatLngBounds(LatLng(minLat, minLng), LatLng(maxLat, maxLng))
            naverMap.moveCamera(CameraUpdate.fitBounds(bounds, 300))
            val Offset = PointF(0.0f, (-350).toFloat())
            naverMap.moveCamera(CameraUpdate.scrollBy(Offset))

        }
    }

    private fun moveCameraForVisit(visit: VisitData){
        val targetLatLng = LatLng(visit.lat_set, visit.lng_set)
        val Offset = PointF(0.0f, (-350).toFloat())

        naverMap.moveCamera(CameraUpdate.scrollTo(targetLatLng))
        naverMap.moveCamera(CameraUpdate.zoomTo(18.0))
        naverMap.moveCamera(CameraUpdate.scrollBy(Offset))
    }
}