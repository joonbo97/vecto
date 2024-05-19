package com.vecto_example.vecto.data.repository

import android.util.Log
import com.google.gson.Gson
import com.vecto_example.vecto.data.Auth
import com.vecto_example.vecto.retrofit.VectoService
import okhttp3.MultipartBody

class UserRepository (private val vectoService: VectoService) {
    /*   User 관련 API 함수   */

    suspend fun postUploadProfileImage(imagePart: MultipartBody.Part): Result<String>{
        return try {
            val response = vectoService.uploadImage("Bearer ${Auth.accessToken}", imagePart)

            if(response.isSuccessful){
                Log.d("postUploadProfileImage", "SUCCESS: ${response.body()}")
                Result.success(response.body()!!.result!!)
            } else {
                val errorBody = response.errorBody()?.string()
                val gson = Gson()
                val errorResponse: VectoService.VectoResponse<*>? = gson.fromJson(errorBody, VectoService.VectoResponse::class.java)

                Log.d("postUploadProfileImage", "FAIL: $errorBody")
                Result.failure(Exception(errorResponse?.code))
            }
        } catch (e: Exception) {
            Log.e("postUploadProfileImage", "ERROR", e)
            Result.failure(Exception("ERROR"))
        }
    }

    suspend fun patchUserProfile(updateData: VectoService.UserUpdateData): Result<String> {
        return try{
            val response = vectoService.updateUserProfile("Bearer ${Auth.accessToken}", updateData)
            if (response.isSuccessful) {
                //성공인 경우, 결과를 Result.success에 담아 반환
                Result.success("SUCCESS")
            } else {
                val errorBody = response.errorBody()?.string()
                val gson = Gson()
                val errorResponse: VectoService.VectoResponse<*>? = gson.fromJson(errorBody, VectoService.VectoResponse::class.java)

                Log.d("patchUserProfile", "FAIL: $errorBody")
                Result.failure(Exception(errorResponse?.code))
            }

        } catch (e: Exception) {
            // network 에러가 발생한 경우
            Result.failure(Exception("ERROR"))
        }
    }

    /*   User 계정 관련 API 함수   */
    suspend fun login(loginRequest: VectoService.LoginRequest): Result<VectoService.UserToken>{
        return try {
            val response = vectoService.loginUser(loginRequest)

            if(response.isSuccessful){
                Log.d("login", "SUCCESS")
                Result.success(response.body()!!.result!!)
            } else {
                Log.d("login", "FAIL")
                Result.failure(Exception("FAIL"))
            }
        } catch (e: Exception) {
            Log.d("login", "ERROR")
            Result.failure(Exception("ERROR"))
        }
    }

    suspend fun register(registerRequest: VectoService.RegisterRequest): Result<String>{
        return try {
            val response = vectoService.registerUser(registerRequest)

            if(response.isSuccessful){
                Log.d("register", "SUCCESS: ${response.body()}")
                Result.success("SUCCESS")
            } else {
                val errorBody = response.errorBody()?.string()
                val gson = Gson()
                val errorResponse: VectoService.VectoResponse<*>? = gson.fromJson(errorBody, VectoService.VectoResponse::class.java)

                Log.d("register", "FAIL: $errorBody")
                Result.failure(Exception(errorResponse?.code))
            }
        } catch (e: Exception) {
            Log.e("register", "ERROR", e)
            Result.failure(Exception("ERROR"))
        }
    }

    suspend fun getUserInfo(userId: String): Result<VectoService.UserInfoResponse> {

        return try {
            val response = vectoService.getUserInfo(userId)

            if(response.isSuccessful){
                Log.d("getUserInfo", "SUCCESS: ${response.body()}")

                Result.success(response.body()!!.result!!)
            }
            else{
                val errorBody = response.errorBody()?.string()
                val gson = Gson()
                val errorResponse: VectoService.VectoResponse<*>? = gson.fromJson(errorBody, VectoService.VectoResponse::class.java)

                Log.d("getUserInfo", "FAIL: $errorBody")
                Result.failure(Exception(errorResponse?.code))
            }
        } catch (e: Exception) {
            Log.e("getUserInfo", "ERROR", e)
            Result.failure(Exception("ERROR"))
        }
    }

    suspend fun checkUserId(userId: String): Result<String>{
        return try{
            val response = vectoService.checkIdDuplicate(VectoService.IdCheckRequest(userId))
            if (response.isSuccessful) {
                Log.d("checkUserId", "SUCCESS: ${response.body()}")
                Result.success("SUCCESS")
            } else {
                val errorBody = response.errorBody()?.string()
                val gson = Gson()
                val errorResponse: VectoService.VectoResponse<*>? = gson.fromJson(errorBody, VectoService.VectoResponse::class.java)

                Log.d("checkUserId", "FAIL: $errorBody")
                Result.failure(Exception(errorResponse?.code))
            }
        } catch (e: Exception) {
            Log.e("checkUserId", "ERROR", e)
            Result.failure(Exception("ERROR"))
        }
    }

    //인증 메일 발송
    suspend fun sendMail(email: String): Result<String>{
        return try {
            val response = vectoService.sendMail(VectoService.MailRequest(email))
            if (response.isSuccessful) {
                Log.d("sendMail", "SUCCESS: ${response.body()}")
                Result.success("SUCCESS")
            } else {
                val errorBody = response.errorBody()?.string()
                val gson = Gson()
                val errorResponse: VectoService.VectoResponse<*>? = gson.fromJson(errorBody, VectoService.VectoResponse::class.java)

                Log.d("sendMail", "FAIL: $errorBody")
                Result.failure(Exception(errorResponse?.code))
            }
        } catch (e: Exception) {
            Log.e("sendMail", "ERROR", e)
            Result.failure(Exception("ERROR"))
        }
    }

    /*   유저간 interaction 관련   */

    suspend fun getFollowRelation(userIdList: List<String>): Result<VectoService.FollowResponse> {
        return try {
            val response = vectoService.getFollowRelation("Bearer ${Auth.accessToken}", VectoService.UserIdList(userIdList))

            if(response.isSuccessful) {
                Log.d("getFollowRelation", "SUCCESS: ${response.body()}")

                Result.success(response.body()!!.result!!)
            } else {
                val errorBody = response.errorBody()?.string()
                val gson = Gson()
                val errorResponse: VectoService.VectoResponse<*>? = gson.fromJson(errorBody, VectoService.VectoResponse::class.java)

                Log.d("getFollowRelation", "FAIL: $errorBody")
                Result.failure(Exception(errorResponse?.code))
            }
        } catch (e: Exception) {
            Log.e("getFollowRelation", "ERROR", e)
            Result.failure(Exception("ERROR"))
        }
    }

    suspend fun postFollow(userId: String): Result<String> {
        return try {
            val response = vectoService.postFollow("Bearer ${Auth.accessToken}", userId)

            if(response.isSuccessful) {
                Log.d("postFollow", "SUCCESS: ${response.body()}")

                Result.success(response.body()!!.code)
            } else {
                val errorBody = response.errorBody()?.string()
                val gson = Gson()
                val errorResponse: VectoService.VectoResponse<*>? = gson.fromJson(errorBody, VectoService.VectoResponse::class.java)

                Log.d("postFollow", "FAIL: $errorBody")
                Result.failure(Exception(errorResponse?.code))
            }
        } catch (e: Exception) {
            Log.e("postFollow", "ERROR", e)
            Result.failure(Exception("ERROR"))
        }
    }

    suspend fun deleteFollow(userId: String): Result<String> {
        return try {
            val response = vectoService.deleteFollow("Bearer ${Auth.accessToken}", userId)

            if(response.isSuccessful) {
                Log.d("deleteFollow", "SUCCESS: ${response.body()}")

                Result.success(response.body()!!.code)
            } else {
                val errorBody = response.errorBody()?.string()
                val gson = Gson()
                val errorResponse: VectoService.VectoResponse<*>? = gson.fromJson(errorBody, VectoService.VectoResponse::class.java)

                Log.d("deleteFollow", "FAIL: $errorBody")
                Result.failure(Exception(errorResponse?.code))
            }
        } catch (e: Exception) {
            Log.e("deleteFollow", "ERROR", e)
            Result.failure(Exception("ERROR"))
        }
    }

    //팔로우 유저 리스트 확인
    suspend fun getFollowerList(userId: String): Result<VectoService.FollowListResponse> {

        return try {
            val response = vectoService.getFollowerList(userId)

            if(response.isSuccessful) {
                Log.d("getFollowerList", "SUCCESS: ${response.body()}")

                Result.success(response.body()!!.result!!)
            } else {
                Log.d("getFollowerList", "FAIL: ${response.errorBody()?.string()}")
                Result.failure(Exception("FAIL"))
            }
        } catch (e: Exception) {
            Log.e("getFollowerList", "ERROR", e)
            Result.failure(Exception("ERROR"))
        }
    }

    //팔로잉 유저 리스트 확인
    suspend fun getFollowingList(userId: String): Result<VectoService.FollowListResponse> {
        return try {
            val response = vectoService.getFollowingList(userId)

            if(response.isSuccessful) {
                Log.d("getFollowingList", "SUCCESS: ${response.body()}")

                Result.success(response.body()!!.result!!)
            } else {
                Log.d("getFollowingList", "FAIL: ${response.errorBody()?.string()}")
                Result.failure(Exception("FAIL"))
            }
        } catch (e: Exception) {
            Log.e("getFollowingList", "ERROR", e)
            Result.failure(Exception("ERROR"))
        }
    }

    //신고 관련
    suspend fun postComplaint(complaintRequest: VectoService.ComplaintRequest): Result<String> {
        return try {
            val response = vectoService.postComplaint("Bearer ${Auth.accessToken}", complaintRequest)

            if(response.isSuccessful) {
                Log.d("postComplaint", "SUCCESS: ${response.body()}")

                Result.success(response.body()!!.code)
            } else {
                val errorBody = response.errorBody()?.string()
                val gson = Gson()
                val errorResponse: VectoService.VectoResponse<*>? = gson.fromJson(errorBody, VectoService.VectoResponse::class.java)

                Log.d("postComplaint", "FAIL: $errorBody")
                Result.failure(Exception(errorResponse?.code))
            }
        } catch (e: Exception) {
            Log.e("postComplaint", "ERROR", e)
            Result.failure(Exception("ERROR"))
        }
    }

    //로그 아웃
    suspend fun postLogout(): Result<String> {
        return try {
            val response = vectoService.postLogout("Bearer ${Auth.accessToken}")

            if(response.isSuccessful) {
                Log.d("postLogout", "SUCCESS: ${response.body()}")

                Result.success(response.body()!!.code)
            } else {
                val errorBody = response.errorBody()?.string()
                val gson = Gson()
                val errorResponse: VectoService.VectoResponse<*>? = gson.fromJson(errorBody, VectoService.VectoResponse::class.java)

                Log.d("postLogout", "FAIL: $errorBody")
                Result.failure(Exception(errorResponse?.code))
            }
        } catch (e: Exception) {
            Log.e("postLogout", "ERROR", e)
            Result.failure(Exception("ERROR"))
        }
    }

    //탈퇴 관련
    suspend fun deleteAccount(): Result<String> {
        return try {
            val response = vectoService.deleteAccount("Bearer ${Auth.accessToken}")

            if(response.isSuccessful) {
                Log.d("deleteAccount", "SUCCESS: ${response.body()}")

                Result.success(response.body()!!.code)
            } else {
                val errorBody = response.errorBody()?.string()
                val gson = Gson()
                val errorResponse: VectoService.VectoResponse<*>? = gson.fromJson(errorBody, VectoService.VectoResponse::class.java)

                Log.d("deleteAccount", "FAIL: $errorBody")
                Result.failure(Exception(errorResponse?.code))
            }
        } catch (e: Exception) {
            Log.e("deleteAccount", "ERROR", e)
            Result.failure(Exception("ERROR"))
        }
    }
}