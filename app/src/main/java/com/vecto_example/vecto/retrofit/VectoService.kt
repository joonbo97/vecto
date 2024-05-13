package com.vecto_example.vecto.retrofit

import com.vecto_example.vecto.data.model.LocationData
import com.vecto_example.vecto.data.model.VisitData
import com.vecto_example.vecto.data.model.VisitDataForWrite
import okhttp3.MultipartBody
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

    /*   사용자 계정 관련   */

    //로그인
    @POST("login")
    suspend fun loginUser(
        @Body request: LoginRequest
    ): Response<VectoResponse<String>>

    //가입
    @POST("user")
    suspend fun registerUser(
        @Body request: RegisterRequest
    ): Response<VectoResponse<String>>

    //ID 중복 체크
    @POST("userId/check")
    suspend fun checkIdDuplicate(
        @Body request: IdCheckRequest
    ): Response<VectoResponse<Unit>>

    //인증 메일 발송
    @POST("mail")
    suspend fun sendMail(
        @Body request: MailRequest
    ): Response<VectoResponse<Unit>>

    //사용자 정보 확인
    @GET("user")
    suspend fun getUserInfo(
        @Query("userId") userId: String
    ): Response<VectoResponse<UserInfoResponse>>

    //프로필 이미지 업로드
    @Multipart
    @POST("upload/profile")
    suspend fun uploadImage(
        @Header("Authorization")
        authorization: String,
        @Part image: MultipartBody.Part
    ): Response<VectoResponse<String>>

    //사용자 정보 변경
    @PATCH("user")
    suspend fun updateUserProfile(
        @Header("Authorization") authorization: String,
        @Body userData: UserUpdateData
    ): Response<VectoResponse<String>>

    //사용자 신고
    @POST("complaint")
    suspend fun postComplaint(
        @Header("Authorization") authorization: String,
        @Body request: ComplaintRequest
    ): Response<VectoResponse<Unit>>


    /*   게시글 관련   */

    //게시글 추가
    @POST("feed")
    suspend fun addFeed(
        @Header("Authorization") authorization: String,
        @Body request: FeedDataForUpload
    ): Response<VectoResponse<Int>>

    //이미지 업로드
    @Multipart
    @POST("upload/feed")
    suspend fun uploadImages(
        @Header("Authorization") authorization: String,
        @Part images: List<MultipartBody.Part>
    ): Response<VectoResponse<ImageResponse>>

    //게시글 좋아요
    @POST("feed/{feedId}/likes")
    suspend fun postFeedLike(
        @Header("Authorization") authorization: String,
        @Path("feedId") feedId: Int,
    ): Response<VectoResponse<Unit>>


    //게시글 좋아요 취소
    @DELETE("feed/{feedId}/likes")
    suspend fun deleteFeedLike(
        @Header("Authorization") authorization: String,
        @Path("feedId") feedId: Int,
    ): Response<VectoResponse<Unit>>

    //게시글 삭제
    @DELETE("feed/{feedId}")
    suspend fun deleteFeed(
        @Header("Authorization") authorization: String,
        @Path("feedId") feedId: Int
    ):Response<VectoResponse<Unit>>

    //게시글 수정
    @PATCH("feed")
    suspend fun updateFeed(
        @Header("Authorization") authorization: String,
        @Body updatePostData: UpdateFeedRequest
    ): Response<VectoResponse<String>>

    //게시글 정보 확인 (비 로그인)
    @GET("feed/{feedId}")
    suspend fun getFeedInfo(
        @Path("feedId") feedId: Int
    ): Response<VectoResponse<FeedInfo>>

    //게시글 정보 확인 (로그인)
    @POST("feed/{feedId}")
    suspend fun getFeedInfo(
        @Header("Authorization") authorization: String,
        @Path("feedId") feedId: Int
    ): Response<VectoResponse<FeedInfo>>

    //게시글 목록 조회 (비 로그인)
    @GET("feed/feedList")
    suspend fun getFeedList(
        @Query("page") page: Int
    ): Response<VectoResponse<FeedPageResponse>>

    //게시글 목록 조회 (로그인)
    @GET("feed/feeds/personal")
    suspend fun getPersonalFeedList(
        @Header("Authorization") authorization: String,
        @Query("page") page: Int,
        @Query("isFollowPage") isFollowPage: Boolean
    ): Response<VectoResponse<FeedPageResponse>>

    //게시글 검색 결과 목록 조회 (비 로그인)
    @GET("feed/feeds/search")
    suspend fun getSearchFeedList(
        @Query("page") page: Int,
        @Query("q") q: String
    ): Response<VectoResponse<FeedPageResponse>>

    //게시글 검색 결과 목록 조회 (로그인)
    @POST("feed/feeds/search")
    suspend fun postSearchFeedList(
        @Header("Authorization") authorization: String,
        @Query("page") page: Int,
        @Query("q") q: String
    ): Response<VectoResponse<FeedPageResponse>>

    //좋아요 한 게시글 목록 조회
    @POST("feed/likes")
    suspend fun postLikeFeedList(
        @Header("Authorization") authorization: String,
        @Query("page") page: Int
    ): Response<VectoResponse<FeedPageResponse>>

    //특정 사용자 업로드 게시글 조회 (비 로그인)
    @GET("feed/user")
    suspend fun getUserFeedList(
        @Query("userId") userId: String,
        @Query("page") page: Int
    ): Response<VectoResponse<FeedPageResponse>>

    //특정 사용자 업로드 게시글 조회 (로그인)
    @POST("feed/user")
    suspend fun postUserFeedList(
        @Header("Authorization") authorization: String,
        @Query("userId") userId: String,
        @Query("page") page: Int
    ): Response<VectoResponse<FeedPageResponse>>


    /*   User Interaction 관련   */

    //팔로우 여부 조회
    @POST("follow")
    suspend fun getFollowRelation(
        @Header("Authorization") authorization: String,
        @Body userIds: UserIdList
    ):Response<VectoResponse<FollowResponse>>

    //팔로우 추가
    @POST("follow/{userId}")
    suspend fun postFollow(
        @Header("Authorization") authorization: String,
        @Path("userId") userId: String
    ):Response<VectoResponse<Unit>>

    //팔로우 취소
    @DELETE("follow/{userId}")
    suspend fun deleteFollow(
        @Header("Authorization") authorization: String,
        @Path("userId") userId: String
    ):Response<VectoResponse<Unit>>

    //팔로우 유저 리스트 반환
    @GET("follow/follower")
    suspend fun getFollowerList(
        @Query("userId") userId: String
    ):Response<VectoResponse<FollowListResponse>>

    @GET("follow/followed")
    suspend fun getFollowingList(
        @Query("userId") userId: String
    ):Response<VectoResponse<FollowListResponse>>

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
    suspend fun postCommentLike(
        @Header("Authorization") authorization: String,
        @Path("commentId") commentId: Int
    ): Response<VectoResponse<Unit>>

    //댓글 좋아요 취소
    @DELETE("comment/{commentId}/likes")
    suspend fun deleteCommentLike(
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
        val feeds: List<FeedInfo>,
        val lastPage: Boolean,
        val followPage: Boolean
    )

    data class ImageResponse(
        val url: List<String>
    )

    data class FeedDataForUpload(
        val title: String, //제목
        val content: String?, //내용
        val uploadtime: String, //게시 시간
        var image: MutableList<String>?, //이미지
        val location: MutableList<LocationData>, //경로 정보
        val visit: MutableList<VisitDataForWrite>, //방문지 정보
        var mapimage: MutableList<String>?
    )

    data class FeedInfo(
        val feedId: Int,
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

    data class FeedInfoWithFollow(
        val feedInfo: FeedInfo,
        var isFollowing: Boolean
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

    data class UpdateFeedRequest(
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

    data class UserIdList(
        val userId: List<String>
    )

    data class FollowResponse(
        val followRelations: List<FollowRelation>
    )

    data class FollowRelation(
        val userId: String,
        val relation: String
    )

    data class FollowListResponse(
        val followRelations: List<FollowList>
    )

    data class FollowList(
        val userId: String,
        var relation: String,
        val profileUrl: String,
        val nickName: String
    )

}