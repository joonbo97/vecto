package com.example.vecto.ui_bottom

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.lifecycleScope
import com.example.vecto.LocationService
import com.example.vecto.MainActivity
import com.example.vecto.MainActivity.DataStoreUtils.myDataStore
import com.example.vecto.R
import com.example.vecto.databinding.FragmentTodayCourseBinding
import com.example.vecto.guide.GuideActivity
import com.naver.maps.map.CameraUpdate
import com.naver.maps.map.LocationTrackingMode
import com.naver.maps.map.MapFragment
import com.naver.maps.map.NaverMap
import com.naver.maps.map.OnMapReadyCallback
import com.naver.maps.map.util.FusedLocationSource
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch


class TodayCourseFragment : Fragment(), OnMapReadyCallback {
    private lateinit var binding: FragmentTodayCourseBinding
    private lateinit var mainDataStore: DataStore<Preferences>

    //map설정 관련
    private lateinit var mapView: MapFragment
    private lateinit var locationSource: FusedLocationSource // 위치를 반환하는 구현체
    private lateinit var naverMap: NaverMap

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
    ): View? {
        binding = FragmentTodayCourseBinding.inflate(inflater, container, false)

        lifecycleScope.launch {
            if(!isGuideFlag()){
                showGuide()
                saveGuideFlag(true)
            }
        }

        initMap()
        val smallButton = binding.ButtonSmall
        var asd = isServiceRunning(LocationService::class.java)
        if(!asd)
        {
            smallButton.animate().translationXBy(450f).duration = 0
            binding.TextForLargeRight.visibility = View.INVISIBLE
            binding.TextForLargeLeft.visibility = View.VISIBLE
            smallButton.text = "위치 수집 종료"
        }


        smallButton.setOnClickListener {
            if(asd) {
                it.animate().translationXBy(450f).duration = 500
                binding.TextForLargeRight.visibility = View.INVISIBLE
                binding.TextForLargeLeft.visibility = View.VISIBLE
                smallButton.text = "위치 수집 종료"
                asd = false
            }
            else {
                it.animate().translationXBy(-450f).duration = 500
                binding.TextForLargeRight.visibility = View.VISIBLE
                binding.TextForLargeLeft.visibility = View.INVISIBLE

                smallButton.text = "위치 수집 시작"
                asd = true
            }
        }

        return binding.root
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