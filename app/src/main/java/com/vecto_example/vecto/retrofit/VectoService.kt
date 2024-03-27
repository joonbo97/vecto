package com.vecto_example.vecto.retrofit

import com.vecto_example.vecto.data.model.LocationData
import com.vecto_example.vecto.data.model.VisitData
import com.vecto_example.vecto.data.model.VisitDataForWite
import okhttp3.MultipartBody
import retrofit2.Call
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.PATCH
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
        @Query("userId") userId: String
    ): Call<VectoResponse<UserInfoResponse>>

    @PATCH("user")
    fun updateUser(
        @Header("Authorization") authorization: String,
        @Body userData: UserUpdateData
    ): Call<VectoResponse<String>>

    @POST("feed")
    fun addPost(
        @Header("Authorization") authorization: String,
        @Body request: PostDataForUpload
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

    @GET("feed")
    fun getUserPost(
        @Query("userId") userId: String,
        @Query("page") page: Int
    ): Call<VectoResponse<List<Int>>>

    @GET("feed/likes")
    fun getUserLikePost(
        @Query("userId") userId: String,
        @Query("page") page: Int
    ): Call<VectoResponse<List<Int>>>

    /*@GET("feed/feedList")
    fun getFeedList(
        @Query("page") page: Int
    ): Call<VectoResponse<List<Int>>>*/

    @GET("feed/feeds/search")
    fun getSearchFeedList(
        @Query("page") page: Int,
        @Query("q") q: String
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

    @POST("feed/{feedId}/comments")
    fun getComment(
        @Header("Authorization") authorization: String,
        @Path("feedId") feedId: Int
    ): Call<VectoResponse<CommentListResponse>>

    @POST("feed/comment")
    fun sendComment(
        @Header("Authorization") authorization: String,
        @Body request: CommentRequest
    ): Call<VectoResponse<String>>

    @POST("comment/{commentId}/likes")
    fun sendCommentLike(
        @Header("Authorization") authorization: String,
        @Path("commentId") commentId: Int
    ): Call<VectoResponse<Unit>>


    @DELETE("comment/{commentId}/likes")
    fun cancelCommentLike(
        @Header("Authorization") authorization: String,
        @Path("commentId") commentId: Int
    ): Call<VectoResponse<Unit>>

    @DELETE("feed/comment")
    fun deleteComment(
        @Header("Authorization") authorization: String,
        @Query("commentId") commentId: Int
    ): Call<VectoResponse<Unit>>

    @GET("follow/{userId}")//팔로우 여부 반환 code: S027이면 이미 팔로우 S028이면 안한 상태
    fun getFollow(
        @Header("Authorization") authorization: String,
        @Path("userId") userId: String
    ):Call<VectoResponse<Unit>>

    @POST("follow/{userId}")//팔로우 등록 S023이면 등록 성공
    fun sendFollow(
        @Header("Authorization") authorization: String,
        @Path("userId") userId: String
    ):Call<VectoResponse<Unit>>

    @DELETE("follow/{userId}")
    fun deleteFollow(
        @Header("Authorization") authorization: String,
        @Path("userId") userId: String
    ):Call<VectoResponse<Unit>>

    @DELETE("feed/{feedId}")
    fun deleteFeed(
        @Header("Authorization") authorization: String,
        @Path("feedId") feedId: Int
    ):Call<VectoResponse<Unit>>

    @PATCH("feed/comment")
    fun updateComment(
        @Header("Authorization") authorization: String,
        @Body commentData: CommentUpdateRequest
    ): Call<VectoResponse<String>>

    @PATCH("feed")
    fun updatePost(
        @Header("Authorization") authorization: String,
        @Body updatePostData: UpdatePostRequest
    ): Call<VectoResponse<String>>

    @GET("feed/feeds/personal")
    suspend fun getPersonalFeedList(
        @Header("Authorization") authorization: String,
        @Query("page") page: Int,
        @Query("isFollowPage") isFollowPage: Boolean
    ): Response<VectoResponse<FeedResponse>>

    @GET("feed/feedList")
    suspend fun getFeedList(
        @Query("page") page: Int
    ): Response<VectoResponse<FeedResponse>>

    @GET("feed/{feedId}")
    suspend fun getFeedInfo2(
        @Path("feedId") feedId: Int
    ): Response<VectoResponse<PostResponse>>



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
        val profileUrl: String?,
        val feedCount: Int,
        val followerCount: Int,
        val followingCount: Int
    )

    data class FeedResponse(
        val nextPage: Int,
        val feedIds: List<Int>,
        val lastPage: Boolean,
        val followPage: Boolean
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

    data class PostDataForUpload(
        val title: String, //제목
        val content: String?, //내용
        val uploadtime: String, //게시 시간
        var image: MutableList<String>?, //이미지
        val location: MutableList<LocationData>, //경로 정보
        val visit: MutableList<VisitDataForWite>, //방문지 정보
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
        val commentId: Int,
        val nickName: String,
        val userId: String,
        var content: String,
        val timeDifference: String,
        val profileUrl: String?,
        var commentCount: Int,
        var likeFlag: Boolean
    )

    data class CommentRequest(
        val feedId: Int,
        val content: String
    )

    data class UserUpdateData(
        val userId: String?,
        val userPw: String?,
        val provider: String,
        val nickName: String?,
    )

    data class CommentUpdateRequest(
        val commentId: Int,
        val content: String
    )

    data class UpdatePostRequest(
        val feedId: Int,
        val title: String, //제목
        val content: String?, //내용
        var image: MutableList<String>?, //이미지
        val location: MutableList<LocationData>, //경로 정보
        val visit: MutableList<VisitDataForWite>, //방문지 정보
        var mapimage: MutableList<String>?
    )
}