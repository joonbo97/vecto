package com.vecto_example.vecto.ui.mypage

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vecto_example.vecto.data.Auth
import com.vecto_example.vecto.data.repository.UserRepository
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

class MypageViewModel(private val repository: UserRepository): ViewModel() {

    fun logout(){
        Auth.setLoginFlag(false)
        Auth.setUserData("", "", "", "", "")
        Auth.token = ""
    }
}