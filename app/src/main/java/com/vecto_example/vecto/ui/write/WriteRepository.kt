package com.vecto_example.vecto.ui.write

import android.util.Log
import com.google.gson.Gson
import com.vecto_example.vecto.data.Auth
import com.vecto_example.vecto.data.model.VisitData
import com.vecto_example.vecto.retrofit.NaverSearchApiService
import com.vecto_example.vecto.retrofit.VectoService
import okhttp3.MultipartBody

class WriteRepository(private val vectoService: VectoService, private val naverService: NaverSearchApiService) {
    /*   게시글 작성 Repository   */

    suspend fun uploadImages(imageParts: List<MultipartBody.Part>): Result<VectoService.ImageResponse> {
        /*   여러 장의 이미지를 S3에 업로드 후, url 을 응답 값으로 받는 함수   */

        return try {
            val response = vectoService.uploadImages("Bearer ${Auth.token}", imageParts)

            if(response.isSuccessful){
                Log.d("uploadImages", "SUCCESS: ${response.body()}")
                Result.success(response.body()!!.result!!)
            }
            else{
                val errorBody = response.errorBody()?.string()
                //val gson = Gson()
                //val errorResponse: VectoService.VectoResponse<*>? = gson.fromJson(errorBody, VectoService.VectoResponse::class.java)
                Log.d("uploadImages", "FAIL: ${errorBody}")
                //Log.d("uploadImages", "FAIL: ${response.errorBody()}")
                //Result.failure(Exception(errorResponse!!.code))
                Result.failure(Exception("FAIL"))
            }
        } catch (e: Exception) {
            Log.e("uploadImages", "ERROR", e)
            Result.failure(e)
        }
    }

    suspend fun addFeed(postDataForUpload: VectoService.PostDataForUpload): Result<Int> {
        /*   게시글 업로드   */

        return try {
            val response = vectoService.addFeed("Bearer ${Auth.token}", postDataForUpload)

            if(response.isSuccessful){
                Log.d("addFeed", "SUCCESS: ${response.body()}")
                Result.success(response.body()!!.result!!)
            }
            else{
                val errorBody = response.errorBody()?.string()
                val gson = Gson()
                val errorResponse: VectoService.VectoResponse<*>? = gson.fromJson(errorBody, VectoService.VectoResponse::class.java)
                //Log.d("addFeed", "FAIL: ${response.errorBody()}")
                Log.d("addFeed", "FAIL: ${errorBody}")
                Result.failure(Exception(errorResponse!!.code))
            }
        } catch (e: Exception) {
            Log.e("addFeed", "ERROR", e)
            Result.failure(e)
        }
    }

    suspend fun updateFeed(updatePostRequest: VectoService.UpdatePostRequest): Result<String> {
        /*   게시글 수정   */

        return try {
            val response = vectoService.updateFeed("Bearer ${Auth.token}", updatePostRequest)

            if(response.isSuccessful){
                Log.d("updateFeed", "SUCCESS: ${response.body()}")
                Result.success(response.body()!!.result!!)
            }
            else{
                val errorBody = response.errorBody()?.string()
                val gson = Gson()
                val errorResponse: VectoService.VectoResponse<*>? = gson.fromJson(errorBody, VectoService.VectoResponse::class.java)
                Log.d("updateFeed", "FAIL: ${response.errorBody()}")
                Result.failure(Exception(errorResponse!!.code))
            }
        } catch (e: Exception) {
            Log.e("updateFeed", "ERROR", e)
            Result.failure(e)
        }
    }

    suspend fun reverseGeocode(visitData: VisitData): Result<NaverSearchApiService.ReverseGeocodeResponse> {
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