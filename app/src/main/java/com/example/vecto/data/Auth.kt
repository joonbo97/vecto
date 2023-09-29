package com.example.vecto.data

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.naver.maps.geometry.LatLng

object Auth {
    private val _coin: MutableLiveData<Int> = MutableLiveData(0)
    val coin: LiveData<Int> = _coin

    fun setCoin(coinValue: Int) {
        _coin.value = coinValue
    }

    val pathPoints: MutableList<LatLng> = mutableListOf()
}