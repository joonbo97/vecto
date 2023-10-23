package com.example.vecto.retrofit

import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query

interface VectoService {

    @POST("user")
    fun loginUser(
        @Body request: LoginRequest
    ): Call<ResponseBody>



    companion object {
        fun create(): VectoService {
            val retrofit = Retrofit.Builder()
                .baseUrl("https://vec-to.net/")
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            return retrofit.create(VectoService::class.java)
        }

    }

    data class LoginRequest (
        val userId: String,
        val userPw: String? = null,
        val fcmToken: String
    )
}