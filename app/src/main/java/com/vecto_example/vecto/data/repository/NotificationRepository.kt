package com.vecto_example.vecto.data.repository

import android.util.Log
import com.google.gson.Gson
import com.vecto_example.vecto.data.Auth
import com.vecto_example.vecto.retrofit.VectoService

class NotificationRepository (private val vectoService: VectoService) {
    /*   Notification 관련 API 함수   */

    suspend fun getNewNotificationFlag(): Result<Boolean>{
        return try{
            val response = vectoService.getNewNotificationFlag("Bearer ${Auth.accessToken}")
            if (response.isSuccessful) {

                if(response.body()?.result == true) //새로운 알림이 있을 경우
                {
                    Log.d("getNewNotificationFlag", "SUCCESS: ${response.body()?.result}")
                    Result.success(true)
                }
                else                                //새로운 알림이 없을 경우
                {
                    Log.d("getNewNotificationFlag", "SUCCESS: ${response.body()?.result}")
                    Result.success(false)
                }

            } else {
                val errorBody = response.errorBody()?.string()
                val gson = Gson()
                val errorResponse: VectoService.VectoResponse<*>? = gson.fromJson(errorBody, VectoService.VectoResponse::class.java)

                Log.d("getNewNotificationFlag", "FAIL: $errorBody")
                Result.failure(Exception(errorResponse?.code))
            }
        } catch (e: Exception) {
            Log.d("getNewNotificationFlag", "ERROR: ${e.message}")
            Result.failure(Exception("ERROR"))
        }
    }

    suspend fun getNotification(pageNo: Int): Result<VectoService.NotificationResponse> {
        /*   사용자의 알림 기록 확인   */
        return try{
            val response = vectoService.getNotification("Bearer ${Auth.accessToken}", pageNo)

            if (response.isSuccessful) {
                Result.success(response.body()?.result!!)
            } else {
                val errorBody = response.errorBody()?.string()
                val gson = Gson()
                val errorResponse: VectoService.VectoResponse<*>? = gson.fromJson(errorBody, VectoService.VectoResponse::class.java)

                Log.d("getNotification", "FAIL: $errorBody")
                Result.failure(Exception(errorResponse?.code))
            }
        } catch (e: Exception) {
            Log.d("getNotification", "ERROR: ${e.message}")
            Result.failure(Exception("ERROR"))
        }
    }
}