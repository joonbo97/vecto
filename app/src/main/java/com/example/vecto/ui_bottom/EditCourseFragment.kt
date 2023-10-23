package com.example.vecto.ui_bottom

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.graphics.Color
import android.graphics.PointF
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.vecto.R
import com.example.vecto.VerticalOverlapItemDecoration
import com.example.vecto.data.LocationData
import com.example.vecto.data.LocationDatabase
import com.example.vecto.data.PathData
import com.example.vecto.data.VisitData
import com.example.vecto.data.VisitDatabase
import com.example.vecto.databinding.FragmentEditCourseBinding
import com.example.vecto.retrofit.TMapAPIService
import com.naver.maps.geometry.LatLng
import com.naver.maps.geometry.LatLngBounds
import com.naver.maps.map.CameraUpdate
import com.naver.maps.map.MapFragment
import com.naver.maps.map.NaverMap
import com.naver.maps.map.OnMapReadyCallback
import com.naver.maps.map.overlay.CircleOverlay
import com.naver.maps.map.overlay.Marker
import com.naver.maps.map.overlay.OverlayImage
import com.naver.maps.map.overlay.PathOverlay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class EditCourseFragment : Fragment(), OnMapReadyCallback, MyCourseAdapter.OnItemClickListener {
    lateinit var binding: FragmentEditCourseBinding

    //map설정 관련
    private lateinit var mapView: MapFragment
    private lateinit var naverMap: NaverMap

    private lateinit var locationDataList: MutableList<LocationData>
    private lateinit var visitDataList: MutableList<VisitData>
    private lateinit var selectedVisitData: VisitData
    private lateinit var selectedPathData: MutableList<LocationData>

    private lateinit var myCourseAdapter: MyCourseAdapter

    //overlay 관련
    private val visitMarkers = mutableListOf<Marker>()
    private val pathOverlays = mutableListOf<PathOverlay>()
    private val circleOverlays = mutableListOf<CircleOverlay>()

    //UI관련
    private lateinit var DateText: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentEditCourseBinding.inflate(inflater, container, false)


        DateText = binding.TextForLargeRight
        showDatePickerDialog()

        initMap()

        binding.CalendarLargeBoxImage.setOnClickListener {
            showDatePickerDialog()
        }

        binding.SearchButtonImage.setOnClickListener {
            getPlace(1)
        }

        return binding.root
    }


    private fun initMap(){
        mapView = childFragmentManager.findFragmentById(R.id.naver_map_Edit) as MapFragment?
            ?: MapFragment.newInstance().also {
                childFragmentManager.beginTransaction().add(R.id.naver_map_Edit, it).commit()
            }
        mapView.getMapAsync(this)
    }

    override fun onMapReady(naverMap: NaverMap) {
        this.naverMap = naverMap
        naverMap.moveCamera(CameraUpdate.zoomTo(18.0))
        naverMap.uiSettings.isZoomControlEnabled = false

        setVistiLoaction()
    }

    private fun setVistiLoaction() {
        locationDataList = LocationDatabase(requireContext()).getTodayLocationData()
        visitDataList = VisitDatabase(requireContext()).getTodayVisitData()

        addPathOverlayForLoacation(locationDataList)
        for (visitdatalist in visitDataList) {
            addVisitMarker(visitdatalist)
        }

    }









    private fun addPathOverlayForLoacation(pathPoints: MutableList<LocationData>){
        val pathLatLng = mutableListOf<LatLng>()

        for(i in 0 until pathPoints.size) {
            pathLatLng.add(LatLng(pathPoints[i].lat, pathPoints[i].lng))
        }

        addPathOverlay(pathLatLng)
    }

    private fun addPathOverlay(pathPoints: MutableList<LatLng>){
        val pathOverlay = PathOverlay()

        if(pathPoints.size > 1) {
            pathOverlay.coords = pathPoints
            pathOverlay.width = 20
            pathOverlay.color = ContextCompat.getColor(requireContext(), R.color.vecto_pathcolor)
            pathOverlay.patternImage = OverlayImage.fromResource(R.drawable.pathoverlay_pattern)
            pathOverlay.patternInterval = 50
            pathOverlay.map = naverMap
            pathOverlays.add(pathOverlay)
        }
    }

    private fun addVisitMarker(visitData: VisitData){
        val visitMarker = Marker()
        visitMarker.icon = OverlayImage.fromResource(R.drawable.marker_image)

        if(visitData.name.isNotEmpty()) {
            visitMarker.position = LatLng(visitData.lat_set, visitData.lng_set)
        }
        else {
            visitMarker.position = LatLng(visitData.lat, visitData.lng)
        }

        visitMarker.map = naverMap

        visitMarkers.add(visitMarker)
    }

    private fun addPlaceMarker(poi: TMapAPIService.Poi){
        val visitMarker = Marker()

        visitMarker.position = LatLng(poi.frontLat, poi.frontLon)
        visitMarker.subCaptionText = poi.name
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

    @SuppressLint("NotifyDataSetChanged")
    private fun showDatePickerDialog(){
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        DatePickerDialog(requireContext(), { _, selectedYear, selectedMonth, selectedDay ->

            val selectedDate = if((selectedMonth > 8) && (selectedDay > 9)) {
                "$selectedYear-${selectedMonth + 1}-$selectedDay"
            } else if(selectedMonth > 8) {
                "$selectedYear-${selectedMonth + 1}-0$selectedDay"
            } else if(selectedDay > 9) {
                "$selectedYear-0${selectedMonth + 1}-$selectedDay"
            } else{
                "$selectedYear-0${selectedMonth + 1}-0$selectedDay"
            }

            DateText.text = selectedDate


            val previousDate = getPreviousDate(selectedDate)

            val filteredData = VisitDatabase(requireContext()).getAllVisitData().filter { visitData ->
                val visitDate = visitData.datetime.substring(0, 10)
                val endDate = visitData.endtime.substring(0, 10)
                visitDate == previousDate && endDate == selectedDate
            }

            visitDataList = VisitDatabase(requireContext()).getAllVisitData().filter {
                it.datetime.startsWith(selectedDate)
            }.toMutableList()

            //종료 시간이 선택 날짜인 방문지 추가
            if(filteredData.isNotEmpty())
                visitDataList.add(0, filteredData[0])

            if(visitDataList.isNotEmpty()){
                //방문 장소가 있을 경우

                deleteOverlay()

                /*RecyclerView Adapter 설정*/
                myCourseAdapter = MyCourseAdapter(requireContext(), this)
                val locationRecyclerView = binding.LocationRecyclerView
                locationRecyclerView.adapter = myCourseAdapter
                locationRecyclerView.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
                locationRecyclerView.addItemDecoration(VerticalOverlapItemDecoration(42))

                //선택한 날짜의 방문지의 처음과 끝까지의 경로
                locationDataList = LocationDatabase(requireContext()).getBetweenLocationData(visitDataList.first().datetime, visitDataList.last().datetime)

                addPathOverlayForLoacation(locationDataList)
                moveCameraForPath(locationDataList)


                val locationDataforPath = mutableListOf<LocationData>()
                var cnt = 1

                //location 첫 좌표 넣어줌.
                locationDataforPath.add(LocationData(visitDataList[0].datetime, visitDataList[0].lat_set, visitDataList[0].lng_set))

                for (visitdatalist in visitDataList){
                    addCircleOverlay(visitdatalist)
                    addVisitMarker(visitdatalist)

                    myCourseAdapter.visitdata.add(visitdatalist)
                }

                for (locationData in locationDataList){
                    if(visitDataList.size > 1) { //저장된 시각이 같으면 방문지점 도착경로 1 cycle 완료
                        if (locationData.datetime == visitDataList[cnt].datetime) {
                            //다음 방문 지점의 경로 좌표에 도달하면, 방문지점 좌표까지 추가해서, adapter에 넘겨주고, 비운후 방문지점 좌표 추가해서 시작
                            locationDataforPath.add(locationData)
                            val pathData = PathData(locationDataforPath.toMutableList())
                            myCourseAdapter.pathdata.add(pathData)

                            locationDataforPath.clear()
                            locationDataforPath.add(locationData)
                            cnt++

                            if (cnt == visitDataList.size) {
                                Log.d("location", "마지막 항목에 도달하여 종료합니다. 저장된 경로 수: ${myCourseAdapter.pathdata}")
                                break
                            }
                        } else {
                            locationDataforPath.add(locationData)
                        }
                    }

                }

                myCourseAdapter.notifyDataSetChanged()
            }
            else{
                //방문 장소 없을 경우
                deleteOverlay()
            }

        }, year, month, day).show()
    }

    override fun onItemClick(data: Any) {
        deleteOverlay()

        if (data is VisitData){
            addVisitMarker(data)//선택한 visitdata를 마커에 추가
            addCircleOverlay(data)

            moveCameraForVisit(data)
            selectedVisitData = data
        }
        else if(data is PathData)
        {
            locationDataList = data.coordinates

            addPathOverlayForLoacation(data.coordinates)
            moveCameraForPath(data.coordinates)

            selectedPathData = data.coordinates
        }
    }

    private fun getPreviousDate(selectedDate: String): String {
        val selectedDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val selectedDateObj = selectedDateFormat.parse(selectedDate)
        val calendar = Calendar.getInstance()
        calendar.time = selectedDateObj!!
        calendar.add(Calendar.DAY_OF_MONTH, -1)


        return selectedDateFormat.format(calendar.time)
    }

    /*주변 정보 얻는 함수*/
    private fun getPlace(page: Int){
        val tMapAPIService = TMapAPIService.create()

        val call = tMapAPIService.searchNearbyPOI(1, "식당;카페;편의점;병원;음식점;지하철;마트;백화점;대학교;기숙사;공원;아파트;오피스텔;빌라", TMapAPIService.key(), page, 1,200 , selectedVisitData.lat, selectedVisitData.lng)
        call.enqueue(object : Callback<TMapAPIService.POIResponse> {
            override fun onResponse(call: Call<TMapAPIService.POIResponse>, response: Response<TMapAPIService.POIResponse>) {
                if (response.isSuccessful) {
                    val pois = response.body()?.searchPoiInfo?.pois?.poi
                    val totalCount = response.body()?.searchPoiInfo?.totalCount ?: 0
                    val countPerPage = response.body()?.searchPoiInfo?.count ?: 1

                    pois?.forEach { poi ->
                        Log.d("POI Name", poi.name)
                        Log.d("POI Latitude", poi.frontLat.toString())
                        Log.d("POI Longitude", poi.frontLon.toString())
                        addPlaceMarker(poi)
                    }

                    // 현재 페이지의 결과가 마지막이 아닌 경우 다음 페이지 요청
                    if (totalCount > page * countPerPage) {
                        getPlace(page + 1)
                    }

                } else {
                    Log.d("POI", response.message())
                }
            }

            override fun onFailure(call: Call<TMapAPIService.POIResponse>, t: Throwable) {
                Log.d("POI", t.message.toString())
            }
        })

    }

}