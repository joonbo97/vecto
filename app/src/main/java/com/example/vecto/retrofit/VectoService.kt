package com.example.vecto.retrofit

import com.example.vecto.data.LocationData
import com.example.vecto.data.VisitData
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface VectoService {

    @POST("login")
    fun loginUser(
        @Body request: LoginRequest
    ): Call<VectoResponse>

    @POST("user")
    fun registerUser(
        @Body request: RegisterRequest
    ): Call<VectoResponse>

    @POST("userId/check")
    fun idCheck(
        @Body request: IdCheckRequest
    ): Call<VectoResponse>

    @GET("user")
    fun getUserInfo(
        @Header("Authorization")
        authorization: String
    ): Call<UserInfoResponse>

    @POST("feed")
    fun addPost(
        @Header("Authorization")
        authorization: String,
        @Body request: PostData
    ): Call<String>

    @Multipart
    @POST("upload/image")
    fun uploadImage(
        @Part image: MultipartBody.Part
    ): Call<String>


    companion object {
        fun create(): VectoService {
            val retrofit = Retrofit.Builder()
                .baseUrl("https://vec-to.net/")
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            return retrofit.create(VectoService::class.java)
        }
    }

    data class LoginRequest (
        val userId: String,
        val userPw: String? = null,
        val fcmToken: String
    )

    data class RegisterRequest(
        val userId: String,
        val userPw: String?,
        val provider: String,
        val nickName: String,
        val email: String?
    )

    data class IdCheckRequest(
        val userId: String
    )
    data class VectoResponse(
        val status: Int,
        val code: String,
        val message: String,
        val token: String
    )

    data class UserInfoResponse(
        val userId: String,
        val provider: String,
        val nickName: String,
        val email: String?
    )

    data class PostData(
        val title: String, //제목
        val content: String?, //내용
        val uploadtime: String, //게시 시간
        val image: MutableList<String>?, //이미지
        val location: MutableList<LocationData>, //경로 정보
        val visit: MutableList<VisitData> //방문지 정보
    )
}