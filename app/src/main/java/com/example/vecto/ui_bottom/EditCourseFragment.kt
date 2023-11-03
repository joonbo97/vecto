package com.example.vecto.ui_bottom

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.content.res.Resources
import android.graphics.Color
import android.graphics.PointF
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.vecto.LocationService
import com.example.vecto.R
import com.example.vecto.VerticalOverlapItemDecoration
import com.example.vecto.data.LocationData
import com.example.vecto.data.LocationDatabase
import com.example.vecto.data.PathData
import com.example.vecto.data.VisitData
import com.example.vecto.data.VisitDatabase
import com.example.vecto.databinding.FragmentEditCourseBinding
import com.example.vecto.dialog.DeleteVisitDialog
import com.example.vecto.dialog.EditVisitDialog
import com.example.vecto.retrofit.GooglePlacesApi
import com.example.vecto.retrofit.GooglePlacesApi.Companion.key
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
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter


class EditCourseFragment : Fragment(), OnMapReadyCallback, MyCourseAdapter.OnItemClickListener {
    lateinit var binding: FragmentEditCourseBinding

    //map설정 관련
    private lateinit var mapView: MapFragment
    private lateinit var naverMap: NaverMap

    private lateinit var locationDataList: MutableList<LocationData>
    private lateinit var visitDataList: MutableList<VisitData>
    private lateinit var selectedVisitData: VisitData

    private lateinit var myCourseAdapter: MyCourseAdapter

    //overlay 관련
    private val visitMarkers = mutableListOf<Marker>()
    private val pathOverlays = mutableListOf<PathOverlay>()
    private val circleOverlays = mutableListOf<CircleOverlay>()

    private val buttonMarkers = mutableListOf<Marker>()


    //UI관련
    private lateinit var DateText: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentEditCourseBinding.inflate(inflater, container, false)


        DateText = binding.TextForLargeRight


        initMap()
        showDatePickerDialog()


        binding.CalendarLargeBoxImage.setOnClickListener {
            showDatePickerDialog()
        }

        binding.SearchButtonImage.setOnClickListener {
            getPlace(1)
            //getPlace2()
        }

        binding.editCourseButton.setOnClickListener {
            setButtonVisibility(0, false)
            setButtonVisibility(1, true)
        }

        binding.editCourseButtonNO.setOnClickListener {
            //TODO 바뀌었던 경로 원래대로 돌려주기

            setButtonVisibility(0, true)
            setButtonVisibility(1, false)
        }

        binding.editCourseButtonOK.setOnClickListener {
            //TODO 경로 바뀌고 추천 받기
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


        naverMap.addOnCameraChangeListener { _, _ ->
            if(visitMarkers.isNotEmpty() && buttonMarkers.isNotEmpty()) {
                val baseMarker = visitMarkers[0]
                val buttonMarker1 = buttonMarkers[0]
                val buttonMarker2 = buttonMarkers[1]
                val buttonMarker3 = buttonMarkers[2]

                adjustMarkerDistanceFromBaseMarker1(naverMap, baseMarker.position, buttonMarker1)
                adjustMarkerDistanceFromBaseMarker2(naverMap, baseMarker.position, buttonMarker2)
                adjustMarkerDistanceFromBaseMarker3(naverMap, baseMarker.position, buttonMarker3)
            }
        }


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
        if(visitData.name.isNotEmpty())
            visitMarker.icon = OverlayImage.fromResource(R.drawable.marker_image)
        else
            visitMarker.icon = OverlayImage.fromResource(R.drawable.marker_image_off)

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

        buttonMarkers.forEach{ it.map = null }
        buttonMarkers.clear()
    }


    /*지도의 버튼 관련 함수*/
    private fun addButtonMarker(visitData: VisitData, p: Int) {

        val buttonMarker1 = Marker().apply {
            icon = OverlayImage.fromResource(R.drawable.marker_delete_button)
            position = LatLng(visitData.lat, visitData.lng)
            map = naverMap
        }

        val buttonMarker2 = Marker().apply {
            icon = OverlayImage.fromResource(R.drawable.marker_edit_button)
            position = LatLng(visitData.lat, visitData.lng)
            map = naverMap
        }

        val buttonMarker3 = Marker().apply {
            icon = OverlayImage.fromResource(R.drawable.marker_search_button)
            position = LatLng(visitData.lat, visitData.lng)
            map = naverMap
        }

        buttonMarkers.add(buttonMarker1)
        buttonMarkers.add(buttonMarker2)
        buttonMarkers.add(buttonMarker3)

        buttonMarker1.setOnClickListener {
            val deleteVisitDialog = DeleteVisitDialog(requireContext())
            deleteVisitDialog.showDialog()
            deleteVisitDialog.onOkButtonClickListener = {
                deleteVisit(visitData, p)

                Toast.makeText(context, "방문지 삭제가 완료되었습니다.", Toast.LENGTH_SHORT).show()
                deleteOverlay()
                addVisitMarker(visitData)
                addCircleOverlay(visitData)
            }

            true
        }

        buttonMarker2.setOnClickListener {
            val editVisitDialog = EditVisitDialog(requireContext())
            editVisitDialog.showDialog()
            editVisitDialog.onOkButtonClickListener = {
                editVisitDialog(visitData, it, p)

                Toast.makeText(context, "방문지 수정이 완료되었습니다.", Toast.LENGTH_SHORT).show()
                deleteOverlay()
                addVisitMarker(visitData)
                addCircleOverlay(visitData)
            }

            true
        }

        buttonMarker3.setOnClickListener {
            Toast.makeText(context, "ASD", Toast.LENGTH_SHORT).show()
            true
        }

        adjustMarkerDistanceFromBaseMarker1(naverMap, LatLng(visitData.lat, visitData.lng), buttonMarker1)
        adjustMarkerDistanceFromBaseMarker2(naverMap, LatLng(visitData.lat, visitData.lng), buttonMarker2)
        adjustMarkerDistanceFromBaseMarker3(naverMap, LatLng(visitData.lat, visitData.lng), buttonMarker3)
    }

    private fun editVisitDialog(visitData: VisitData, visitName: String, position: Int) {
        val newVisitData = visitData.copy(name = visitName)

        VisitDatabase(requireContext()).updateVisitData(visitData, newVisitData)
        myCourseAdapter.visitdata[position] = newVisitData
        myCourseAdapter.notifyItemChanged(position * 2)
    }

    private fun deleteVisit(visitData: VisitData, position: Int) {

        //Index 기준으로 이전 방문지와 거리 비교
        fun checkDistanceBefore(index: Int): Boolean{
            return checkDistance(
                LatLng(visitData.lat, visitData.lng),
                LatLng(myCourseAdapter.visitdata[index - 1].lat, myCourseAdapter.visitdata[index - 1].lng),
                100
            )
        }
        //Index 기준으로 이후 방문지와 거리 비교
        fun checkDistanceAfter(index: Int): Boolean{
            return checkDistance(
                LatLng(visitData.lat, visitData.lng),
                LatLng(myCourseAdapter.visitdata[index + 1].lat, myCourseAdapter.visitdata[index + 1].lng),
                100
            )
        }

        if(myCourseAdapter.visitdata.size != 1) {
            if (position == 0)// 만약 해당 날짜의 첫번째 방문지인 경우
            {
                if(checkDistanceAfter(position))//만약 직후 방문지와 합병가능한 거리에 있다면
                {

                    //재할당을 위한 데이터를 만듬
                    val newVisitData = myCourseAdapter.visitdata[1].copy(
                        datetime = visitData.datetime,
                        staytime = getTimeDiff(visitData.datetime, myCourseAdapter.visitdata[1].endtime)
                    )

                    VisitDatabase(requireContext()).updateVisitData(myCourseAdapter.visitdata[1], newVisitData)

                    myCourseAdapter.visitdata[1] = newVisitData//다음 방문지에 이전 방문지 데이터를 합친다.
                    myCourseAdapter.visitdata.removeAt(0)//최초 방문지 데이터 삭제

                    //최초 방문지가 사라지는 경우, 경로의 [0] 과 [1] 사이의 데이터를 없애면 된다.
                    myCourseAdapter.pathdata.removeAt(0)

                    VisitDatabase(requireContext()).deleteVisitDataForEndtime(visitData.endtime)
                }
                else//합병이 불가능한 거리에 있다면
                {
                    myCourseAdapter.visitdata.removeAt(0)
                    myCourseAdapter.pathdata.removeAt(0)


                    VisitDatabase(requireContext()).deleteVisitData(visitData.datetime)
                }

            } else if (position == myCourseAdapter.visitdata.lastIndex)// 만약 해당 날짜의 마지막 방문지인 경우
            {
                if(checkDistanceBefore(position))//마지막과 직전 방문지가 합병 가능한 거리에 있다면
                {
                    //재할당을 위한 데이터를 만듬
                    val newVisitData = myCourseAdapter.visitdata[position - 1].copy(
                        endtime = visitData.endtime,
                        staytime = getTimeDiff(myCourseAdapter.visitdata[position - 1].datetime, visitData.endtime)
                    )

                    VisitDatabase(requireContext()).updateVisitData(myCourseAdapter.visitdata[position - 1], newVisitData)

                    myCourseAdapter.visitdata[position - 1] = newVisitData//직전 방문지에 이전 방문지 데이터를 합친다.
                    myCourseAdapter.visitdata.removeAt(position)//마지막 방문지 데이터 삭제

                    //최초 방문지가 사라지는 경우, 방문지 [position - 1] 과 [position] 사이의 경로 데이터를 없애면 된다.
                    myCourseAdapter.pathdata.removeAt(position - 1)

                    VisitDatabase(requireContext()).deleteVisitData(visitData.datetime)
                }
                else
                {
                    myCourseAdapter.visitdata.removeAt(position)
                    myCourseAdapter.pathdata.removeAt(position - 1)

                    VisitDatabase(requireContext()).deleteVisitData(visitData.datetime)
                }
            }
            else// 중간에 있는 방문지인 경우
            {
                if(checkDistanceBefore(position))
                {
                    //재할당을 위한 데이터를 만듬
                    val newVisitData = myCourseAdapter.visitdata[position - 1].copy(
                        endtime = visitData.endtime,
                        staytime = getTimeDiff(myCourseAdapter.visitdata[position - 1].datetime, visitData.endtime)
                    )

                    VisitDatabase(requireContext()).updateVisitData(myCourseAdapter.visitdata[position - 1], newVisitData)

                    myCourseAdapter.visitdata[position - 1] = newVisitData//직전 방문지에 이전 방문지 데이터를 합친다.
                    myCourseAdapter.visitdata.removeAt(position)//해당 위치 방문지 데이터 삭제

                    VisitDatabase(requireContext()).deleteVisitData(visitData.datetime)

                    val newPath: List<LocationData> =
                        myCourseAdapter.pathdata[position - 1].coordinates + myCourseAdapter.pathdata[position].coordinates

                    myCourseAdapter.pathdata[position - 1].coordinates.clear()
                    myCourseAdapter.pathdata[position - 1].coordinates.addAll(newPath)
                    myCourseAdapter.pathdata.removeAt(position)
                }
                else if(checkDistanceAfter(position))
                {
                    //재할당을 위한 데이터를 만듬
                    val newVisitData = myCourseAdapter.visitdata[position + 1].copy(
                        datetime = visitData.datetime,
                        staytime = getTimeDiff(visitData.datetime, myCourseAdapter.visitdata[position + 1].endtime)
                    )

                    VisitDatabase(requireContext()).updateVisitData(myCourseAdapter.visitdata[1], newVisitData)

                    myCourseAdapter.visitdata[position + 1] = newVisitData
                    myCourseAdapter.visitdata.removeAt(position)

                    VisitDatabase(requireContext()).deleteVisitDataForEndtime(visitData.endtime)

                    val newPath: List<LocationData> =
                        myCourseAdapter.pathdata[position - 1].coordinates + myCourseAdapter.pathdata[position].coordinates

                    myCourseAdapter.pathdata[position - 1].coordinates.clear()
                    myCourseAdapter.pathdata[position - 1].coordinates.addAll(newPath)
                    myCourseAdapter.pathdata.removeAt(position)
                }
                else //합병 x
                {
                    myCourseAdapter.visitdata.removeAt(position)

                    VisitDatabase(requireContext()).deleteVisitData(visitData.datetime)


                    val newPath: List<LocationData> =
                        myCourseAdapter.pathdata[position - 1].coordinates + myCourseAdapter.pathdata[position].coordinates

                    myCourseAdapter.pathdata[position - 1].coordinates.clear()
                    myCourseAdapter.pathdata[position - 1].coordinates.addAll(newPath)
                    myCourseAdapter.pathdata.removeAt(position)

                }
            }
        }
        myCourseAdapter.notifyDataSetChanged()
    }

    fun adjustMarkerDistanceFromBaseMarker3(naverMap: NaverMap, position: LatLng, buttonMarker: Marker) {
        val baseZoom = 15.0
        val scaleFactor = Math.pow(2.0, naverMap.cameraPosition.zoom - baseZoom)

        val baseMarkerPosition = position
        val density = Resources.getSystem().displayMetrics.density
        val distanceInMeters = 33 * density / scaleFactor
        val offsetInMeters = 10 * density / scaleFactor
        val offsetInDegreesLat = offsetInMeters / 111000
        val distanceFromBaseMarkerInDegreesLat = distanceInMeters / 111000  // 1 degree latitude is approximately 111000 meters
        val distanceFromBaseMarkerInDegreesLon = distanceInMeters / (Math.cos(Math.toRadians(baseMarkerPosition.latitude)) * 111000)  // Adjusting for longitude based on latitude

        val newPosition = LatLng(
            baseMarkerPosition.latitude + distanceFromBaseMarkerInDegreesLat - offsetInDegreesLat,
            baseMarkerPosition.longitude + distanceFromBaseMarkerInDegreesLon
        )
        buttonMarker.position = newPosition
    }


    private fun adjustMarkerDistanceFromBaseMarker2(naverMap: NaverMap, position: LatLng, buttonMarker: Marker) {
        val baseZoom = 15.0
        val scaleFactor = Math.pow(2.0, naverMap.cameraPosition.zoom - baseZoom)

        val baseMarkerPosition = position
        val density = Resources.getSystem().displayMetrics.density
        val distanceInMeters = 38 * density / scaleFactor
        val distanceFromBaseMarkerInDegrees = distanceInMeters / 111000  // 1 degree is approximately 111000 meters

        val newPosition = LatLng(baseMarkerPosition.latitude + distanceFromBaseMarkerInDegrees, baseMarkerPosition.longitude)
        buttonMarker.position = newPosition
    }

    private fun adjustMarkerDistanceFromBaseMarker1(naverMap: NaverMap, position: LatLng, buttonMarker: Marker) {
        val baseZoom = 15.0
        val scaleFactor = Math.pow(2.0, naverMap.cameraPosition.zoom - baseZoom)

        val baseMarkerPosition = position
        val density = Resources.getSystem().displayMetrics.density
        val distanceInMeters = 33 * density / scaleFactor
        val offsetInMeters = 10 * density / scaleFactor
        val offsetInDegreesLat = offsetInMeters / 111000
        val distanceFromBaseMarkerInDegreesLat = distanceInMeters / 111000  // 1 degree latitude is approximately 111000 meters
        val distanceFromBaseMarkerInDegreesLon = distanceInMeters / (Math.cos(Math.toRadians(baseMarkerPosition.latitude)) * 111000)  // Adjusting for longitude based on latitude

        val newPosition = LatLng(
            baseMarkerPosition.latitude + distanceFromBaseMarkerInDegreesLat - offsetInDegreesLat,
            baseMarkerPosition.longitude - distanceFromBaseMarkerInDegreesLon  // 경도를 감소시켜 왼쪽으로 이동
        )
        buttonMarker.position = newPosition
    }






    /*Camera 관련 함수*/
    /*____________________________________________________________________________________________*/
    private fun moveCameraForPath(pathPoints: MutableList<LocationData>){
        if(pathPoints.isNotEmpty()) {
            val minLat = pathPoints.minOf { it.lat }
            val maxLat = pathPoints.maxOf { it.lat }
            val minLng = pathPoints.minOf { it.lng }
            val maxLng = pathPoints.maxOf { it.lng }

            val bounds = LatLngBounds(LatLng(minLat , minLng), LatLng(maxLat, maxLng))
            naverMap.moveCamera(CameraUpdate.fitBounds(bounds, 350))
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
        setButtonVisibility(0, false)
        setButtonVisibility(1, false)

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
            naverMap.onSymbolClickListener = null

            /*RecyclerView Adapter 설정*/
            myCourseAdapter = MyCourseAdapter(requireContext(), this)
            val locationRecyclerView = binding.LocationRecyclerView
            locationRecyclerView.adapter = myCourseAdapter
            locationRecyclerView.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
            while (locationRecyclerView.itemDecorationCount > 0) {
                locationRecyclerView.removeItemDecorationAt(0)
            }
            //locationRecyclerView.itemAnimator = null
            locationRecyclerView.addItemDecoration(VerticalOverlapItemDecoration(42))

            setRecyclerView(selectedDate)

        }, year, month, day).show()
    }

    override fun onItemClick(data: Any, position: Int) {
        deleteOverlay()

        if (data is VisitData){

            setButtonVisibility(0, false)
            setButtonVisibility(1, false)

            naverMap.setOnSymbolClickListener { symbol ->
                if(checkDistance(LatLng(data.lat, data.lng), symbol.position,
                        LocationService.CHECKDISTANCE
                    )) {
                    val newVisitData = data.copy(name = symbol.caption, lat_set = symbol.position.latitude, lng_set = symbol.position.longitude)


                    deleteOverlay()
                    addVisitMarker(newVisitData)//선택한 newVisitData를 마커에 추가
                    addCircleOverlay(newVisitData)


                    VisitDatabase(requireContext()).updateVisitData(data, newVisitData)
                    updateVisitData(data, newVisitData)

                    Toast.makeText(context, "변경완료", Toast.LENGTH_SHORT).show()
                }
                else
                {
                    Toast.makeText(context, "허용범위 외부의 장소입니다.", Toast.LENGTH_SHORT).show()
                }
                true
            }

            addVisitMarker(data)
            addButtonMarker(data, position)
            addCircleOverlay(data)

            moveCameraForVisit(data)
            selectedVisitData = data
        }
        else if(data is PathData)
        {
            setButtonVisibility(0, true)
            setButtonVisibility(1, false)

            naverMap.onSymbolClickListener = null

            locationDataList = data.coordinates

            addPathOverlayForLoacation(data.coordinates)
            moveCameraForPath(data.coordinates)
        }
    }

    private fun updateVisitData(oldVisitData: VisitData, newVisitData: VisitData){

        for(i in myCourseAdapter.visitdata.indices){

            if(myCourseAdapter.visitdata[i] == oldVisitData) {
                myCourseAdapter.visitdata[i] = newVisitData
                myCourseAdapter.notifyItemChanged(i * 2)
                break
            }
        }
    }

    private fun setRecyclerView(selectedDate: String){
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
    }

    private fun getPreviousDate(selectedDate: String): String {
        val selectedDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val selectedDateObj = selectedDateFormat.parse(selectedDate)
        val calendar = Calendar.getInstance()
        calendar.time = selectedDateObj!!
        calendar.add(Calendar.DAY_OF_MONTH, -1)


        return selectedDateFormat.format(calendar.time)
    }

    private fun getTimeDiff(datetime1: String, datetime2: String): Int{
        val FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")

        val date1 = LocalDateTime.parse(datetime1, FORMAT)
        val date2 = LocalDateTime.parse(datetime2, FORMAT)

        return Duration.between(date1, date2).toMinutes().toInt()
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
naverMap.takeSnapshot {  }
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

    private fun getPlace2(){
        val googlePlacesApi = GooglePlacesApi.create()

        val call = googlePlacesApi.getNearbyPlaces("${selectedVisitData.lat},${selectedVisitData.lng}", 1000, key())
        call.enqueue(object : Callback<GooglePlacesApi.PlacesResponse> {
            override fun onResponse(call: Call<GooglePlacesApi.PlacesResponse>, response: Response<GooglePlacesApi.PlacesResponse>) {
                if(response.isSuccessful) {
                    Log.d("POI", response.message())
                    val places = response.body()?.results
                    places?.forEach {
                        val name = it.name
                        val lat = it.geometry.location.lat
                        val lng = it.geometry.location.lng

                        addPlaceMarker(
                            TMapAPIService.Poi(
                                it.name,
                                it.geometry.location.lat,
                                it.geometry.location.lng
                            )
                        )
                    }
                } else {
                    Log.d("POI실패2", response.message())

                }
            }

            override fun onFailure(call: Call<GooglePlacesApi.PlacesResponse>, t: Throwable) {
                Log.d("POI실패", t.message.toString())

            }
        })

    }

    private fun checkDistance(centerLatLng: LatLng, currentLatLng: LatLng, checkDistance: Int): Boolean{
        return centerLatLng.distanceTo(currentLatLng) <= checkDistance.toDouble()
    }

    private fun setButtonVisibility(t: Int, v: Boolean){//true면 활성화
        when(t){
            0 -> {
                if(v)
                {
                    binding.textInitButton.visibility = View.VISIBLE
                    binding.editCourseButton.visibility = View.VISIBLE
                }
                else{
                    binding.textInitButton.visibility = View.INVISIBLE
                    binding.editCourseButton.visibility = View.INVISIBLE
                }
            }

            1 -> {
                if(v)
                {
                    binding.textNoButton.visibility = View.VISIBLE
                    binding.editCourseButtonNO.visibility = View.VISIBLE
                    binding.textOkButton.visibility = View.VISIBLE
                    binding.editCourseButtonOK.visibility = View.VISIBLE
                }
                else
                {
                    binding.textNoButton.visibility = View.INVISIBLE
                    binding.editCourseButtonNO.visibility = View.INVISIBLE
                    binding.textOkButton.visibility = View.INVISIBLE
                    binding.editCourseButtonOK.visibility = View.INVISIBLE
                }
            }
        }
    }
}