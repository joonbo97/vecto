package com.vecto_example.vecto.utils

import android.content.Context
import android.content.res.Resources
import android.graphics.Typeface
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.naver.maps.geometry.LatLng
import com.naver.maps.map.NaverMap
import com.naver.maps.map.overlay.Marker
import com.naver.maps.map.overlay.OverlayImage
import com.vecto_example.vecto.MyClusterItem
import com.vecto_example.vecto.R
import com.vecto_example.vecto.data.model.VisitData
import com.vecto_example.vecto.retrofit.TMapAPIService
import ted.gun0912.clustering.clustering.Cluster
import ted.gun0912.clustering.geometry.TedLatLng
import ted.gun0912.clustering.naver.TedNaverClustering
import java.util.concurrent.Executors
import kotlin.math.pow

class MapMarkerManager(private val context: Context, private val naverMap: NaverMap) {
    private val visitMarkers = mutableListOf<Marker>()
    private val buttonMarkers = mutableListOf<Marker>()

    private var tedNaverClustering: TedNaverClustering<MyClusterItem>? = null

    interface OnButtonClickListener {
        fun onEditVisit(visitData: VisitData, p: Int)

        fun onDeleteVisit(visitData: VisitData, p: Int)

        fun onSearchVisit(visitData: VisitData, p: Int)
    }

    interface OnClusterClickListener {
        fun onMarkerClick(visitData: VisitData, clusterItem: MyClusterItem, position: Int)

        fun onClusterClick(visitData: VisitData, cluster: Cluster<MyClusterItem>, position: Int)
    }

    var buttonClickListener: OnButtonClickListener? = null
    var clusterClickListener: OnClusterClickListener? = null

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

    /*지도의 버튼 관련 함수*/
    fun addButtonMarker(visitData: VisitData, p: Int) {
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
            buttonClickListener?.onDeleteVisit(visitData, p)

            true
        }

        buttonMarker2.setOnClickListener {
            buttonClickListener?.onEditVisit(visitData, p)

            true
        }

        buttonMarker3.setOnClickListener {
            buttonClickListener?.onSearchVisit(visitData, p)

            buttonMarker3.onClickListener = null

            true
        }

        adjustMarkerDistanceFromBaseMarker1(naverMap, LatLng(visitData.lat, visitData.lng), buttonMarker1)
        adjustMarkerDistanceFromBaseMarker2(naverMap, LatLng(visitData.lat, visitData.lng), buttonMarker2)
        adjustMarkerDistanceFromBaseMarker3(naverMap, LatLng(visitData.lat, visitData.lng), buttonMarker3)
    }

    fun adjustAllButtonMarkers() {
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

    private fun adjustMarkerDistanceFromBaseMarker1(naverMap: NaverMap, position: LatLng, buttonMarker: Marker) {
        val baseZoom = 15.0
        val scaleFactor = Math.pow(2.0, naverMap.cameraPosition.zoom - baseZoom)

        val density = Resources.getSystem().displayMetrics.density
        val distanceInMeters = 33 * density / scaleFactor
        val offsetInMeters = 10 * density / scaleFactor
        val offsetInDegreesLat = offsetInMeters / 111000
        val distanceFromBaseMarkerInDegreesLat = distanceInMeters / 111000  // 1 degree latitude is approximately 111000 meters
        val distanceFromBaseMarkerInDegreesLon = distanceInMeters / (Math.cos(Math.toRadians(
            position.latitude
        )) * 111000)  // Adjusting for longitude based on latitude

        val newPosition = LatLng(
            position.latitude + distanceFromBaseMarkerInDegreesLat - offsetInDegreesLat,
            position.longitude - distanceFromBaseMarkerInDegreesLon  // 경도를 감소시켜 왼쪽으로 이동
        )
        buttonMarker.position = newPosition
    }

    private fun adjustMarkerDistanceFromBaseMarker2(naverMap: NaverMap, position: LatLng, buttonMarker: Marker) {
        val baseZoom = 15.0
        val scaleFactor = 2.0.pow(naverMap.cameraPosition.zoom - baseZoom)

        val density = Resources.getSystem().displayMetrics.density
        val distanceInMeters = 38 * density / scaleFactor
        val distanceFromBaseMarkerInDegrees = distanceInMeters / 111000  // 1 degree is approximately 111000 meters

        val newPosition = LatLng(position.latitude + distanceFromBaseMarkerInDegrees, position.longitude)
        buttonMarker.position = newPosition
    }

    private fun adjustMarkerDistanceFromBaseMarker3(naverMap: NaverMap, position: LatLng, buttonMarker: Marker) {
        val baseZoom = 15.0
        val scaleFactor = 2.0.pow(naverMap.cameraPosition.zoom - baseZoom)

        val density = Resources.getSystem().displayMetrics.density
        val distanceInMeters = 33 * density / scaleFactor
        val offsetInMeters = 10 * density / scaleFactor
        val offsetInDegreesLat = offsetInMeters / 111000
        val distanceFromBaseMarkerInDegreesLat = distanceInMeters / 111000  // 1 degree latitude is approximately 111000 meters
        val distanceFromBaseMarkerInDegreesLon = distanceInMeters / (Math.cos(Math.toRadians(
            position.latitude
        )) * 111000)  // Adjusting for longitude based on latitude

        val newPosition = LatLng(
            position.latitude + distanceFromBaseMarkerInDegreesLat - offsetInDegreesLat,
            position.longitude + distanceFromBaseMarkerInDegreesLon
        )
        buttonMarker.position = newPosition
    }

    fun setMarkerClustering(placelist: MutableList<TMapAPIService.Poi>, visitData: VisitData, p: Int) {
        val clusterItems = placelist.map { MyClusterItem(TedLatLng( it.frontLat, it.frontLon), it.name) }

        tedNaverClustering = TedNaverClustering.with<MyClusterItem>(context, naverMap)
            .customMarker { clusterItem ->
                Marker().apply {
                    position = LatLng(clusterItem.getTedLatLng().latitude, clusterItem.getTedLatLng().longitude)
                    icon = OverlayImage.fromResource(R.drawable.place_marker)
                    captionText = clusterItem.getTitle()
                }
            }
            .markerClickListener {
                clusterClickListener?.onMarkerClick(visitData, it, p)
            }
            .minClusterSize(2)
            .customCluster { cluster ->
                // FrameLayout을 사용하여 이미지와 텍스트를 결합합니다.
                FrameLayout(context).apply {
                    val imageView = ImageView(context).apply {
                        setImageResource(R.drawable.place_marker_gray)
                    }

                    val textView = TextView(context).apply {
                        text = cluster.size.toString() // 클러스터에 포함된 마커의 수를 표시
                        setTextColor(ContextCompat.getColor(context, R.color.white))
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
            .clusterClickListener {
                clusterClickListener?.onClusterClick(visitData, it, p)
            }
            .make()


        // MyClusterItem 목록을 TedNaverClustering에 추가
        tedNaverClustering!!.addItems(clusterItems)
        //endLoading()
        val executor= Executors.newSingleThreadExecutor()
        Handler(Looper.getMainLooper())
        executor.execute {
            tedNaverClustering!!.addItems(clusterItems)
            // 클러스터링을 실행
        }
    }

    fun deleteMarker() {
        visitMarkers.forEach { it.map = null }
        visitMarkers.clear()

        buttonMarkers.forEach{ it.map = null }
        buttonMarkers.clear()

        tedNaverClustering?.clearItems()
        if(tedNaverClustering != null)
            Handler(Looper.getMainLooper()).post {
                //UI 갱신
            }

        tedNaverClustering = null
    }

}