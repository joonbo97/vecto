package com.vecto_example.vecto.data.repository

import android.util.Log
import com.google.gson.Gson
import com.vecto_example.vecto.data.Auth
import com.vecto_example.vecto.retrofit.VectoService

class CommentRepository(private val vectoService: VectoService) {
    /*   Comment 관련된 API 함수   */
    suspend fun getCommentList(feedId: Int, nextCommentId: Int?): Result<VectoService.CommentListResponse> {
        /*   모든 댓글을 최신 순으로 확인 할 수 있는 함수   */
        return try{
            val response = vectoService.getCommentList(feedId, nextCommentId)

            if(response.isSuccessful){
                Log.d("getCommentList", "SUCCESS: ${response.body()}")

                Result.success(response.body()!!.result!!)
            }
            else{
                val errorBody = response.errorBody()?.string()
                val gson = Gson()
                val errorResponse: VectoService.VectoResponse<*>? = gson.fromJson(errorBody, VectoService.VectoResponse::class.java)

                Log.d("getCommentList", "FAIL: $errorBody")
                Result.failure(Exception(errorResponse!!.code))
            }
        } catch (e: Exception) {
            Log.e("getCommentList", "ERROR", e)
            Result.failure(e)
        }
    }

    suspend fun getPersonalCommentList(feedId: Int, nextCommentId: Int?): Result<VectoService.CommentListResponse> {
        /*   로그인 시 댓글 요청 함수   */

        return try{
            val response = vectoService.getCommentList("Bearer ${Auth.accessToken}", feedId, nextCommentId)

            if(response.isSuccessful){
                Log.d("getPersonalCommentList", "SUCCESS: ${response.body()}")

                Result.success(response.body()!!.result!!)
            }
            else{
                val errorBody = response.errorBody()?.string()
                val gson = Gson()
                val errorResponse: VectoService.VectoResponse<*>? = gson.fromJson(errorBody, VectoService.VectoResponse::class.java)

                Log.d("getPersonalCommentList", "FAIL: $errorBody")
                Result.failure(Exception(errorResponse!!.code))
            }
        } catch (e: Exception) {
            Log.e("getPersonalCommentList", "ERROR", e)
            Result.failure(e)
        }
    }

    suspend fun addComment(feedId: Int, content: String): Result<String> {
        /*   댓글 추가 함수   */

        return try {
            val response = vectoService.addComment("Bearer ${Auth.accessToken}", VectoService.CommentRequest(feedId, content))

            if(response.isSuccessful){
                Log.d("addComment", "SUCCESS")
                Result.success("SUCCESS")
            } else {
                val errorBody = response.errorBody()?.string()
                val gson = Gson()
                val errorResponse: VectoService.VectoResponse<*>? = gson.fromJson(errorBody, VectoService.VectoResponse::class.java)

                Log.d("addComment", "FAIL: $errorBody")
                Result.failure(Exception(errorResponse!!.code))
            }
        } catch (e: Exception) {
            Log.e("addComment", "ERROR", e)
            Result.failure(Exception("ERROR"))
        }
    }
    suspend fun sendCommentLike(commentId: Int): Result<String> {
        /*   댓글 좋아요 추가 함수   */

        return try {
            val response = vectoService.postCommentLike("Bearer ${Auth.accessToken}", commentId)

            if(response.isSuccessful){
                Log.d("sendCommentLike", "SUCCESS")
                Result.success("SUCCESS")
            } else {
                val errorBody = response.errorBody()?.string()
                val gson = Gson()
                val errorResponse: VectoService.VectoResponse<*>? = gson.fromJson(errorBody, VectoService.VectoResponse::class.java)

                Log.d("sendCommentLike", "FAIL: $errorBody")
                Result.failure(Exception(errorResponse!!.code))
            }

        } catch (e: Exception) {
            Log.e("sendCommentLike", "ERROR", e)
            Result.failure(Exception("ERROR"))
        }
    }

    suspend fun cancelCommentLike(commentId: Int): Result<String> {
        /*   댓글 좋아요 취소 함수   */

        return try {
            val response = vectoService.deleteCommentLike("Bearer ${Auth.accessToken}", commentId)

            if(response.isSuccessful){
                Log.d("cancelCommentLike", "SUCCESS")
                Result.success("SUCCESS")
            } else {
                val errorBody = response.errorBody()?.string()
                val gson = Gson()
                val errorResponse: VectoService.VectoResponse<*>? = gson.fromJson(errorBody, VectoService.VectoResponse::class.java)

                Log.d("cancelCommentLike", "FAIL: $errorBody")
                Result.failure(Exception(errorResponse!!.code))
            }

        } catch (e: Exception) {
            Log.e("cancelCommentLike", "ERROR", e)
            Result.failure(Exception("ERROR"))
        }
    }

    suspend fun updateComment(updateCommentRequest: VectoService.CommentUpdateRequest): Result<String> {
        /*   댓글 수정 함수   */

        return try {
            val response = vectoService.updateComment("Bearer ${Auth.accessToken}", updateCommentRequest)

            if(response.isSuccessful){
                Log.d("updateComment", "SUCCESS")
                Result.success("SUCCESS")
            } else {
                val errorBody = response.errorBody()?.string()
                val gson = Gson()
                val errorResponse: VectoService.VectoResponse<*>? = gson.fromJson(errorBody, VectoService.VectoResponse::class.java)

                Log.d("updateComment", "FAIL: $errorBody")
                Result.failure(Exception(errorResponse!!.code))
            }

        } catch (e: Exception) {
            Log.e("updateComment", "ERROR", e)
            Result.failure(Exception("ERROR"))
        }

    }

    suspend fun deleteComment(commentId: Int): Result<String>  {
        /*   댓글 삭제 함수   */

        return try {
            val response = vectoService.deleteComment("Bearer ${Auth.accessToken}", commentId)

            if(response.isSuccessful){
                Log.d("deleteComment", "SUCCESS")
                Result.success("SUCCESS")
            } else {
                val errorBody = response.errorBody()?.string()
                val gson = Gson()
                val errorResponse: VectoService.VectoResponse<*>? = gson.fromJson(errorBody, VectoService.VectoResponse::class.java)

                Log.d("deleteComment", "FAIL: $errorBody")
                Result.failure(Exception(errorResponse!!.code))
            }

        } catch (e: Exception) {
            Log.e("deleteComment", "ERROR", e)
            Result.failure(Exception("ERROR"))
        }
    }

}