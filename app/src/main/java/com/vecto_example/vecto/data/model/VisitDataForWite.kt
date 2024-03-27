package com.vecto_example.vecto.data.model

data class VisitDataForWite(
    val datetime: String,
    val endtime: String,
    val lat: Double,
    val lng: Double,
    val lat_set: Double,
    val lng_set: Double,
    val staytime: Int,
    val name: String,
    val address: String
)
