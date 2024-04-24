package com.vecto_example.vecto.data.repository

import android.util.Log
import com.vecto_example.vecto.data.Auth
import com.vecto_example.vecto.retrofit.VectoService

class FeedRepository (private val vectoService: VectoService) {
    /*   Feed 관련 API 함수   */
    suspend fun getFeedList(pageNo: Int): Result<VectoService.FeedPageResponse> {
        /*   모든 게시물을 최신 순으로 확인 할 수 있는 함수   */
        return try{
            val response = vectoService.getFeedList(pageNo)

            if(response.isSuccessful){
                Log.d("getFeedList", "SUCCESS: ${response.body()}")

                Result.success(response.body()!!.result!!)
            } else {
                Log.d("getFeedList", "FAIL: ${response.errorBody()?.string()}")

                Result.failure(Exception("FAIL"))
            }
        } catch (e: Exception) {
            Log.e("getFeedList", "ERROR", e)
            Result.failure(Exception("ERROR"))
        }
    }

    suspend fun getPersonalFeedList(isFollowPage: Boolean, pageNo: Int): Result<VectoService.FeedPageResponse> {
        /*   로그인 시 알고리즘에 맞는 게시물을 요청하는 함수   */
        return try {
            val response = vectoService.getPersonalFeedList("Bearer ${Auth.token}", pageNo, isFollowPage)

            if(response.isSuccessful){
                Log.d("getPersonalFeedList", "SUCCESS: ${response.body()}")

                Result.success(response.body()!!.result!!)
            } else {
                Log.d("getPersonalFeedList", "FAIL: ${response.errorBody()?.string()}")

                Result.failure(Exception("FAIL"))
            }
        } catch (e: Exception) {
            Log.e("getPersonalFeedList", "ERROR", e)
            Result.failure(Exception("ERROR"))
        }
    }

    suspend fun getSearchFeedList(query: String, pageNo: Int): Result<VectoService.FeedPageResponse> {
        /*   검색 시 결과에 맞는 게시물을 확인 할 수 있는 함수   */
        return try {
            val response = vectoService.getSearchFeedList(pageNo, query)

            if(response.isSuccessful){
                Log.d("getSearchFeedList", "SUCCESS: ${response.body()}")

                Result.success(response.body()!!.result!!)
            }
            else{
                Log.d("getSearchFeedList", "FAIL: ${response.errorBody()?.string()}")

                Result.failure(Exception("FAIL"))
            }
        } catch (e: Exception) {
            Log.e("getSearchFeedList", "ERROR", e)
            Result.failure(Exception("ERROR"))
        }
    }

    suspend fun postSearchFeedList(query: String, pageNo: Int): Result<VectoService.FeedPageResponse> {
        /*   검색 시 결과에 맞는 게시물을 확인 할 수 있는 함수   */
        return try {
            val response = vectoService.postSearchFeedList("Bearer ${Auth.token}", pageNo, query)

            if(response.isSuccessful){
                Log.d("postSearchFeedList", "SUCCESS: ${response.body()}")

                Result.success(response.body()!!.result!!)
            }
            else{
                Log.d("postSearchFeedList", "FAIL: ${response.errorBody()?.string()}")

                Result.failure(Exception("FAIL"))
            }
        } catch (e: Exception) {
            Log.e("postSearchFeedList", "ERROR", e)
            Result.failure(Exception("ERROR"))
        }
    }

    suspend fun getLikeFeedList(pageNo: Int): Result<VectoService.FeedPageResponse> {
        /*   좋아요 누른 게시물 확인   */
        return try {
            val response = vectoService.getLikeFeedList("Bearer ${Auth.token}", pageNo)

            if(response.isSuccessful){
                Log.d("getLikeFeedList", "SUCCESS: ${response.body()}")

                Result.success(response.body()!!.result!!)
            }
            else{
                Log.d("getLikeFeedList", "FAIL: ${response.errorBody()?.string()}")

                Result.failure(Exception("FAIL"))
            }
        } catch (e:Exception) {
            Log.e("getLikeFeedList", "ERROR", e)
            Result.failure(Exception("ERROR"))
        }
    }

    suspend fun getUserFeedList(userId: String, pageNo: Int): Result<VectoService.FeedPageResponse> {
        /*   사용자가 작성한 게시물 확인 (비 로그인)   */
        return try {
            val response = vectoService.getUserFeedList(userId, pageNo)

            if(response.isSuccessful){
                Log.d("getUserFeedList", "SUCCESS: ${response.body()}")

                Result.success(response.body()!!.result!!)
            }
            else{
                Log.d("getUserFeedList", "FAIL: ${response.errorBody()?.string()}")

                Result.failure(Exception("FAIL"))
            }
        } catch (e: Exception) {
            Log.e("getUserFeedList", "ERROR", e)
            Result.failure(Exception("ERROR"))
        }
    }

    suspend fun postUserFeedList(userId: String, pageNo: Int): Result<VectoService.FeedPageResponse> {
        /*   사용자가 작성한 게시물 확인 (로그인)   */
        return try {
            val response = vectoService.postUserFeedList("Bearer ${Auth.token}", userId, pageNo)

            if(response.isSuccessful){
                Log.d("postUserFeedList", "SUCCESS: ${response.body()}")

                Result.success(response.body()!!.result!!)
            }
            else{
                Log.d("postUserFeedList", "FAIL: ${response.errorBody()?.string()}")

                Result.failure(Exception("FAIL"))
            }
        } catch (e: Exception) {
            Log.e("postUserFeedList", "ERROR", e)
            Result.failure(Exception("ERROR"))
        }
    }

    suspend fun getFeedInfo(feedId: Int): VectoService.FeedInfoResponse {
        /*   FeedId를 통해 게시물 상세 정보를 확인 할 수 있는 함수   */

        val response = if (Auth.loginFlag.value == true) {
            vectoService.getFeedInfo("Bearer ${Auth.token}", feedId)
        } else {
            vectoService.getFeedInfo(feedId)
        }

        if(response.isSuccessful){
            return response.body()!!.result!!
        }
        else{
            Log.d("getUserFeedList", "FAIL: ${response.errorBody()}")

            throw Exception("FAIL")
        }
    }

    suspend fun postFeedLike(feedId: Int): Result<String> {
        /*   게시글 좋아요 추가 함수   */
        return try {
            val response = vectoService.postFeedLike("Bearer ${Auth.token}", feedId)

            if(response.isSuccessful){
                Log.d("postFeedLike", "SUCCESS")
                Result.success("SUCCESS")
            } else {
                Log.d("postFeedLike", "FAIL: ${response.errorBody()?.string()}")

                Result.failure(Exception("FAIL"))
            }
        } catch (e: Exception) {
            Log.e("postFeedLike", "ERROR", e)
            Result.failure(Exception("ERROR"))
        }
    }

    suspend fun deleteFeedLike(feedId: Int): Result<String> {
        /*   게시글 좋아요 삭제 함수   */
        return try {
            val response = vectoService.deleteFeedLike("Bearer ${Auth.token}", feedId)

            if(response.isSuccessful){
                Log.d("deleteFeedLike", "SUCCESS")
                Result.success("SUCCESS")
            } else {
                Log.d("deleteFeedLike", "FAIL: ${response.errorBody()?.string()}")

                Result.failure(Exception("FAIL"))
            }
        } catch (e: Exception) {
            Log.e("deleteFeedLike", "ERROR", e)
            Result.failure(Exception("ERROR"))
        }
    }

    suspend fun deleteFeed(feedId: Int): Result<String> {
        /*   게시글 삭제 함수   */
        return try {
            val response = vectoService.deleteFeed("Bearer ${Auth.token}", feedId)

            if(response.isSuccessful){
                Log.d("deleteFeed", "SUCCESS")
                Result.success("SUCCESS")
            } else {
                Log.d("deleteFeed", "FAIL: ${response.errorBody()?.string()}")

                Result.failure(Exception("FAIL"))
            }
        } catch (e: Exception) {
            Log.e("deleteFeed", "ERROR", e)
            Result.failure(Exception("ERROR"))
        }
    }

}