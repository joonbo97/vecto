package com.example.vecto.retrofit

import com.google.gson.annotations.SerializedName
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface TMapAPIService {
    @POST("routes/pedestrian?")
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
    ): Call<GeoJsonResponse>



    companion object {
        fun create(): TMapAPIService {
            val retrofit = Retrofit.Builder()
                .baseUrl("https://apis.openapi.sk.com/tmap/")
                .addConverterFactory(GsonConverterFactory.create()) // JSON 응답을 파싱하기 위한 Converter 추가
                .build()

            return retrofit.create(TMapAPIService::class.java)
        }

        fun key(): String{
            return "7E3Vznne0r3bGkN2cbh8M1lXpvNbZEvYaRGyr8by"
        }
    }

    data class Geometry(val type: String, val coordinates: Any)
    data class Properties(val index: Int, val name: String, val description: String)
    data class Feature(val type: String, val geometry: Geometry, val properties: Properties)
    data class GeoJsonResponse(val type: String, val features: List<Feature>)

}



