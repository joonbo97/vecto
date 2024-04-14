package com.vecto_example.vecto.utils

import com.naver.maps.geometry.LatLng
import ted.gun0912.clustering.clustering.TedClusterItem
import ted.gun0912.clustering.geometry.TedLatLng

class MyClusterItem(private val position: TedLatLng, private val title: String) : TedClusterItem {
    override fun getTedLatLng(): TedLatLng {
        return position
    }

    fun getTitle(): String {
        return title
    }
}
