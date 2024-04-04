package com.vecto_example.vecto.data.repository

import android.util.Log
import com.vecto_example.vecto.data.Auth
import com.vecto_example.vecto.retrofit.VectoService

class FeedRepository (private val vectoService: VectoService) {
    /*   Feed와 관련된 API 함수   */
    suspend fun getFeedList(pageNo: Int): VectoService.FeedPageResponse {
        /*   모든 게시물을 최신 순으로 확인 할 수 있는 알고리즘   */

        val response = vectoService.getFeedList(pageNo)

        if(response.isSuccessful){
            Log.d("get Feed ID", "${response.body()}")

            return response.body()!!.result!!
        }
        else{
            throw Exception("Failed: ${response.errorBody()?.string()}")
        }
    }

    suspend fun getPersonalFeedList(isFollowPage: Boolean, pageNo: Int): VectoService.FeedPageResponse {
        /*   로그인 시 알고리즘에 맞는 게시물을 요청하는 함수   */

        val response = vectoService.getPersonalFeedList("Bearer ${Auth.token}", pageNo, isFollowPage)

        if(response.isSuccessful){
            Log.d("get Personal Feed ID", "${response.body()}")

            return response.body()!!.result!!
        }
        else{
            throw Exception("Failed: ${response.errorBody()?.string()}")
        }
    }

    suspend fun getSearchFeedList(query: String, pageNo: Int): VectoService.FeedPageResponse {
        /*   검색 시 결과에 맞는 게시물을 확인 할 수 있는 함수   */

        val response = vectoService.getSearchFeedList(pageNo, query)

        if(response.isSuccessful){
            Log.d("get Search Feed ID", "${response.body()}")

            return response.body()!!.result!!
        }
        else{
            throw Exception("Failed: ${response.errorBody()?.string()}")
        }
    }

    suspend fun getLikeFeedList(pageNo: Int): VectoService.FeedPageResponse {
        /*   좋아요 누른 게시물 확인   */
        val response = vectoService.getLikeFeedList(Auth._userId.value.toString(), pageNo)

        if(response.isSuccessful){
            Log.d("get LikeFeed ID", "${response.body()}")

            return response.body()!!.result!!
        }
        else{
            throw Exception("Failed: ${response.errorBody()?.string()}")
        }
    }

    suspend fun getUserFeedList(userId: String, pageNo: Int): VectoService.FeedPageResponse {
        /*   사용자가 작성한 게시물 확인   */
        val response = vectoService.getUserFeedList(userId, pageNo)

        if(response.isSuccessful){
            Log.d("get UserFeed ID", "${response.body()}")

            return response.body()!!.result!!
        }
        else{
            throw Exception("Failed: ${response.errorBody()?.string()}")
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
            throw Exception("Failed: ${response.errorBody()?.string()}")
        }
    }
}