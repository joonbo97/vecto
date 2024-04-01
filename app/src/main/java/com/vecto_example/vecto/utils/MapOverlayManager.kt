package com.vecto_example.vecto.utils

import android.content.Context
import android.graphics.PointF
import androidx.core.content.ContextCompat
import com.naver.maps.geometry.LatLng
import com.naver.maps.geometry.LatLngBounds
import com.naver.maps.map.CameraUpdate
import com.naver.maps.map.NaverMap
import com.naver.maps.map.overlay.OverlayImage
import com.naver.maps.map.overlay.PathOverlay
import com.vecto_example.vecto.R
import com.vecto_example.vecto.data.model.LocationData
import com.vecto_example.vecto.data.model.VisitData
import com.vecto_example.vecto.retrofit.VectoService

class MapOverlayManager(private val context: Context, private val mapMarkerManager: MapMarkerManager, private val naverMap: NaverMap) {
    private val pathOverlays = mutableListOf<PathOverlay>()


    /*   Overlay, Maker 삭제 함수   */
    fun deleteOverlay() {
        pathOverlays.forEach{ it.map = null}
        pathOverlays.clear()

        mapMarkerManager.deleteMarker()
    }

    /*   경로 선 생성 함수   */
    fun addPathOverlay(pathPoints: MutableList<LatLng>){
        val pathOverlay = PathOverlay()

        if(pathPoints.size > 1) {
            pathOverlay.coords = pathPoints
            pathOverlay.width = 20
            pathOverlay.color = ContextCompat.getColor(context, R.color.vecto_pathcolor)
            pathOverlay.outlineColor = ContextCompat.getColor(context, R.color.vecto_pathcolor)
            pathOverlay.patternImage = OverlayImage.fromResource(R.drawable.pathoverlay_pattern)
            pathOverlay.patternInterval = 50
            pathOverlay.map = naverMap
            pathOverlays.add(pathOverlay)
        }
    }

    /*   게시물 경로 표시   */
    fun addPathOverlayForLocation(pathPoints: MutableList<LocationData>) {
        val pathLatLng = mutableListOf<LatLng>()

        for (i in 0 until pathPoints.size) {
            pathLatLng.add(LatLng(pathPoints[i].lat, pathPoints[i].lng))
        }

        addPathOverlay(pathLatLng)
    }

    /*   다음 게시물 Overlay 생성 함수   */
    fun addOverlayForPost(feedInfo: VectoService.FeedInfoResponse) {
        deleteOverlay()

        for(i in 0 until feedInfo.visit.size)
            mapMarkerManager.addVisitMarker(feedInfo.visit[i])

        addPathOverlayForLocation(feedInfo.location.toMutableList())

        if(feedInfo.visit.size == 1)
        {
            moveCameraForVisit(feedInfo.visit[0])
        }
        else
        {
            moveCameraForPath(feedInfo.location.toMutableList())
        }
    }





    /*   카메라 함수   */
    fun moveCameraForPath(pathPoints: MutableList<LocationData>){
        if(pathPoints.isNotEmpty()) {
            val minLat = pathPoints.minOf { it.lat }
            val maxLat = pathPoints.maxOf { it.lat }
            val minLng = pathPoints.minOf { it.lng }
            val maxLng = pathPoints.maxOf { it.lng }

            val bounds = LatLngBounds(LatLng(minLat , minLng), LatLng(maxLat, maxLng))
            naverMap.moveCamera(CameraUpdate.fitBounds(bounds, 150, 200, 150, 150))
            val Offset = PointF(0.0f, (-50).toFloat())
            naverMap.moveCamera(CameraUpdate.scrollBy(Offset))
        }
    }

    fun moveCameraForVisit(visit: VisitData){
        val targetLatLng = LatLng(visit.lat_set, visit.lng_set)
        val Offset = PointF(0.0f, (-50).toFloat())

        naverMap.moveCamera(CameraUpdate.scrollTo(targetLatLng))
        naverMap.moveCamera(CameraUpdate.zoomTo(18.0))
        naverMap.moveCamera(CameraUpdate.scrollBy(Offset))
    }

}