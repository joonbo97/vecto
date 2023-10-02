package com.example.vecto.retrofit

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Query

interface NaverSearchApiService {
    @Headers("X-Naver-Client-Id: m8NoMqHTJvHI0gLx5aNe", "X-Naver-Client-Secret: sy1Sb7aNf_")
    @GET("/v1/search/local.json")
    fun searchNearbyPlaces(
        @Query("query") query: String,
        @Query("display") display: Int = 10, // 결과 개수 (기본값 10)
        @Query("start") start: Int = 1, // 시작 위치 (기본값 1)
        @Query("sort") sort: String = "random", // 정렬 방법 (기본값 random)
        @Query("radius") radius: Int = 50, // 반경 (단위: 미터, 기본값 50)
        @Query("x") longitude: Double,
        @Query("y") latitude: Double
    ): Call<PlacesResponse>
}