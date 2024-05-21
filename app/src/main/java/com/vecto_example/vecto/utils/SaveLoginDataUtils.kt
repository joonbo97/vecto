package com.vecto_example.vecto.utils

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import com.vecto_example.vecto.data.Auth
import com.vecto_example.vecto.retrofit.VectoService

class SaveLoginDataUtils {
    companion object {
        fun saveLoginInformation(context: Context, userId: String, provider: String, fcmToken: String) {
            val sharedPreferences = context.getSharedPreferences("MyAppPreferences", Context.MODE_PRIVATE)

            val editor = sharedPreferences.edit()
            // 기존 정보 삭제
            editor.remove("userId")
            editor.remove("accessToken")
            editor.remove("refreshToken")
            editor.remove("provider")
            editor.apply()

            // 새로운 정보 저장
            editor.putString("userId", userId)
            editor.putString("accessToken", Auth.accessToken)
            editor.putString("refreshToken", Auth.refreshToken)
            editor.putString("provider", provider)
            if (!sharedPreferences.contains("FCM")) {
                editor.putString("FCM", fcmToken)
            }
            editor.apply()
        }

        fun deleteData(context: Context) {
            val sharedPreferences = context.getSharedPreferences("MyAppPreferences", Context.MODE_PRIVATE)

            val editor = sharedPreferences.edit()
            // 기존 정보 삭제
            editor.remove("userId")
            editor.remove("accessToken")
            editor.remove("refreshToken")
            editor.remove("provider")
            editor.apply()

            Auth.loginFlag.value = false
        }

        @SuppressLint("ApplySharedPref")
        fun changeToken(context: Context, accessToken: String?, refreshToken: String?) {
            Log.d("changeToken", "${accessToken}, ${refreshToken}")

            if(accessToken == null || refreshToken == null)
                return

            Auth.setToken(VectoService.UserToken(accessToken, refreshToken))

            val sharedPreferences = context.getSharedPreferences("MyAppPreferences", Context.MODE_PRIVATE)

            val editor = sharedPreferences.edit()

            // 기존 정보 삭제
            editor.apply {
                remove("accessToken")
                remove("refreshToken")
                putString("accessToken", accessToken)
                putString("refreshToken", refreshToken)
            }.commit()

            editor.remove("accessToken")
            editor.remove("refreshToken")

            // 새로운 정보 저장
            editor.putString("accessToken", Auth.accessToken)
            editor.putString("refreshToken", Auth.refreshToken)

            editor.apply()
        }
    }
}