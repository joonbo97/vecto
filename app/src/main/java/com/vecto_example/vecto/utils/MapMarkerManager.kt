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

    private var tedNaverClustering: TedNaverClustering<MyClusterItem>? = null

    interface OnClusterClickListener {
        fun onMarkerClick(visitData: VisitData, clusterItem: MyClusterItem, position: Int)

        fun onClusterClick(visitData: VisitData, cluster: Cluster<MyClusterItem>, position: Int)
    }

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

        tedNaverClustering?.clearItems()
        if(tedNaverClustering != null)
            Handler(Looper.getMainLooper()).post {
                //UI 갱신
            }

        tedNaverClustering = null
    }

}