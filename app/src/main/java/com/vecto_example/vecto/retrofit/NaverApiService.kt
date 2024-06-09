package com.vecto_example.vecto.retrofit

import com.vecto_example.vecto.BuildConfig
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.Query

interface NaverApiService {
    @Headers("X-NCP-APIGW-API-KEY-ID: ${BuildConfig.NAVER_KEY1}", "X-NCP-APIGW-API-KEY: ${BuildConfig.NAVER_KEY2}")
    @GET("gc")
    suspend fun reverseGeocode(
        @Query("coords") coords: String,
        @Query("orders") orders: String,
        @Query("output") output: String,
    ): Response<ReverseGeocodeResponse>

    @Headers("X-NCP-APIGW-API-KEY-ID: ${BuildConfig.NAVER_KEY1}", "X-NCP-APIGW-API-KEY: ${BuildConfig.NAVER_KEY2}")
    @GET("geocode")
    suspend fun getGeocode(
        @Query("query") query: String
    ): Response<GeocodeResponse>

    @GET("search/local.json")
    suspend fun getSearch(
        @Header("X-Naver-Client-Id") clientId: String,
        @Header("X-Naver-Client-Secret") clientSecret: String,
        @Query("query") query: String,
        @Query("display") display: Int,
        @Query("start") start: Int
    ): Response<SearchResult>



    companion object {
        fun create(): NaverApiService {
            val retrofit = Retrofit.Builder()
                .baseUrl("https://naveropenapi.apigw.ntruss.com/map-reversegeocode/v2/")
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            return retrofit.create(NaverApiService::class.java)
        }

        fun createSearch(): NaverApiService {
            val retrofit = Retrofit.Builder()
                .baseUrl("https://openapi.naver.com/v1/")
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            return retrofit.create(NaverApiService::class.java)
        }
    }

    data class ReverseGeocodeResponse(
        val status: Status,
        val results: List<ReverseGeocodeResult>
    )

    data class GeocodeResponse(
        val status: String,
        val addresses: List<Address>?
    )

    data class Address(
        val y: String,
        val x: String
    )

    data class Status(
        val code: Int,
        val name: String,
        val message: String
    )

    data class ReverseGeocodeResult(
        val name: String,
        val code: Code?,
        val region: Region?,
        val land: Land?,
        val addition0: Addition?
    )

    data class GeocodeResult(
        val name: String,
        val code: Code?,
        val region: Region?
    )

    data class SearchResult(
        val items: List<Item>
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
        val name: String?,
    )

    data class Land(
        val name: String?,
        val number1: String?,
        val number2: String?,
    )

    data class Addition(
        val value: String,
        val type: String
    )

    data class Item(
        val title: String
    )
}
