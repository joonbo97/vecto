package com.vecto_example.vecto.utils

import android.content.Context
import android.graphics.Color
import android.graphics.PointF
import androidx.core.content.ContextCompat
import com.naver.maps.geometry.LatLng
import com.naver.maps.geometry.LatLngBounds
import com.naver.maps.map.CameraAnimation
import com.naver.maps.map.CameraUpdate
import com.naver.maps.map.NaverMap
import com.naver.maps.map.overlay.CircleOverlay
import com.naver.maps.map.overlay.OverlayImage
import com.naver.maps.map.overlay.PathOverlay
import com.vecto_example.vecto.R
import com.vecto_example.vecto.data.model.LocationData
import com.vecto_example.vecto.data.model.PathData
import com.vecto_example.vecto.data.model.VisitData
import com.vecto_example.vecto.retrofit.VectoService

class MapOverlayManager(private val context: Context, private val mapMarkerManager: MapMarkerManager, private val naverMap: NaverMap) {
    private val pathOverlays = mutableListOf<PathOverlay>()
    private val circleOverlays = mutableListOf<CircleOverlay>()


    /*   Overlay, Maker 삭제 함수   */
    fun deleteOverlay() {
        pathOverlays.forEach{ it.map = null }
        pathOverlays.clear()

        circleOverlays.forEach{ it.map = null }
        circleOverlays.clear()

        mapMarkerManager.deleteMarker()
    }

    fun deletePathOverlay() {
        pathOverlays.forEach{ it.map = null }
        pathOverlays.clear()
    }

    /*   경로 선 생성 함수   */
    fun addPathOverlay(pathPoints: MutableList<LatLng>){
        val pathOverlay = PathOverlay()

        if(pathPoints.size > 1) {
            pathOverlay.coords = pathPoints
            pathOverlay.width = 20
            pathOverlay.zIndex = 20
            pathOverlay.color = ContextCompat.getColor(context, R.color.vecto_pathcolor)
            pathOverlay.outlineColor = ContextCompat.getColor(context, R.color.vecto_pathcolor)
            pathOverlay.patternImage = OverlayImage.fromResource(R.drawable.pathoverlay_pattern)
            pathOverlay.patternInterval = 50
            pathOverlay.map = naverMap
            pathOverlays.add(pathOverlay)
        }
    }

    //Alpha 경로선 생성
    fun addAlphaPathOverlay(pathPoints: MutableList<LatLng>){
        val pathOverlay = PathOverlay()

        if(pathPoints.size > 1) {
            pathOverlay.coords = pathPoints
            pathOverlay.width = 20
            pathOverlay.color = Color.argb(255, 186, 198, 213)
            pathOverlay.outlineColor = Color.argb(255, 186, 198, 213)
            pathOverlay.patternImage = OverlayImage.fromResource(R.drawable.pathoverlay_pattern)
            pathOverlay.patternInterval = 50
            pathOverlay.map = naverMap
            pathOverlays.add(pathOverlay)
        }
    }

    /*   경로 표시   */
    fun addPathOverlayForLocation(pathPoints: MutableList<LocationData>) {
        val pathLatLng = mutableListOf<LatLng>()

        for (i in 0 until pathPoints.size) {
            pathLatLng.add(LatLng(pathPoints[i].lat, pathPoints[i].lng))
        }

        addPathOverlay(pathLatLng)
    }

    fun addPathOverlayForPathList(pathList: List<PathData>) {
        pathList.forEach{
            addPathOverlayForLocation(it.coordinates)
        }
    }

    fun addPathOverlayForAlpha(pathPoints: MutableList<LocationData>) {
        val pathLatLng = mutableListOf<LatLng>()

        for (i in 0 until pathPoints.size) {
            pathLatLng.add(LatLng(pathPoints[i].lat, pathPoints[i].lng))
        }

        addAlphaPathOverlay(pathLatLng)
    }

    /*   다음 게시물 Overlay 생성 함수   */
    fun addOverlayForPost(feedInfo: VectoService.FeedInfo) {
        deleteOverlay()

        for(i in 0 until feedInfo.visit.size)
            mapMarkerManager.addNumberMarker(feedInfo.visit[i])

        addPathOverlayForLocation(feedInfo.location.toMutableList())
    }

    fun addCircleOverlay(visitData: VisitData){
        val circleOverlay = CircleOverlay()
        circleOverlay.center = LatLng(visitData.lat, visitData.lng)
        circleOverlay.radius = 50.0 // radius 50m로 설정

        if(visitData.name.isEmpty()) {
            circleOverlay.color = Color.argb(20, 255, 0, 0) // 원의 색상 설정
        }
        else {
            circleOverlay.color = Color.argb(20, 0, 255, 0) // 원의 색상 설정
        }

        circleOverlay.map = naverMap

        circleOverlays.add(circleOverlay)
    }

    fun changePathColor() {
        for(i in 0 until pathOverlays.size) {
            pathOverlays[i].color = Color.argb(255, 186, 198, 213)
            pathOverlays[i].outlineColor = Color.argb(255, 186, 198, 213)
        }
    }

    fun setHighLightOverlay(pathDataList: MutableList<PathData>, itemPosition: Int){
        deletePathOverlay()

        pathDataList.forEachIndexed { index, pathData ->
            if(index == itemPosition)
                addPathOverlayForLocation(pathData.coordinates)
            else
                addPathOverlayForAlpha(pathData.coordinates)
        }
    }



    /*   카메라 함수   */
    fun moveCameraForPathOffset(pathPoints: MutableList<LocationData>, offset: Int){
        if(pathPoints.isNotEmpty()) {
            val minLat = pathPoints.minOf { it.lat }
            val maxLat = pathPoints.maxOf { it.lat }
            val minLng = pathPoints.minOf { it.lng }
            val maxLng = pathPoints.maxOf { it.lng }

            val bounds = LatLngBounds(LatLng(minLat , minLng), LatLng(maxLat, maxLng))
            naverMap.moveCamera(CameraUpdate.fitBounds(bounds, 50, 350, 50, offset))
        }
    }

    fun moveCameraForPathOffsetWithAnimation(pathPoints: MutableList<LocationData>, offset: Int){
        if(pathPoints.isNotEmpty()) {
            val minLat = pathPoints.minOf { it.lat }
            val maxLat = pathPoints.maxOf { it.lat }
            val minLng = pathPoints.minOf { it.lng }
            val maxLng = pathPoints.maxOf { it.lng }

            val bounds = LatLngBounds(LatLng(minLat , minLng), LatLng(maxLat, maxLng))
            naverMap.moveCamera(CameraUpdate.fitBounds(bounds, 50, 350, 50, offset).animate(CameraAnimation.Easing))
        }
    }

    fun moveCameraForPathUpload(pathPoints: MutableList<LocationData>){
        if(pathPoints.isNotEmpty()) {
            val minLat = pathPoints.minOf { it.lat }
            val maxLat = pathPoints.maxOf { it.lat }
            val minLng = pathPoints.minOf { it.lng }
            val maxLng = pathPoints.maxOf { it.lng }

            val bounds = LatLngBounds(LatLng(minLat, minLng), LatLng(maxLat, maxLng))
            naverMap.moveCamera(CameraUpdate.fitBounds(bounds, 50, 150, 50, 50))

        }
    }
    fun moveCameraForVisitOffset(visit: VisitData, offset: Int){
        val targetLatLng = LatLng(visit.lat_set, visit.lng_set)
        val offSet = PointF(0.0f, (-offset).toFloat())

        naverMap.moveCamera(CameraUpdate.scrollTo(targetLatLng))
        naverMap.moveCamera(CameraUpdate.zoomTo(18.0))
        naverMap.moveCamera(CameraUpdate.scrollBy(offSet))
    }

    fun moveCameraForVisitUpload(visit: VisitData){
        val targetLatLng = LatLng(visit.lat_set, visit.lng_set)

        naverMap.moveCamera(CameraUpdate.scrollTo(targetLatLng))
        naverMap.moveCamera(CameraUpdate.zoomTo(18.0))
    }

}