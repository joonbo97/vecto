package com.example.vecto.ui_bottom

import android.app.DatePickerDialog
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
import com.example.vecto.R
import com.example.vecto.data.LocationData
import com.example.vecto.data.LocationDatabase
import com.example.vecto.data.PathData
import com.example.vecto.data.VisitData
import com.example.vecto.data.VisitDatabase
import com.example.vecto.databinding.FragmentEditCourseBinding
import com.example.vecto.editlocation.MyLocationAdapter
import com.naver.maps.geometry.LatLng
import com.naver.maps.map.CameraUpdate
import com.naver.maps.map.MapFragment
import com.naver.maps.map.NaverMap
import com.naver.maps.map.OnMapReadyCallback
import com.naver.maps.map.overlay.CircleOverlay
import com.naver.maps.map.overlay.Marker
import com.naver.maps.map.overlay.OverlayImage
import com.naver.maps.map.overlay.PathOverlay
import java.util.Calendar

class EditCourseFragment : Fragment(), OnMapReadyCallback {
    lateinit var binding: FragmentEditCourseBinding

    //map설정 관련
    private lateinit var mapView: MapFragment
    private lateinit var naverMap: NaverMap

    private lateinit var locationDataList: MutableList<LocationData>
    private lateinit var visitDataList: MutableList<VisitData>

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

            /*visitDataList = VisitDatabase(this).getAllVisitData().filter {
                it.datetime.startsWith(selectedDate)
            }.toMutableList()*/
        }, year, month, day).show()
    }

}