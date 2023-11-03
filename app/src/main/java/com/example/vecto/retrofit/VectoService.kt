package com.example.vecto.retrofit

import com.example.vecto.data.LocationData
import com.example.vecto.data.VisitData
import okhttp3.MultipartBody
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

interface VectoService {

    @POST("login")
    fun loginUser(
        @Body request: LoginRequest
    ): Call<VectoResponse<String>>

    @POST("user")
    fun registerUser(
        @Body request: RegisterRequest
    ): Call<VectoResponse<String>>

    @POST("userId/check")
    fun idCheck(
        @Body request: IdCheckRequest
    ): Call<VectoResponse<Unit>>

    @GET("user")
    fun getUserInfo(
        @Header("Authorization")
        authorization: String
    ): Call<VectoResponse<UserInfoResponse>>

    @POST("feed")
    fun addPost(
        @Header("Authorization") authorization: String,
        @Body request: PostData
    ): Call<VectoResponse<Int>>

    @Multipart
    @POST("upload/profile")
    fun uploadImage(
        @Header("Authorization")
        authorization: String,
        @Part image: MultipartBody.Part
    ): Call<VectoResponse<String>>

    @Multipart
    @POST("upload/feed")
    fun uploadImages(
        @Header("Authorization") authorization: String,
        @Part images: List<MultipartBody.Part>
    ): Call<VectoResponse<ImageResponse>>

    @POST("mail")
    fun sendMail(
        @Body request: MailRequest
    ): Call<VectoResponse<Unit>>

    @GET("feed/feedList")
    fun getFeedList(
        @Query("page") page: Int
    ): Call<VectoResponse<List<Int>>>

    @GET("feed/{feedId}")
    fun getFeedInfo(
        @Path("feedId") feedId: Int
    ): Call<VectoResponse<PostResponse>>

    @POST("feed/{feedId}")
    fun getFeedInfo(
        @Header("Authorization") authorization: String,
        @Path("feedId") feedId: Int
    ): Call<VectoResponse<PostResponse>>

    @POST("feed/{feedId}/likes")
    fun sendLike(
        @Header("Authorization") authorization: String,
        @Path("feedId") feedId: Int,
    ): Call<VectoResponse<Unit>>


    @DELETE("feed/{feedId}/likes")
    fun cancelLike(
        @Header("Authorization") authorization: String,
        @Path("feedId") feedId: Int,
    ): Call<VectoResponse<Unit>>

    @GET("feed/{feedId}/comments")
    fun getComment(
        @Path("feedId") feedId: Int
    ): Call<VectoResponse<CommentListResponse>>


    companion object {
        fun create(): VectoService {
            val retrofit = Retrofit.Builder()
                .baseUrl("https://vec-to.net/")
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            return retrofit.create(VectoService::class.java)
        }
    }

    data class MailRequest(
        val email: String
    )

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
        val email: String?,
        val code: Int?
    )

    data class IdCheckRequest(
        val userId: String
    )
    data class VectoResponse<T>(
        val status: Int,
        val code: String,
        val message: String,
        val result: T?
    )

    data class UserInfoResponse(
        val userId: String,
        val provider: String,
        val nickName: String,
        val email: String?,
        val profileUrl: String?
    )

    data class ImageResponse(
        val url: List<String>
    )

    data class PostData(
        val title: String, //제목
        val content: String?, //내용
        val uploadtime: String, //게시 시간
        var image: MutableList<String>?, //이미지
        val location: MutableList<LocationData>, //경로 정보
        val visit: MutableList<VisitData>, //방문지 정보
        var mapimage: MutableList<String>?
    )

    data class PostResponse(
        val title: String,
        val content: String,
        val timeDifference: String,
        val image: List<String>,
        val location: List<LocationData>,
        val visit: List<VisitData>,
        var likeCount: Int,
        val commentCount: Int,
        val nickName: String,
        val userProfile: String?,
        val userId: String,
        val mapImage: List<String>,
        var likeFlag: Boolean
    )

    data class CommentListResponse(
        val comments: List<CommentResponse>
    )

    data class CommentResponse(
        val nickName: String,
        val content: String,
        val timeDifference: String,
        val profileUrl: String?
    )
}