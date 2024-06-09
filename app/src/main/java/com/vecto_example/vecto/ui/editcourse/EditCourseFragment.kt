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
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.vecto_example.vecto.service.LocationService
import com.vecto_example.vecto.data.model.LocationData
import com.vecto_example.vecto.data.model.LocationDatabase
import com.vecto_example.vecto.data.model.PathData
import com.vecto_example.vecto.data.model.VisitData
import com.vecto_example.vecto.data.model.VisitDatabase
import com.vecto_example.vecto.dialog.EditVisitDialog
import com.vecto_example.vecto.retrofit.TMapAPIService
import com.naver.maps.geometry.LatLng
import com.naver.maps.map.CameraUpdate
import com.naver.maps.map.MapFragment
import com.naver.maps.map.NaverMap
import com.naver.maps.map.OnMapReadyCallback
import com.vecto_example.vecto.utils.MyClusterItem
import com.vecto_example.vecto.R
import com.vecto_example.vecto.data.repository.NaverRepository
import com.vecto_example.vecto.data.repository.TMapRepository
import com.vecto_example.vecto.databinding.FragmentEditCourseBinding
import com.vecto_example.vecto.dialog.CalendarDialog
import com.vecto_example.vecto.dialog.DeleteDialog
import com.vecto_example.vecto.popupwindow.PlacePopupWindow
import com.vecto_example.vecto.retrofit.NaverApiService
import com.vecto_example.vecto.ui.editcourse.adapter.MyCourseAdapter
import com.vecto_example.vecto.utils.DataBaseUtils
import com.vecto_example.vecto.utils.DateTimeUtils
import com.vecto_example.vecto.utils.MapMarkerManager
import com.vecto_example.vecto.utils.MapOverlayManager
import com.vecto_example.vecto.utils.ToastMessageUtils
import kotlinx.coroutines.launch
import ted.gun0912.clustering.clustering.Cluster
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class EditCourseFragment : Fragment(), OnMapReadyCallback, MyCourseAdapter.OnItemClickListener,
    CalendarDialog.OnDateSelectedListener, MapMarkerManager.OnClusterClickListener {
    private lateinit var binding: FragmentEditCourseBinding

    private val editCourseViewModel: EditCourseViewModel by viewModels {
        EditCourseViewModelFactory(TMapRepository(TMapAPIService.create()), NaverRepository(NaverApiService.create(), NaverApiService.createSearch()))
    }

    //Overlay, Marker 관리
    private lateinit var mapMarkerManager: MapMarkerManager
    private lateinit var mapOverlayManager: MapOverlayManager

    //DataBase
    private lateinit var visitDatabase: VisitDatabase
    private lateinit var locationDatabase: LocationDatabase

    //map 설정
    private lateinit var mapView: MapFragment
    private lateinit var naverMap: NaverMap

    //Adapter
    private lateinit var myCourseAdapter: MyCourseAdapter

    //Dialog
    private lateinit var calendarDialog: CalendarDialog

    //데이터 관련
    private lateinit var allPathData: MutableList<LocationData>

    private val placeList = mutableListOf<TMapAPIService.Poi>()

    private var offset = 350

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentEditCourseBinding.inflate(inflater, container, false)

        initMap()
        initRecyclerView()
        initListeners()
        initSlide()

        return binding.root
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        visitDatabase = VisitDatabase(context)
        locationDatabase = LocationDatabase(context)
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
            editCourseViewModel.setButtonVisibility(EditCourseViewModel.ButtonType.SELECT.name)

            mapOverlayManager.changePathColor()

            editCourseViewModel.recommendRoute(getSelectedPathData().coordinates, getSelectedVisitData().transportType)

            editCourseViewModel.responsePathData.clear()
        }

        //추천 경로 변경 취소
        binding.editCourseButtonNO.setOnClickListener {
            mapOverlayManager.deleteOverlay()
            mapOverlayManager.addPathOverlayForLocation(getSelectedPathData().coordinates)

            ToastMessageUtils.showToast(requireContext(), getString(R.string.course_edit_cancel))

            editCourseViewModel.setButtonVisibility(EditCourseViewModel.ButtonType.EDIT_PATH.name)
        }

        //추천 경로로 변경
        binding.editCourseButtonOK.setOnClickListener {
            editCourseViewModel.overlayStart()

            changeCourseData()

            editCourseViewModel.overlayDone()
        }

        //새로 고침
        binding.RefreshButton.setOnClickListener {
            /*   Map clear   */
            mapOverlayManager.deleteOverlay()

            mapOverlayManager.addPathOverlayForPathList(myCourseAdapter.pathdata)
            mapMarkerManager.addBasicMarkers(myCourseAdapter.visitdata)

            /*   Move Camera   */
            moveCamera()

            /*   UI clear   */
            editCourseViewModel.setButtonVisibility(EditCourseViewModel.ButtonType.NONE.name)
            myCourseAdapter.clearSelect()
        }
    }

    private fun moveCamera() {
        if(myCourseAdapter.visitdata.size == 1)
            mapOverlayManager.moveCameraForVisitOffset(myCourseAdapter.visitdata[0], offset)
        else
            mapOverlayManager.moveCameraForPathOffset(allPathData, dpToPx((offset + 20).toFloat(), requireContext()))
    }

    private fun changeCourseData() {    //추천 경로로 변경 함수
        val startTime = LocalDateTime.parse(getSelectedPathData().coordinates.first().datetime, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"))

        visitDatabase.updateVisitDataDistance(getSelectedPathData().coordinates.first().datetime, editCourseViewModel.totalDistance)   //total distance 변경
        myCourseAdapter.visitdata[myCourseAdapter.selectedPosition / 2].distance = editCourseViewModel.totalDistance

        //시작과 끝을 제외한 기존 경로 삭제
        locationDatabase.deleteLocationDataBetween(getSelectedPathData().coordinates.first().datetime, getSelectedPathData().coordinates.last().datetime)

        getSelectedPathData().coordinates.clear()

        //시작 시간은 시작 지점의 시간.
        editCourseViewModel.responsePathData.forEachIndexed { index, point ->
            locationDatabase.addLocationData(LocationData(startTime.plusSeconds(index.toLong() + 1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")), point.latitude, point.longitude))
            getSelectedPathData().coordinates.add(LocationData(startTime.plusSeconds(index.toLong() + 1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")), point.latitude, point.longitude))
        }

        getSelectedPathData().coordinates.add(0, getSelectedPathData().coordinates.first())
        getSelectedPathData().coordinates.add(getSelectedPathData().coordinates.last())

        myCourseAdapter.notifyItemChanged(myCourseAdapter.selectedPosition)
        mapOverlayManager.deleteOverlay()
        mapOverlayManager.addPathOverlayForLocation(getSelectedPathData().coordinates)

        ToastMessageUtils.showToast(requireContext(), getString(R.string.course_edit_success))

        editCourseViewModel.setButtonVisibility(EditCourseViewModel.ButtonType.EDIT_PATH.name)
    }

    override fun onPause() {
        super.onPause()

        if(::calendarDialog.isInitialized)
            calendarDialog.dismiss()
    }

    private fun initObservers() {
        /*   날짜 선택 Observer   */
        editCourseViewModel.date.observe(viewLifecycleOwner){
            editCourseViewModel.setButtonVisibility(EditCourseViewModel.ButtonType.NONE.name)

            if (it == null) {
                calendarDialog = CalendarDialog(requireContext())
                calendarDialog.onDateSelectedListener = this
                calendarDialog.showDialog()
                binding.TextForLargeRight.text = "날짜를 선택해주세요."
            } else {
                binding.TextForLargeRight.text = it
                setAdapterData(it)
            }
        }

        /*   API 오류 Observer   */
        editCourseViewModel.responseErrorLiveData.observe(viewLifecycleOwner){
            if(it == "FAIL")
                ToastMessageUtils.showToast(requireContext(), getString(R.string.APIFailToastMessage))
            else if(it == "ERROR")
                ToastMessageUtils.showToast(requireContext(), getString(R.string.APIErrorToastMessage))
        }

        /*   경로 추천 API 응답 Observer   */
        editCourseViewModel.responseRecommendLiveData.observe(viewLifecycleOwner){ tMapResponse ->

            editCourseViewModel.responsePathData.add(editCourseViewModel.start)

            tMapResponse.features.forEach { feature ->
                when (feature.geometry.type) {
                    "Point" -> {
                        (feature.geometry.coordinates as? List<*>)?.let { coordinateList ->
                            if(coordinateList.size >= 2 && coordinateList.all { it is Double }){
                                val latLng = LatLng(coordinateList[1] as Double, coordinateList[0] as Double)
                                editCourseViewModel.responsePathData.add(latLng)
                            }
                        }
                    }

                    "LineString" -> {
                        (feature.geometry.coordinates as? List<*>)?.forEach { coordinateItem ->
                            (coordinateItem as? List<*>)?.let { coordinate ->
                                if (coordinate.size >= 2 && coordinate.all { it is Double }) {
                                    val latLng = LatLng(coordinate[1] as Double, coordinate[0] as Double)
                                    editCourseViewModel.responsePathData.add(latLng)
                                }
                            }
                        }
                    }
                }
            }

            editCourseViewModel.responsePathData.add(editCourseViewModel.end)
            mapOverlayManager.addPathOverlay(editCourseViewModel.responsePathData)

            editCourseViewModel.overlayDone()
        }

        /*   ReverseGeocode 완료 Observer   */
        lifecycleScope.launch {
            editCourseViewModel.setVisitDataList.collect {
                updateVisitAddressData(it)
            }
        }

        /*   주변 장소 검색 완료 Observer   */
        editCourseViewModel.isFinished.observe(viewLifecycleOwner){
            if(it) {    //종료 되었을 때
                mapMarkerManager.setMarkerClustering(editCourseViewModel.poiResponseList)
                editCourseViewModel.overlayDone()
            }
            else {      //추가가 더 있는 경우
                editCourseViewModel.searchNearbyPoi(getSelectedVisitData(), getString(R.string.tmapcategory))
            }
        }


        /*   Button Visibility Observers   */
        editCourseViewModel.editVisitButton.observe(viewLifecycleOwner){
            if(it) {
                binding.visitEditButton.visibility = View.VISIBLE
                binding.visitDeleteButton.visibility = View.VISIBLE
                binding.visitSearchButton.visibility = View.VISIBLE

                binding.visitEditButtonText.visibility = View.VISIBLE
                binding.visitDeleteButtonText.visibility = View.VISIBLE
                binding.visitSearchButtonText.visibility = View.VISIBLE
            } else {
                binding.visitEditButton.visibility = View.INVISIBLE
                binding.visitDeleteButton.visibility = View.INVISIBLE
                binding.visitSearchButton.visibility = View.INVISIBLE

                binding.visitEditButtonText.visibility = View.INVISIBLE
                binding.visitDeleteButtonText.visibility = View.INVISIBLE
                binding.visitSearchButtonText.visibility = View.INVISIBLE

                naverMap.onSymbolClickListener = null
            }
        }

        editCourseViewModel.buttonSelect.observe(viewLifecycleOwner){
            if(it) {
                binding.textNoButton.visibility = View.VISIBLE
                binding.editCourseButtonNO.visibility = View.VISIBLE
                binding.textOkButton.visibility = View.VISIBLE
                binding.editCourseButtonOK.visibility = View.VISIBLE
            } else {
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

        /*   Loading Observer   */
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

        /*   Block Observer   */
        editCourseViewModel.isBlock.observe(viewLifecycleOwner){
            if(it) {
                binding.constraintBlock.visibility = View.VISIBLE

                binding.EditLayout.visibility = View.INVISIBLE
                binding.RefreshButton.visibility = View.INVISIBLE

                mapOverlayManager.deleteOverlay()
            }
            else {
                binding.constraintBlock.visibility = View.GONE

                binding.EditLayout.visibility = View.VISIBLE
                binding.RefreshButton.visibility = View.VISIBLE
            }
        }
    }

    private fun dpToPx(dp: Float, context: Context): Int {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, context.resources.displayMetrics).toInt()
    }

    private fun getSelectedPathData(): PathData {
        return myCourseAdapter.pathdata[myCourseAdapter.selectedPosition / 2]
    }

    private fun getSelectedVisitData(): VisitData {
        return myCourseAdapter.visitdata[myCourseAdapter.selectedPosition / 2]
    }

    private fun getSelectedItemPosition(): Int {
        return (myCourseAdapter.selectedPosition / 2)
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
        naverMap.isIndoorEnabled = true

        mapMarkerManager = MapMarkerManager(requireContext(), naverMap)
        mapOverlayManager = MapOverlayManager(requireContext(), mapMarkerManager, naverMap)

        mapMarkerManager.clusterClickListener = this

        val selectedDate = arguments?.getString("selectedDateKey")
        if(selectedDate != null)
            editCourseViewModel.setDate(selectedDate)

        initObservers()
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun setAdapterData(selectedDate: String){
        myCourseAdapter.visitdata.clear()
        myCourseAdapter.pathdata.clear()
        myCourseAdapter.clearSelect()

        mapOverlayManager.deleteOverlay()

        val previousDate = DateTimeUtils.getPreviousDate(selectedDate)

        val filteredData = visitDatabase.getAllVisitData().filter { visitData ->
            val visitDate = visitData.datetime.substring(0, 10)
            val endDate = visitData.endtime.substring(0, 10)
            visitDate == previousDate && endDate == selectedDate
        }

        val visitDataList = visitDatabase.getAllVisitData().filter {
            it.datetime.startsWith(selectedDate)
        }.toMutableList()

        //시작 날짜가 previousDate, 종료 시간이 datetime 방문지 추가
        if(filteredData.isNotEmpty())
            visitDataList.add(0, filteredData[0])

        if(visitDataList.isNotEmpty()){ //방문 장소가 있을 경우

            setAddress(visitDataList)

            //선택한 날짜의 방문지의 처음과 끝까지의 경로
            allPathData = locationDatabase.getBetweenLocationData(visitDataList.first().datetime, visitDataList.last().datetime)

            DataBaseUtils.checkLocationDataValidation(visitDataList, allPathData, locationDatabase)

            mapOverlayManager.addPathOverlayForLocation(allPathData)

            moveCamera()

            val locationDataForPath = mutableListOf<LocationData>()
            var cnt = 1

            mapMarkerManager.addBasicMarkers(visitDataList)
            myCourseAdapter.visitdata.addAll(visitDataList)

            var totalDistance = 0.0
            var lastLocation = allPathData.first()

            for (locationData in allPathData){
                if(visitDataList.size > 1) { //저장된 시각이 같으면 방문지점 도착경로 1 cycle 완료

                    if(visitDataList[cnt - 1].distance == 0){   //거리 설정 값이 없는 경우 설정 작업
                        totalDistance += editCourseViewModel.calculateDistance(LatLng(lastLocation.lat, lastLocation.lng), LatLng(locationData.lat, locationData.lng))
                        lastLocation = locationData
                    }

                    if (locationData.datetime == visitDataList[cnt].datetime) {
                        //다음 방문 지점의 경로 좌표에 도달하면, 방문지점 좌표까지 추가해서, adapter에 넘겨주고, 비운후 방문지점 좌표 추가해서 시작
                        locationDataForPath.add(locationData)
                        val pathData = PathData(locationDataForPath.toMutableList())
                        myCourseAdapter.pathdata.add(pathData)

                        locationDataForPath.clear()
                        locationDataForPath.add(locationData)

                        if(visitDataList[cnt - 1].distance == 0){
                            visitDatabase.updateVisitDataDistance(visitDataList[cnt - 1].datetime, if(totalDistance.toInt() == 0) 1 else totalDistance.toInt())
                        }

                        cnt++
                        totalDistance = 0.0

                        if (cnt == visitDataList.size) {
                            break
                        }
                    } else {
                        locationDataForPath.add(locationData)
                    }

                }

            }

            myCourseAdapter.notifyDataSetChanged()
        }

        editCourseViewModel.overlayDone()
    }

    private fun setAddress(visitDataList: MutableList<VisitData>){
        editCourseViewModel.reverseGeocode(visitDataList)
    }

    /*   RecyclerView 초기화 함수   */
    private fun initRecyclerView(){
        myCourseAdapter = MyCourseAdapter()
        myCourseAdapter.itemClickListener = this

        val locationRecyclerView = binding.LocationRecyclerView
        locationRecyclerView.adapter = myCourseAdapter
        locationRecyclerView.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        locationRecyclerView.itemAnimator = null

        locationRecyclerView.addItemDecoration(VerticalOverlapItemDecoration(60))
    }

    private fun editVisitButtonClick() {    //방문지 수정 선택
        val editVisitDialog = EditVisitDialog(requireContext())
        editVisitDialog.showDialog()
        editVisitDialog.onOkButtonClickListener = {
            updateVisitData(it, getSelectedVisitData().lat_set, getSelectedVisitData().lng_set)
        }
    }

    private fun deleteVisitButtonClick(visitData: VisitData, p: Int) {  //방문지 삭제 선택
        val deleteDialog = DeleteDialog(requireContext(), DeleteDialog.VISIT)
        deleteDialog.showDialog()
        deleteDialog.onOkButtonClickListener = {
            deleteVisit(visitData, p)

            ToastMessageUtils.showToast(requireContext(), getString(R.string.visit_delete_success))
            mapOverlayManager.deleteOverlay()
            mapMarkerManager.addVisitMarkerBasic(visitData)
            mapOverlayManager.addCircleOverlay(visitData)
        }
    }

    private fun searchVisitButtonClick(visitData: VisitData) {  //주변 검색 선택
        placeList.clear()
        editCourseViewModel.poiResponseList.clear()
        editCourseViewModel.searchNearbyPoi(visitData, getString(R.string.tmapcategory))
    }

    override fun onClusterClick(cluster: Cluster<MyClusterItem>) {
        val names = cluster.items.map { it.getTitle() } // 클러스터에 포함된 모든 아이템의 이름을 가져옵니다.

        val placePopupWindow = PlacePopupWindow(requireContext())
        placePopupWindow.showPopupWindow(binding.visitSearchButton, names) { name ->
            updateVisitData(name, cluster.position.latitude, cluster.position.longitude)
        }
    }

    /*   데이터 수정 관련   */
    private fun updateVisitData(name: String, lat: Double, lng: Double) {
        if(!editCourseViewModel.checkDistance(LatLng(getSelectedVisitData().lat, getSelectedVisitData().lng),
                LatLng(lat, lng), LocationService.CHECKDISTANCE)) {
            ToastMessageUtils.showToast(requireContext(), getString(R.string.visit_out_range))

            return
        }

        editCourseViewModel.overlayStart()

        val newVisitData = getSelectedVisitData().copy(name = name, lat_set = lat, lng_set = lng)

        visitDatabase.updateVisitData(getSelectedVisitData(), newVisitData)   //DB 데이터 변경
        myCourseAdapter.updateVisitData(newVisitData, myCourseAdapter.selectedPosition) //Adapter 데이터 변경

        mapOverlayManager.deleteOverlay()
        mapMarkerManager.addBasicMarkerWithPosition(getSelectedVisitData(), getSelectedItemPosition())
        mapOverlayManager.addCircleOverlay(getSelectedVisitData())
        mapOverlayManager.moveCameraForVisitOffset(getSelectedVisitData(), offset)  //Overlay 재설정

        editCourseViewModel.overlayDone()

        ToastMessageUtils.showToast(requireContext(), getString(R.string.visit_edit_success))
    }

    private fun updateVisitAddressData(visitDataList: MutableList<VisitData>) {

        visitDataList.forEach {
            visitDatabase.updateVisitDataAddress(it)
        }

        if(myCourseAdapter.visitdata.size == visitDataList.size){
            mapOverlayManager.deleteOverlay()

            mapOverlayManager.addPathOverlayForLocation(allPathData)
            mapMarkerManager.addBasicMarkers(visitDataList)

            myCourseAdapter.visitdata = visitDataList
            myCourseAdapter.notifyDataSetChanged()
        }
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

        fun mergeWithBeforeData(position: Int){ //이전 방문지와 합병
            locationDatabase.deleteLocationDataBetween(myCourseAdapter.visitdata[position - 1].datetime, myCourseAdapter.visitdata[position].endtime)
            locationDatabase.updateLocationData(myCourseAdapter.visitdata[position - 1].datetime, myCourseAdapter.visitdata[position].lat_set, myCourseAdapter.visitdata[position].lng_set)

            //재할당을 위한 데이터를 만듬
            val newVisitData = myCourseAdapter.visitdata[position - 1].copy(
                endtime = visitData.endtime,
                staytime = editCourseViewModel.getTimeDiff(myCourseAdapter.visitdata[position - 1].datetime, visitData.endtime),
                distance = myCourseAdapter.visitdata[position].distance //이전 distance 값 사용
            )

            visitDatabase.updateVisitData(myCourseAdapter.visitdata[position - 1], newVisitData)  //기존 데이터를 업데이트 데이터로 변경

            myCourseAdapter.visitdata[position - 1] = newVisitData//직전 방문지에 이전 방문지 데이터를 합친다.
            myCourseAdapter.visitdata.removeAt(position)//해당 위치 방문지 데이터 삭제

            visitDatabase.deleteVisitData(visitData.datetime)
        }

        fun mergeWithAfterData(position: Int){  //이후 방문지와 합병
            locationDatabase.deleteLocationDataBetween(myCourseAdapter.visitdata[position].datetime, myCourseAdapter.visitdata[position + 1].endtime)
            locationDatabase.updateLocationData(myCourseAdapter.visitdata[position].datetime, myCourseAdapter.visitdata[position + 1].lat_set, myCourseAdapter.visitdata[position + 1].lng_set)

            //재할당을 위한 데이터를 만듬
            val newVisitData = myCourseAdapter.visitdata[position + 1].copy(
                datetime = visitData.datetime,
                staytime = editCourseViewModel.getTimeDiff(visitData.datetime, myCourseAdapter.visitdata[position + 1].endtime)
                //position - 1 의 방문지 distance 를 변경해야하지만, 합쳐질 정도로 가까우므로 같은 값 사용
            )

            visitDatabase.updateVisitData(myCourseAdapter.visitdata[position + 1], newVisitData)  //기존 데이터를 업데이트 데이터로 변경

            myCourseAdapter.visitdata[position + 1] = newVisitData
            myCourseAdapter.visitdata.removeAt(position)

            visitDatabase.deleteVisitDataForEndtime(visitData.endtime)
        }

        fun mergePathData(position: Int){   //경로 합병
            val newPath: List<LocationData> =
                myCourseAdapter.pathdata[position - 1].coordinates + myCourseAdapter.pathdata[position].coordinates

            myCourseAdapter.pathdata[position - 1].coordinates.clear()
            myCourseAdapter.pathdata[position - 1].coordinates.addAll(newPath)
            myCourseAdapter.pathdata.removeAt(position)
        }

        if(myCourseAdapter.visitdata.size != 1) {
            if (position == 0)// 만약 해당 날짜의 첫번째 방문지인 경우
            {
                if(checkDistanceAfter(position))//만약 직후 방문지와 합병가능한 거리에 있다면
                {
                    mergeWithAfterData(position)

                    //최초 방문지가 사라지는 경우, 경로의 [0]데이터 삭제
                    myCourseAdapter.pathdata.removeAt(position)
                }
                else//합병이 불가능한 거리에 있다면
                {
                    myCourseAdapter.visitdata.removeAt(0)
                    myCourseAdapter.pathdata.removeAt(0)

                    visitDatabase.deleteVisitData(visitData.datetime)
                }

            }
            else if (position == myCourseAdapter.visitdata.lastIndex)// 만약 해당 날짜의 마지막 방문지인 경우
            {
                if(checkDistanceBefore(position))//마지막과 직전 방문지가 합병 가능한 거리에 있다면
                {
                    mergeWithBeforeData(position)

                    myCourseAdapter.pathdata.removeAt(position - 1)
                }
                else
                {
                    if(myCourseAdapter.visitdata[position - 1].distance != 0)
                        visitDatabase.updateVisitDataDistance(myCourseAdapter.visitdata[position - 1].datetime, 0)    //새로운 방문지 추가를 대비하여 초기화

                    myCourseAdapter.visitdata.removeAt(position)
                    myCourseAdapter.pathdata.removeAt(position - 1)

                    visitDatabase.deleteVisitData(visitData.datetime)
                }
            }
            else// 중간에 있는 방문지인 경우
            {
                if(checkDistanceBefore(position))
                {
                    mergeWithBeforeData(position)

                    mergePathData(position)
                }
                else if(checkDistanceAfter(position))
                {
                    mergeWithAfterData(position)

                    mergePathData(position)
                }
                else //합병 x
                {
                    val newDistance = myCourseAdapter.visitdata[position - 1].distance + myCourseAdapter.visitdata[position].distance

                    visitDatabase.updateVisitDataDistance(myCourseAdapter.visitdata[position - 1].datetime, newDistance)

                    myCourseAdapter.visitdata[position - 1].distance = newDistance
                    myCourseAdapter.visitdata.removeAt(position)

                    visitDatabase.deleteVisitData(visitData.datetime)

                    mergePathData(position)
                }
            }
        }
        else
        {
            myCourseAdapter.visitdata.removeAt(position)

            visitDatabase.deleteVisitData(visitData.datetime)

            mapOverlayManager.deleteOverlay()
        }

        myCourseAdapter.notifyDataSetChanged()

        editCourseViewModel.overlayDone()
    }

    /*   override 관련 함수   */
    override fun onDateSelected(date: String) {
        editCourseViewModel.setDate(date)
        naverMap.onSymbolClickListener = null
    }

    //Cluster 마커 클릭
    override fun onMarkerClick(clusterItem: MyClusterItem) {
        updateVisitData(clusterItem.getTitle(), clusterItem.getTedLatLng().latitude, clusterItem.getTedLatLng().longitude)
    }

    override fun onVisitItemClick() {
        mapOverlayManager.deleteOverlay()
        editCourseViewModel.overlayStart()

        /*   Button Visibility 초기화   */
        editCourseViewModel.setButtonVisibility(EditCourseViewModel.ButtonType.EDIT_VISIT.name)

        naverMap.setOnSymbolClickListener { symbol ->
            updateVisitData(symbol.caption, symbol.position.latitude, symbol.position.longitude)
            true
        }

        binding.visitEditButton.setOnClickListener {
            editVisitButtonClick()
        }

        binding.visitDeleteButton.setOnClickListener {
            deleteVisitButtonClick(getSelectedVisitData(), getSelectedItemPosition())
        }

        binding.visitSearchButton.setOnClickListener {
            searchVisitButtonClick(getSelectedVisitData())
        }

        mapMarkerManager.addBasicMarkerWithPosition(getSelectedVisitData(), getSelectedItemPosition())
        mapOverlayManager.addCircleOverlay(getSelectedVisitData())

        mapOverlayManager.moveCameraForVisitOffset(getSelectedVisitData(), offset)

        editCourseViewModel.overlayDone()
    }

    override fun onPathItemClick() {
        mapOverlayManager.deleteOverlay()
        editCourseViewModel.overlayStart()

        editCourseViewModel.setButtonVisibility(EditCourseViewModel.ButtonType.EDIT_PATH.name)

        mapOverlayManager.addPathOverlayForLocation(getSelectedPathData().coordinates)
        mapOverlayManager.moveCameraForPathOffset(getSelectedPathData().coordinates, dpToPx((offset + 20).toFloat(), requireContext()))

        editCourseViewModel.overlayDone()
    }

    override fun onPathTypeClick(type: String) {
        visitDatabase.updateVisitDataType(getSelectedVisitData().datetime, type)

        myCourseAdapter.successChangeType(type, getSelectedItemPosition())
    }

}