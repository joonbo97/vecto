package com.vecto_example.vecto.ui.comment

import android.util.Log
import com.vecto_example.vecto.data.Auth
import com.vecto_example.vecto.retrofit.VectoService

class CommentRepository(private val vectoService: VectoService) {
    /*   Comment 관련된 API 함수   */
    suspend fun getCommentList(feedId: Int): VectoService.CommentListResponse {
        /*   모든 댓글을 최신 순으로 확인 할 수 있는 함수   */

        val response = vectoService.getCommentList(feedId)

        if(response.isSuccessful){
            Log.d("getCommentList", "${response.body()}")

            return response.body()!!.result!!
        }
        else{
            throw Exception("getCommentList Failed: ${response.errorBody()?.string()}")
        }
    }

    suspend fun getPersonalCommentList(feedId: Int): VectoService.CommentListResponse {
        /*   로그인 시 댓글 요청 함수   */

        val response = vectoService.getCommentList("Bearer ${Auth.token}", feedId)

        if(response.isSuccessful){
            Log.d("getPersonalCommentList", "${response.body()}")

            return response.body()!!.result!!
        }
        else{
            throw Exception("getPersonalCommentList Failed: ${response.errorBody()?.string()}")
        }
    }
}