package com.example.vecto

interface LocationServiceCallback {
    fun onLocationUpdate(latitude: Double, longitude: Double)
}