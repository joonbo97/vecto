package com.vecto_example.vecto.data.repository

import android.util.Log
import com.google.gson.Gson
import com.vecto_example.vecto.data.Auth
import com.vecto_example.vecto.retrofit.VectoService

class TokenRepository(private val vectoService: VectoService) {
    suspend fun reissueToken(): Result<VectoService.UserToken>{
        return try {
            val response = vectoService.reissueToken("Bearer ${Auth.accessToken}", Auth.refreshToken)

            if(response.isSuccessful){
                Log.d("reissueToken", "SUCCESS: ${response.body()}")
                Result.success(response.body()!!.result!!)
            } else {
                val errorBody = response.errorBody()?.string()
                val gson = Gson()
                val errorResponse: VectoService.VectoResponse<*>? = gson.fromJson(errorBody, VectoService.VectoResponse::class.java)

                Log.d("reissueToken", "FAIL: $errorBody")
                Result.failure(Exception(errorResponse!!.code))
            }
        } catch (e: Exception) {
            Log.e("reissueToken",  "ERROR", e)
            Result.failure(Exception("ERROR"))
        }
    }
}