package com.vecto_example.vecto.data.repository

import android.util.Log
import com.vecto_example.vecto.BuildConfig
import com.vecto_example.vecto.data.model.VisitData
import com.vecto_example.vecto.retrofit.NaverApiService

class NaverRepository(private val naverApiService: NaverApiService, private val naverSearchService: NaverApiService) {
    /*   Naver 관련 Repo   */

    suspend fun reverseGeocode(visitData: VisitData): Result<NaverApiService.ReverseGeocodeResponse> {
        /*   Lat Lng 으로 지역 반환   */

        return try {
            val response = naverApiService.reverseGeocode("${visitData.lng_set},${visitData.lat_set}", "roadaddr", "json")

            if(response.isSuccessful){
                Log.d("reverseGeocode", "SUCCESS: ${response.body()}")
                Result.success(response.body()!!)
            }
            else{
                Log.d("reverseGeocode", "FAIL: ${response.errorBody()?.string()}")
                Result.failure(Exception("FAIL"))
            }
        } catch (e: Exception) {
            Log.e("reverseGeocode", "ERROR", e)
            Result.failure(Exception("ERROR"))
        }
    }

    suspend fun getSearch(query: String): Result<NaverApiService.SearchResult> {
        /*   검색   */

        return try {
            val response = naverSearchService.getSearch(BuildConfig.NAVER_CLIENT_ID, BuildConfig.NAVER_CLIENT_SECRET, query, 1)

            if(response.isSuccessful){
                Log.d("getSearch", "SUCCESS: ${response.body()}")
                Result.success(response.body()!!)
            } else {
                Log.d("getSearch", "FAIL: ${response.errorBody()?.string()}")
                Result.failure(Exception("FAIL"))
            }
        } catch (e: Exception) {
            Log.e("getSearch", "ERROR", e)
            Result.failure(Exception("ERROR"))
        }
    }
}