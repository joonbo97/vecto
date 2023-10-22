package com.example.vecto.data

data class RegisterData(
    val userId: String,
    val userPw: String,
    val provider: String,
    val nickName: String,
    val email: String,
    val requestType: String
)
