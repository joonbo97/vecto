package com.vecto_example.vecto.ui_bottom

import android.Manifest
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getColor
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.lifecycleScope
import com.vecto_example.vecto.Actions
import com.vecto_example.vecto.LocationService
import com.vecto_example.vecto.MainActivity
import com.vecto_example.vecto.MainActivity.DataStoreUtils.myDataStore
import com.vecto_example.vecto.data.model.LocationData
import com.vecto_example.vecto.data.model.LocationDatabase
import com.vecto_example.vecto.data.model.VisitData
import com.vecto_example.vecto.data.model.VisitDatabase
import com.vecto_example.vecto.dialog.EndServiceDialog
import com.vecto_example.vecto.dialog.StartServiceDialog
import com.vecto_example.vecto.ui.guide.activity.GuideActivity
import com.naver.maps.geometry.LatLng
import com.naver.maps.map.CameraUpdate
import com.naver.maps.map.LocationTrackingMode
import com.naver.maps.map.MapFragment
import com.naver.maps.map.NaverMap
import com.naver.maps.map.OnMapReadyCallback
import com.naver.maps.map.overlay.Marker
import com.naver.maps.map.overlay.OverlayImage
import com.naver.maps.map.overlay.PathOverlay
import com.naver.maps.map.util.FusedLocationSource
import com.vecto_example.vecto.R
import com.vecto_example.vecto.databinding.FragmentTodayCourseBinding
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch


class TodayCourseFragment : Fragment(), OnMapReadyCallback {
    private lateinit var binding: FragmentTodayCourseBinding
    private lateinit var mainDataStore: DataStore<Preferences>

    //map설정 관련
    private lateinit var mapView: MapFragment
    private lateinit var locationSource: FusedLocationSource // 위치를 반환하는 구현체
    private lateinit var naverMap: NaverMap

    private lateinit var locationDataList: MutableList<LocationData>
    private lateinit var visitDataList: MutableList<VisitData>

    //overlay 관련
    private val visitMarkers = mutableListOf<Marker>()
    private val pathOverlays = mutableListOf<PathOverlay>()

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1000
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if(context is MainActivity)
            mainDataStore = MainActivity().myDataStore
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentTodayCourseBinding.inflate(inflater, container, false)

        lifecycleScope.launch {

            if(!isGuideFlag() ||
                ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_DENIED ||
                ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_DENIED ||
                ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_DENIED){
                showGuide()
                saveGuideFlag(true)
            }
            else
            {
                initMap()
            }
        }

        return binding.root
    }

    var moveValue: Float = 0.0f
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val smallButton = binding.ButtonSmall
        val largeButton = binding.ButtonLarge
        var serviceFlag = isServiceRunning(LocationService::class.java)
        var onlyFlag: Boolean = false


        val layoutListener = ViewTreeObserver.OnGlobalLayoutListener {
            // 버튼의 크기 정보를 여기에서 가져옵니다.
            if(onlyFlag == false) {
                moveValue = largeButton.width.toFloat() - smallButton.width.toFloat() - 50f
                Log.d("TEST", "movevalue : $moveValue")
                Log.d("TEST", "largeButton : ${largeButton.width.toFloat()}")
                Log.d("TEST", "smallButton : ${smallButton.width.toFloat()}")

                if (!serviceFlag) {
                    smallButton.animate().translationXBy(moveValue).duration = 0
                    binding.TextForLargeRight.visibility = View.INVISIBLE
                    binding.TextForLargeLeft.visibility = View.VISIBLE
                    smallButton.text = "위치 수집 종료"
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
                    binding.TextForLargeRight.visibility = View.INVISIBLE
                    binding.TextForLargeLeft.visibility = View.VISIBLE

                    smallButton.text = "위치 수집 종료"
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
                    binding.TextForLargeRight.visibility = View.VISIBLE
                    binding.TextForLargeLeft.visibility = View.INVISIBLE

                    smallButton.text = "위치 수집 시작"
                    serviceFlag = true

                    val serviceIntent = Intent(requireContext(), LocationService::class.java)
                    serviceIntent.action = Actions.START_FOREGROUND
                    requireActivity().startService(serviceIntent)
                }
            }

            smallButton.isEnabled = false
            largeButton.isEnabled = false

            Handler(Looper.getMainLooper()).postDelayed({
                smallButton.isEnabled = true
                largeButton.isEnabled = true
            }, 1000)
        }

    }

    private fun setVistiLoaction() {
        locationDataList = LocationDatabase(requireContext()).getTodayLocationData()
        visitDataList = VisitDatabase(requireContext()).getTodayVisitData()

        addPathOverlayForLoacation(locationDataList)
        for (visitdatalist in visitDataList) {
            addVisitMarker(visitdatalist)
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

    private object PreferencesKeys{
        val guideFlag = booleanPreferencesKey("guide_flag")
    }

    private suspend fun saveGuideFlag(value: Boolean){
        mainDataStore.edit{preferences ->
            preferences[PreferencesKeys.guideFlag] = value
        }
    }

    private suspend fun isGuideFlag(): Boolean{
        val preferences = mainDataStore.data.first()
        return preferences[PreferencesKeys.guideFlag] ?: false
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

        setVistiLoaction()
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
            pathOverlay.color = getColor(requireContext(), R.color.vecto_pathcolor)
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

}