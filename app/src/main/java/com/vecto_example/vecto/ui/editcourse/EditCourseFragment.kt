package com.vecto_example.vecto.ui.editcourse

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Resources
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.vecto_example.vecto.LocationService
import com.vecto_example.vecto.VerticalOverlapItemDecoration
import com.vecto_example.vecto.data.model.LocationData
import com.vecto_example.vecto.data.model.LocationDatabase
import com.vecto_example.vecto.data.model.PathData
import com.vecto_example.vecto.data.model.VisitData
import com.vecto_example.vecto.data.model.VisitDatabase
import com.vecto_example.vecto.dialog.DeleteVisitDialog
import com.vecto_example.vecto.dialog.EditVisitDialog
import com.vecto_example.vecto.retrofit.TMapAPIService
import com.naver.maps.geometry.LatLng
import com.naver.maps.map.CameraUpdate
import com.naver.maps.map.MapFragment
import com.naver.maps.map.NaverMap
import com.naver.maps.map.OnMapReadyCallback
import com.vecto_example.vecto.MyClusterItem
import com.vecto_example.vecto.R
import com.vecto_example.vecto.data.repository.TMapRepository
import com.vecto_example.vecto.databinding.FragmentEditCourseBinding
import com.vecto_example.vecto.dialog.CalendarDialog
import com.vecto_example.vecto.popupwindow.PlacePopupWindow
import com.vecto_example.vecto.ui.editcourse.adapter.MyCourseAdapter
import com.vecto_example.vecto.utils.MapMarkerManager
import com.vecto_example.vecto.utils.MapOverlayManager
import ted.gun0912.clustering.clustering.Cluster
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class EditCourseFragment : Fragment(), OnMapReadyCallback, MyCourseAdapter.OnItemClickListener,
    CalendarDialog.OnDateSelectedListener, MapMarkerManager.OnButtonClickListener,
    MapMarkerManager.OnClusterClickListener {
    private lateinit var binding: FragmentEditCourseBinding

    private val editCourseViewModel: EditCourseViewModel by viewModels {
        EditCourseViewModelFactory(TMapRepository(TMapAPIService.create()))
    }

    //Overlay, Marker 관리
    private lateinit var mapMarkerManager: MapMarkerManager
    private lateinit var mapOverlayManager: MapOverlayManager

    //map 설정
    private lateinit var mapView: MapFragment
    private lateinit var naverMap: NaverMap

    //Adapter
    private lateinit var myCourseAdapter: MyCourseAdapter

    //데이터 관련
    private lateinit var locationDataList: MutableList<LocationData>    //선택된 경로 정보

    private var pathposition: Int = 0

    private val placeList = mutableListOf<TMapAPIService.Poi>()

    private var offset = 350

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentEditCourseBinding.inflate(inflater, container, false)

        initMap()
        initListeners()
        initSlide()

        return binding.root
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initSlide() {
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

    }

    private fun initListeners() {
        //날짜 선택
        binding.CalendarLargeBoxImage.setOnClickListener {
            editCourseViewModel.setDate(null)
        }

        //추천 경로 가져온 후 보여 줌
        binding.editCourseButton.setOnClickListener {
            editCourseViewModel.setButtonRecommend(false)
            editCourseViewModel.setButtonSelect(true)

            mapOverlayManager.changePathColor()

            editCourseViewModel.recommendRoute(locationDataList)

            editCourseViewModel.responsePathData.clear()
        }

        //추천 경로 변경 취소
        binding.editCourseButtonNO.setOnClickListener {
            mapOverlayManager.deleteOverlay()
            mapOverlayManager.addPathOverlayForLocation(locationDataList)

            Toast.makeText(requireContext(), "경로 변경이 취소되었습니다.", Toast.LENGTH_SHORT).show()

            editCourseViewModel.setButtonRecommend(true)
            editCourseViewModel.setButtonSelect(false)
        }

        //추천 경로로 변경
        binding.editCourseButtonOK.setOnClickListener {
            editCourseViewModel.overlayStart()

            val startTime = LocalDateTime.parse(locationDataList.first().datetime, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"))

            //시작과 끝을 제외한 기존 경로 삭제
            LocationDatabase(requireContext()).deleteLocationDataBetween(locationDataList.first().datetime, locationDataList.last().datetime)

            myCourseAdapter.pathdata[pathposition].coordinates.clear()

            //시작 시간은 시작 지점의 시간.

            editCourseViewModel.responsePathData.forEachIndexed { index, point ->
                LocationDatabase(requireContext()).addLocationData(LocationData(startTime.plusSeconds(index.toLong() + 1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")), point.latitude, point.longitude))
                myCourseAdapter.pathdata[pathposition].coordinates.add(LocationData(startTime.plusSeconds(index.toLong() + 1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")), point.latitude, point.longitude))
            }

            myCourseAdapter.pathdata[pathposition].coordinates.add(0, locationDataList.first())
            myCourseAdapter.pathdata[pathposition].coordinates.add(locationDataList.last())

            myCourseAdapter.notifyItemChanged(pathposition)
            mapOverlayManager.deleteOverlay()
            mapOverlayManager.addPathOverlayForLocation(myCourseAdapter.pathdata[pathposition].coordinates)


            Toast.makeText(requireContext(), "해당 경로 변경이 완료되었습니다.", Toast.LENGTH_SHORT).show()

            editCourseViewModel.setButtonRecommend(true)
            editCourseViewModel.setButtonSelect(false)

            editCourseViewModel.overlayDone()
        }

        //새로 고침
        binding.RefreshButton.setOnClickListener {
            editCourseViewModel.overlayStart()

            editCourseViewModel.setButtonRecommend(false)
            editCourseViewModel.setButtonSelect(false)
            binding.RefreshButton.visibility =View.GONE

            initRecyclerView()
            setRecyclerView(binding.TextForLargeRight.text.toString())
        }
    }

    private fun initObservers() {
        /*   날짜 선택 Observer   */
        editCourseViewModel.date.observe(viewLifecycleOwner){
            editCourseViewModel.setButtonRecommend(false)
            editCourseViewModel.setButtonSelect(false)
            binding.RefreshButton.visibility = View.GONE

            if (it == null) {
                val calendarDialog = CalendarDialog(requireContext())
                calendarDialog.onDateSelectedListener = this
                calendarDialog.showDialog()
                binding.TextForLargeRight.text = "날짜를 선택해주세요."
            }
            else
            {
                binding.TextForLargeRight.text = it
                initRecyclerView()
                setRecyclerView(it)
            }
        }

        /*   Block Observer   */
        editCourseViewModel.isBlock.observe(viewLifecycleOwner){
            if(it) {
                binding.constraintBlock.visibility = View.VISIBLE
                binding.EditLayout.visibility = View.INVISIBLE
                mapOverlayManager.deleteOverlay()
            }
            else {
                binding.constraintBlock.visibility = View.GONE
                binding.EditLayout.visibility = View.VISIBLE
            }
        }

        /*   API 오류 Observer   */
        editCourseViewModel.responseErrorLiveData.observe(viewLifecycleOwner){
            if(it == "FAIL")
                Toast.makeText(requireContext(), getText(R.string.APIFailToastMessage), Toast.LENGTH_SHORT).show()
            else if(it == "ERROR")
                Toast.makeText(requireContext(), getText(R.string.APIErrorToastMessage), Toast.LENGTH_SHORT).show()
        }

        /*   경로 추천 API 응답 Observer   */
        editCourseViewModel.responseRecommendLiveData.observe(viewLifecycleOwner){

            editCourseViewModel.responsePathData.add(editCourseViewModel.start)

            it.features.forEach { feature ->
                when (feature.geometry.type) {
                    "Point" -> {
                        val coordinate = feature.geometry.coordinates as List<Double>
                        val latLng = LatLng(coordinate[1], coordinate[0])
                        editCourseViewModel.responsePathData.add(latLng)
                    }

                    "LineString" -> {
                        val coordinates = feature.geometry.coordinates as List<List<Double>>
                        coordinates.forEach { coordinate ->
                            val latLng = LatLng(coordinate[1], coordinate[0])
                            editCourseViewModel.responsePathData.add(latLng)
                        }
                    }
                }
            }

            editCourseViewModel.responsePathData.add(editCourseViewModel.end)
            mapOverlayManager.addPathOverlay(editCourseViewModel.responsePathData)

            editCourseViewModel.overlayDone()
        }

        editCourseViewModel.isLoading.observe(viewLifecycleOwner){

            if(it) {
                binding.constraintProgress.visibility = View.VISIBLE
                binding.progressBar.visibility = View.VISIBLE
            }
            else{
                binding.constraintProgress.visibility = View.GONE
                binding.progressBar.visibility = View.GONE
            }

        }

        /*   주변 장소 검색 완료 Observer   */
        editCourseViewModel.isFinished.observe(viewLifecycleOwner){
            if(it) {    //종료 되었을 때
                mapMarkerManager.setMarkerClustering(editCourseViewModel.poiResponseList, editCourseViewModel.selectedVisitData, editCourseViewModel.position)
                editCourseViewModel.overlayDone()
            }
            else {      //추가가 더 있는 경우
                editCourseViewModel.searchNearbyPoi(editCourseViewModel.selectedVisitData, getString(R.string.tmapcategory), editCourseViewModel.position)
            }
        }

        editCourseViewModel.buttonSelect.observe(viewLifecycleOwner){
            if(it)
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

        editCourseViewModel.buttonRecommend.observe(viewLifecycleOwner){
            if(it){
                binding.textInitButton.visibility = View.VISIBLE
                binding.editCourseButton.visibility = View.VISIBLE
            } else {
                binding.textInitButton.visibility = View.INVISIBLE
                binding.editCourseButton.visibility = View.INVISIBLE
            }
        }
    }

    private fun dpToPx(dp: Float, context: Context): Int {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, context.resources.displayMetrics).toInt()
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

        mapMarkerManager = MapMarkerManager(requireContext(), naverMap)
        mapOverlayManager = MapOverlayManager(requireContext(), mapMarkerManager, naverMap)

        mapMarkerManager.buttonClickListener = this
        mapMarkerManager.clusterClickListener = this

        naverMap.addOnCameraChangeListener { _, _ ->
            mapMarkerManager.adjustAllButtonMarkers()
        }

        val selectedDate = arguments?.getString("selectedDateKey")
        if(selectedDate != null)
            editCourseViewModel.setDate(selectedDate)

        initObservers()
    }
    override fun onItemClick(data: Any, position: Int) {
        mapOverlayManager.deleteOverlay()
        binding.RefreshButton.visibility = View.VISIBLE
        editCourseViewModel.overlayStart()

        if (data is VisitData){

            editCourseViewModel.setButtonRecommend(false)
            editCourseViewModel.setButtonSelect(false)

            naverMap.setOnSymbolClickListener { symbol ->
                editCourseViewModel.overlayStart()

                if(editCourseViewModel.checkDistance(LatLng(data.lat, data.lng), symbol.position,
                        LocationService.CHECKDISTANCE
                    )) {
                    val newVisitData = data.copy(name = symbol.caption, lat_set = symbol.position.latitude, lng_set = symbol.position.longitude)


                    mapOverlayManager.deleteOverlay()
                    mapMarkerManager.addVisitMarkerBasic(newVisitData)//선택한 newVisitData를 마커에 추가


                    VisitDatabase(requireContext()).updateVisitData(data, newVisitData)
                    updateVisitData(data, newVisitData)

                    mapMarkerManager.addButtonMarker(data, position)
                    mapOverlayManager.addCircleOverlay(data)

                    mapOverlayManager.moveCameraForVisitOffset(newVisitData, offset)
                    editCourseViewModel.selectedVisitData = data

                    Toast.makeText(context, "선택한 장소로 변경이 완료되었습니다.", Toast.LENGTH_SHORT).show()
                }
                else
                {
                    Toast.makeText(context, "허용범위 외부의 장소입니다.", Toast.LENGTH_SHORT).show()
                }

                editCourseViewModel.overlayDone()

                true
            }

            editCourseViewModel.selectedVisitData = data

            mapMarkerManager.addVisitMarkerBasic(data)
            mapMarkerManager.addButtonMarker(data, position)
            mapOverlayManager.addCircleOverlay(data)

            mapOverlayManager.moveCameraForVisitOffset(data, offset)

        }
        else if(data is PathData)
        {
            pathposition = position
            editCourseViewModel.setButtonRecommend(true)
            editCourseViewModel.setButtonSelect(false)

            naverMap.onSymbolClickListener = null

            locationDataList = data.coordinates

            mapOverlayManager.addPathOverlayForLocation(data.coordinates)
            mapOverlayManager.moveCameraForPathOffset(data.coordinates, offset)
        }

        editCourseViewModel.overlayDone()
    }

    private fun setRecyclerView(selectedDate: String){
        myCourseAdapter.visitdata.clear()
        myCourseAdapter.pathdata.clear()

        val previousDate = editCourseViewModel.getPreviousDate(selectedDate)

        val filteredData = VisitDatabase(requireContext()).getAllVisitData().filter { visitData ->
            val visitDate = visitData.datetime.substring(0, 10)
            val endDate = visitData.endtime.substring(0, 10)
            visitDate == previousDate && endDate == selectedDate
        }

        val visitDataList = VisitDatabase(requireContext()).getAllVisitData().filter {
            it.datetime.startsWith(selectedDate)
        }.toMutableList()

        //종료 시간이 선택 날짜인 방문지 추가
        if(filteredData.isNotEmpty())
            visitDataList.add(0, filteredData[0])

        if(visitDataList.isNotEmpty()){
            //방문 장소가 있을 경우

            mapOverlayManager.deleteOverlay()

            //선택한 날짜의 방문지의 처음과 끝까지의 경로
            locationDataList = LocationDatabase(requireContext()).getBetweenLocationData(visitDataList.first().datetime, visitDataList.last().datetime)

            mapOverlayManager.addPathOverlayForLocation(locationDataList)
            mapOverlayManager.moveCameraForPathOffset(locationDataList, offset)


            val locationDataforPath = mutableListOf<LocationData>()
            var cnt = 1

            //location 첫 좌표 넣어줌.
            locationDataforPath.add(LocationData(visitDataList[0].datetime, visitDataList[0].lat_set, visitDataList[0].lng_set))

            for (visitdatalist in visitDataList){
                mapOverlayManager.addCircleOverlay(visitdatalist)
                mapMarkerManager.addVisitMarkerBasic(visitdatalist)

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
            mapOverlayManager.deleteOverlay()
        }

        editCourseViewModel.overlayDone()
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

    /*   override 관련 함수   */

    override fun onDateSelected(date: String) {
        editCourseViewModel.setDate(date)
        naverMap.onSymbolClickListener = null

        initRecyclerView()

        setRecyclerView(date)
    }

    override fun onEditVisit(visitData: VisitData, p: Int) {
        val editVisitDialog = EditVisitDialog(requireContext())
        editVisitDialog.showDialog()
        editVisitDialog.onOkButtonClickListener = {
            editVisitDialog(visitData, it, p)

            Toast.makeText(context, "방문지 수정이 완료되었습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDeleteVisit(visitData: VisitData, p: Int) {
        val deleteVisitDialog = DeleteVisitDialog(requireContext())
        deleteVisitDialog.showDialog()
        deleteVisitDialog.onOkButtonClickListener = {
            deleteVisit(visitData, p)

            Toast.makeText(context, "방문지 삭제가 완료되었습니다.", Toast.LENGTH_SHORT).show()
            mapOverlayManager.deleteOverlay()
            mapMarkerManager.addVisitMarkerBasic(visitData)
            mapOverlayManager.addCircleOverlay(visitData)
        }
    }

    override fun onSearchVisit(visitData: VisitData, p: Int) {
        placeList.clear()
        editCourseViewModel.searchNearbyPoi(visitData, getString(R.string.tmapcategory), p)
    }

    override fun onMarkerClick(visitData: VisitData, clusterItem: MyClusterItem, position: Int) {
        editVisitDialog(visitData, clusterItem.getTitle(), position)

        Toast.makeText(context, "수정이 완료되었습니다.", Toast.LENGTH_SHORT).show()
    }

    override fun onClusterClick(visitData: VisitData, cluster: Cluster<MyClusterItem>, position: Int) {
        val names = cluster.items.map { it.getTitle() } // 클러스터에 포함된 모든 아이템의 이름을 가져옵니다.

        val placePopupWindow = PlacePopupWindow(requireContext())
        placePopupWindow.showPopupWindow(binding.RefreshButton, names) { name ->

            editVisitDialog(visitData, name, position)

            Toast.makeText(context, "수정이 완료되었습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    /*   데이터 수정 관련   */
    private fun updateVisitData(oldVisitData: VisitData, newVisitData: VisitData){

        for(i in myCourseAdapter.visitdata.indices){

            if(myCourseAdapter.visitdata[i] == oldVisitData) {
                myCourseAdapter.visitdata[i] = newVisitData
                myCourseAdapter.notifyItemChanged(i * 2)
                break
            }
        }
    }

    private fun editVisitDialog(visitData: VisitData, visitName: String, position: Int) {
        editCourseViewModel.overlayStart()

        val newVisitData = visitData.copy(name = visitName)

        VisitDatabase(requireContext()).updateVisitData(visitData, newVisitData)
        myCourseAdapter.visitdata[position] = newVisitData
        myCourseAdapter.notifyItemChanged(position * 2)

        mapOverlayManager.deleteOverlay()
        mapMarkerManager.addVisitMarkerBasic(visitData)
        mapOverlayManager.addCircleOverlay(visitData)

        editCourseViewModel.overlayDone()
    }

    private fun deleteVisit(visitData: VisitData, position: Int) {
        editCourseViewModel.overlayStart()

        //Index 기준으로 이전 방문지와 거리 비교
        fun checkDistanceBefore(index: Int): Boolean{
            return editCourseViewModel.checkDistance(
                LatLng(visitData.lat, visitData.lng),
                LatLng(myCourseAdapter.visitdata[index - 1].lat, myCourseAdapter.visitdata[index - 1].lng),
                100
            )
        }
        //Index 기준으로 이후 방문지와 거리 비교
        fun checkDistanceAfter(index: Int): Boolean{
            return editCourseViewModel.checkDistance(
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
                        staytime = editCourseViewModel.getTimeDiff(visitData.datetime, myCourseAdapter.visitdata[1].endtime)
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
                        staytime = editCourseViewModel.getTimeDiff(myCourseAdapter.visitdata[position - 1].datetime, visitData.endtime)
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
                        staytime = editCourseViewModel.getTimeDiff(myCourseAdapter.visitdata[position - 1].datetime, visitData.endtime)
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
                        staytime = editCourseViewModel.getTimeDiff(visitData.datetime, myCourseAdapter.visitdata[position + 1].endtime)
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

            mapOverlayManager.deleteOverlay()
        }
        myCourseAdapter.notifyDataSetChanged()

        editCourseViewModel.overlayDone()
    }

}