package com.vecto_example.vecto.data.repository

import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.vecto_example.vecto.R
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
    suspend fun login(loginRequest: VectoService.LoginRequest): Result<String>{
        return try {
            val response = vectoService.loginUser(loginRequest)

            if(response.isSuccessful){
                Log.d("LOGIN", "SUCCESS")
                Result.success(response.body()!!.result.toString())
            } else {
                Log.d("LOGIN", "FAIL")
                Result.failure(Exception("FAIL"))
            }
        } catch (e: Exception) {
            Log.d("LOGIN", "ERROR")
            Result.failure(Exception("ERROR"))
        }
    }

    suspend fun register(registerRequest: VectoService.RegisterRequest): Result<String>{
        return try {
            val response = vectoService.registerUser(registerRequest)

            if(response.isSuccessful){
                Log.d("REGISTER", "SUCCESS")
                Result.success("SUCCESS")
            } else {
                val errorBody = response.errorBody()?.string()
                val gson = Gson()
                val errorResponse: VectoService.VectoResponse<*>? = gson.fromJson(errorBody, VectoService.VectoResponse::class.java)

                Result.failure(Exception(errorResponse!!.code))
            }
        } catch (e: Exception) {
            Log.e("REGISTER", "ERROR", e)
            Result.failure(Exception("ERROR"))
        }
    }

    suspend fun getUserInfo(userId: String): Result<VectoService.UserInfoResponse> {

        return try {
            val response = vectoService.getUserInfo(userId)

            if(response.isSuccessful){
                Log.d("getUserInfo", "SUCCESS: ${response.body()?.result}")

                Result.success(response.body()!!.result!!)
            }
            else{
                val errorBody = response.errorBody()?.string()
                val gson = Gson()
                val errorResponse: VectoService.VectoResponse<*>? = gson.fromJson(errorBody, VectoService.VectoResponse::class.java)

                Log.d("getUserInfo", "FAIL: ${response.errorBody()}")
                Result.failure(Exception(errorResponse!!.code))
            }
        } catch (e: Exception) {
            Log.e("getUserInfo", "ERROR", e)
            Result.failure(e)
        }
    }

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

    /*   유저간 interaction 관련   */
    suspend fun getFollow(userId: String): Result<String> {
        return try {
            val response = vectoService.getFollow2("Bearer ${Auth.token}", userId)

            if(response.isSuccessful) {
                Log.d("getFollow", "SUCCESS: ${response.body()}")

                Result.success(response.body()!!.code)
            } else {
                Log.d("getFollow", "FAIL: ${response.errorBody()?.string()}")
                Result.failure(Exception("FAIL"))
            }
        } catch (e: Exception) {
            Log.e("getFollow", "ERROR", e)
            Result.failure(Exception("ERROR"))
        }
    }

    suspend fun postFollow(userId: String): Result<String> {
        return try {
            val response = vectoService.postFollow2("Bearer ${Auth.token}", userId)

            if(response.isSuccessful) {
                Log.d("postFollow", "SUCCESS: ${response.body()}")

                Result.success(response.body()!!.code)
            } else {
                Log.d("postFollow", "FAIL: ${response.errorBody()?.string()}")
                Result.failure(Exception("FAIL"))
            }
        } catch (e: Exception) {
            Log.e("postFollow", "ERROR", e)
            Result.failure(Exception("ERROR"))
        }
    }

    suspend fun deleteFollow(userId: String): Result<String> {
        return try {
            val response = vectoService.deleteFollow2("Bearer ${Auth.token}", userId)

            if(response.isSuccessful) {
                Log.d("deleteFollow", "SUCCESS: ${response.body()}")

                Result.success(response.body()!!.code)
            } else {
                Log.d("deleteFollow", "FAIL: ${response.errorBody()?.string()}")
                Result.failure(Exception("FAIL"))
            }
        } catch (e: Exception) {
            Log.e("deleteFollow", "ERROR", e)
            Result.failure(Exception("ERROR"))
        }
    }

    //신고 관련
    suspend fun postComplaint(complaintRequest: VectoService.ComplaintRequest): Result<String> {
        return try {
            val response = vectoService.postComplaint("Bearer ${Auth.token}", complaintRequest)

            if(response.isSuccessful) {
                Log.d("postComplaint", "SUCCESS: ${response.body()}")

                Result.success(response.body()!!.code)
            } else {
                Log.d("postComplaint", "FAIL: ${response.errorBody()?.string()}")
                Result.failure(Exception("FAIL"))
            }
        } catch (e: Exception) {
            Log.e("postComplaint", "ERROR", e)
            Result.failure(Exception("ERROR"))
        }
    }
}