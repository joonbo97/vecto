package com.example.vecto.retrofit

import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface TMapAPIService {
    @POST("routes/pedestrian")
    fun getRecommendedRoute(
        @Query("version") version: Int,
        @Query("appKey") appKey: String,
        @Query("startY") startY: Double,
        @Query("startX") startX: Double,
        @Query("endY") endY: Double,
        @Query("endX") endX: Double,
        @Query("reqCoordType") reqCoordType: String,
        @Query("resCoordType") resCoordType: String,
        @Query("startName") startName: String,
        @Query("endName") endName: String,
        @Query("searchOption") searchOption: Int
    ): Call<RouteResponse>



    companion object {
        fun create(): TMapAPIService {
            val retrofit = Retrofit.Builder()
                .baseUrl("https://apis.openapi.sk.com/tmap/")
                .addConverterFactory(ScalarsConverterFactory.create()) // 문자열 응답을 받기 위해 사용
                .build()

            return retrofit.create(TMapAPIService::class.java)
        }

        fun key(): String{
            return "7E3Vznne0r3bGkN2cbh8M1lXpvNbZEvYaRGyr8by"
        }
    }

    data class Geometry(
        val type: String,
        val coordinates: List<Double>
    )

    data class Feature(
        val geometry: Geometry
    )

    data class RouteResponse(
        val features: List<Feature>
    )
}



