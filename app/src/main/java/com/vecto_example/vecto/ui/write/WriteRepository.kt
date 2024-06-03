package com.vecto_example.vecto.ui.write

import android.util.Log
import com.google.gson.Gson
import com.vecto_example.vecto.data.Auth
import com.vecto_example.vecto.data.model.VisitData
import com.vecto_example.vecto.retrofit.NaverApiService
import com.vecto_example.vecto.retrofit.VectoService
import okhttp3.MultipartBody

class WriteRepository(private val vectoService: VectoService, private val naverService: NaverApiService) {
    /*   게시글 작성 Repository   */

    suspend fun uploadImages(imageParts: List<MultipartBody.Part>): Result<VectoService.ImageResponse> {
        /*   여러 장의 이미지를 S3에 업로드 후, url 을 응답 값으로 받는 함수   */

        return try {
            val response = vectoService.uploadImages("Bearer ${Auth.accessToken}", imageParts)

            if(response.isSuccessful){
                Log.d("uploadImages", "SUCCESS: ${response.body()}")
                Result.success(response.body()!!.result!!)
            }
            else{
                val errorBody = response.errorBody()?.string()
                val gson = Gson()
                val errorResponse: VectoService.VectoResponse<*>? = gson.fromJson(errorBody, VectoService.VectoResponse::class.java)
                Log.d("uploadImages", "FAIL: $errorBody")
                if(errorResponse?.code?.isNotEmpty() == true)
                    Result.failure(Exception(errorResponse.code))
                else
                    Result.failure(Exception("FAIL"))
            }
        } catch (e: Exception) {
            Log.e("uploadImages", "ERROR", e)
            Result.failure(e)
        }
    }

    suspend fun addFeed(feedDataForUpload: VectoService.FeedDataForUpload): Result<Int> {
        /*   게시글 업로드   */

        return try {
            val response = vectoService.addFeed("Bearer ${Auth.accessToken}", feedDataForUpload)

            if(response.isSuccessful){
                Log.d("addFeed", "SUCCESS: ${response.body()}")
                Result.success(response.body()!!.result!!)
            }
            else{
                val errorBody = response.errorBody()?.string()
                val gson = Gson()
                val errorResponse: VectoService.VectoResponse<*>? = gson.fromJson(errorBody, VectoService.VectoResponse::class.java)
                Log.d("addFeed", "FAIL: $errorBody")
                if(errorResponse?.code?.isNotEmpty() == true)
                    Result.failure(Exception(errorResponse.code))
                else
                    Result.failure(Exception("FAIL"))
            }
        } catch (e: Exception) {
            Log.e("addFeed", "ERROR", e)
            Result.failure(e)
        }
    }

    suspend fun updateFeed(updateFeedRequest: VectoService.UpdateFeedRequest): Result<String> {
        /*   게시글 수정   */

        return try {
            val response = vectoService.updateFeed("Bearer ${Auth.accessToken}", updateFeedRequest)

            if(response.isSuccessful){
                Log.d("updateFeed", "SUCCESS: ${response.body()}")
                Result.success(response.body()!!.result!!)
            }
            else{
                val errorBody = response.errorBody()?.string()
                val gson = Gson()
                val errorResponse: VectoService.VectoResponse<*>? = gson.fromJson(errorBody, VectoService.VectoResponse::class.java)
                Log.d("updateFeed", "FAIL: ${response.errorBody()}")
                if(errorResponse?.code?.isNotEmpty() == true)
                    Result.failure(Exception(errorResponse.code))
                else
                    Result.failure(Exception("FAIL"))
            }
        } catch (e: Exception) {
            Log.e("updateFeed", "ERROR", e)
            Result.failure(e)
        }
    }

    suspend fun reverseGeocode(visitData: VisitData): Result<NaverApiService.ReverseGeocodeResponse> {
        /*   Lat Lng 으로 지역 반환   */

        return try {
            val response = naverService.reverseGeocode("${visitData.lng_set},${visitData.lat_set}", "legalcode", "json")

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
}