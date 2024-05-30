package com.vecto_example.vecto.data.repository

import android.util.Log
import com.google.gson.Gson
import com.vecto_example.vecto.data.Auth
import com.vecto_example.vecto.retrofit.VectoService

class FeedRepository (private val vectoService: VectoService) {
    /*   Feed 관련 API 함수   */
    suspend fun getFeedList(nextFeedId: Int?): Result<VectoService.FeedPageResponse> {
        /*   모든 게시물을 최신 순으로 확인 할 수 있는 함수   */
        return try{
            val response = vectoService.getFeedList(nextFeedId)

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

    suspend fun getPersonalFeedList(isFollowPage: Boolean, nextFeedId: Int?): Result<VectoService.FeedPageResponse> {
        /*   로그인 시 알고리즘에 맞는 게시물을 요청하는 함수   */
        return try {
            val response = vectoService.getPersonalFeedList("Bearer ${Auth.accessToken}", nextFeedId, isFollowPage)

            if(response.isSuccessful){
                Log.d("getPersonalFeedList", "SUCCESS: ${response.body()}")

                Result.success(response.body()!!.result!!)
            } else {
                val errorBody = response.errorBody()?.string()
                val gson = Gson()
                val errorResponse: VectoService.VectoResponse<*>? = gson.fromJson(errorBody, VectoService.VectoResponse::class.java)

                Log.d("getPersonalFeedList", "FAIL: $errorBody")
                Result.failure(Exception(errorResponse?.code))
            }
        } catch (e: Exception) {
            Log.e("getPersonalFeedList", "ERROR", e)
            Result.failure(Exception("ERROR"))
        }
    }

    suspend fun getSearchFeedList(query: String, nextFeedId: Int?): Result<VectoService.FeedPageResponse> {
        /*   검색 시 결과에 맞는 게시물을 확인 할 수 있는 함수   */
        return try {
            val response = vectoService.getSearchFeedList(nextFeedId, query)

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

    suspend fun postSearchFeedList(query: String, nextFeedId: Int?): Result<VectoService.FeedPageResponse> {
        /*   검색 시 결과에 맞는 게시물을 확인 할 수 있는 함수   */
        return try {
            val response = vectoService.postSearchFeedList("Bearer ${Auth.accessToken}", nextFeedId, query)

            if(response.isSuccessful){
                Log.d("postSearchFeedList", "SUCCESS: ${response.body()}")

                Result.success(response.body()!!.result!!)
            }
            else{
                val errorBody = response.errorBody()?.string()
                val gson = Gson()
                val errorResponse: VectoService.VectoResponse<*>? = gson.fromJson(errorBody, VectoService.VectoResponse::class.java)

                Log.d("postSearchFeedList", "FAIL: $errorBody")
                Result.failure(Exception(errorResponse?.code))
            }
        } catch (e: Exception) {
            Log.e("postSearchFeedList", "ERROR", e)
            Result.failure(Exception("ERROR"))
        }
    }

    suspend fun postLikeFeedList(nextFeedId: Int?): Result<VectoService.FeedPageResponse> {
        /*   좋아요 누른 게시물 확인   */
        return try {
            val response = vectoService.postLikeFeedList("Bearer ${Auth.accessToken}", nextFeedId)

            if(response.isSuccessful){
                Log.d("getLikeFeedList", "SUCCESS: ${response.body()}")

                Result.success(response.body()!!.result!!)
            }
            else{
                val errorBody = response.errorBody()?.string()
                val gson = Gson()
                val errorResponse: VectoService.VectoResponse<*>? = gson.fromJson(errorBody, VectoService.VectoResponse::class.java)

                Log.d("postLikeFeedList", "FAIL: $errorBody")
                Result.failure(Exception(errorResponse?.code))
            }
        } catch (e:Exception) {
            Log.e("getLikeFeedList", "ERROR", e)
            Result.failure(Exception("ERROR"))
        }
    }

    suspend fun getUserFeedList(userId: String, nextFeedId: Int?): Result<VectoService.FeedPageResponse> {
        /*   사용자가 작성한 게시물 확인 (비 로그인)   */
        return try {
            val response = vectoService.getUserFeedList(userId, nextFeedId)

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

    suspend fun postUserFeedList(userId: String, nextFeedId: Int?): Result<VectoService.FeedPageResponse> {
        /*   사용자가 작성한 게시물 확인 (로그인)   */
        return try {
            val response = vectoService.postUserFeedList("Bearer ${Auth.accessToken}", userId, nextFeedId)

            if(response.isSuccessful){
                Log.d("postUserFeedList", "SUCCESS: ${response.body()}")

                Result.success(response.body()!!.result!!)
            }
            else{
                val errorBody = response.errorBody()?.string()
                val gson = Gson()
                val errorResponse: VectoService.VectoResponse<*>? = gson.fromJson(errorBody, VectoService.VectoResponse::class.java)

                Log.d("postUserFeedList", "FAIL: $errorBody")
                Result.failure(Exception(errorResponse?.code))
            }
        } catch (e: Exception) {
            Log.e("postUserFeedList", "ERROR", e)
            Result.failure(Exception("ERROR"))
        }
    }

    suspend fun getFeedInfo(feedId: Int): Result<VectoService.FeedInfo> {
        /*   FeedId를 통해 게시물 상세 정보를 확인 할 수 있는 함수   */
        return try {

            val response = if (Auth.loginFlag.value == true) {
                vectoService.getFeedInfo("Bearer ${Auth.accessToken}", feedId)
            } else {
                vectoService.getFeedInfo(feedId)
            }

            if(response.isSuccessful){
                Log.d("getFeedInfo", "SUCCESS: ${response.body()}")

                Result.success(response.body()!!.result!!)
            }
            else{
                val errorBody = response.errorBody()?.string()
                val gson = Gson()
                val errorResponse: VectoService.VectoResponse<*>? = gson.fromJson(errorBody, VectoService.VectoResponse::class.java)

                Log.d("getFeedInfo", "FAIL: $errorBody")
                Result.failure(Exception(errorResponse?.code))
            }
        } catch (e: Exception) {
            Log.e("getFeedInfo", "ERROR", e)
            Result.failure(Exception("ERROR"))
        }
    }

    suspend fun postFeedLike(feedId: Int): Result<String> {
        /*   게시글 좋아요 추가 함수   */
        return try {
            val response = vectoService.postFeedLike("Bearer ${Auth.accessToken}", feedId)

            if(response.isSuccessful){
                Log.d("postFeedLike", "SUCCESS")
                Result.success("SUCCESS")
            } else {
                val errorBody = response.errorBody()?.string()
                val gson = Gson()
                val errorResponse: VectoService.VectoResponse<*>? = gson.fromJson(errorBody, VectoService.VectoResponse::class.java)

                Log.d("postFeedLike", "FAIL: $errorBody")
                Result.failure(Exception(errorResponse?.code))
            }
        } catch (e: Exception) {
            Log.e("postFeedLike", "ERROR", e)
            Result.failure(Exception("ERROR"))
        }
    }

    suspend fun deleteFeedLike(feedId: Int): Result<String> {
        /*   게시글 좋아요 삭제 함수   */
        return try {
            val response = vectoService.deleteFeedLike("Bearer ${Auth.accessToken}", feedId)

            if(response.isSuccessful){
                Log.d("deleteFeedLike", "SUCCESS")
                Result.success("SUCCESS")
            } else {
                val errorBody = response.errorBody()?.string()
                val gson = Gson()
                val errorResponse: VectoService.VectoResponse<*>? = gson.fromJson(errorBody, VectoService.VectoResponse::class.java)

                Log.d("deleteFeedLike", "FAIL: $errorBody")
                Result.failure(Exception(errorResponse?.code))
            }
        } catch (e: Exception) {
            Log.e("deleteFeedLike", "ERROR", e)
            Result.failure(Exception("ERROR"))
        }
    }

    suspend fun deleteFeed(feedId: Int): Result<String> {
        /*   게시글 삭제 함수   */
        return try {
            val response = vectoService.deleteFeed("Bearer ${Auth.accessToken}", feedId)

            if(response.isSuccessful){
                Log.d("deleteFeed", "SUCCESS")
                Result.success("SUCCESS")
            } else {
                val errorBody = response.errorBody()?.string()
                val gson = Gson()
                val errorResponse: VectoService.VectoResponse<*>? = gson.fromJson(errorBody, VectoService.VectoResponse::class.java)

                Log.d("deleteFeed", "FAIL: $errorBody")
                Result.failure(Exception(errorResponse?.code))
            }
        } catch (e: Exception) {
            Log.e("deleteFeed", "ERROR", e)
            Result.failure(Exception("ERROR"))
        }
    }

}