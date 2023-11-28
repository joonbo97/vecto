package com.vecto_example.vecto.model.data

data class NotificationData(
    val datetime: String,
    val feedId: Int,
    val text: String,
    val showFlag: Int //0이면 확인 안한것, 1이면 확인한 데이터
)
data class NotificationDataResult(
    val id: Int,
    val datetime: String,
    val feedId: Int,
    val text: String,
    val showFlag: Int
)

