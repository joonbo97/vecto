package com.vecto_example.vecto.retrofit

import com.vecto_example.vecto.data.model.LocationData
import com.vecto_example.vecto.data.model.VisitData
import com.vecto_example.vecto.data.model.VisitDataForWrite
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
    suspend fun loginUser(
        @Body request: LoginRequest
    ): Response<VectoResponse<String>>

    @POST("user")
    suspend fun registerUser(
        @Body request: RegisterRequest
    ): Response<VectoResponse<String>>

    @POST("userId/check")
    fun idCheck(
        @Body request: IdCheckRequest
    ): Call<VectoResponse<Unit>>

    @POST("userId/check")
    suspend fun idCheck2(
        @Body request: IdCheckRequest
    ): Response<VectoResponse<Unit>>

    @GET("user")
    suspend fun getUserInfo(
        @Query("userId") userId: String
    ): Response<VectoResponse<UserInfoResponse>>

    @POST("feed")
    suspend fun addFeed(
        @Header("Authorization") authorization: String,
        @Body request: PostDataForUpload
    ): Response<VectoResponse<Int>>

    @Multipart
    @POST("upload/feed")
    suspend fun uploadImages(
        @Header("Authorization") authorization: String,
        @Part images: List<MultipartBody.Part>
    ): Response<VectoResponse<ImageResponse>>

    @POST("mail")
    fun sendMail(
        @Body request: MailRequest
    ): Call<VectoResponse<Unit>>

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

    @GET("follow/{userId}")
    fun getFollow(
        @Header("Authorization") authorization: String,
        @Path("userId") userId: String
    ):Call<VectoResponse<Unit>>

    @GET("follow/{userId}")
    suspend fun getFollow2(
        @Header("Authorization") authorization: String,
        @Path("userId") userId: String
    ):Response<VectoResponse<Unit>>

    @POST("follow/{userId}")
    fun sendFollow(
        @Header("Authorization") authorization: String,
        @Path("userId") userId: String
    ):Call<VectoResponse<Unit>>

    @POST("follow/{userId}")
    suspend fun postFollow2(
        @Header("Authorization") authorization: String,
        @Path("userId") userId: String
    ):Response<VectoResponse<Unit>>

    @DELETE("follow/{userId}")
    fun deleteFollow(
        @Header("Authorization") authorization: String,
        @Path("userId") userId: String
    ):Call<VectoResponse<Unit>>

    @DELETE("follow/{userId}")
    suspend fun deleteFollow2(
        @Header("Authorization") authorization: String,
        @Path("userId") userId: String
    ):Response<VectoResponse<Unit>>

    @DELETE("feed/{feedId}")
    fun deleteFeed(
        @Header("Authorization") authorization: String,
        @Path("feedId") feedId: Int
    ):Call<VectoResponse<Unit>>

    @PATCH("feed")
    suspend fun updateFeed(
        @Header("Authorization") authorization: String,
        @Body updatePostData: UpdatePostRequest
    ): Response<VectoResponse<String>>

    @POST("complaint")
    suspend fun postComplaint(
        @Header("Authorization") authorization: String,
        @Body request: ComplaintRequest
    ): Response<VectoResponse<Unit>>

    @GET("feed/feeds/personal")
    suspend fun getPersonalFeedList(
        @Header("Authorization") authorization: String,
        @Query("page") page: Int,
        @Query("isFollowPage") isFollowPage: Boolean
    ): Response<VectoResponse<FeedPageResponse>>

    @GET("feed/feedList")
    suspend fun getFeedList(
        @Query("page") page: Int
    ): Response<VectoResponse<FeedPageResponse>>

    @GET("feed/feeds/search")
    suspend fun getSearchFeedList(
        @Query("page") page: Int,
        @Query("q") q: String
    ): Response<VectoResponse<FeedPageResponse>>

    @GET("feed/likes")
    suspend fun getLikeFeedList(
        @Query("userId") userId: String,
        @Query("page") page: Int
    ): Response<VectoResponse<FeedPageResponse>>

    @GET("feed")
    suspend fun getUserFeedList(
        @Query("userId") userId: String,
        @Query("page") page: Int
    ): Response<VectoResponse<FeedPageResponse>>

    @GET("feed/{feedId}")
    suspend fun getFeedInfo(
        @Path("feedId") feedId: Int
    ): Response<VectoResponse<FeedInfoResponse>>

    @POST("feed/{feedId}")
    suspend fun getFeedInfo(
        @Header("Authorization") authorization: String,
        @Path("feedId") feedId: Int
    ): Response<VectoResponse<FeedInfoResponse>>

    @Multipart
    @POST("upload/profile") //프로필 이미지 업로드
    suspend fun uploadImage(
        @Header("Authorization")
        authorization: String,
        @Part image: MultipartBody.Part
    ): Response<VectoResponse<String>>

    @PATCH("user")
    suspend fun updateUserProfile(
        @Header("Authorization") authorization: String,
        @Body userData: UserUpdateData
    ): Response<VectoResponse<String>>

    /*   Comment 관련   */

    //비 로그인 시 댓글 목록
    @GET("feed/{feedId}/comments")
    suspend fun getCommentList(
        @Path("feedId") feedId: Int,
        @Query("page") page: Int
    ): Response<VectoResponse<CommentListResponse>>

    //로그인 시 댓글 목록
    @POST("feed/{feedId}/comments")
    suspend fun getCommentList(
        @Header("Authorization") authorization: String,
        @Path("feedId") feedId: Int,
        @Query("page") page: Int
    ): Response<VectoResponse<CommentListResponse>>

    //댓글 추가
    @POST("feed/comment")
    suspend fun addComment(
        @Header("Authorization") authorization: String,
        @Body request: CommentRequest
    ): Response<VectoResponse<String>>

    //댓글 좋아요
    @POST("comment/{commentId}/likes")
    suspend fun sendCommentLike(
        @Header("Authorization") authorization: String,
        @Path("commentId") commentId: Int
    ): Response<VectoResponse<Unit>>

    //댓글 좋아요 취소
    @DELETE("comment/{commentId}/likes")
    suspend fun cancelCommentLike(
        @Header("Authorization") authorization: String,
        @Path("commentId") commentId: Int
    ): Response<VectoResponse<Unit>>

    //댓글 수정
    @PATCH("feed/comment")
    suspend fun updateComment(
        @Header("Authorization") authorization: String,
        @Body commentData: CommentUpdateRequest
    ): Response<VectoResponse<String>>

    //댓글 삭제
    @DELETE("feed/comment")
    suspend fun deleteComment(
        @Header("Authorization") authorization: String,
        @Query("commentId") commentId: Int
    ): Response<VectoResponse<Unit>>

    /*   Notification 관련   */
    @GET("push/new")
    suspend fun getNewNotificationFlag(
        @Header("Authorization") authorization: String
    ): Response<VectoResponse<Boolean>>

    @GET("push")
    suspend fun getNotification(
        @Header("Authorization") authorization: String,
        @Query("page") page: Int
    ): Response<VectoResponse<NotificationResponse>>




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

    data class FeedPageResponse(
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
        val visit: MutableList<VisitDataForWrite>, //방문지 정보
        var mapimage: MutableList<String>?
    )

    data class FeedInfoResponse(
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
        val nextPage: Int,
        val comments: List<CommentResponse>,
        val lastPage: Boolean
    )

    data class CommentResponse(
        val commentId: Int,
        val updatedBefore: Boolean,
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
        val visit: MutableList<VisitDataForWrite>, //방문지 정보
        var mapimage: MutableList<String>?
    )

    data class ComplaintRequest(
        val complaintType: String,
        val toUserId: String,
        val content: String?
    )

    data class NotificationResponse(
        val nextPage: Int,
        val notifications: List<Notification>,
        val lastPage: Boolean
    )

    data class Notification(
        val notificationType: String,
        var requestedBefore: Boolean,
        val feedId: Int,
        val fromUserId: String,
        val content: String,
        val timeDifference: String
    )

}