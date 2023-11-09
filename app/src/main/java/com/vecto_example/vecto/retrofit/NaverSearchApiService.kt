package com.vecto_example.vecto.retrofit

import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Query

interface NaverSearchApiService {
    @Headers("X-NCP-APIGW-API-KEY-ID: 4liop0rwtx", "X-NCP-APIGW-API-KEY: qwC1Wx0vMv0lGUTxAsR2EcncEZGbTBpWrq29rxfo")
    @GET("gc")
    fun reverseGeocode(
        @Query("coords") coords: String,
        @Query("orders") orders: String, //legalcode,addr,admcode,roadaddr
        @Query("output") output: String,  //output=json
    ): Call<ReverseGeocodeResponse>

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
        val results: List<Result>
    )

    data class Status(
        val code: Int,
        val name: String,
        val message: String
    )

    data class Result(
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
