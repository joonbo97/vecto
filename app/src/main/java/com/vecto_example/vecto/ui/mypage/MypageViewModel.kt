package com.vecto_example.vecto.ui.mypage

import androidx.lifecycle.ViewModel
import com.vecto_example.vecto.data.Auth
import com.vecto_example.vecto.data.repository.UserRepository

class MypageViewModel(private val repository: UserRepository): ViewModel() {

    fun logout(){
        Auth.setLoginFlag(false)
        Auth.setUserData("", "", "", "", "")
        Auth.accessToken = ""
    }
}