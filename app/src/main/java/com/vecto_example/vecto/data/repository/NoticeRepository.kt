package com.vecto_example.vecto.data.repository

import android.util.Log
import com.google.gson.Gson
import com.vecto_example.vecto.retrofit.VectoService

class NoticeRepository (private val vectoService: VectoService) {
    /*   공지 사항 API 함수   */

    suspend fun getNoticeList(): Result<List<VectoService.NoticeListResponse>> {
        return try {
            val response = vectoService.getNoticeList()

            if(response.isSuccessful) {
                Log.d("getNoticeList", "SUCCESS: ${response.body()?.result}")
                Result.success(response.body()?.result!!)
            } else {
                val errorBody = response.errorBody()?.string()
                val gson = Gson()
                val errorResponse: VectoService.VectoResponse<*>? = gson.fromJson(errorBody, VectoService.VectoResponse::class.java)

                Log.d("getNoticeList", "FAIL: $errorBody")
                Result.failure(Exception(errorResponse?.code))
            }
        }  catch (e: Exception) {
            Log.d("getNoticeList", "ERROR: ${e.message}")
            Result.failure(Exception("ERROR"))
        }
    }

    suspend fun getNotice(id: Int): Result<VectoService.NoticeResponse>{
        return try {
            val response = vectoService.getNotice(id)

            if(response.isSuccessful) {
                Log.d("getNotice", "SUCCESS: ${response.body()?.result}")
                Result.success(response.body()?.result!!)
            } else {
                val errorBody = response.errorBody()?.string()
                val gson = Gson()
                val errorResponse: VectoService.VectoResponse<*>? = gson.fromJson(errorBody, VectoService.VectoResponse::class.java)

                Log.d("getNotice", "FAIL: $errorBody")
                Result.failure(Exception(errorResponse?.code))
            }
        }  catch (e: Exception) {
            Log.d("getNotice", "ERROR: ${e.message}")
            Result.failure(Exception("ERROR"))
        }
    }

    suspend fun getNewNotice(): Result<VectoService.NoticeResponse>{
        return try {
            val response = vectoService.getNewNotice()

            if(response.isSuccessful) {
                Log.d("getNewNotice", "SUCCESS: ${response.body()?.result}")
                Result.success(response.body()?.result!!)
            } else {
                val errorBody = response.errorBody()?.string()
                val gson = Gson()
                val errorResponse: VectoService.VectoResponse<*>? = gson.fromJson(errorBody, VectoService.VectoResponse::class.java)

                Log.d("getNewNotice", "FAIL: $errorBody")
                Result.failure(Exception(errorResponse?.code))
            }
        }  catch (e: Exception) {
            Log.d("getNewNotice", "ERROR: ${e.message}")
            Result.failure(Exception("ERROR"))
        }
    }
}