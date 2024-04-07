package com.vecto_example.vecto.data.repository

import android.util.Log
import com.vecto_example.vecto.data.Auth
import com.vecto_example.vecto.retrofit.VectoService
import okhttp3.MultipartBody

class UserRepository (private val vectoService: VectoService) {
    /*   User 관련 API 함수   */

    suspend fun postUploadProfileImage(imagePart: MultipartBody.Part){
        val response = vectoService.uploadImage("Bearer ${Auth.token}", imagePart)

        if(response.isSuccessful){
            Log.d("POST UPLOAD IMAGE SUCCESS", "${response.body()}")
            Auth._profileImage.value = response.body()?.result
        }
        else{
            throw Exception("Failed: ${response.errorBody()?.string()}")
        }
    }

    suspend fun patchUserProfile(updateData: VectoService.UserUpdateData): Result<String> {
        return try{
            val response = vectoService.updateUserProfile("Bearer ${Auth.token}", updateData)
            if (response.isSuccessful) {
                //성공인 경우, 결과를 Result.success에 담아 반환
                Result.success("SUCCESS")
            } else {
                //서버로부터 오류 응답을 받은 경우, 에러 메시지를 포함하여 Result.failure로 반환
                Result.failure(Exception("FAIL"))
            }

        } catch (e: Exception) {
            // network 에러가 발생한 경우
            Result.failure(Exception("ERROR"))
        }
    }

    /*   User 계정 관련 API 함수   */
    suspend fun checkUserId(userId: String): Result<Boolean>{
        return try{
            val response = vectoService.idCheck2(VectoService.IdCheckRequest(userId))
            if (response.isSuccessful) {
                Result.success(true)
            } else {
                if(response.code() == 400){
                    Log.d("ID_CHECK", "IS DUPLICATED ID")
                    Result.success(false)
                } else {
                    Log.d("ID_CHECK", "성공했으나 서버 오류")
                    Result.failure(Exception("서버오류"))
                }
            }
        } catch (e: Exception) {
            Log.d("ID_CHECK", "실패")
            Result.failure(e)
        }
    }
}