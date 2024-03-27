package com.vecto_example.vecto.ui.search

import android.util.Log
import com.vecto_example.vecto.data.Auth
import com.vecto_example.vecto.retrofit.VectoService

class SearchRepository(private val vectoService: VectoService) {

    suspend fun getFeedList(pageNo: Int): VectoService.FeedResponse {
        val response = vectoService.getFeedList(pageNo)

        if(response.isSuccessful){
            Log.d("get Feed ID", "${response.body()}")

            return response.body()!!.result!!
        }
        else{
            throw Exception("Failed: ${response.errorBody()?.string()}")
        }
    }

    suspend fun getPersonalFeedList(isFollowPage: Boolean, pageNo: Int): VectoService.FeedResponse {
        val response = vectoService.getPersonalFeedList("Bearer ${Auth.token}", pageNo, isFollowPage)

        if(response.isSuccessful){
            Log.d("get Personal Feed ID", "${response.body()}")

            return response.body()!!.result!!
        }
        else{
            throw Exception("Failed: ${response.errorBody()?.string()}")
        }
    }

    suspend fun getFeedInfo(feedId: Int): VectoService.PostResponse {
        val response = vectoService.getFeedInfo2(feedId)

        if(response.isSuccessful){
            return response.body()!!.result!!
        }
        else{
            throw Exception("Failed: ${response.errorBody()?.string()}")
        }
    }
}