package com.vecto_example.vecto.ui.todaycourse

import android.Manifest
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.vecto_example.vecto.service.Actions
import com.vecto_example.vecto.service.LocationService
import com.vecto_example.vecto.data.model.LocationData
import com.vecto_example.vecto.data.model.LocationDatabase
import com.vecto_example.vecto.data.model.VisitData
import com.vecto_example.vecto.data.model.VisitDatabase
import com.vecto_example.vecto.dialog.EndServiceDialog
import com.vecto_example.vecto.dialog.StartServiceDialog
import com.vecto_example.vecto.ui.guide.activity.GuideActivity
import com.naver.maps.map.CameraUpdate
import com.naver.maps.map.LocationTrackingMode
import com.naver.maps.map.MapFragment
import com.naver.maps.map.NaverMap
import com.naver.maps.map.OnMapReadyCallback
import com.naver.maps.map.util.FusedLocationSource
import com.vecto_example.vecto.R
import com.vecto_example.vecto.databinding.FragmentTodayCourseBinding
import com.vecto_example.vecto.utils.MapMarkerManager
import com.vecto_example.vecto.utils.MapOverlayManager


class TodayCourseFragment : Fragment(), OnMapReadyCallback {
    private lateinit var binding: FragmentTodayCourseBinding

    private lateinit var todayCourseViewModel: TodayCourseViewModel

    private lateinit var mapMarkerManager: MapMarkerManager
    private lateinit var mapOverlayManager: MapOverlayManager

    //map설정 관련
    private lateinit var mapView: MapFragment
    private lateinit var locationSource: FusedLocationSource // 위치를 반환하는 구현체
    private lateinit var naverMap: NaverMap

    private lateinit var locationDataList: MutableList<LocationData>
    private lateinit var visitDataList: MutableList<VisitData>


    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1000
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        todayCourseViewModel = ViewModelProvider(this)[TodayCourseViewModel::class.java]
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentTodayCourseBinding.inflate(inflater, container, false)

        permissionCheck()

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initObservers()

        initButton()
    }

    private fun initButton() {
        val smallButton = binding.ButtonSmall
        val smallButtonText = binding.ButtonSmallText
        val largeButton = binding.ButtonLarge
        var moveValue = 0.0f
        var serviceFlag = isServiceRunning(LocationService::class.java)
        var onlyFlag = false


        val layoutListener = ViewTreeObserver.OnGlobalLayoutListener {
            // 버튼의 크기 정보
            if(!onlyFlag) {
                moveValue = largeButton.width.toFloat() / 2

                if (!serviceFlag) {
                    smallButton.animate().translationXBy(moveValue).duration = 0
                    smallButtonText.animate().translationXBy(moveValue).duration = 0
                    binding.TextForLargeRight.visibility = View.INVISIBLE
                    binding.TextForLargeLeft.visibility = View.VISIBLE
                    smallButtonText.text = "위치 수집 종료"
                }

                onlyFlag = true
            }
        }

        // ViewTreeObserver를 등록합니다.
        smallButton.viewTreeObserver.addOnGlobalLayoutListener(layoutListener)
        largeButton.viewTreeObserver.addOnGlobalLayoutListener(layoutListener)

        largeButton.setOnClickListener {
            if (serviceFlag) {
                val endServiceDialog = EndServiceDialog(requireContext())
                endServiceDialog.showDialog()
                endServiceDialog.onOkButtonClickListener = {
                    smallButton.animate().translationXBy(moveValue).duration = 500
                    smallButtonText.animate().translationXBy(moveValue).duration = 500
                    binding.TextForLargeRight.visibility = View.INVISIBLE
                    binding.TextForLargeLeft.visibility = View.VISIBLE

                    smallButtonText.text = "위치 수집 종료"
                    serviceFlag = false

                    val serviceIntent = Intent(requireContext(), LocationService::class.java)
                    serviceIntent.action = Actions.STOP_FOREGROUND
                    requireActivity().startService(serviceIntent)
                }
            } else {
                val startServiceDialog = StartServiceDialog(requireContext())
                startServiceDialog.showDialog()
                startServiceDialog.onOkButtonClickListener = {
                    smallButton.animate().translationXBy(-moveValue).duration = 500
                    smallButtonText.animate().translationXBy(-moveValue).duration = 500
                    binding.TextForLargeRight.visibility = View.VISIBLE
                    binding.TextForLargeLeft.visibility = View.INVISIBLE

                    smallButtonText.text = "위치 수집 시작"
                    serviceFlag = true

                    val serviceIntent = Intent(requireContext(), LocationService::class.java)
                    serviceIntent.action = Actions.START_FOREGROUND
                    requireActivity().startService(serviceIntent)
                }
            }

            smallButton.isEnabled = false
            largeButton.isEnabled = false

            Handler(Looper.getMainLooper()).postDelayed({ smallButton.isEnabled = true
                largeButton.isEnabled = true
            }, 1000)
        }
    }

    private fun initObservers() {

        todayCourseViewModel.isPermissionGained.observe(viewLifecycleOwner) {
            if(!it){
                showGuide()
            } else {
                initMap()
            }
        }

    }

    private fun permissionCheck() {
        if( ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_DENIED ||
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_DENIED ||
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_DENIED) {
            todayCourseViewModel.updatePermissionState(false)
        } else {
            todayCourseViewModel.updatePermissionState(true)
        }
    }

    /*   오늘 기록된 경로를 불러오는 함수   */
    private fun setVisitLocation() {
        locationDataList = LocationDatabase(requireContext()).getTodayLocationData()
        visitDataList = VisitDatabase(requireContext()).getTodayVisitData()

        mapOverlayManager.addPathOverlayForLocation(locationDataList)
        for (visitdatalist in visitDataList) {
            mapMarkerManager.addVisitMarkerBasic(visitdatalist)
        }

    }


    private fun initMap(){
        mapView = childFragmentManager.findFragmentById(R.id.naver_map_Today) as MapFragment?
            ?: MapFragment.newInstance().also {
                childFragmentManager.beginTransaction().add(R.id.naver_map_Today, it).commit()
            }
        locationSource = FusedLocationSource(this, LOCATION_PERMISSION_REQUEST_CODE)
        mapView.getMapAsync(this)
    }

    private fun showGuide(){
        val intent = Intent(context, GuideActivity::class.java) //Guide 화면으로 이동
        startActivity(intent)
    }

    override fun onMapReady(naverMap: NaverMap) {
        this.naverMap = naverMap
        naverMap.locationSource = locationSource
        naverMap.locationTrackingMode = LocationTrackingMode.Follow
        naverMap.moveCamera(CameraUpdate.zoomTo(18.0))
        naverMap.uiSettings.isZoomControlEnabled = false

        mapMarkerManager = MapMarkerManager(requireContext(), naverMap)
        mapOverlayManager = MapOverlayManager(requireContext(), mapMarkerManager, naverMap)

        setVisitLocation()
    }


    private fun isServiceRunning(serviceClass: Class<*>): Boolean {
        val manager = activity?.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        for (service in manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.name == service.service.className) {
                return true
            }
        }
        return false
    }

}