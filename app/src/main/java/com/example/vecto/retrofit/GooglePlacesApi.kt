package com.example.vecto.retrofit

import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

interface GooglePlacesApi {
    @GET("place/nearbysearch/json")
    fun getNearbyPlaces(
        @Query("location") location: String,
        @Query("radius") radius: Int,
        @Query("key") apiKey: String
    ): Call<PlacesResponse>

    companion object {
        fun create(): GooglePlacesApi {
            val retrofit = Retrofit.Builder()
                .baseUrl("https://maps.googleapis.com/maps/api/")
                .addConverterFactory(GsonConverterFactory.create()) // JSON 응답을 파싱하기 위한 Converter 추가
                .build()

            return retrofit.create(GooglePlacesApi::class.java)
        }

        fun key(): String{
            return "AIzaSyB5FqF8vybQ7NJYtXn-cmn1-mU8ITtKSA4"
        }
    }

    data class PlacesResponse(
        val results: List<Place>
    )

    data class Place(
        val name: String,
        val geometry: Geometry
    )

    data class Geometry(
        val location: Location
    )

    data class Location(
        val lat: Double,
        val lng: Double
    )
}