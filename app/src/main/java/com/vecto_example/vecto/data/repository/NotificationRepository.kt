package com.vecto_example.vecto.data.repository

import android.util.Log
import com.vecto_example.vecto.data.Auth
import com.vecto_example.vecto.retrofit.VectoService

class NotificationRepository (private val vectoService: VectoService) {
    /*   Notification 관련 API 함수   */

    suspend fun getNewNotificationFlag(): Result<Boolean>{
        return try{
            val response = vectoService.getNewNotificationFlag("Bearer ${Auth.token}")
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

                Log.d("getNewNotificationFlag", "FAIL: ${response.body()}")
                Result.failure(Exception("서버오류"))

            }
        } catch (e: Exception) {
            Log.d("getNewNotificationFlag", "ERROR: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun getNotification(pageNo: Int): VectoService.NotificationResponse {
        /*   사용자의 알림 기록 확인   */

        val response = vectoService.getNotification("Bearer ${Auth.token}", pageNo)

        if(response.isSuccessful){
            Log.d("getNotification", "${response.body()}")

            return response.body()!!.result!!
        }
        else{
            throw Exception("Failed: ${response.errorBody()?.string()}")
        }
    }
}