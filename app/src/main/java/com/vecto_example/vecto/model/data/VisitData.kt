package com.vecto_example.vecto.model.data

//datetime: 방문시각 endtime: 방문종료시각,  latlng: 백그라운드에서 측정한 방문 좌표, latlng_set: 사용자가 설정한 방문 좌표, staytime: 머무른 시간, name: 방문 장소명
data class VisitData (
    val datetime: String,
    val endtime: String,
    val lat: Double,
    val lng: Double,
    val lat_set: Double,
    val lng_set: Double,
    val staytime: Int,
    val name: String
)