package com.vecto_example.vecto.retrofit

import com.google.gson.annotations.SerializedName
import com.vecto_example.vecto.BuildConfig
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
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

    @GET("pois/search/around?")
    fun searchNearbyPOI(
        @Query("version") version: Int,
        @Query("categories") categories: String,
        @Query("appKey") appKey: String,
        @Query("page") page: Int,
        @Query("radius") radius: Int,
        @Query("count") count: Int,
        @Query("centerLat") centerLat: Double,
        @Query("centerLon") centerLon: Double
        ): Call<POIResponse>


    companion object {
        fun create(): TMapAPIService {
            val retrofit = Retrofit.Builder()
                .baseUrl("https://apis.openapi.sk.com/tmap/")
                .addConverterFactory(GsonConverterFactory.create()) // JSON 응답을 파싱하기 위한 Converter 추가
                .build()

            return retrofit.create(TMapAPIService::class.java)
        }

        fun key(): String{
            return BuildConfig.TMAP_KEY
        }
    }

    data class Geometry(val type: String, val coordinates: Any)
    data class Properties(val index: Int, val name: String, val description: String)
    data class Feature(val type: String, val geometry: Geometry, val properties: Properties)
    data class GeoJsonResponse(val type: String, val features: List<Feature>)

    data class POIResponse(
        val searchPoiInfo: SearchPoiInfo
    )

    data class SearchPoiInfo(
        @SerializedName("totalCount") val totalCount: Int,
        @SerializedName("count") val count: Int,
        val pois: Pois
    )

    data class Pois(
        val poi: List<Poi>
    )

    data class Poi(
        val name: String,
        val frontLat: Double,
        val frontLon: Double
    )
}



