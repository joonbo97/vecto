package com.vecto_example.vecto.ui_bottom

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Resources
import android.graphics.Color
import android.graphics.PointF
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.vecto_example.vecto.LocationService
import com.vecto_example.vecto.VerticalOverlapItemDecoration
import com.vecto_example.vecto.data.LocationData
import com.vecto_example.vecto.data.LocationDatabase
import com.vecto_example.vecto.data.PathData
import com.vecto_example.vecto.data.VisitData
import com.vecto_example.vecto.data.VisitDatabase
import com.vecto_example.vecto.dialog.DeleteVisitDialog
import com.vecto_example.vecto.dialog.EditVisitDialog
import com.vecto_example.vecto.retrofit.TMapAPIService
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
import com.vecto_example.vecto.MyClusterItem
import com.vecto_example.vecto.R
import com.vecto_example.vecto.databinding.FragmentEditCourseBinding
import com.vecto_example.vecto.dialog.CalendarDialog
import com.vecto_example.vecto.dialog.PlacePopupWindow
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import ted.gun0912.clustering.geometry.TedLatLng
import ted.gun0912.clustering.naver.TedNaverClustering
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.Executors


class EditCourseFragment : Fragment(), OnMapReadyCallback, MyCourseAdapter.OnItemClickListener,
    CalendarDialog.OnDateSelectedListener {
    lateinit var binding: FragmentEditCourseBinding

    //map설정 관련
    private lateinit var mapView: MapFragment
    private lateinit var naverMap: NaverMap

    private lateinit var locationDataList: MutableList<LocationData>
    private lateinit var visitDataList: MutableList<VisitData>
    private lateinit var selectedVisitData: VisitData

    private var responsePathData = mutableListOf<LatLng>()

    private lateinit var myCourseAdapter: MyCourseAdapter

    private var path_position: Int = 0

    //overlay 관련
    private val visitMarkers = mutableListOf<Marker>()
    private val pathOverlays = mutableListOf<PathOverlay>()
    private val circleOverlays = mutableListOf<CircleOverlay>()

    private val placeMarkers = mutableListOf<Marker>()

    private var tedNaverClustering: TedNaverClustering<MyClusterItem>? = null

    private val buttonMarkers = mutableListOf<Marker>()

    private val placelist = mutableListOf<TMapAPIService.Poi>()

    //UI관련
    private lateinit var DateText: TextView

    var offset = 350

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentEditCourseBinding.inflate(inflater, container, false)


        DateText = binding.TextForLargeRight

        initMap()

        binding.CalendarLargeBoxImage.setOnClickListener {
            showDatePickerDialog(null)
        }

        //추천 경로 선택
        binding.editCourseButton.setOnClickListener {
            setButtonVisibility(0, false)
            setButtonVisibility(1, true)
            startLoading()

            for(i in 0 until pathOverlays.size) {
                pathOverlays[i].color = Color.argb(255, 186, 198, 213)
                pathOverlays[i].outlineColor = Color.argb(255, 186, 198, 213)
            }

            val Start = LatLng(locationDataList.first().lat, locationDataList.first().lng)
            val End = LatLng(locationDataList.last().lat, locationDataList.last().lng)

            //TMap API를 통한 최적 경로 불러오기
            val tMapAPIService = TMapAPIService.create()
            val call = tMapAPIService.getRecommendedRoute(1, TMapAPIService.key(), Start.latitude, Start.longitude, End.latitude, End.longitude, "WGS84GEO", "WGS84GEO", "출발지_이름", "도착지_이름", 0)

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
                        endLoading()

                    } else {
                        Toast.makeText(requireContext(), "경로 불러오기에 실패해였습니다.", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<TMapAPIService.GeoJsonResponse>, t: Throwable) {
                    Log.e("Retrofit", t.message.toString())
                    Toast.makeText(requireContext(), getString(R.string.APIFailToastMessage), Toast.LENGTH_SHORT).show()
                }
            })
        }

        binding.editCourseButtonNO.setOnClickListener {
            deleteOverlay()
            addPathOverlayForLoacation(locationDataList)

            Toast.makeText(requireContext(), "경로 변경이 취소되었습니다.", Toast.LENGTH_SHORT).show()

            setButtonVisibility(0, true)
            setButtonVisibility(1, false)
        }

        binding.editCourseButtonOK.setOnClickListener {
            startLoading()
            val startTime = LocalDateTime.parse(locationDataList.first().datetime, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"))

            //시작과 끝을 제외한 기존 경로 삭제
            LocationDatabase(requireContext()).deleteLocationDataBetween(locationDataList.first().datetime, locationDataList.last().datetime)

            myCourseAdapter.pathdata[path_position].coordinates.clear()

            //시작 시간은 시작 지점의 시간.

            responsePathData.forEachIndexed { index, point ->
                LocationDatabase(requireContext()).addLocationData(LocationData(startTime.plusSeconds(index.toLong() + 1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")), point.latitude, point.longitude))
                myCourseAdapter.pathdata[path_position].coordinates.add(LocationData(startTime.plusSeconds(index.toLong() + 1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")), point.latitude, point.longitude))
            }

            myCourseAdapter.pathdata[path_position].coordinates.add(0, locationDataList.first())
            myCourseAdapter.pathdata[path_position].coordinates.add(locationDataList.last())

            myCourseAdapter.notifyItemChanged(path_position)
            deleteOverlay()
            addPathOverlayForLoacation(myCourseAdapter.pathdata[path_position].coordinates)


            Toast.makeText(requireContext(), "해당 경로 변경이 완료되었습니다.", Toast.LENGTH_SHORT).show()

            setButtonVisibility(0, true)
            setButtonVisibility(1, false)
            endLoading()
        }

        binding.RefreshButton.setOnClickListener {
            setButtonVisibility(0, false)
            setButtonVisibility(1, false)
            binding.RefreshButton.visibility =View.GONE

            initRecyclerView()
            setRecyclerView(DateText.text.toString())
        }

        val topMargin = dpToPx(150f, requireContext()) // 상단에서 최소 150dp
        val bottomMargin = dpToPx(100f, requireContext()) // 하단에서 최소 100dp
        val screenHeight = Resources.getSystem().displayMetrics.heightPixels
        var lastY = 0f

        binding.slide.setOnTouchListener { view, event ->
            val layoutParams = binding.EditLayout.layoutParams as ConstraintLayout.LayoutParams
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    lastY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val newY = event.rawY
                    val deltaY = newY - lastY
                    var newHeight = layoutParams.height - deltaY.toInt()

                    // 상단 마진과 하단 마진을 고려하여 새로운 높이를 조정합니다.
                    newHeight = newHeight.coerceAtLeast(topMargin)
                    newHeight = newHeight.coerceAtMost(screenHeight - bottomMargin)

                    layoutParams.height = newHeight
                    offset = (newHeight / resources.displayMetrics.density).toInt()
                    binding.EditLayout.layoutParams = layoutParams
                    binding.EditLayout.requestLayout()

                    lastY = newY
                    true
                }
                MotionEvent.ACTION_UP -> {
                    view.performClick()
                    true
                }
                else -> false
            }
        }

        return binding.root
    }

    private fun dpToPx(dp: Float, context: Context): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp,
            context.resources.displayMetrics
        ).toInt()
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

        val selectedDate = arguments?.getString("selectedDateKey")
        showDatePickerDialog(selectedDate)
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
            pathOverlay.outlineColor = ContextCompat.getColor(requireContext(), R.color.vecto_pathcolor)
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

    private fun deleteOverlay() {
        pathOverlays.forEach{ it.map = null}
        pathOverlays.clear()

        visitMarkers.forEach { it.map = null }
        visitMarkers.clear()

        circleOverlays.forEach{ it.map = null }
        circleOverlays.clear()

        buttonMarkers.forEach{ it.map = null }
        buttonMarkers.clear()

        placeMarkers.forEach{ it.map = null }
        placeMarkers.clear()

        tedNaverClustering?.clearItems()
        if(tedNaverClustering != null)
            Handler(Looper.getMainLooper()).post {
            //UI 갱신
            }

        tedNaverClustering = null
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
            }

            true
        }

        buttonMarker3.setOnClickListener {
            placelist.clear()
            getPlace(1, visitData, p)
            startLoading()

            buttonMarker3.onClickListener = null

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

        deleteOverlay()
        addVisitMarker(visitData)
        addCircleOverlay(visitData)
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
        else
        {
            myCourseAdapter.visitdata.removeAt(position)

            VisitDatabase(requireContext()).deleteVisitData(visitData.datetime)

            deleteOverlay()
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
            val Offset = PointF(0.0f, (-offset).toFloat())
            naverMap.moveCamera(CameraUpdate.scrollBy(Offset))

        }
    }

    private fun moveCameraForVisit(visit: VisitData){
        val targetLatLng = LatLng(visit.lat_set, visit.lng_set)
        val Offset = PointF(0.0f, (-offset).toFloat())

        naverMap.moveCamera(CameraUpdate.scrollTo(targetLatLng))
        naverMap.moveCamera(CameraUpdate.zoomTo(18.0))
        naverMap.moveCamera(CameraUpdate.scrollBy(Offset))
    }

    private fun showDatePickerDialog(date: String?) {
        setButtonVisibility(0, false)
        setButtonVisibility(1, false)
        binding.RefreshButton.visibility = View.GONE

        if (date == null) {
            val calendarDialog = CalendarDialog(requireContext())
            calendarDialog.onDateSelectedListener = this
            calendarDialog.showDialog()
            DateText.text = "날짜를 선택해주세요."
            setBlock(true)
        }
        else
        {
            DateText.text = date
            initRecyclerView()
            setRecyclerView(date)
            setBlock(false)
        }
    }

    override fun onItemClick(data: Any, position: Int) {
        deleteOverlay()
        binding.RefreshButton.visibility = View.VISIBLE

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

                    addButtonMarker(data, position)
                    addCircleOverlay(data)

                    moveCameraForVisit(data)
                    selectedVisitData = data

                    Toast.makeText(context, "선택한 장소로 변경이 완료되었습니다.", Toast.LENGTH_SHORT).show()
                }
                else
                {
                    Toast.makeText(context, "허용범위 외부의 장소입니다.", Toast.LENGTH_SHORT).show()
                }
                true
            }

            selectedVisitData = data

            addVisitMarker(data)
            addButtonMarker(data, position)
            addCircleOverlay(data)

            moveCameraForVisit(data)
        }
        else if(data is PathData)
        {
            path_position = position
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
        myCourseAdapter.visitdata.clear()
        myCourseAdapter.pathdata.clear()

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

    private fun getTimeDiff(datetime1: String, datetime2: String): Int {
        val FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")

        val date1 = LocalDateTime.parse(datetime1, FORMAT)
        val date2 = LocalDateTime.parse(datetime2, FORMAT)

        return Duration.between(date1, date2).toMinutes().toInt()
    }

    /*주변 정보 얻는 함수*/
    private fun getPlace(page: Int, visitData: VisitData, p: Int){
        val tMapAPIService = TMapAPIService.create()
        Log.d("POI Name", "START")

        val call = tMapAPIService.searchNearbyPOI(1, getString(R.string.tmapcategory), TMapAPIService.key(), page, 1,100 , selectedVisitData.lat, selectedVisitData.lng)
        call.enqueue(object : Callback<TMapAPIService.POIResponse> {
            override fun onResponse(call: Call<TMapAPIService.POIResponse>, response: Response<TMapAPIService.POIResponse>) {
                if (response.isSuccessful) {
                    val pois = response.body()?.searchPoiInfo?.pois?.poi
                    val totalCount = response.body()?.searchPoiInfo?.totalCount ?: 0
                    val countPerPage = response.body()?.searchPoiInfo?.count ?: 1
                    var finishflag = false

                    if(pois != null) {
                        for (poi in pois) {
                            if(checkDistance(LatLng(selectedVisitData.lat, selectedVisitData.lng), LatLng(poi.frontLat, poi.frontLon), 100)){
                            Log.d("POI Name", poi.name)
                            Log.d("POI Latitude", poi.frontLat.toString())
                            Log.d("POI Longitude", poi.frontLon.toString())
                                placelist.add(poi)
                            }
                            else
                            {
                                finishflag = true
                                break
                            }
                        }
                        // 현재 페이지의 결과가 마지막이 아닌 경우 다음 페이지 요청
                        if(!finishflag) {
                            if (totalCount > page * countPerPage) {
                                getPlace(page + 1, visitData, p)
                                Log.d("POI ADD PAGE", page.toString())
                            }
                        }

                        if(finishflag || totalCount <= page * countPerPage )//마지막 작업이라면
                        {
                            setmarkerclustering(visitData, p)
                        }
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

    private fun setmarkerclustering(visitData: VisitData, p: Int) {
        val clusterItems = placelist.map { MyClusterItem(TedLatLng( it.frontLat, it.frontLon), it.name) }

        tedNaverClustering = TedNaverClustering.with<MyClusterItem>(requireContext(), naverMap)
            .customMarker { clusterItem ->
                Marker().apply {
                    position = LatLng(clusterItem.getTedLatLng().latitude, clusterItem.getTedLatLng().longitude)
                    icon = OverlayImage.fromResource(R.drawable.place_marker)
                    captionText = clusterItem.getTitle()
                }
            }
            .markerClickListener {
                editVisitDialog(visitData, it.getTitle(), p)
                deleteOverlay()
                addVisitMarker(visitData)
                addCircleOverlay(visitData)

                Toast.makeText(context, "수정이 왼료되었습니다.", Toast.LENGTH_SHORT).show()
            }
            .minClusterSize(2)
            .customCluster { cluster ->
                // 이 예제에서는 FrameLayout을 사용하여 이미지와 텍스트를 결합합니다.
                FrameLayout(requireContext()).apply {
                    val imageView = ImageView(requireContext()).apply {
                        setImageResource(R.drawable.place_marker_gray)
                    }

                    val textView = TextView(requireContext()).apply {
                        text = cluster.size.toString() // 클러스터에 포함된 마커의 수를 표시
                        setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
                        typeface = Typeface.create("lineseedkr_bd", Typeface.NORMAL)
                        setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                    }

                    // imageView와 textView를 FrameLayout에 추가합니다.
                    this.addView(imageView)
                    this.addView(textView)

                    // 레이아웃 파라미터 설정으로 위치 등을 조정할 수 있습니다.
                    // 예를 들어, textView의 위치를 이미지의 중앙에 오도록 설정할 수 있습니다.
                    val layoutParams = FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.WRAP_CONTENT,
                        FrameLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        gravity = Gravity.CENTER
                    }
                    textView.layoutParams = layoutParams
                }
            }
            .clusterClickListener { cluster ->
                // 클러스터 클릭 시 실행될 로직
                val names = cluster.items.map { it.getTitle() } // 클러스터에 포함된 모든 아이템의 이름을 가져옵니다.

                val placePopupWindow = PlacePopupWindow(requireContext())
                placePopupWindow.showPopupWindow(binding.RefreshButton, names) { name ->

                    editVisitDialog(visitData, name, p)
                    deleteOverlay()
                    addVisitMarker(visitData)
                    addCircleOverlay(visitData)

                    Toast.makeText(context, "수정이 왼료되었습니다.", Toast.LENGTH_SHORT).show()
                }
            }
            .make()


        // MyClusterItem 목록을 TedNaverClustering에 추가
        tedNaverClustering!!.addItems(clusterItems)
        endLoading()
        val executor= Executors.newSingleThreadExecutor()
        Handler(Looper.getMainLooper())
        executor.execute {
            tedNaverClustering!!.addItems(clusterItems)
            // 클러스터링을 실행
        }
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

    /*RecyclerView Adapter 설정*/
    private fun initRecyclerView(){
        myCourseAdapter = MyCourseAdapter(requireContext(), this)
        val locationRecyclerView = binding.LocationRecyclerView
        locationRecyclerView.adapter = myCourseAdapter
        locationRecyclerView.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        while (locationRecyclerView.itemDecorationCount > 0) {
            locationRecyclerView.removeItemDecorationAt(0)
        }
        locationRecyclerView.addItemDecoration(VerticalOverlapItemDecoration(42))
    }

    private fun startLoading(){
        binding.constraintProgress.visibility = View.VISIBLE
        binding.progressBar.visibility = View.VISIBLE
    }

    private fun endLoading(){
        binding.constraintProgress.visibility = View.GONE
        binding.progressBar.visibility = View.GONE
    }

    private fun setBlock(flag: Boolean){
        if(flag) {
            binding.constraintBlock.visibility = View.VISIBLE
            binding.EditLayout.visibility = View.INVISIBLE
            deleteOverlay()
        }
        else {
            binding.constraintBlock.visibility = View.GONE
            binding.EditLayout.visibility = View.VISIBLE
        }
    }

    override fun onDateSelected(date: String) {
        DateText.text = date
        naverMap.onSymbolClickListener = null

        initRecyclerView()

        setRecyclerView(date)
        setBlock(false)
    }
}