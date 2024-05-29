package com.vecto_example.vecto.data.repository

import android.util.Log
import com.vecto_example.vecto.retrofit.TMapAPIService

class TMapRepository(private val tMapAPIService: TMapAPIService) {
    /*   TMap 관련 API 모음   */

    suspend fun recommendRoute(recommendRouteRequest: TMapAPIService.RecommendRouteRequest): Result<TMapAPIService.GeoJsonResponse> {
        /*  도보 추천 경로를 받을 수 있는 함수 */
        return try {
            val response = tMapAPIService.recommendedRoute(
                recommendRouteRequest.version, recommendRouteRequest.appKey,
                recommendRouteRequest.startY, recommendRouteRequest.startX,
                recommendRouteRequest.endY, recommendRouteRequest.endX,
                recommendRouteRequest.reqCoordType, recommendRouteRequest.resCoordType,
                recommendRouteRequest.startName, recommendRouteRequest.endName,
                recommendRouteRequest.searchOption
            )

            if (response.isSuccessful) {
                Log.d("getRecommendRoute", "SUCCESS: ${response.body()}")

                Result.success(response.body()!!)
            } else {
                Log.d("getRecommendRoute", "FAIL: ${response.errorBody()?.string()}")
                Result.failure(Exception("ERROR"))
            }
        } catch (e: Exception) {
            Log.e("getRecommendRoute", "ERROR", e)
            Result.failure(Exception("FAIL"))
        }
    }

    suspend fun recommendCarRoute(recommendRouteRequest: TMapAPIService.RecommendRouteRequest): Result<TMapAPIService.GeoJsonResponse> {
        /*  자동차 경로를 받을 수 있는 함수 */
        return try {
            val response = tMapAPIService.recommendedCarRoute(
                recommendRouteRequest.version, recommendRouteRequest.appKey,
                recommendRouteRequest.startY, recommendRouteRequest.startX,
                recommendRouteRequest.endY, recommendRouteRequest.endX,
                recommendRouteRequest.reqCoordType, recommendRouteRequest.resCoordType,
                recommendRouteRequest.startName, recommendRouteRequest.endName,
                recommendRouteRequest.searchOption
            )

            if (response.isSuccessful) {
                Log.d("recommendCarRoute", "SUCCESS: ${response.body()}")

                Result.success(response.body()!!)
            } else {
                Log.d("recommendCarRoute", "FAIL: ${response.errorBody()?.string()}")
                Result.failure(Exception("ERROR"))
            }
        } catch (e: Exception) {
            Log.e("recommendCarRoute", "ERROR", e)
            Result.failure(Exception("FAIL"))
        }
    }

    suspend fun searchNearbyPoi(searchNearbyPOIRequest: TMapAPIService.SearchNearbyPoiRequest): Result<TMapAPIService.POIResponse> {
        /*   주변 검색   */
        return try {
            val response = tMapAPIService.searchNearbyPoi(
                searchNearbyPOIRequest.version, searchNearbyPOIRequest.categories,
                searchNearbyPOIRequest.appKey, searchNearbyPOIRequest.page,
                searchNearbyPOIRequest.radius, searchNearbyPOIRequest.count,
                searchNearbyPOIRequest.centerLat, searchNearbyPOIRequest.centerLon)

            if (response.isSuccessful) {
                // 로깅을 통해 성공한 응답 내용을 확인
                Log.d("searchNearbyPoi", "SUCCESS: ${response.body()}")

                Result.success(response.body()!!)
            } else {
                //실패인 경우
                Log.d("searchNearbyPoi", "FAIL: ${response.errorBody()?.string()}")
                Result.failure(Exception("ERROR"))
            }
        } catch (e: Exception) {
            //예외 발생
            Log.e("searchNearbyPoi", "ERROR", e)
            Result.failure(Exception("FAIL"))
        }
    }

}