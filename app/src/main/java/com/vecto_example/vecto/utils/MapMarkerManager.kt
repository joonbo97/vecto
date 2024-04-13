package com.vecto_example.vecto.utils

import com.naver.maps.geometry.LatLng
import com.naver.maps.map.NaverMap
import com.naver.maps.map.overlay.Marker
import com.naver.maps.map.overlay.OverlayImage
import com.vecto_example.vecto.R
import com.vecto_example.vecto.data.model.VisitData

class MapMarkerManager(private val naverMap: NaverMap) {
    private val visitMarkers = mutableListOf<Marker>()

    fun addVisitMarkerBasic(visitData: VisitData){
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

    fun addVisitMarker(visitData: VisitData){
        val visitMarker = Marker()

        visitMarker.icon = OverlayImage.fromResource(getMarkerIcon())

        if(visitData.name.isNotEmpty()) {
            visitMarker.position = LatLng(visitData.lat_set, visitData.lng_set)
        }
        else {
            visitMarker.position = LatLng(visitData.lat, visitData.lng)
        }

        visitMarker.map = naverMap

        visitMarkers.add(visitMarker)
    }

    private fun getMarkerIcon(): Int{
        return when(visitMarkers.size){
            0 -> R.drawable.marker_number_1
            1 -> R.drawable.marker_number_2
            2 -> R.drawable.marker_number_3
            3 -> R.drawable.marker_number_4
            4 -> R.drawable.marker_number_5
            5 -> R.drawable.marker_number_6
            6 -> R.drawable.marker_number_7
            7 -> R.drawable.marker_number_8
            8 -> R.drawable.marker_number_9
            else -> R.drawable.marker_image
        }
    }

    fun deleteMarker() {
        visitMarkers.forEach { it.map = null }
        visitMarkers.clear()
    }

}