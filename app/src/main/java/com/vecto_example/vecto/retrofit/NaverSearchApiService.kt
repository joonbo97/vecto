package com.vecto_example.vecto.retrofit

import com.vecto_example.vecto.BuildConfig
import retrofit2.Call
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Query

interface NaverSearchApiService {
    @Headers("X-NCP-APIGW-API-KEY-ID: ${BuildConfig.NAVER_KEY1}", "X-NCP-APIGW-API-KEY: ${BuildConfig.NAVER_KEY2}")
    @GET("gc")
    suspend fun reverseGeocode(
        @Query("coords") coords: String,
        @Query("orders") orders: String, //legalcode,addr,admcode,roadaddr
        @Query("output") output: String,  //output=json
    ): Response<ReverseGeocodeResponse>

    companion object {
        fun create(): NaverSearchApiService {
            val retrofit = Retrofit.Builder()
                .baseUrl("https://naveropenapi.apigw.ntruss.com/map-reversegeocode/v2/")
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            return retrofit.create(NaverSearchApiService::class.java)
        }
    }

    data class ReverseGeocodeResponse(
        val status: Status,
        val results: List<NaverResult>
    )

    data class Status(
        val code: Int,
        val name: String,
        val message: String
    )

    data class NaverResult(
        val name: String,
        // Assuming additional fields based on the example JSON you provided:
        val code: Code?,
        val region: Region?
        // Add other fields here as needed
    )

    data class Code(
        val id: String,
        val type: String,
        val mappingId: String
    )

    data class Region(
        val area0: Area,
        val area1: Area,
        val area2: Area,
        val area3: Area
    )

    data class Area(
        val name: String,
    )
}
